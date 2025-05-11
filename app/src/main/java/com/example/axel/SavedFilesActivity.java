package com.example.axel;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.PopupMenu;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class SavedFilesActivity extends BaseActivity {
    private ListView listView;
    private DatabaseHelper dbHelper;
    private List<String> records;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 15;
    private ArrayAdapter<String> adapter;
    private ImageButton btnPrev, btnNext;
    private TextView tvPageInfo;
    private LinearLayout paginationContainer;
    private int totalPages = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_files);


        dbHelper = new DatabaseHelper(this);
        listView = findViewById(R.id.listView);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        tvPageInfo = findViewById(R.id.tv_page_info);
        paginationContainer = findViewById(R.id.pagination_container);
        //Страницы

        btnPrev.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                updatePage();
            }
        });
        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                updatePage();
                updateButtonColors();
            }
        });
        //ffff
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        setupNavigation();
        ImageView menuButton = findViewById(R.id.menu_button);

        menuButton.setOnClickListener(v -> {
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.openDrawer(GravityCompat.START);
        });
        //fffff

        //Страницы
        // Загрузка данных
        records = dbHelper.getAllRecords();

        // Настройка адаптера
        adapter = new ArrayAdapter<String>(this, R.layout.list_item_record, R.id.tv_record, records) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                        "SELECT " + DatabaseHelper.COLUMN_IS_FFT
                                + " FROM " + DatabaseHelper.TABLE_RECORDS
                                + " ORDER BY " + DatabaseHelper.COLUMN_ID
                                + " LIMIT 1 OFFSET " + position, null
                );

                TextView fftTag = view.findViewById(R.id.tv_fft_tag);
                if (cursor.moveToFirst() && cursor.getInt(0) == 1) {
                    fftTag.setVisibility(View.VISIBLE);
                } else {
                    fftTag.setVisibility(View.GONE);
                }
                cursor.close();

                ImageButton btnMenu = view.findViewById(R.id.btn_menu);
                btnMenu.setOnClickListener(v -> showPopupMenu(v, position));

                return view;
            }
        };

        updatePagination();
        listView.setAdapter(adapter);
    }
    private void updatePage() {
        records = dbHelper.getRecordsPaginated(currentPage, PAGE_SIZE);
        if(records.isEmpty() && currentPage > 0) {
            currentPage--;
            records = dbHelper.getRecordsPaginated(currentPage, PAGE_SIZE);
        }
        adapter.clear();
        adapter.addAll(records);
        updateButtonColors();
    }

    private void updateButtonColors() {
        int activeColor = ContextCompat.getColor(this, R.color.pagination_enabled);
        int inactiveColor = ContextCompat.getColor(this, R.color.pagination_disabled);

        btnPrev.setColorFilter(currentPage > 0 ? activeColor : inactiveColor);
        btnNext.setColorFilter(currentPage < totalPages - 1 ? activeColor : inactiveColor);
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

    @Override
    protected int getLayoutId() {
        return R.layout.activity_saved_files;
    }
    @Override
    protected void initViews() {

    }

    private void updatePagination() {
        int totalRecords = dbHelper.getRecordsCount();
        totalPages = (int) Math.ceil((double) totalRecords / PAGE_SIZE);

        // Корректируем текущую страницу если она стала невалидной
        if(totalPages > 0 && currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }

        // Обновляем видимость пагинации
        paginationContainer.setVisibility(totalPages > 1 ? View.VISIBLE : View.GONE);
        updatePageInfo();
    }


    private void updatePageInfo() {
        String pageText = (currentPage + 1) + " / " + totalPages;
        tvPageInfo.setText(pageText);
        updateButtonColors();
    }

    private void deleteRecord(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Удалить запись?")
                .setPositiveButton("Да", (dialog, which) -> {
                    // Получаем ID с учетом текущей страницы
                    int recordId = getRecordId(position);
                    if(recordId == -1) return;

                    // Удаляем запись из БД
                    dbHelper.deleteRecord(recordId);

                    // Полностью перезагружаем данные
                    updatePagination();
                    updatePage();
                })
                .setNegativeButton("Нет", null)
                .show();
    }
    private int getRecordId(int position) {
        // Рассчитываем общий offset с учетом текущей страницы
        int offset = currentPage * PAGE_SIZE + position;

        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_ID +
                        " FROM " + DatabaseHelper.TABLE_RECORDS +
                        " ORDER BY " + DatabaseHelper.COLUMN_ID +
                        " LIMIT 1 OFFSET " + offset, null);

        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }


    private void shareFile(int position) {
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_PATH + // Исправлено на COLUMN_PATH
                        " FROM " + DatabaseHelper.TABLE_RECORDS +
                        " ORDER BY " + DatabaseHelper.COLUMN_ID +
                        " LIMIT 1 OFFSET " + position, null);

        if (cursor.moveToFirst()) {
            String path = cursor.getString(0);
            File file = new File(path);
            if (file.exists()) {
                FileUtils.shareFile(this, file);
            } else {
                Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Ошибка доступа к записи", Toast.LENGTH_SHORT).show();
        }
        cursor.close();
    }
}