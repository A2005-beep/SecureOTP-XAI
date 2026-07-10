package com.example.secureotp_xai.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// 1. Ensure version is bumped to 2 (since we added AI fields)
// 2. Ensure AlertEntity.class is listed here
@Database(entities = {AlertEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract AlertDao alertDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "secure_otp_alerts.db")
                            // 💡 This is critical: it wipes the old database
                            // and recreates it with the new AI fields.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
