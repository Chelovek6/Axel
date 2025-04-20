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
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_RECORDS = "records";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_IS_FFT = "is_fft";
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

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_RECORDS
                    + " ADD COLUMN " + COLUMN_IS_FFT + " INTEGER DEFAULT 0");
        }
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
