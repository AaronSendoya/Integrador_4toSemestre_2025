package com.example.sistema_riesgos.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.sistema_riesgos.R;
import com.example.sistema_riesgos.models.ReportModel;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.List;

public class MapViewFragment extends Fragment {

    private static final double DEFAULT_LAT = -17.7833;
    private static final double DEFAULT_LON = -63.1821;
    private static final double DEFAULT_ZOOM = 12.0;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    private MapView mapView;
    private SharedRiskViewModel viewModel;
    private MyLocationNewOverlay myLocationOverlay;
    private ImageButton btnMyLocation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configuración osmdroid
        Configuration.getInstance().load(
                requireContext().getApplicationContext(),
                requireContext().getSharedPreferences("osmdroid", 0)
        );

        mapView = view.findViewById(R.id.map);
        btnMyLocation = view.findViewById(R.id.btn_my_location);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        GeoPoint startPoint = new GeoPoint(DEFAULT_LAT, DEFAULT_LON);
        mapView.getController().setZoom(DEFAULT_ZOOM);
        mapView.getController().setCenter(startPoint);

        // Overlay de mi ubicación
        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(requireContext()),
                mapView
        );
        mapView.getOverlays().add(myLocationOverlay);

        // ViewModel compartido
        viewModel = new ViewModelProvider(requireActivity())
                .get(SharedRiskViewModel.class);

        viewModel.getReports().observe(getViewLifecycleOwner(), this::updateMarkers);

        // Botón "ir a mi ubicación"
        btnMyLocation.setOnClickListener(v -> centerOnMyLocation());
    }

    private void updateMarkers(List<ReportModel> reports) {
        if (mapView == null) return;

        mapView.getOverlays().removeIf(overlay -> overlay instanceof Marker);

        if (reports == null || reports.isEmpty()) {
            mapView.invalidate();
            return;
        }

        for (ReportModel r : reports) {
            GeoPoint pos = new GeoPoint(r.getLatitud(), r.getLongitud());
            Marker marker = new Marker(mapView);
            marker.setPosition(pos);
            marker.setTitle(r.getTipo());
            marker.setSubDescription("Nivel: " + r.getNivel());
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(marker);
        }

        mapView.invalidate();
    }

    private void centerOnMyLocation() {
        // Permisos de ubicación
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
            return;
        }

        // Activar overlay si no está activo
        myLocationOverlay.enableMyLocation();

        GeoPoint myLoc = myLocationOverlay.getMyLocation();
        if (myLoc != null) {
            mapView.getController().animateTo(myLoc);
            mapView.getController().setZoom(16.0);
        } else {
            Toast.makeText(requireContext(),
                    "Obteniendo ubicación...",
                    Toast.LENGTH_SHORT).show();

            myLocationOverlay.runOnFirstFix(() -> {
                GeoPoint firstFix = myLocationOverlay.getMyLocation();
                if (firstFix != null) {
                    requireActivity().runOnUiThread(() -> {
                        mapView.getController().animateTo(firstFix);
                        mapView.getController().setZoom(16.0);
                    });
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                centerOnMyLocation();
            } else {
                Toast.makeText(requireContext(),
                        "Permiso de ubicación denegado",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation();
        }
    }

    @Override
    public void onPause() {
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }
}
