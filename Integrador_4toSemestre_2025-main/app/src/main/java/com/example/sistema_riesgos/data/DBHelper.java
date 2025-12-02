package com.example.sistema_riesgos.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.example.sistema_riesgos.models.RiskReportState;
import com.example.sistema_riesgos.models.UserModel;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "risk_guard.db";
    private static final int DB_VERSION = 2;

    // Tabla usuarios
    public static final String TABLE_USUARIOS = "usuarios";
    public static final String COL_USU_ID = "id";
    public static final String COL_USU_REMOTE_ID = "remote_id";
    public static final String COL_USU_NOMBRE = "nombre";
    public static final String COL_USU_CORREO = "correo";
    public static final String COL_USU_TELEFONO = "telefono";
    public static final String COL_USU_PASSWORD = "password";
    public static final String COL_USU_SYNC_STATUS = "sync_status";

    // Tabla registros
    public static final String TABLE_REGISTROS = "registros";
    public static final String COL_REG_ID = "id";
    public static final String COL_REG_REMOTE_ID = "remote_id";
    public static final String COL_REG_TIPO = "tipo";
    public static final String COL_REG_DESCRIPCION = "descripcion";
    public static final String COL_REG_LATITUD = "latitud";
    public static final String COL_REG_LONGITUD = "longitud";
    public static final String COL_REG_FECHA_MILLIS = "fecha_millis";
    public static final String COL_REG_ESTADO = "estado";
    public static final String COL_REG_NIVEL = "nivel";
    public static final String COL_REG_URL_FOTO = "url_foto";
    public static final String COL_REG_ID_USUARIO = "id_usuario";
    public static final String COL_REG_SYNC_STATUS = "sync_status";
    public static final String COL_REG_AI_CONFIDENCE = "ai_confidence";

    // Valores de sincronización
    public static final int SYNC_STATUS_PENDING = 0;
    public static final int SYNC_STATUS_SYNCED = 1;
    public static final int SYNC_STATUS_ERROR = 2;

    public DBHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String createUsuariosTable = "CREATE TABLE " + TABLE_USUARIOS + " ("
                + COL_USU_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USU_REMOTE_ID + " TEXT, "
                + COL_USU_NOMBRE + " TEXT NOT NULL, "
                + COL_USU_CORREO + " TEXT NOT NULL, "
                + COL_USU_TELEFONO + " TEXT NOT NULL, "
                + COL_USU_PASSWORD + " TEXT NOT NULL, "
                + COL_USU_SYNC_STATUS + " INTEGER NOT NULL DEFAULT " + SYNC_STATUS_PENDING
                + ");";

        String createRegistrosTable = "CREATE TABLE " + TABLE_REGISTROS + " ("
                + COL_REG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_REG_REMOTE_ID + " TEXT, "
                + COL_REG_TIPO + " TEXT NOT NULL, "
                + COL_REG_DESCRIPCION + " TEXT NOT NULL, "
                + COL_REG_LATITUD + " REAL NOT NULL, "
                + COL_REG_LONGITUD + " REAL NOT NULL, "
                + COL_REG_FECHA_MILLIS + " INTEGER NOT NULL, "
                + COL_REG_ESTADO + " TEXT NOT NULL, "
                + COL_REG_NIVEL + " TEXT, "
                + COL_REG_URL_FOTO + " TEXT, "
                + COL_REG_ID_USUARIO + " INTEGER NOT NULL, "
                + COL_REG_SYNC_STATUS + " INTEGER NOT NULL DEFAULT " + SYNC_STATUS_PENDING + ", "
                + COL_REG_AI_CONFIDENCE + " REAL DEFAULT 0.0"
                + ");";

        db.execSQL(createUsuariosTable);
        db.execSQL(createRegistrosTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REGISTROS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USUARIOS);
        onCreate(db);
    }

    // --------- OPERACIONES USUARIOS ---------

    public long insertUsuario(String nombre,
            String correo,
            String telefono,
            String password,
            int syncStatus) {

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USU_NOMBRE, nombre);
        values.put(COL_USU_CORREO, correo);
        values.put(COL_USU_TELEFONO, telefono);
        values.put(COL_USU_PASSWORD, password);
        values.put(COL_USU_SYNC_STATUS, syncStatus);

        return db.insert(TABLE_USUARIOS, null, values);
    }

    public void markUsuarioSynced(long localId, String remoteId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USU_REMOTE_ID, remoteId);
        values.put(COL_USU_SYNC_STATUS, SYNC_STATUS_SYNCED);

        db.update(TABLE_USUARIOS, values, COL_USU_ID + " = ?", new String[] { String.valueOf(localId) });
    }

    public Cursor getPendingUsuarios() {
        SQLiteDatabase db = getReadableDatabase();
        String selection = COL_USU_SYNC_STATUS + " = ?";
        String[] args = { String.valueOf(SYNC_STATUS_PENDING) };

        return db.query(
                TABLE_USUARIOS,
                null,
                selection,
                args,
                null,
                null,
                COL_USU_ID + " ASC");
    }

    public void markUsuarioSyncError(long localId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USU_SYNC_STATUS, SYNC_STATUS_ERROR);

        db.update(TABLE_USUARIOS, values, COL_USU_ID + " = ?", new String[] { String.valueOf(localId) });
    }

    // --------- OPERACIONES REGISTROS ---------

    public long insertRegistro(String tipo,
            String descripcion,
            double latitud,
            double longitud,
            long fechaMillis,
            String estado,
            String nivel,
            String urlFoto,
            long idUsuarioLocal,
            int syncStatus,
            double aiConfidence) {

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_REG_TIPO, tipo);
        values.put(COL_REG_DESCRIPCION, descripcion);
        values.put(COL_REG_LATITUD, latitud);
        values.put(COL_REG_LONGITUD, longitud);
        values.put(COL_REG_FECHA_MILLIS, fechaMillis);
        values.put(COL_REG_ESTADO, estado);
        values.put(COL_REG_NIVEL, nivel);
        values.put(COL_REG_URL_FOTO, urlFoto);
        values.put(COL_REG_ID_USUARIO, idUsuarioLocal);
        values.put(COL_REG_SYNC_STATUS, syncStatus);
        values.put(COL_REG_AI_CONFIDENCE, aiConfidence);

        return db.insert(TABLE_REGISTROS, null, values);
    }

    public Cursor getPendingRegistros() {
        SQLiteDatabase db = getReadableDatabase();
        String selection = COL_REG_SYNC_STATUS + " = ?";
        String[] args = { String.valueOf(SYNC_STATUS_PENDING) };

        return db.query(
                TABLE_REGISTROS,
                null,
                selection,
                args,
                null,
                null,
                COL_REG_FECHA_MILLIS + " ASC");
    }

    public void markRegistroSynced(long localId, String remoteId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_REG_REMOTE_ID, remoteId);
        values.put(COL_REG_SYNC_STATUS, SYNC_STATUS_SYNCED);
        values.put(COL_REG_ESTADO, "PROCESSED");

        db.update(TABLE_REGISTROS, values, COL_REG_ID + " = ?", new String[] { String.valueOf(localId) });
    }

    public void markRegistroSyncError(long localId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_REG_SYNC_STATUS, SYNC_STATUS_ERROR);
        values.put(COL_REG_ESTADO, RiskReportState.ERROR.toValue());

        db.update(TABLE_REGISTROS, values, COL_REG_ID + " = ?", new String[] { String.valueOf(localId) });
    }

    // --------- AUTH ---------

    public long validateUser(String email, String password) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = { COL_USU_ID };
        String selection = COL_USU_CORREO + " = ? AND " + COL_USU_PASSWORD + " = ?";
        String[] selectionArgs = { email, password };

        try (Cursor cursor = db.query(
                TABLE_USUARIOS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(COL_USU_ID));
            }
        }

        return -1;
    }

    public boolean checkUserExists(String email) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = { COL_USU_ID };
        String selection = COL_USU_CORREO + " = ?";
        String[] selectionArgs = { email };

        try (Cursor cursor = db.query(
                TABLE_USUARIOS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null)) {
            return cursor != null && cursor.getCount() > 0;
        }
    }

    public Cursor getUserById(long userId) {
        SQLiteDatabase db = getReadableDatabase();

        String selection = COL_USU_ID + " = ?";
        String[] selectionArgs = { String.valueOf(userId) };

        return db.query(
                TABLE_USUARIOS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null);
    }

    public Cursor getAllRegistros() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(
                TABLE_REGISTROS,
                null,
                null,
                null,
                null,
                null,
                COL_REG_FECHA_MILLIS + " DESC");
    }

    public int updateRegistro(long localId,
            String tipo,
            String descripcion,
            String estado,
            String nivel) {

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_REG_TIPO, tipo);
        values.put(COL_REG_DESCRIPCION, descripcion);
        values.put(COL_REG_ESTADO, estado);
        values.put(COL_REG_NIVEL, nivel);

        return db.update(
                TABLE_REGISTROS,
                values,
                COL_REG_ID + " = ?",
                new String[] { String.valueOf(localId) });
    }

    public int deleteRegistro(long localId) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(
                TABLE_REGISTROS,
                COL_REG_ID + " = ?",
                new String[] { String.valueOf(localId) });
    }

    // --------- NUEVOS HELPERS PARA LOGIN REMOTO ---------

    /**
     * Devuelve el id local de un usuario por correo, o -1 si no existe.
     */
    public long getUserIdByEmail(String email) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = { COL_USU_ID };
        String selection = COL_USU_CORREO + " = ?";
        String[] selectionArgs = { email };

        try (Cursor cursor = db.query(
                TABLE_USUARIOS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(COL_USU_ID));
            }
        }

        return -1;
    }

    /**
     * Inserta o actualiza un usuario proveniente del backend y devuelve su id
     * local.
     */
    public long upsertUsuarioFromRemote(UserModel user, int syncStatus) {
        if (user == null || user.getCorreo() == null) {
            return -1;
        }

        SQLiteDatabase db = getWritableDatabase();
        long existingId = getUserIdByEmail(user.getCorreo());

        ContentValues values = new ContentValues();
        values.put(COL_USU_NOMBRE, user.getNombre());
        values.put(COL_USU_CORREO, user.getCorreo());
        values.put(COL_USU_TELEFONO, user.getTelefono());
        values.put(COL_USU_PASSWORD, user.getPassword());
        values.put(COL_USU_REMOTE_ID, user.getId());
        values.put(COL_USU_SYNC_STATUS, syncStatus);

        if (existingId == -1) {
            long newId = db.insert(TABLE_USUARIOS, null, values);
            return newId;
        } else {
            db.update(TABLE_USUARIOS, values, COL_USU_ID + " = ?",
                    new String[] { String.valueOf(existingId) });
            return existingId;
        }
    }
}
