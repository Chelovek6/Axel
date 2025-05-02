package com.example.axel;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "records.db";
    private static final int DATABASE_VERSION = 3;
    //CSV
    public static final String TABLE_RECORDS = "records";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_IS_FFT = "is_fft";

    //Расписание
    public static final String TABLE_SCHEDULES = "schedules";
    public static final String COLUMN_SCHEDULE_ID = "schedule_id";
    public static final String COLUMN_START_TIME = "start_time"; // Время в миллисекундах
    public static final String COLUMN_DAYS_OF_WEEK = "days"; // Например: "1,3,5" для Пн, Ср, Пт
    public static final String COLUMN_DURATION = "duration"; // Длительность в минутах
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_TYPE = "type"; // "main" или "fft"
    public static final String COLUMN_IS_ACTIVE = "is_active"; // 1 или 0


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }




    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_RECORDS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_PATH + " TEXT,"
                + COLUMN_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + COLUMN_IS_FFT + " INTEGER DEFAULT 0)";
        db.execSQL(CREATE_TABLE);

        String CREATE_SCHEDULES_TABLE = "CREATE TABLE " + TABLE_SCHEDULES + "("
                + COLUMN_SCHEDULE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_START_TIME + " INTEGER NOT NULL,"
                + COLUMN_DAYS_OF_WEEK + " TEXT NOT NULL,"
                + COLUMN_DURATION + " INTEGER DEFAULT 1,"
                + COLUMN_DESCRIPTION + " TEXT,"
                + COLUMN_TYPE + " TEXT NOT NULL,"
                + COLUMN_IS_ACTIVE + " INTEGER DEFAULT 1,"
                + "CHECK (" + COLUMN_DURATION + " >= 1),"
                + "CHECK (" + COLUMN_TYPE + " IN ('main', 'fft')),"
                + "UNIQUE (" + COLUMN_START_TIME + ", " + COLUMN_TYPE + ")"
                + ")";
        db.execSQL(CREATE_SCHEDULES_TABLE);

    }
    public List<String> getRecordsPaginated(int page, int pageSize) {
        List<String> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_RECORDS +
                " ORDER BY " + COLUMN_DATE + " DESC" +
                " LIMIT " + pageSize + " OFFSET " + (page * pageSize);
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                records.add(cursor.getString(1) + "\n" + cursor.getString(3));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return records;
    }

    public List<Schedule> getAllSchedules() {
        List<Schedule> schedules = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_SCHEDULES;

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                Schedule schedule = new Schedule(
                        cursor.getInt(0),
                        cursor.getLong(1),
                        cursor.getString(2),
                        cursor.getInt(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getInt(6) == 1
                );
                cursor.close(); // Добавить закрытие курсора
                db.close();     // Закрыть базу
                schedules.add(schedule);
            }
        }
        return schedules;
    }

    public List<Schedule> getAllSchedulesByType(String type) {
        List<Schedule> schedules = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_SCHEDULES + " WHERE " + COLUMN_TYPE + " = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{type})) {
            while (cursor.moveToNext()) {
                Schedule schedule = new Schedule(
                        cursor.getInt(0),
                        cursor.getLong(1),
                        cursor.getString(2),
                        cursor.getInt(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getInt(6) == 1
                );
                schedules.add(schedule);
            }
        }
        return schedules;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULES);

        // Создаём заново
        onCreate(db);
    }
    public void deleteAllSchedules() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SCHEDULES, null, null);
        db.close();
    }

    public void deleteRecord(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RECORDS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
    public void addRecord(String name, String path, boolean isFFT) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_PATH, path);
        values.put(COLUMN_IS_FFT, isFFT ? 1 : 0); // Конвертация boolean в INTEGER
        db.insert(TABLE_RECORDS, null, values);
        db.close();
    }

    public long addSchedule(Schedule schedule) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Проверка уникальности времени и дней
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_SCHEDULES +
                        " WHERE " + COLUMN_START_TIME + " = ? AND " +
                        COLUMN_DAYS_OF_WEEK + " = ? AND " +
                        COLUMN_TYPE + " = ?",
                new String[]{
                        String.valueOf(schedule.getStartTime()),
                        schedule.getDaysOfWeek(),
                        schedule.getType()
                });

        if (cursor.getCount() > 0) {
            cursor.close();
            return -1;
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_START_TIME, schedule.getStartTime());
        values.put(COLUMN_DAYS_OF_WEEK, schedule.getDaysOfWeek());
        values.put(COLUMN_DURATION, schedule.getDuration());
        values.put(COLUMN_DESCRIPTION, schedule.getDescription());
        values.put(COLUMN_TYPE, schedule.getType());
        values.put(COLUMN_IS_ACTIVE, schedule.isActive() ? 1 : 0);

        long result = db.insert(TABLE_SCHEDULES, null, values);
        db.close();
        return result;
    }


    public List<Schedule> getActiveSchedules(String type) {
        List<Schedule> schedules = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_SCHEDULES +
                " WHERE " + COLUMN_TYPE + " = ? AND " +
                COLUMN_IS_ACTIVE + " = 1";

        try (Cursor cursor = db.rawQuery(query, new String[]{type})) {
            while (cursor.moveToNext()) {
                Schedule schedule = new Schedule(
                        cursor.getInt(0),
                        cursor.getLong(1),
                        cursor.getString(2),
                        cursor.getInt(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getInt(6) == 1
                );
                schedules.add(schedule);
            }
        }
        return schedules;
    }
    public int deleteSchedule(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_SCHEDULES,
                COLUMN_SCHEDULE_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    public void updateSchedule(Schedule schedule) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_START_TIME, schedule.getStartTime());
        values.put(COLUMN_DAYS_OF_WEEK, schedule.getDaysOfWeek());
        values.put(COLUMN_DURATION, schedule.getDuration());
        values.put(COLUMN_DESCRIPTION, schedule.getDescription());
        values.put(COLUMN_IS_ACTIVE, schedule.isActive() ? 1 : 0);

        db.update(TABLE_SCHEDULES,
                values,
                COLUMN_SCHEDULE_ID + " = ?",
                new String[]{String.valueOf(schedule.getId())});
        db.close();
    }

    public List<String> getAllRecords() {
        List<String> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RECORDS +
                " ORDER BY " + COLUMN_DATE + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                records.add(cursor.getString(1) + "\n" + cursor.getString(3));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return records;
    }

}
