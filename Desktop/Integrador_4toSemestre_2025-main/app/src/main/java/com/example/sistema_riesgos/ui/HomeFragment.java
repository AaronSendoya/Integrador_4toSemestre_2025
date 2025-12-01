package com.example.sistema_riesgos.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.sistema_riesgos.R;
import com.example.sistema_riesgos.data.ApiRepository;
import com.example.sistema_riesgos.models.ReportModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "HomeFragment";
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_AVATAR_URI = "avatar_uri";

    // Header
    private TextView tvAlertTitle;
    private TextView tvAlertRecommendation;
    private TextView btnViewOnMap;
    private Button btnQuickReport;
    private ImageView ivHomeAvatar;

    // Card de clima
    private TextView tvSensorCity;
    private ImageView ivSensorConditionIcon;
    private TextView tvSensorCondition;
    private TextView tvSensorHumidity;
    private TextView tvSensorTemperature;
    private TextView tvSensorRainChance;
    private TextView tvSensorWind;
    private TextView tvSensorPrecipNow;
    private TextView tvSensorPrecipTotal;
    private TextView tvSensorPressure;
    private TextView tvSensorUv;

    private ApiRepository apiRepository;
    private FusedLocationProviderClient fusedLocationClient;
    private SharedRiskViewModel sharedRiskViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Header
        tvAlertTitle = view.findViewById(R.id.tv_alert_title);
        tvAlertRecommendation = view.findViewById(R.id.tv_alert_recommendation);
        btnViewOnMap = view.findViewById(R.id.btn_view_on_map);
        btnQuickReport = view.findViewById(R.id.btn_quick_report);
        ivHomeAvatar = view.findViewById(R.id.iv_home_avatar);

        // Click en avatar -> Perfil
        ivHomeAvatar.setOnClickListener(v -> {
            if (getActivity() == null) return;
            Intent intent = new Intent(getActivity(), ProfileActivity.class);
            startActivity(intent);
        });

        // Cargar avatar si existe
        loadHeaderAvatar();

        // Card clima
        tvSensorCity = view.findViewById(R.id.tv_sensor_city);
        ivSensorConditionIcon = view.findViewById(R.id.iv_sensor_condition_icon);
        tvSensorCondition = view.findViewById(R.id.tv_sensor_condition);
        tvSensorHumidity = view.findViewById(R.id.tv_sensor_humidity);
        tvSensorTemperature = view.findViewById(R.id.tv_sensor_temperature);
        tvSensorRainChance = view.findViewById(R.id.tv_sensor_rain_chance);
        tvSensorWind = view.findViewById(R.id.tv_sensor_wind);
        tvSensorPrecipNow = view.findViewById(R.id.tv_sensor_precip_now);
        tvSensorPrecipTotal = view.findViewById(R.id.tv_sensor_precip_total);
        tvSensorPressure = view.findViewById(R.id.tv_sensor_pressure);
        tvSensorUv = view.findViewById(R.id.tv_sensor_uv);

        // ViewModel para últimas alertas
        sharedRiskViewModel = new ViewModelProvider(requireActivity())
                .get(SharedRiskViewModel.class);
        sharedRiskViewModel.getReports().observe(
                getViewLifecycleOwner(),
                this::updateLastAlert
        );

        // Navegación rápida a mapa e informe
        btnViewOnMap.setOnClickListener(v -> {
            BottomNavigationView bottomNav =
                    requireActivity().findViewById(R.id.bottom_nav_bar);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_map);
            }
        });

        btnQuickReport.setOnClickListener(v -> {
            BottomNavigationView bottomNav =
                    requireActivity().findViewById(R.id.bottom_nav_bar);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_report);
            }
        });

        // Repositorio y GPS
        apiRepository = ApiRepository.getInstance(requireContext().getApplicationContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Cargar clima real usando la ubicación REAL del dispositivo
        loadWeatherWithLocation();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Si el usuario cambia el avatar en el perfil y vuelve al Home
        loadHeaderAvatar();
    }

    // ------------------ AVATAR HEADER ------------------ //

    private void loadHeaderAvatar() {
        if (!isAdded() || ivHomeAvatar == null) return;

        Context ctx = requireContext();
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String uriString = prefs.getString(KEY_AVATAR_URI, null);

        if (uriString == null || uriString.isEmpty()) {
            ivHomeAvatar.setImageResource(R.drawable.ic_user_placeholder);
            return;
        }

        try {
            Uri uri = Uri.parse(uriString);
            ivHomeAvatar.setImageURI(uri);
        } catch (SecurityException | IllegalArgumentException e) {
            Log.e(TAG, "No se pudo cargar avatar en Home, reseteando.", e);
            prefs.edit().remove(KEY_AVATAR_URI).apply();
            ivHomeAvatar.setImageResource(R.drawable.ic_user_placeholder);
        }
    }

    // ------------------ CLIMA + GPS ------------------ //

    private void loadWeatherWithLocation() {
        if (!isAdded()) return;

        boolean fineGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            requestLocationAndWeather();
        } else {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
            setSensorNoLocation();
        }
    }

    private void requestLocationAndWeather() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (!isAdded()) return;
                        if (location != null) {
                            fetchWeather(location);
                        } else {
                            fusedLocationClient.getCurrentLocation(
                                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                                            null
                                    )
                                    .addOnSuccessListener(currentLocation -> {
                                        if (!isAdded()) return;
                                        if (currentLocation != null) {
                                            fetchWeather(currentLocation);
                                        } else {
                                            setSensorNoLocation();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        if (!isAdded()) return;
                                        setSensorNoLocation();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        setSensorNoLocation();
                    });
        } catch (SecurityException e) {
            setSensorNoLocation();
        }
    }

    private void fetchWeather(@NonNull Location location) {
        fetchWeather(location.getLatitude(), location.getLongitude());
    }

    /**
     * ApiRepository.getCurrentWeather(lat, lon) debe llamar a:
     *  forecast.json?key=...&q=lat,lon&days=1
     */
    private void fetchWeather(double lat, double lon) {
        Call<JsonObject> call = apiRepository.getCurrentWeather(lat, lon);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call,
                                   @NonNull Response<JsonObject> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();

                        // 0) Ciudad / ubicación
                        String cityText = "--";
                        if (body.has("location")) {
                            JsonObject locationObj = body.getAsJsonObject("location");
                            String name = locationObj.has("name")
                                    ? locationObj.get("name").getAsString()
                                    : "";
                            String region = locationObj.has("region")
                                    ? locationObj.get("region").getAsString()
                                    : "";
                            String country = locationObj.has("country")
                                    ? locationObj.get("country").getAsString()
                                    : "";

                            if (!name.isEmpty() && !region.isEmpty()) {
                                cityText = name + ", " + region;
                            } else if (!name.isEmpty()) {
                                cityText = name;
                            } else if (!country.isEmpty()) {
                                cityText = country;
                            }
                        }

                        // 1) Datos "current"
                        JsonObject current = body.getAsJsonObject("current");

                        double humidity = current != null && current.has("humidity")
                                ? current.get("humidity").getAsDouble()
                                : Double.NaN;
                        double tempC = current != null && current.has("temp_c")
                                ? current.get("temp_c").getAsDouble()
                                : Double.NaN;
                        double windKph = current != null && current.has("wind_kph")
                                ? current.get("wind_kph").getAsDouble()
                                : Double.NaN;
                        double gustKph = current != null && current.has("gust_kph")
                                ? current.get("gust_kph").getAsDouble()
                                : Double.NaN;
                        double precipNow = current != null && current.has("precip_mm")
                                ? current.get("precip_mm").getAsDouble()
                                : Double.NaN;
                        double pressure = current != null && current.has("pressure_mb")
                                ? current.get("pressure_mb").getAsDouble()
                                : Double.NaN;
                        double uv = current != null && current.has("uv")
                                ? current.get("uv").getAsDouble()
                                : Double.NaN;

                        String conditionText = "--";
                        String conditionIconUrl = null;
                        if (current != null && current.has("condition")) {
                            JsonObject cond = current.getAsJsonObject("condition");
                            if (cond != null) {
                                if (cond.has("text")) {
                                    conditionText = cond.get("text").getAsString();
                                }
                                if (cond.has("icon")) {
                                    // WeatherAPI devuelve algo tipo: "//cdn.weatherapi.com/..."
                                    String iconPath = cond.get("icon").getAsString();
                                    if (iconPath != null && !iconPath.isEmpty()) {
                                        if (iconPath.startsWith("//")) {
                                            conditionIconUrl = "https:" + iconPath;
                                        } else {
                                            conditionIconUrl = iconPath;
                                        }
                                    }
                                }
                            }
                        }

                        // 2) Pronóstico del día (totalprecip_mm + prob. lluvia)
                        String rainChanceText = "-- %";
                        double totalPrecip = Double.NaN;

                        if (body.has("forecast")) {
                            JsonObject forecast = body.getAsJsonObject("forecast");
                            if (forecast != null && forecast.has("forecastday")) {
                                JsonArray days = forecast.getAsJsonArray("forecastday");
                                if (days != null && days.size() > 0) {
                                    JsonObject firstDay = days.get(0).getAsJsonObject();
                                    if (firstDay != null && firstDay.has("day")) {
                                        JsonObject day = firstDay.getAsJsonObject("day");
                                        if (day != null) {
                                            if (day.has("daily_chance_of_rain")) {
                                                int rainChance = day
                                                        .get("daily_chance_of_rain")
                                                        .getAsInt();
                                                rainChanceText = rainChance + " %";
                                            }
                                            if (day.has("totalprecip_mm")) {
                                                totalPrecip = day
                                                        .get("totalprecip_mm")
                                                        .getAsDouble();
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ---- Actualizamos UI de clima con valores reales ----
                        tvSensorCity.setText(cityText);

                        // Condición texto + icono
                        tvSensorCondition.setText(conditionText);
                        if (conditionIconUrl != null && !conditionIconUrl.isEmpty()) {
                            Glide.with(requireContext())
                                    .load(conditionIconUrl)
                                    .into(ivSensorConditionIcon);
                        } else {
                            ivSensorConditionIcon.setImageDrawable(null);
                        }

                        if (!Double.isNaN(humidity)) {
                            tvSensorHumidity.setText(
                                    String.format(Locale.getDefault(), "%.0f%%", humidity)
                            );
                        } else {
                            tvSensorHumidity.setText("-- %");
                        }

                        if (!Double.isNaN(tempC)) {
                            tvSensorTemperature.setText(
                                    String.format(Locale.getDefault(), "%.1f °C", tempC)
                            );
                        } else {
                            tvSensorTemperature.setText("-- °C");
                        }

                        tvSensorRainChance.setText(rainChanceText);

                        // Viento: velocidad + ráfagas
                        if (!Double.isNaN(windKph) || !Double.isNaN(gustKph)) {
                            StringBuilder windText = new StringBuilder();
                            if (!Double.isNaN(windKph)) {
                                windText.append(
                                        String.format(Locale.getDefault(),
                                                "%.1f km/h", windKph)
                                );
                            }
                            if (!Double.isNaN(gustKph)) {
                                if (windText.length() > 0) windText.append(" · ");
                                windText.append(
                                        String.format(Locale.getDefault(),
                                                "ráfagas %.1f km/h", gustKph)
                                );
                            }
                            tvSensorWind.setText(windText.toString());
                        } else {
                            tvSensorWind.setText("-- km/h");
                        }

                        // Lluvia actual
                        if (!Double.isNaN(precipNow)) {
                            tvSensorPrecipNow.setText(
                                    String.format(Locale.getDefault(),
                                            "%.1f mm", precipNow)
                            );
                        } else {
                            tvSensorPrecipNow.setText("-- mm");
                        }

                        // Lluvia total del día
                        if (!Double.isNaN(totalPrecip)) {
                            tvSensorPrecipTotal.setText(
                                    String.format(Locale.getDefault(),
                                            "%.1f mm", totalPrecip)
                            );
                        } else {
                            tvSensorPrecipTotal.setText("-- mm");
                        }

                        // Presión
                        if (!Double.isNaN(pressure)) {
                            tvSensorPressure.setText(
                                    String.format(Locale.getDefault(),
                                            "%.0f hPa", pressure)
                            );
                        } else {
                            tvSensorPressure.setText("-- hPa");
                        }

                        // UV
                        if (!Double.isNaN(uv)) {
                            tvSensorUv.setText(
                                    String.format(Locale.getDefault(),
                                            "%.1f", uv)
                            );
                        } else {
                            tvSensorUv.setText("--");
                        }

                    } catch (Exception e) {
                        setSensorDefaults();
                    }
                } else {
                    setSensorDefaults();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                setSensorDefaults();
            }
        });
    }

    // ------------------ ESTADOS DE UI (fallbacks) ------------------ //

    private void setSensorDefaults() {
        tvSensorCity.setText("--");
        tvSensorCondition.setText("--");
        ivSensorConditionIcon.setImageDrawable(null);

        tvSensorHumidity.setText(getString(R.string.home_sensor_value_humidity_default));
        tvSensorTemperature.setText("-- °C");
        tvSensorRainChance.setText("-- %");
        tvSensorWind.setText("-- km/h");
        tvSensorPrecipNow.setText("-- mm");
        tvSensorPrecipTotal.setText("-- mm");
        tvSensorPressure.setText("-- hPa");
        tvSensorUv.setText("--");
    }

    private void setSensorNoLocation() {
        tvSensorCity.setText("Sin ubicación");
        tvSensorCondition.setText("--");
        ivSensorConditionIcon.setImageDrawable(null);

        tvSensorHumidity.setText("-- %");
        tvSensorTemperature.setText("-- °C");
        tvSensorRainChance.setText("-- %");
        tvSensorWind.setText("-- km/h");
        tvSensorPrecipNow.setText("-- mm");
        tvSensorPrecipTotal.setText("-- mm");
        tvSensorPressure.setText("-- hPa");
        tvSensorUv.setText("--");
    }

    // ------------------ MANEJO DE PERMISOS ------------------ //

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = false;
            if (grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result == PackageManager.PERMISSION_GRANTED) {
                        granted = true;
                        break;
                    }
                }
            }

            if (granted) {
                loadWeatherWithLocation();
            } else {
                setSensorNoLocation();
            }
        }
    }

    // ------------------ ALERTA PRINCIPAL ------------------ //

    private void updateLastAlert(List<ReportModel> reports) {
        if (reports == null || reports.isEmpty()) {
            tvAlertTitle.setText(R.string.home_no_alerts);
        } else {
            ReportModel last = reports.get(0);
            String text = last.getTipo() + " • Nivel " + last.getNivel();
            tvAlertTitle.setText(text);
        }
    }
}
