package com.example.sistema_riesgos.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sistema_riesgos.R;
import com.example.sistema_riesgos.core.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_AVATAR = 3001;
    private static final String TAG = "ProfileActivity";
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_AVATAR_URI = "avatar_uri";

    private ImageView ivProfileAvatar;
    private Button btnChangeAvatar;
    private Button btnLogout;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        ivProfileAvatar = findViewById(R.id.iv_profile_avatar);
        btnChangeAvatar = findViewById(R.id.btn_change_avatar);
        btnLogout = findViewById(R.id.btn_logout);

        // Datos del usuario (si los guardas)
        loadUserInfo();

        // Avatar guardado (si hay)
        loadSavedAvatar();

        // Cambiar avatar
        btnChangeAvatar.setOnClickListener(v -> openImagePicker());

        // Cerrar sesión
        btnLogout.setOnClickListener(v -> {
            SessionManager.clearSession(ProfileActivity.this);
            prefs.edit().remove(KEY_AVATAR_URI).apply();
            ivProfileAvatar.setImageResource(R.drawable.ic_user_placeholder);

            Intent i = new Intent(ProfileActivity.this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    // Si tienes nombre/correo guardado en SharedPreferences
    private void loadUserInfo() {
        TextView tvName = findViewById(R.id.tv_profile_name);
        TextView tvEmail = findViewById(R.id.tv_profile_email);

        if (tvName != null) {
            String name = prefs.getString("user_name", "Usuario");
            tvName.setText(name);
        }
        if (tvEmail != null) {
            String email = prefs.getString("user_email", "--");
            tvEmail.setText(email);
        }
    }

    private void loadSavedAvatar() {
        String uriString = prefs.getString(KEY_AVATAR_URI, null);

        if (uriString == null || uriString.isEmpty()) {
            ivProfileAvatar.setImageResource(R.drawable.ic_user_placeholder);
            return;
        }

        try {
            Uri uri = Uri.parse(uriString);
            // Aquí antes te crasheaba: ahora lo envolvemos en try/catch
            ivProfileAvatar.setImageURI(uri);
        } catch (SecurityException | IllegalArgumentException e) {
            Log.e(TAG, "No se pudo acceder al avatar guardado, limpiando preferencia.", e);
            prefs.edit().remove(KEY_AVATAR_URI).apply();
            ivProfileAvatar.setImageResource(R.drawable.ic_user_placeholder);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

        // Para poder tomar permiso persistente
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        startActivityForResult(intent, REQUEST_CODE_PICK_AVATAR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_AVATAR
                && resultCode == Activity.RESULT_OK
                && data != null) {

            Uri uri = data.getData();
            if (uri != null) {
                // Tomar permiso persistente para que funcione incluso tras reiniciar la app
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                try {
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                } catch (SecurityException e) {
                    Log.w(TAG, "No se pudo tomar permiso persistente para la imagen", e);
                }

                // Guardar URI en SharedPreferences
                prefs.edit()
                        .putString(KEY_AVATAR_URI, uri.toString())
                        .apply();

                // Mostrar inmediatamente en el perfil
                ivProfileAvatar.setImageURI(uri);
            }
        }
    }
}
