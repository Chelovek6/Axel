package com.example.axel;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.List;

public class SavedFilesActivity extends AppCompatActivity {
    private ListView listView;
    private DatabaseHelper dbHelper;
    private List<String> records;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 15;
    private ArrayAdapter<String> adapter;
    private Button btnPrev, btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_files);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        dbHelper = new DatabaseHelper(this);
        listView = findViewById(R.id.listView);

        //Страницы
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnPrev.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                updatePage();
            }
        });
        btnNext.setOnClickListener(v -> {
            currentPage++;
            updatePage();
        });
        //Страницы
        // Загрузка данных
        records = dbHelper.getAllRecords();

        // Настройка адаптера
        adapter = new ArrayAdapter<String>(this, R.layout.list_item_record, R.id.tv_record, records) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                ImageButton btnMenu = view.findViewById(R.id.btn_menu);
                btnMenu.setOnClickListener(v -> showPopupMenu(v, position));

                return view;
            }
        };

        listView.setAdapter(adapter);
    }
    private void updatePage() {
        records = dbHelper.getRecordsPaginated(currentPage, PAGE_SIZE);
        adapter.clear();
        adapter.addAll(records);
    }
    private void showPopupMenu(View anchor, int position) {
        PopupMenu popup = new PopupMenu(this, anchor);

        // Программное создание меню
        popup.getMenu().add(0, 1, 0, "Отправить");
        popup.getMenu().add(0, 2, 0, "Удалить");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    shareFile(position);
                    return true;
                case 2:
                    deleteRecord(position);
                    return true;
                default:
                    return false;
            }
        });

        popup.show();
    }

    private void deleteRecord(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Удалить запись?")
                .setPositiveButton("Да", (dialog, which) -> {
                    // Реальное удаление из БД
                    dbHelper.deleteRecord(position + 1);

                    // Обновление списка
                    records.remove(position);
                    ((ArrayAdapter)listView.getAdapter()).notifyDataSetChanged();
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private void shareFile(int position) {
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_PATH +
                        " FROM " + DatabaseHelper.TABLE_RECORDS +
                        " LIMIT 1 OFFSET " + position, null
        );

        if (cursor.moveToFirst()) {
            String path = cursor.getString(0);
            File file = new File(path);
            FileUtils.shareFile(this, file);
        }
        cursor.close();
    }
}