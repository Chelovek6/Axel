package com.example.axel.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import com.example.axel.utils.DatabaseHelper;
import com.example.axel.R;
import com.example.axel.models.Schedule;
import com.example.axel.managers.ScheduleManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleAdapter extends ArrayAdapter<Schedule> {
    public ScheduleAdapter(Context context, List<Schedule> schedules) {
        super(context, R.layout.item_schedule, schedules);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Schedule schedule = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_schedule, parent, false);
        }

        TextView tvTime = convertView.findViewById(R.id.tv_time);
        TextView tvDays = convertView.findViewById(R.id.tv_days);
        TextView tvDescription = convertView.findViewById(R.id.tv_description);
        TextView tvDuration = convertView.findViewById(R.id.tv_duration);
        Switch switchActive = convertView.findViewById(R.id.switch_active);
        ImageButton btnDelete = convertView.findViewById(R.id.btn_delete);

        // Форматирование времени
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvTime.setText(sdf.format(new Date(schedule.getStartTime())));

        // Дни недели
        tvDays.setText(formatDays(schedule.getDaysOfWeek()));

        // Описание и длительность
        tvDescription.setText(schedule.getDescription());
        tvDuration.setText("Длительность: " + schedule.getDuration() + " мин");

        // Переключатель
        switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DatabaseHelper dbHelper = new DatabaseHelper(getContext());
            schedule.setActive(isChecked);
            dbHelper.updateSchedule(schedule);

            ScheduleManager scheduleManager = new ScheduleManager(getContext());
            scheduleManager.cancelAlarmsForSchedule(schedule);
            if (isChecked) {
                scheduleManager.scheduleRecording(schedule);
            }

            notifyDataSetChanged();
        });

        // Кнопка удаления
        btnDelete.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Удаление")
                    .setMessage("Удалить это расписание?")
                    .setPositiveButton("Да", (dialog, which) -> {
                        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
                        dbHelper.deleteSchedule(schedule.getId());
                        remove(schedule);

                        notifyDataSetChanged();
                        new ScheduleManager(getContext()).cancelAlarm(schedule.getId());
                        Toast.makeText(getContext(), "Расписание удалено", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Нет", null)
                    .show();
        });

        return convertView;
    }

    private String formatDays(String days) {
        if (days == null || days.isEmpty()) return "Дни не выбраны";

        String[] dayNumbers = days.split(",");
        String[] dayNames = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        StringBuilder result = new StringBuilder();

        for (String num : dayNumbers) {
            try {
                int day = Integer.parseInt(num.trim());
                if (day >= 1 && day <= 7) {
                    result.append(dayNames[day-1]).append(", ");
                }
            } catch (NumberFormatException e) {
                Log.e("ScheduleAdapter", "Invalid day format: " + num);
            }
        }

        return result.length() > 0 ?
                result.substring(0, result.length()-2) :
                "Дни не выбраны";
    }
}