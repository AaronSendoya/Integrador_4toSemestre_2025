package com.example.sistema_riesgos.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

/**
 * Worker que sincroniza periódicamente:
 *  - usuarios pendientes (SQLite -> Mongo)
 *  - reportes pendientes (SQLite -> Mongo)
 */
public class SyncWorker extends Worker {

    public static final String UNIQUE_WORK_NAME = "RiskGuardPeriodicSync";

    public SyncWorker(@NonNull Context context,
                      @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        ApiRepository repo = ApiRepository.getInstance(getApplicationContext());

        // Sincroniza usuarios y reportes
        repo.syncPendingUsers();
        repo.syncPendingReports();

        return Result.success();
    }

    /**
     * Programa un trabajo periódico cada 15 minutos
     * (mínimo que permite WorkManager) SOLO cuando haya red.
     */
    public static void enqueuePeriodicSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                15, TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(
                        UNIQUE_WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP, // no duplicar trabajos
                        request
                );
    }
}
