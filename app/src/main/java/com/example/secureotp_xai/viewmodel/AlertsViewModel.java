package com.example.secureotp_xai.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.secureotp_xai.database.AlertEntity;
import com.example.secureotp_xai.database.AlertRepository;
import java.util.List;

public class AlertsViewModel extends AndroidViewModel {

    private AlertRepository repository;
    private final LiveData<List<AlertEntity>> allAlerts;

    public AlertsViewModel(Application application) {
        super(application);
        repository = new AlertRepository(application);
        allAlerts = repository.getAllAlerts();
    }

    public LiveData<List<AlertEntity>> getAllAlerts() { return allAlerts; }
    public void insert(AlertEntity alert) { repository.insert(alert); }
    public void markAsRead(int alertId) { repository.markAsRead(alertId); }
    public void markAllAsRead() { repository.markAllAsRead(); }
    public void clearHistory() { repository.clearHistory(); }
}
