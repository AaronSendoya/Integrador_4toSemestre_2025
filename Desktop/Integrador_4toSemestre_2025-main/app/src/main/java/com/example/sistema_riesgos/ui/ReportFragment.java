package com.example.sistema_riesgos.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.sistema_riesgos.R;
import com.example.sistema_riesgos.core.RiskManagerService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportFragment extends Fragment {

    private static final int REQ_LOCATION = 2001;
    private static final int REQ_CAMERA_PERMISSION = 2002;
    private static final int REQ_TAKE_PHOTO = 2003;
    private static final int REQ_PICK_IMAGE = 2004;

    // Anti-spam: mínimo tiempo entre reportes (ms)
    private static final long MIN_REPORT_INTERVAL_MS = 30_000L; // 30 segundos
    private static final String PREFS_NAME = "riskguard_prefs";
    private static final String KEY_LAST_REPORT_TIME = "last_report_time";

    private Spinner spRiskType;
    private EditText etDescription;
    private Button btnOpenCamera;
    private Button btnPickImage;
    private Button btnSubmitReport;
    private ImageView ivPhotoPreview;

    private FusedLocationProviderClient fusedLocationClient;
    private RiskManagerService riskManagerService;

    // Se actualizan con la ubicación del dispositivo
    private double currentLat = 0.0;
    private double currentLon = 0.0;

    // Ruta real o URI de la foto (se guarda en SQLite como String)
    private String capturedPhotoPath = null;
    private Uri capturedPhotoUri = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spRiskType = view.findViewById(R.id.sp_risk_type);
        etDescription = view.findViewById(R.id.et_description);
        btnOpenCamera = view.findViewById(R.id.btn_open_camera);
        btnPickImage = view.findViewById(R.id.btn_pick_image);
        btnSubmitReport = view.findViewById(R.id.btn_submit_report);
        ivPhotoPreview = view.findViewById(R.id.iv_photo_preview);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        riskManagerService = RiskManagerService.getInstance(requireContext());

        requestLocationIfNeeded();

        btnOpenCamera.setOnClickListener(v -> openCameraWithPermissionCheck());
        btnPickImage.setOnClickListener(v -> openImagePicker());
        btnSubmitReport.setOnClickListener(v -> saveReport());
    }

    // ------------ UBICACIÓN ------------ //

    private void requestLocationIfNeeded() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                currentLat = location.getLatitude();
                                currentLon = location.getLongitude();
                            }
                        }
                    });
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOCATION
            );
        }
    }

    // ------------ CÁMARA + ARCHIVO ------------ //

    private void openCameraWithPermissionCheck() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    REQ_CAMERA_PERMISSION
            );
        }
    }

    private void dispatchTakePictureIntent() {
        if (!isAdded()) return;

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) == null) {
            Toast.makeText(requireContext(),
                    "No se encontró aplicación de cámara.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear archivo donde se guardará la foto
        File photoFile;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(),
                    "No se pudo crear el archivo de imagen.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            capturedPhotoUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile
            );

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedPhotoUri);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivityForResult(takePictureIntent, REQ_TAKE_PHOTO);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String imageFileName = "RG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            storageDir = requireContext().getFilesDir();
        }
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        capturedPhotoPath = image.getAbsolutePath();
        return image;
    }

    // ------------ PICKER DE IMÁGENES ------------ //

    private void openImagePicker() {
        if (!isAdded()) return;

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        startActivityForResult(intent, REQ_PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_TAKE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                if (capturedPhotoUri != null) {
                    ivPhotoPreview.setImageURI(capturedPhotoUri);
                    ivPhotoPreview.setVisibility(View.VISIBLE);
                } else if (capturedPhotoPath != null) {
                    ivPhotoPreview.setImageURI(Uri.fromFile(new File(capturedPhotoPath)));
                    ivPhotoPreview.setVisibility(View.VISIBLE);
                }
            } else {
                capturedPhotoPath = null;
                capturedPhotoUri = null;
                ivPhotoPreview.setVisibility(View.GONE);
            }
        } else if (requestCode == REQ_PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                Uri selectedImageUri = data.getData();

                capturedPhotoUri = selectedImageUri;
                capturedPhotoPath = selectedImageUri.toString();

                try {
                    requireContext().getContentResolver()
                            .takePersistableUriPermission(
                                    selectedImageUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                } catch (Exception e) {
                    // ignoramos
                }

                ivPhotoPreview.setImageURI(selectedImageUri);
                ivPhotoPreview.setVisibility(View.VISIBLE);
            }
        }
    }

    // ------------ GUARDAR REPORTE ------------ //

    private void saveReport() {
        String tipo = spRiskType.getSelectedItem() != null
                ? spRiskType.getSelectedItem().toString()
                : null;

        String descripcion = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(tipo)) {
            Toast.makeText(requireContext(),
                    "Selecciona un tipo de riesgo.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(descripcion)) {
            etDescription.setError("La descripción es obligatoria.");
            return;
        }

        // Validación extra: longitud mínima / máxima
        if (descripcion.length() < 15) {
            etDescription.setError("Añade un poco más de detalle (mínimo 15 caracteres).");
            return;
        }
        if (descripcion.length() > 600) {
            etDescription.setError("La descripción es demasiado larga (máximo 600 caracteres).");
            return;
        }

        // Anti-spam simple: tiempo mínimo entre reportes
        long now = System.currentTimeMillis();
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        long last = prefs.getLong(KEY_LAST_REPORT_TIME, 0L);

        if (now - last < MIN_REPORT_INTERVAL_MS) {
            long segundosRestantes = (MIN_REPORT_INTERVAL_MS - (now - last)) / 1000L + 1;
            Toast.makeText(
                    requireContext(),
                    "Espera " + segundosRestantes + " segundos antes de crear otro reporte.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // Si no tenemos ubicación, avisamos pero dejamos continuar
        if (currentLat == 0.0 && currentLon == 0.0) {
            Toast.makeText(
                    requireContext(),
                    "No se pudo obtener la ubicación exacta, se guardará igualmente.",
                    Toast.LENGTH_SHORT
            ).show();
        }

        // TODO: reemplazar por el id real del usuario logueado
        long idUsuarioLocal = 1L;

        long localId = riskManagerService.createReport(
                tipo,
                descripcion,
                currentLat,
                currentLon,
                capturedPhotoPath,  // ruta de archivo o URI content://
                idUsuarioLocal
        );

        if (localId != -1) {
            // Guardamos timestamp del último reporte válido
            prefs.edit().putLong(KEY_LAST_REPORT_TIME, now).apply();

            Toast.makeText(requireContext(),
                    "Reporte creado. Se sincronizará con el servidor cuando haya conexión.",
                    Toast.LENGTH_LONG).show();
            etDescription.setText("");
        } else {
            Toast.makeText(requireContext(),
                    "Error al guardar el reporte.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ------------ PERMISOS ------------ //

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationIfNeeded();
            }
        } else if (requestCode == REQ_CAMERA_PERMISSION) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                openCameraWithPermissionCheck();
            } else {
                Toast.makeText(requireContext(),
                        "Permiso de cámara denegado.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
