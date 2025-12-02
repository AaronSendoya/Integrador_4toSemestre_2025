package com.example.sistema_riesgos.data;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import com.example.sistema_riesgos.models.ReportModel;
import com.example.sistema_riesgos.models.RiskAnalysisResponse;
import com.example.sistema_riesgos.models.RiskReportState;
import com.example.sistema_riesgos.models.UserModel;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiRepository {

    private static final String BASE_URL_BACKEND = "https://backend-mongodb-integrator.onrender.com/api/";
    private static final String BASE_URL_WEATHER = "https://api.weatherapi.com/v1/";
    private static final String WEATHER_API_KEY = "b4a4daa6242a4070866121153252011";

    private static final String TAG = "ApiRepository";

    private static ApiRepository instance;

    private final RiskGuardApi riskGuardApi;
    private final WeatherApiService weatherApiService;
    private final DBHelper dbHelper;
    private final Context appContext;

    private ApiRepository(Context context) {
        this.appContext = context.getApplicationContext();

        OkHttpClient client = new OkHttpClient.Builder().build();

        Retrofit backendRetrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL_BACKEND)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Retrofit retrofitWeather = new Retrofit.Builder()
                .baseUrl(BASE_URL_WEATHER)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        riskGuardApi = backendRetrofit.create(RiskGuardApi.class);
        weatherApiService = retrofitWeather.create(WeatherApiService.class);
        dbHelper = new DBHelper(appContext);
    }

    public static synchronized ApiRepository getInstance(Context context) {
        if (instance == null) {
            instance = new ApiRepository(context);
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Helper de conectividad
    // -------------------------------------------------------------------------

    public boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                Log.w(TAG, "ConnectivityManager es null, asumimos online.");
                return true;
            }

            Network network = cm.getActiveNetwork();
            if (network == null) {
                Log.d(TAG, "isOnline(): no hay activeNetwork");
                return false;
            }

            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps == null) {
                Log.d(TAG, "isOnline(): no hay NetworkCapabilities");
                return false;
            }

            boolean result = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);

            Log.d(TAG, "isOnline(): " + result);
            return result;
        } catch (Exception e) {
            Log.w(TAG, "isOnline() lanzó excepción, asumimos online", e);
            return true;
        }
    }

    // -------------------------------------------------------------------------
    // USUARIOS (MongoDB)
    // -------------------------------------------------------------------------

    public Call<UserModel> createRemoteUser(UserModel user) {
        return riskGuardApi.createUser(user);
    }

    /**
     * Sincroniza usuarios con sync_status = PENDING hacia el backend.
     */
    public void syncPendingUsers() {
        if (!isOnline()) {
            Log.d(TAG, "syncPendingUsers(): sin conexión, no se intenta sincronizar.");
            return;
        }

        Cursor cursor = dbHelper.getPendingUsuarios();
        if (cursor == null) {
            Log.d(TAG, "syncPendingUsers(): cursor null.");
            return;
        }

        try {
            int count = cursor.getCount();
            Log.d(TAG, "syncPendingUsers(): usuarios pendientes = " + count);
            if (count == 0)
                return;

            int idxId = cursor.getColumnIndexOrThrow(DBHelper.COL_USU_ID);
            int idxNombre = cursor.getColumnIndexOrThrow(DBHelper.COL_USU_NOMBRE);
            int idxCorreo = cursor.getColumnIndexOrThrow(DBHelper.COL_USU_CORREO);
            int idxTelefono = cursor.getColumnIndexOrThrow(DBHelper.COL_USU_TELEFONO);
            int idxPassword = cursor.getColumnIndexOrThrow(DBHelper.COL_USU_PASSWORD);

            while (cursor.moveToNext()) {
                final long localId = cursor.getLong(idxId);
                String nombre = cursor.getString(idxNombre);
                String correo = cursor.getString(idxCorreo);
                String telefono = cursor.getString(idxTelefono);
                String password = cursor.getString(idxPassword);

                UserModel user = new UserModel();
                user.setLocalId(localId);
                user.setNombre(nombre);
                user.setCorreo(correo);
                user.setTelefono(telefono);
                user.setPassword(password);

                Log.d(TAG, "syncPendingUsers(): enviando usuario localId=" + localId
                        + " correo=" + correo);

                createRemoteUser(user).enqueue(new Callback<UserModel>() {
                    @Override
                    public void onResponse(Call<UserModel> call, Response<UserModel> response) {
                        Log.d(TAG, "syncPendingUsers() onResponse code=" + response.code());
                        if (response.isSuccessful() && response.body() != null) {
                            String remoteId = response.body().getId();
                            Log.d(TAG, "Sync usuario OK localId=" + localId + " remoteId=" + remoteId);
                            dbHelper.markUsuarioSynced(localId, remoteId);
                        } else {
                            String errorBody = null;
                            try {
                                if (response.errorBody() != null) {
                                    errorBody = response.errorBody().string();
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Error leyendo errorBody usuario", e);
                            }
                            Log.w(TAG, "Sync usuario ERROR (HTTP " + response.code()
                                    + ") localId=" + localId + " body=" + errorBody);
                            dbHelper.markUsuarioSyncError(localId);
                        }
                    }

                    @Override
                    public void onFailure(Call<UserModel> call, Throwable t) {
                        Log.e(TAG, "Sync usuario FAILURE localId=" + localId + " -> " + t.getMessage(), t);
                        dbHelper.markUsuarioSyncError(localId);
                    }
                });
            }
        } finally {
            cursor.close();
        }
    }

    // -------------------------------------------------------------------------
    // REPORTES (MongoDB)
    // -------------------------------------------------------------------------

    public Call<ReportModel> createRemoteReport(ReportModel report) {
        return riskGuardApi.createReport(report);
    }

    public Call<List<ReportModel>> getAllRemoteReports() {
        return riskGuardApi.getAllReports();
    }

    public Call<List<ReportModel>> getRemoteReportsByUser(String userId) {
        return riskGuardApi.getReportsByUser(userId);
    }

    /**
     * Sincroniza reportes pendientes (SQLite -> backend).
     */
    public void syncPendingReports() {
        if (!isOnline()) {
            Log.d(TAG, "syncPendingReports(): sin conexión, no se intenta sincronizar.");
            return;
        }

        Cursor cursor = dbHelper.getPendingRegistros();
        if (cursor == null) {
            Log.d(TAG, "syncPendingReports(): cursor null.");
            return;
        }

        try {
            int count = cursor.getCount();
            Log.d(TAG, "syncPendingReports(): registros pendientes = " + count);
            if (count == 0)
                return;

            int idxId = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_ID);
            int idxTipo = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_TIPO);
            int idxDesc = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_DESCRIPCION);
            int idxLat = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_LATITUD);
            int idxLon = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_LONGITUD);
            int idxFecha = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_FECHA_MILLIS);
            int idxNivel = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_NIVEL);
            int idxUrlFoto = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_URL_FOTO);
            int idxIdUsuarioLocal = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_ID_USUARIO);
            int idxAiConfidence = cursor.getColumnIndex(DBHelper.COL_REG_AI_CONFIDENCE);

            while (cursor.moveToNext()) {
                final long localId = cursor.getLong(idxId);
                String tipo = cursor.getString(idxTipo);
                String descripcion = cursor.getString(idxDesc);
                double latitud = cursor.getDouble(idxLat);
                double longitud = cursor.getDouble(idxLon);
                long fechaMillis = cursor.getLong(idxFecha);
                String nivel = cursor.getString(idxNivel);
                String urlFoto = cursor.getString(idxUrlFoto);
                long idUsuarioLocal = cursor.getLong(idxIdUsuarioLocal);
                double aiConfidence = (idxAiConfidence != -1) ? cursor.getDouble(idxAiConfidence) : 0.0;

                ReportModel report = new ReportModel();
                report.setTipo(tipo);
                report.setDescripcion(descripcion);
                report.setLatitud(latitud);
                report.setLongitud(longitud);
                report.setFecha(null); // la pone el backend
                report.setEstado(RiskReportState.PROCESSED.toValue());
                report.setNivel(nivel);
                report.setUrlFoto(urlFoto);
                report.setIdUsuario(String.valueOf(idUsuarioLocal));
                report.setAiConfidence(aiConfidence);

                Log.d(TAG, "Sync reporte: localId=" + localId + " tipo=" + tipo
                        + " fechaMillisLocal=" + fechaMillis);

                createRemoteReport(report).enqueue(new Callback<ReportModel>() {
                    @Override
                    public void onResponse(Call<ReportModel> call, Response<ReportModel> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String remoteId = response.body().getId();
                            Log.d(TAG, "Sync reporte OK localId=" + localId + " remoteId=" + remoteId);
                            dbHelper.markRegistroSynced(localId, remoteId);
                        } else {
                            String errorBody = null;
                            try {
                                if (response.errorBody() != null) {
                                    errorBody = response.errorBody().string();
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Error leyendo errorBody reporte", e);
                            }
                            Log.w(TAG, "Sync reporte ERROR (HTTP " + response.code()
                                    + ") localId=" + localId + " body=" + errorBody);
                            dbHelper.markRegistroSyncError(localId);
                        }
                    }

                    @Override
                    public void onFailure(Call<ReportModel> call, Throwable t) {
                        Log.e(TAG, "Sync reporte FAILURE localId=" + localId + " -> " + t.getMessage(), t);
                        dbHelper.markRegistroSyncError(localId);
                    }
                });
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Devuelve los reportes almacenados en SQLite (historial local).
     */
    public List<ReportModel> getLocalReports() {
        List<ReportModel> result = new ArrayList<>();

        Cursor cursor = dbHelper.getAllRegistros();
        if (cursor == null)
            return result;

        try {
            int idxIdLocal = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_ID);
            int idxRemoteId = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_REMOTE_ID);
            int idxTipo = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_TIPO);
            int idxDesc = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_DESCRIPCION);
            int idxLat = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_LATITUD);
            int idxLon = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_LONGITUD);
            int idxFecha = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_FECHA_MILLIS);
            int idxEstado = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_ESTADO);
            int idxNivel = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_NIVEL);
            int idxUrlFoto = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_URL_FOTO);
            int idxIdUsuarioLocal = cursor.getColumnIndexOrThrow(DBHelper.COL_REG_ID_USUARIO);
            int idxAiConfidence = cursor.getColumnIndex(DBHelper.COL_REG_AI_CONFIDENCE);

            while (cursor.moveToNext()) {
                long localId = cursor.getLong(idxIdLocal);
                String remoteId = cursor.getString(idxRemoteId);
                String tipo = cursor.getString(idxTipo);
                String descripcion = cursor.getString(idxDesc);
                double latitud = cursor.getDouble(idxLat);
                double longitud = cursor.getDouble(idxLon);
                long fechaMillis = cursor.getLong(idxFecha);
                String estado = cursor.getString(idxEstado);
                String nivel = cursor.getString(idxNivel);
                String urlFoto = cursor.getString(idxUrlFoto);
                long idUsuarioLocal = cursor.getLong(idxIdUsuarioLocal);
                double aiConfidence = (idxAiConfidence != -1) ? cursor.getDouble(idxAiConfidence) : 0.0;

                ReportModel report = new ReportModel();
                report.setLocalId(localId);
                report.setId(remoteId); // puede ser null si no está sync
                report.setTipo(tipo);
                report.setDescripcion(descripcion);
                report.setLatitud(latitud);
                report.setLongitud(longitud);
                report.setFecha(new Date(fechaMillis));
                report.setEstado(estado);
                report.setNivel(nivel);
                report.setUrlFoto(urlFoto);
                report.setIdUsuario(String.valueOf(idUsuarioLocal));
                report.setAiConfidence(aiConfidence);

                result.add(report);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public void updateRemoteReport(ReportModel report) {
        if (report.getId() == null || report.getId().isEmpty()) {
            return; // aún no está en Mongo
        }
        if (!isOnline()) {
            Log.d(TAG, "updateRemoteReport(): sin conexión, se omite.");
            return;
        }

        riskGuardApi.updateReport(report.getId(), report)
                .enqueue(new Callback<ReportModel>() {
                    @Override
                    public void onResponse(Call<ReportModel> call, Response<ReportModel> response) {
                        Log.d(TAG, "updateRemoteReport() onResponse code=" + response.code());
                    }

                    @Override
                    public void onFailure(Call<ReportModel> call, Throwable t) {
                        Log.e(TAG, "updateRemoteReport() onFailure: " + t.getMessage(), t);
                    }
                });
    }

    /**
     * Elimina un reporte en el backend (si ya tiene id remoto).
     */
    public void deleteRemoteReport(ReportModel report) {
        if (report.getId() == null || report.getId().isEmpty()) {
            return; // sólo local
        }
        if (!isOnline()) {
            Log.d(TAG, "deleteRemoteReport(): sin conexión, se omite.");
            return;
        }

        riskGuardApi.deleteReport(report.getId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        Log.d(TAG, "deleteRemoteReport() onResponse code=" + response.code());
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "deleteRemoteReport() onFailure: " + t.getMessage(), t);
                    }
                });
    }

    // --------- LOGIN REMOTO USUARIO BÁSICO ---------

    public Call<UserModel> loginRemote(UserModel credenciales) {
        return riskGuardApi.login(credenciales);
    }

    // --------- LOGIN HÍBRIDO (REMOTO + FALLBACK LOCAL) ---------

    public interface LoginCallback {
        void onSuccess(long localUserId, String email);

        void onError(String message);
    }

    public void loginUserHybrid(final String email,
            final String password,
            final LoginCallback callback) {

        // Si parece que hay conexión, intentamos primero contra el backend
        if (isOnline()) {
            UserModel cred = new UserModel();
            cred.setCorreo(email);
            cred.setPassword(password);

            loginRemote(cred).enqueue(new Callback<UserModel>() {
                @Override
                public void onResponse(Call<UserModel> call, Response<UserModel> response) {
                    Log.d(TAG, "loginUserHybrid() remote code=" + response.code());

                    if (response.isSuccessful() && response.body() != null) {
                        // ✅ Login remoto OK: sincronizamos usuario en SQLite
                        UserModel remoteUser = response.body();

                        long localId = dbHelper.upsertUsuarioFromRemote(
                                remoteUser,
                                DBHelper.SYNC_STATUS_SYNCED);

                        if (localId != -1) {
                            callback.onSuccess(localId, remoteUser.getCorreo());
                        } else {
                            // Algo raro al guardar en SQLite -> fallback local
                            fallbackLocalLogin(email, password, "Error al guardar usuario local", callback);
                        }
                    } else if (response.code() == 401) {
                        // Credenciales inválidas en backend -> probamos local por si está
                        // desactualizado
                        fallbackLocalLogin(email, password,
                                "Correo o contraseña incorrectos", callback);
                    } else {
                        // Otros errores de servidor -> fallback local
                        fallbackLocalLogin(email, password,
                                "Error del servidor. Probando autenticación local…", callback);
                    }
                }

                @Override
                public void onFailure(Call<UserModel> call, Throwable t) {
                    Log.e(TAG, "loginUserHybrid() onFailure: " + t.getMessage(), t);
                    // Error de red -> usamos solo login local
                    fallbackLocalLogin(email, password,
                            "Sin conexión estable. Probando autenticación local…", callback);
                }
            });
        } else {
            // Sin conexión: solo login local
            fallbackLocalLogin(email, password,
                    "Sin conexión. Usando autenticación local.", callback);
        }
    }

    /**
     * Intenta validar credenciales contra SQLite.
     */
    private void fallbackLocalLogin(String email,
            String password,
            String baseMessage,
            LoginCallback callback) {

        long localId = dbHelper.validateUser(email, password);
        if (localId != -1) {
            callback.onSuccess(localId, email);
        } else {
            callback.onError(baseMessage != null ? baseMessage
                    : "Correo o contraseña incorrectos");
        }
    }

    // ---------- Helpers para actualizar / borrar reportes en SQLite ----------

    public int updateLocalReport(ReportModel report) {
        if (report == null)
            return 0;
        return dbHelper.updateRegistro(
                report.getLocalId(),
                report.getTipo(),
                report.getDescripcion(),
                report.getEstado(),
                report.getNivel());
    }

    public int deleteLocalReport(long localId) {
        return dbHelper.deleteRegistro(localId);
    }

    public Call<JsonObject> getCurrentWeather(double lat, double lon) {
        String query = lat + "," + lon;
        return weatherApiService.getForecastByCoords(
                WEATHER_API_KEY,
                query,
                1);
    }

    public DBHelper getDbHelper() {
        return dbHelper;
    }

    // -------------------------------------------------------------------------
    // IA ANALYSIS
    // -------------------------------------------------------------------------

    public void analyzeImage(File imageFile, Callback<RiskAnalysisResponse> callback) {
        if (!isOnline()) {
            // Si no hay red, fallamos rápido o notificamos error
            callback.onFailure(null, new IOException("No internet connection"));
            return;
        }

        // Crear RequestBody para el archivo
        RequestBody requestFile = RequestBody.create(
                MediaType.parse("image/*"),
                imageFile);

        // Crear MultipartBody.Part
        MultipartBody.Part body = MultipartBody.Part.createFormData(
                "image",
                imageFile.getName(),
                requestFile);

        riskGuardApi.analyzeRiskImage(body).enqueue(callback);
    }
}
