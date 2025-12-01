package com.example.sistema_riesgos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sistema_riesgos.MainActivity;
import com.example.sistema_riesgos.R;
import com.example.sistema_riesgos.data.ApiRepository;
import com.example.sistema_riesgos.data.DBHelper;
import com.example.sistema_riesgos.data.SyncWorker;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName;
    private EditText etPhone;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginLink;

    private ApiRepository apiRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiRepository = ApiRepository.getInstance(getApplicationContext());

        // Initialize views
        etFullName = findViewById(R.id.et_full_name);
        etPhone = findViewById(R.id.et_phone);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLoginLink = findViewById(R.id.tv_login_link);

        btnRegister.setOnClickListener(v -> attemptRegistration());
        tvLoginLink.setOnClickListener(v -> navigateToLogin());
    }

    private void attemptRegistration() {
        etFullName.setError(null);
        etPhone.setError(null);
        etEmail.setError(null);
        etPassword.setError(null);
        etConfirmPassword.setError(null);

        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        boolean hasError = false;

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Ingresa tu nombre completo");
            hasError = true;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Ingresa tu teléfono");
            hasError = true;
        } else if (phone.length() < 8) {
            etPhone.setError("El teléfono debe tener al menos 8 dígitos");
            hasError = true;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Ingresa tu correo electrónico");
            hasError = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Ingresa un correo electrónico válido");
            hasError = true;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Ingresa una contraseña");
            hasError = true;
        } else if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            hasError = true;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Confirma tu contraseña");
            hasError = true;
        } else if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Las contraseñas no coinciden");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        registerUser(fullName, phone, email, password);
    }

    private void registerUser(String fullName, String phone, String email, String password) {
        // 🔒 NUEVO: bloquear registro si NO hay Internet
        if (!apiRepository.isOnline()) {
            Toast.makeText(
                    this,
                    "Se requiere conexión a Internet para crear una cuenta. " +
                            "Conéctate y vuelve a intentarlo.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // LÓGICA QUE YA TENÍAS: insert en SQLite + sync
        DBHelper dbHelper = new DBHelper(this);

        if (dbHelper.checkUserExists(email)) {
            etEmail.setError("Este correo ya está registrado");
            return;
        }

        long userId = dbHelper.insertUsuario(
                fullName,
                email,
                phone,
                password,
                DBHelper.SYNC_STATUS_PENDING
        );

        if (userId != -1) {
            Toast.makeText(this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show();

            // Sincronización inmediata (si hay Internet)
            apiRepository.syncPendingUsers();

            // Programar sync periódica con WorkManager
            SyncWorker.enqueuePeriodicSync(this);

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("extra_local_user_id", userId);
            intent.putExtra("extra_user_email", email);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this,
                    "Error al registrar el usuario. Intenta nuevamente.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
