package com.example.axel;

import android.app.AlarmManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TimePicker;
import android.app.AlertDialog;
import java.util.Calendar;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.List;

public class ScheduleActivity extends AppCompatActivity {
    private ScheduleManager scheduleManager;
    private ListView listView;
    private Button btnAdd;
    private DatabaseHelper dbHelper;
    private Button btnClearDB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkExactAlarmPermission();
        dbHelper = new DatabaseHelper(this);
        setContentView(R.layout.activity_schedule_main);
        btnClearDB = findViewById(R.id.btn_clear_db);
        btnClearDB.setOnClickListener(v -> {
            new DatabaseHelper(this).deleteAllSchedules();
            new ScheduleManager(this).cancelAllAlarms();
            Toast.makeText(this, "All schedules cleared!", Toast.LENGTH_SHORT).show();
            loadSchedules();
        });
        scheduleManager = new ScheduleManager(this);
        listView = findViewById(R.id.list_schedules);
        btnAdd = findViewById(R.id.btn_add_schedule);

        loadSchedules();
        setupListeners();
    }
    @Override
    protected void onResume() {
        super.onResume();
        //scheduleManager.cancelAllAlarms(); Эта строчка убила 5 часов моей жизни
        new ScheduleManager(this).scheduleAllRecordings();
    }

    private void loadSchedules() {
        String type = getIntent().getStringExtra("type");
        List<Schedule> schedules = dbHelper.getActiveSchedules(type);
        ScheduleAdapter adapter = new ScheduleAdapter(this, schedules);
        listView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnAdd.setOnClickListener(v -> showScheduleEditor(null));

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Schedule schedule = (Schedule) parent.getItemAtPosition(position);
            showScheduleEditor(schedule);
        });
    }



    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    private void showScheduleEditor(Schedule schedule) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.activity_schedule, null);
        builder.setView(dialogView);

        // Инициализация элементов
        TimePicker timePicker = dialogView.findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        EditText etDescription = dialogView.findViewById(R.id.et_description);
        EditText etDuration = dialogView.findViewById(R.id.et_duration);
        CheckBox[] daysCheckboxes = new CheckBox[7];
        int[] dayIds = {R.id.mon, R.id.tue, R.id.wed, R.id.thu, R.id.fri, R.id.sat, R.id.sun};
        for (int i = 0; i < dayIds.length; i++) {
            daysCheckboxes[i] = dialogView.findViewById(dayIds[i]);
        }

        // Заполнение данных
        if (schedule != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(schedule.getStartTime());
            timePicker.setHour(cal.get(Calendar.HOUR_OF_DAY));
            timePicker.setMinute(cal.get(Calendar.MINUTE));
            etDescription.setText(schedule.getDescription());
            etDuration.setText(String.valueOf(schedule.getDuration()));

            String[] activeDays = schedule.getDaysOfWeek().split(",");
            for (String day : activeDays) {
                int dayIndex = Integer.parseInt(day.trim()) - 1;
                if (dayIndex >= 0 && dayIndex < 7) {
                    daysCheckboxes[dayIndex].setChecked(true);
                }
            }
        }

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            // Получение данных из формы

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            cal.set(Calendar.MINUTE, timePicker.getMinute());
            long startTime = cal.getTimeInMillis();
            Log.d("ScheduleDebug", "Setting time: " + startTime);
            StringBuilder daysBuilder = new StringBuilder();
            for (int i = 0; i < daysCheckboxes.length; i++) {
                if (daysCheckboxes[i].isChecked()) {
                    daysBuilder.append(i + 1).append(",");
                }
            }
            String days = daysBuilder.length() > 0 ?
                    daysBuilder.substring(0, daysBuilder.length() - 1) : "";

            Schedule newSchedule = new Schedule(
                    0,
                    startTime,
                    days,
                    Integer.parseInt(etDuration.getText().toString()),
                    etDescription.getText().toString(),
                    getIntent().getStringExtra("type"),
                    true

            );


            // Сохранение в БД
            new Thread(() -> {
                if (schedule != null) newSchedule.setId(schedule.getId());
                long result = dbHelper.addSchedule(newSchedule);
                if (result != -1) {
                    new ScheduleManager(ScheduleActivity.this).scheduleAllRecordings();
                }
                runOnUiThread(this::loadSchedules);
            }).start();
        });

        builder.setNegativeButton("Отмена", null);
        builder.create().show();
    }
}
