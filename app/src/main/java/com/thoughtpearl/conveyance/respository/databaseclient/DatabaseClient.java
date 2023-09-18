package com.thoughtpearl.conveyance.respository.databaseclient;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.thoughtpearl.conveyance.respository.database.TripDatabase;

public class DatabaseClient {
    private static DatabaseClient mInstance;
    private static final String DB_NAME = "TrackerApp";

    //our app database object
    private TripDatabase tripDatabase;

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE TripRecord "
                    +"ADD COLUMN reimbursementCost TEXT Default NULL");
            database.execSQL("ALTER TABLE TripRecord "
                    +"ADD COLUMN sanctionDistance TEXT Default NULL");
        }
    };

    private DatabaseClient(Context mCtx) {
        //creating the app database with Room database builder
        //MyToDos is the name of the database
        tripDatabase = Room.databaseBuilder(mCtx, TripDatabase.class, DB_NAME)
                .addMigrations(MIGRATION_1_2)
                .build();

    }

    public static synchronized DatabaseClient getInstance(Context mCtx) {
        if (mInstance == null) {
            synchronized (DatabaseClient.class) {
                if (mInstance == null) {
                    mInstance = new DatabaseClient(mCtx);
                }
            }
        }
        return mInstance;
    }

    public TripDatabase getTripDatabase() {
        return tripDatabase;
    }
}
