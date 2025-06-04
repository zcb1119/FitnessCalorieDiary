package com.example.fitnesscaloriediary;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText etDate, etDuration;
    private RadioGroup rgType;
    private TextView tvResult;
    private Button btnCalculate, btnSave;
    private ListView lvHistory;

    private DatabaseHelper dbHelper;

    // 卡路里消耗系数 (每分钟)
    private final Map<Integer, Double> calorieRates = new HashMap<Integer, Double>() {{
        put(R.id.rbIndoorAerobic, 8.0);   // 室内有氧
        put(R.id.rbIndoorAnaerobic, 5.0); // 室内无氧
        put(R.id.rbOutdoorAerobic, 10.0);  // 室外有氧
        put(R.id.rbOutdoorAnaerobic, 6.0); // 室外无氧
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化视图
        etDate = findViewById(R.id.etDate);
        rgType = findViewById(R.id.rgType);
        etDuration = findViewById(R.id.etDuration);
        tvResult = findViewById(R.id.tvResult);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnSave = findViewById(R.id.btnSave);
        lvHistory = findViewById(R.id.lvHistory);

        // 设置默认日期为今天
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etDate.setText(today);

        // 初始化数据库
        dbHelper = new DatabaseHelper(this);

        // 计算按钮点击事件
        btnCalculate.setOnClickListener(v -> calculateCalories());

        // 保存按钮点击事件
        btnSave.setOnClickListener(v -> saveRecord());

        // 加载历史记录
        loadHistory();
    }

    private void calculateCalories() {
        // 获取输入值
        String durationStr = etDuration.getText().toString();
        int selectedId = rgType.getCheckedRadioButtonId();

        // 验证输入
        if (selectedId == -1) {
            Toast.makeText(this, "请选择训练类型", Toast.LENGTH_SHORT).show();
            return;
        }
        if (durationStr.isEmpty()) {
            Toast.makeText(this, "请输入训练时长", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 计算卡路里
            int duration = Integer.parseInt(durationStr);
            double rate = calorieRates.get(selectedId);
            int calories = (int) (duration * rate);

            // 显示结果
            String type = getSelectedType();
            String result = String.format(
                    "训练计划:\n类型: %s\n时长: %d分钟\n消耗: %d卡路里",
                    type, duration, calories
            );
            tvResult.setText(result);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的训练时长", Toast.LENGTH_SHORT).show();
        }
    }

    private String getSelectedType() {
        int selectedId = rgType.getCheckedRadioButtonId();
        if (selectedId == R.id.rbIndoorAerobic) return "室内有氧";
        if (selectedId == R.id.rbIndoorAnaerobic) return "室内无氧";
        if (selectedId == R.id.rbOutdoorAerobic) return "室外有氧";
        if (selectedId == R.id.rbOutdoorAnaerobic) return "室外无氧";
        return "";
    }

    private void saveRecord() {
        // 获取输入值
        String date = etDate.getText().toString();
        String durationStr = etDuration.getText().toString();
        String result = tvResult.getText().toString();
        String type = getSelectedType();

        // 验证输入
        if (date.isEmpty()) {
            Toast.makeText(this, "请输入日期", Toast.LENGTH_SHORT).show();
            return;
        }
        if (type.isEmpty()) {
            Toast.makeText(this, "请选择训练类型", Toast.LENGTH_SHORT).show();
            return;
        }
        if (durationStr.isEmpty()) {
            Toast.makeText(this, "请输入训练时长", Toast.LENGTH_SHORT).show();
            return;
        }
        if (result.isEmpty()) {
            Toast.makeText(this, "请先计算卡路里", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 获取计算值
            int duration = Integer.parseInt(durationStr);
            String[] lines = result.split("\n");
            int calories = Integer.parseInt(lines[2].split(":")[1].trim().replace("卡路里", ""));

            // 保存到数据库
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_DATE, date);
            values.put(DatabaseHelper.COLUMN_TYPE, type);
            values.put(DatabaseHelper.COLUMN_DURATION, duration);
            values.put(DatabaseHelper.COLUMN_CALORIES, calories);

            db.insert(DatabaseHelper.TABLE_NAME, null, values);
            Toast.makeText(this, "记录已保存", Toast.LENGTH_SHORT).show();

            // 清空输入并刷新历史记录
            etDuration.setText("");
            rgType.clearCheck();
            tvResult.setText("");
            loadHistory();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadHistory() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] columns = {
                DatabaseHelper.COLUMN_DATE,
                DatabaseHelper.COLUMN_TYPE,
                DatabaseHelper.COLUMN_DURATION,
                DatabaseHelper.COLUMN_CALORIES
        };

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_NAME,
                columns,
                null, null, null, null,
                DatabaseHelper.COLUMN_DATE + " DESC",
                "10"  // 仅显示最近10条记录
        );

        List<WorkoutRecord> records = new ArrayList<>();
        while (cursor.moveToNext()) {
            String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE));
            String type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE));
            int duration = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DURATION));
            int calories = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CALORIES));

            records.add(new WorkoutRecord(date, type, duration, calories));
        }
        cursor.close();

        // 创建适配器
        ArrayAdapter<WorkoutRecord> adapter = new ArrayAdapter<WorkoutRecord>(
                this,
                R.layout.list_item_history,
                R.id.tvRecord,
                records
        ) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tvRecord = view.findViewById(R.id.tvRecord);

                WorkoutRecord record = getItem(position);
                String recordText = String.format(
                        "%s: %s - %d分钟 - %d卡路里",
                        record.getDate(), record.getType(), record.getDuration(), record.getCalories()
                );

                tvRecord.setText(recordText);
                return view;
            }
        };

        lvHistory.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}