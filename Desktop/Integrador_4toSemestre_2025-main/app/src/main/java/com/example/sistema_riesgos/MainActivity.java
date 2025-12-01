package com.example.sistema_riesgos;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.sistema_riesgos.data.ApiRepository;
import com.example.sistema_riesgos.data.SyncWorker;
import com.example.sistema_riesgos.ui.AlertsFragment;
import com.example.sistema_riesgos.ui.HistoryFragment;
import com.example.sistema_riesgos.ui.HomeFragment;
import com.example.sistema_riesgos.ui.MapViewFragment;
import com.example.sistema_riesgos.ui.ReportFragment;
import com.example.sistema_riesgos.ui.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private long currentUserId = -1;
    private String currentUserEmail = "";

    private ApiRepository apiRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Comprobamos sesión de usuario (extra / saved state)
        checkUserAuthentication();

        setContentView(R.layout.activity_main);

        // Get user data from intent
        Intent intent = getIntent();
        if (intent != null) {
            currentUserId = intent.getLongExtra("extra_local_user_id", -1);
            currentUserEmail = intent.getStringExtra("extra_user_email");

            if (currentUserId != -1 && currentUserEmail != null) {
                // User is authenticated
                Toast.makeText(this, "Bienvenido " + currentUserEmail, Toast.LENGTH_SHORT).show();
            } else {
                // User is not authenticated, redirect to login
                redirectToLogin();
                return;
            }
        } else {
            // No intent data, check if we have saved instance state
            if (savedInstanceState != null) {
                currentUserId = savedInstanceState.getLong("currentUserId", -1);
                currentUserEmail = savedInstanceState.getString("currentUserEmail", "");

                if (currentUserId == -1) {
                    redirectToLogin();
                    return;
                }
            } else {
                // No saved state, redirect to login
                redirectToLogin();
                return;
            }
        }

        // ----------------- NUEVO: sincronización inicial + WorkManager -----------------
        apiRepository = ApiRepository.getInstance(getApplicationContext());
        // Sincroniza inmediatamente lo pendiente (si hay conexión)
        apiRepository.syncPendingUsers();
        apiRepository.syncPendingReports();
        // Programa sincronización periódica en background
        SyncWorker.enqueuePeriodicSync(this);
        // -------------------------------------------------------------------------------

        bottomNav = findViewById(R.id.bottom_nav_bar);

        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.nav_map) {
                fragment = new MapViewFragment();
            } else if (id == R.id.nav_alerts) {
                // Ahora usamos el nuevo fragmento de alertas reales
                fragment = new AlertsFragment();
            } else if (id == R.id.nav_report) {
                fragment = new ReportFragment();
            } else if (id == R.id.nav_history) {
                fragment = new HistoryFragment();
            }

            if (fragment != null) {
                replaceFragment(fragment);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("currentUserId", currentUserId);
        outState.putString("currentUserEmail", currentUserEmail);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            // Clear user session and redirect to login
            currentUserId = -1;
            currentUserEmail = "";

            redirectToLogin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void replaceFragment(@NonNull Fragment fragment) {
        // Pass user ID to fragment if it's a fragment that needs it
        Bundle args = new Bundle();
        args.putLong("userId", currentUserId);
        args.putString("userEmail", currentUserEmail);
        fragment.setArguments(args);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }

    private void checkUserAuthentication() {
        // Aquí podrías añadir lógica extra si luego guardas sesión en SharedPreferences
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public long getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentUserEmail() {
        return currentUserEmail;
    }
}
