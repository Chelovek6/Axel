package com.example.axel;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public abstract class BaseActivity extends AppCompatActivity {
    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            setupNavigation();
        }

        initViews();

        setupNavigation();
        initViews();
    }

    protected abstract int getLayoutId();
    protected abstract void initViews();

    protected void setupNavigation() {
        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);
    }

    private boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        Class<?> target = null;
        String logMessage = "Selected item: ";

        if (id == R.id.nav_accelerometer) {
            target = MainActivity.class;
            logMessage += "Accelerometer";
        } else if (id == R.id.nav_fft) {
            target = FFTActivity.class;
            logMessage += "FFT";
        } else if (id == R.id.nav_files) {
            target = SavedFilesActivity.class;
            logMessage += "Files";
        } else if (id == R.id.nav_settings) {
            target = SettingsActivity.class;
            logMessage += "Settings";
        }

        Log.d("NavigationDebug", logMessage);

        if (target != null && !this.getClass().equals(target)) {
            Log.d("NavigationDebug", "Starting: " + target.getSimpleName());
            startActivity(new Intent(this, target));
        } else {
            Log.d("NavigationDebug", "Already on target");
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}