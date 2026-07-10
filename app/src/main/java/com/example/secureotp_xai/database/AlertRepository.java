package com.example.secureotp_xai.database;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlertRepository {

    private AlertDao alertDao;
    private LiveData<List<AlertEntity>> allAlerts;

    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public AlertRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        alertDao = db.alertDao();
        allAlerts = alertDao.getAllAlerts();
    }

    public LiveData<List<AlertEntity>> getAllAlerts() { return allAlerts; }

    public void insert(AlertEntity alert) {
        databaseWriteExecutor.execute(() -> alertDao.insertAlert(alert));
    }

    public void markAsRead(int alertId) {
        databaseWriteExecutor.execute(() -> alertDao.markAsRead(alertId));
    }

    public void markAllAsRead() {
        databaseWriteExecutor.execute(() -> alertDao.markAllAsRead());
    }

    public void clearHistory() {
        databaseWriteExecutor.execute(() -> alertDao.clearHistory());
    }
}
