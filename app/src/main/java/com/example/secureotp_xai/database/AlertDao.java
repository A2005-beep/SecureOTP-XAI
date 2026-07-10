package com.example.secureotp_xai.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AlertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAlert(AlertEntity alert);

    @Query("SELECT * FROM alerts_table ORDER BY timestamp DESC")
    LiveData<List<AlertEntity>> getAllAlerts();

    @Query("UPDATE alerts_table SET isRead = 1 WHERE id = :alertId")
    void markAsRead(int alertId);

    @Query("UPDATE alerts_table SET isRead = 1")
    void markAllAsRead();

    // 🟢 CHANGED NAME TO clearHistory() TO MATCH YOUR REPOSITORY
    @Query("DELETE FROM alerts_table")
    void clearHistory();

    @Query("SELECT * FROM alerts_table WHERE id = :alertId")
    AlertEntity getAlertById(int alertId);
}
