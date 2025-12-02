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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.sistema_riesgos.R;
import com.example.sistema_riesgos.core.RiskManagerService;
import com.example.sistema_riesgos.models.RiskAnalysisResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportFragment extends Fragment {

    private static final int REQ_LOCATION = 2001;
    private static final int REQ_CAMERA_PERMISSION = 2002;
    private static final int REQ_TAKE_PHOTO = 2003;
    private static final int REQ_PICK_IMAGE = 2004;

    private static final long MIN_REPORT_INTERVAL_MS = 30_000L;
    private static final String PREFS_NAME = "riskguard_prefs";
    private static final String KEY_LAST_REPORT_TIME = "last_report_time";

    private Spinner spRiskType;
    private EditText etDescription;
    private Button btnOpenCamera;
    private Button btnPickImage;
    private Button btnSubmitReport;
    private ImageView ivPhotoPreview;
    private TextView tvAiResult;
    private TextView tvSuggestedRiskType;

    private FusedLocationProviderClient fusedLocationClient;
    private RiskManagerService riskManagerService;

    private double currentLat = 0.0;
    private double currentLon = 0.0;

    private String capturedPhotoPath = null;
    private Uri capturedPhotoUri = null;

    private double currentAiConfidence = 0.0;

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
        tvAiResult = view.findViewById(R.id.tv_ai_result);
        tvSuggestedRiskType = view.findViewById(R.id.tv_suggested_risk_type);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        riskManagerService = RiskManagerService.getInstance(requireContext());

        requestLocationIfNeeded();

        btnOpenCamera.setOnClickListener(v -> openCameraWithPermissionCheck());
        btnPickImage.setOnClickListener(v -> openImagePicker());
        btnSubmitReport.setOnClickListener(v -> saveReport());
    }

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
                    REQ_LOCATION);
        }
    }

    private void openCameraWithPermissionCheck() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    REQ_CAMERA_PERMISSION);
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
                    photoFile);

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
                storageDir);
        capturedPhotoPath = image.getAbsolutePath();
        return image;
    }

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
        android.util.Log.d("MLKit", "onActivityResult: req=" + requestCode + ", res=" + resultCode);

        if (requestCode == REQ_TAKE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {

                Uri uriForMl = null;

                if (capturedPhotoUri != null) {
                    uriForMl = capturedPhotoUri;
                    ivPhotoPreview.setImageURI(capturedPhotoUri);
                } else if (capturedPhotoPath != null) {
                    Uri localUri = Uri.fromFile(new File(capturedPhotoPath));
                    uriForMl = localUri;
                    ivPhotoPreview.setImageURI(localUri);
                }

                if (uriForMl != null) {
                    ivPhotoPreview.setVisibility(View.VISIBLE);

                    if (capturedPhotoPath != null) {
                        analyzePhoto(capturedPhotoPath);
                    }

                    processImageWithMLKit(uriForMl);
                }

            } else {
                capturedPhotoPath = null;
                capturedPhotoUri = null;
                ivPhotoPreview.setVisibility(View.GONE);
                tvAiResult.setVisibility(View.GONE);
                tvSuggestedRiskType.setVisibility(View.GONE);
            }

        } else if (requestCode == REQ_PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                Uri selectedImageUri = data.getData();
                android.util.Log.d("MLKit", "Image picked: " + selectedImageUri);

                capturedPhotoUri = selectedImageUri;
                capturedPhotoPath = selectedImageUri.toString();

                try {
                    requireContext().getContentResolver()
                            .takePersistableUriPermission(
                                    selectedImageUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception e) {
                    // ignore
                }

                ivPhotoPreview.setImageURI(selectedImageUri);
                ivPhotoPreview.setVisibility(View.VISIBLE);

                if (capturedPhotoPath != null && capturedPhotoPath.startsWith("content://")) {
                    String realPath = com.example.sistema_riesgos.utils.FileUtils.getPath(
                            requireContext(), selectedImageUri);
                    if (realPath != null) {
                        capturedPhotoPath = realPath;
                        android.util.Log.d("MLKit", "Resolved content URI to path: " + capturedPhotoPath);
                        analyzePhoto(capturedPhotoPath);
                    } else {
                        android.util.Log.w("MLKit", "Could not resolve path from URI: " + selectedImageUri);
                    }
                } else if (capturedPhotoPath != null && !capturedPhotoPath.startsWith("content://")) {
                    analyzePhoto(capturedPhotoPath);
                }

                processImageWithMLKit(selectedImageUri);
            }
        }
    }

    private void analyzePhoto(String path) {
        if (path == null) return;

        tvAiResult.setVisibility(View.VISIBLE);
        tvAiResult.setText("Analizando riesgo con IA...");
        tvAiResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_800));

        riskManagerService.analyzePhoto(path, new Callback<RiskAnalysisResponse>() {
            @Override
            public void onResponse(Call<RiskAnalysisResponse> call, Response<RiskAnalysisResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    RiskAnalysisResponse analysis = response.body();
                    String riskType = analysis.getRiskType();
                    double confidence = analysis.getConfidence();
                    currentAiConfidence = confidence;

                    String text = String.format(Locale.getDefault(),
                            "IA Detectó: %s (%.1f%%)", riskType, confidence * 100);

                    tvAiResult.setText(text);
                    tvAiResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
                } else {
                    tvAiResult.setText("No se pudo clasificar el riesgo.");
                }
            }

            @Override
            public void onFailure(Call<RiskAnalysisResponse> call, Throwable t) {
                if (!isAdded()) return;
                tvAiResult.setText("Error de conexión con IA (se guardará sin análisis).");
            }
        });
    }

    private void processImageWithMLKit(@NonNull Uri imageUri) {
        android.util.Log.d("MLKit", "Processing image URI: " + imageUri);

        try {
            InputImage image = InputImage.fromFilePath(requireContext(), imageUri);
            ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

            labeler.process(image)
                    .addOnSuccessListener(labels -> {
                        if (!isAdded()) return;

                        android.util.Log.d("MLKit", "Labels found: " + labels.size());
                        for (ImageLabel label : labels) {
                            android.util.Log.d(
                                    "MLKit",
                                    "Label: " + label.getText() + " (" + label.getConfidence() + ")"
                            );
                        }

                        suggestRiskType(labels);
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("MLKit", "Error processing image", e);
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void suggestRiskType(List<ImageLabel> labels) {
        String suggestedType = "Otros";
        boolean matchFound = false;

        if (labels == null || labels.isEmpty()) {
            selectRiskTypeInSpinner(suggestedType);
            tvSuggestedRiskType.setText(
                    "La IA no detectó un riesgo claro en la imagen. Se sugiere 'Otros' (puedes cambiarlo)."
            );
            tvSuggestedRiskType.setVisibility(View.VISIBLE);
            return;
        }

        for (ImageLabel label : labels) {
            String text = label.getText().toLowerCase();
            android.util.Log.d("MLKit", "Checking label: " + text);

            if (text.contains("fire") || text.contains("smoke") || text.contains("flame") || text.contains("burning")) {
                suggestedType = "Incendio";
                matchFound = true;
                break;
            } else if (text.contains("water") || text.contains("flood") || text.contains("river")
                    || text.contains("lake") || text.contains("rain")) {
                suggestedType = "Inundación";
                matchFound = true;
                break;
            } else if (text.contains("storm") || text.contains("wind") || text.contains("cloud")
                    || text.contains("tree")) {
                suggestedType = "Vientos Fuertes";
                matchFound = true;
                break;
            }
        }

        android.util.Log.d("MLKit", "Match found: " + matchFound + ", Suggested: " + suggestedType);

        if (matchFound) {
            selectRiskTypeInSpinner(suggestedType);
            tvSuggestedRiskType.setText(
                    "Sugerencia automática según la foto: " + suggestedType + " (puedes cambiarla)"
            );
            tvSuggestedRiskType.setVisibility(View.VISIBLE);
        } else {
            selectRiskTypeInSpinner(suggestedType);
            tvSuggestedRiskType.setText(
                    "La IA no encontró un tipo específico. Se sugiere 'Otros' (puedes cambiarlo)."
            );
            tvSuggestedRiskType.setVisibility(View.VISIBLE);
        }
    }

    private void selectRiskTypeInSpinner(@NonNull String type) {
        String[] types = getResources().getStringArray(R.array.risk_types);
        for (int i = 0; i < types.length; i++) {
            if (types[i].equalsIgnoreCase(type)) {
                android.util.Log.d("MLKit", "Setting spinner to index: " + i + " (" + types[i] + ")");
                spRiskType.setSelection(i);
                return;
            }
        }
    }

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

        if (descripcion.length() < 15) {
            etDescription.setError("Añade un poco más de detalle (mínimo 15 caracteres).");
            return;
        }
        if (descripcion.length() > 600) {
            etDescription.setError("La descripción es demasiado larga (máximo 600 caracteres).");
            return;
        }

        long now = System.currentTimeMillis();
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        long last = prefs.getLong(KEY_LAST_REPORT_TIME, 0L);

        if (now - last < MIN_REPORT_INTERVAL_MS) {
            long segundosRestantes = (MIN_REPORT_INTERVAL_MS - (now - last)) / 1000L + 1;
            Toast.makeText(
                    requireContext(),
                    "Espera " + segundosRestantes + " segundos antes de crear otro reporte.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentLat == 0.0 && currentLon == 0.0) {
            Toast.makeText(
                    requireContext(),
                    "No se pudo obtener la ubicación exacta, se guardará igualmente.",
                    Toast.LENGTH_SHORT).show();
        }

        long idUsuarioLocal = 1L;

        long localId = riskManagerService.createReport(
                tipo,
                descripcion,
                currentLat,
                currentLon,
                capturedPhotoPath,
                idUsuarioLocal,
                currentAiConfidence);

        if (localId != -1) {
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
