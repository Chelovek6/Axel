package com.example.axel.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileUtils {

    public static void showSaveDialog(Activity activity, File tempCsvFile, boolean isFFT) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        String defaultFileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());

        EditText input = new EditText(activity);
        input.setText(defaultFileName);
        input.setSelection(0, defaultFileName.length());
        input.setHint("Введите название файла");
        builder.setView(input);

        AtomicBoolean isSaved = new AtomicBoolean(false);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            isSaved.set(true);
            String userFileName = input.getText().toString().trim();
            if (userFileName.isEmpty()) userFileName = defaultFileName;

            File finalFile = new File(activity.getCacheDir(), userFileName + ".csv");

            if (tempCsvFile.renameTo(finalFile)) {
                new DatabaseHelper(activity).addRecord(userFileName, finalFile.getAbsolutePath(), isFFT);
                shareFile(activity, finalFile);
            } else {
                Toast.makeText(activity, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> {
            if (tempCsvFile.exists()) tempCsvFile.delete();
        });

        AlertDialog dialog = builder.create();

        dialog.setOnDismissListener(dialogInterface -> {
            if (!isSaved.get() && tempCsvFile.exists()) {
                File finalFile = new File(activity.getCacheDir(), defaultFileName + ".csv");
                if (tempCsvFile.renameTo(finalFile)) {
                    new DatabaseHelper(activity).addRecord(defaultFileName, finalFile.getAbsolutePath(), isFFT);
                    Toast.makeText(activity, "Автосохранено: " + defaultFileName, Toast.LENGTH_LONG).show();
                }
            }
        });

        dialog.show();
    }
    public static void shareFile(Context context, File file) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getApplicationContext().getPackageName() + ".provider",
                    file
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, "Поделиться через"));
        } catch (Exception e) {
            Toast.makeText(context, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
