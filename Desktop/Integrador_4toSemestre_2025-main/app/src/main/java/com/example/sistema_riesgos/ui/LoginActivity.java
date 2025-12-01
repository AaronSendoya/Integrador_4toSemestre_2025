package com.example.sistema_riesgos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sistema_riesgos.MainActivity;
import com.example.sistema_riesgos.R;
import com.example.sistema_riesgos.core.SessionManager;
import com.example.sistema_riesgos.data.ApiRepository;
import com.example.sistema_riesgos.data.DBHelper;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private TextView btnRegisterLink;
    private ProgressBar progressBar;

    // DBHelper queda por si lo necesitas en otros flujos
    private DBHelper dbHelper;
    private ApiRepository apiRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔹 Auto-login: si ya hay sesión guardada, ir directo a MainActivity
        if (SessionManager.isLoggedIn(this)) {
            Intent intent = new Intent(this, MainActivity.class);

            // Pasamos también los extras si están guardados
            String userIdStr = SessionManager.getUserId(this);
            String userEmail = SessionManager.getUserEmail(this);

            if (userIdStr != null) {
                try {
                    long localUserId = Long.parseLong(userIdStr);
                    intent.putExtra("extra_local_user_id", localUserId);
                } catch (NumberFormatException ignored) {
                }
            }
            if (userEmail != null) {
                intent.putExtra("extra_user_email", userEmail);
            }

            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // Initialize helpers
        dbHelper = new DBHelper(this);
        apiRepository = ApiRepository.getInstance(getApplicationContext());

        // Initialize views
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegisterLink = findViewById(R.id.btn_register_link);
        progressBar = findViewById(R.id.progress_bar);

        // Set click listeners
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegisterLink.setOnClickListener(v -> navigateToRegister());
    }

    private void attemptLogin() {
        // Reset errors
        etEmail.setError(null);
        etPassword.setError(null);

        // Get values
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean hasError = false;
        View focusView = null;

        // Validate email
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Ingresa tu correo electrónico");
            focusView = etEmail;
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Ingresa un correo electrónico válido");
            focusView = etEmail;
            hasError = true;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Ingresa tu contraseña");
            if (!hasError) {
                focusView = etPassword;
                hasError = true;
            }
        } else if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            if (!hasError) {
                focusView = etPassword;
                hasError = true;
            }
        }

        if (hasError) {
            if (focusView != null) {
                focusView.requestFocus();
            }
            return;
        }

        // Show progress bar and attempt login híbrido
        showProgress(true);
        loginUser(email, password);
    }

    private void loginUser(String email, String password) {
        apiRepository.loginUserHybrid(email, password, new ApiRepository.LoginCallback() {
            @Override
            public void onSuccess(long localUserId, String emailFromRepo) {
                showProgress(false);
                Toast.makeText(LoginActivity.this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();

                // 🔹 Guardar sesión para mantener al usuario logueado offline
                String finalEmail = (emailFromRepo != null && !emailFromRepo.isEmpty())
                        ? emailFromRepo
                        : email;

                SessionManager.saveSession(
                        LoginActivity.this,
                        String.valueOf(localUserId), // userId (local)
                        null,                        // nombre (si luego lo tienes, lo puedes poner)
                        finalEmail,                  // email
                        null                         // token (si luego usas JWT, lo guardas aquí)
                );

                // Ir a MainActivity pasando los extras como antes
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("extra_local_user_id", localUserId);
                intent.putExtra("extra_user_email", finalEmail);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                showProgress(false);
                etPassword.setError("Correo o contraseña incorrectos");
                etPassword.requestFocus();
                Toast.makeText(
                        LoginActivity.this,
                        message != null ? message
                                : "Error al iniciar sesión. Verifica tus credenciales.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void navigateToRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
        // Optional: Add transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnRegisterLink.setEnabled(!show);
    }
}
