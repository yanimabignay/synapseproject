package com.hm.synapse;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SynapseBlockEntity.class, UserEntity.class}, version = 5)
public abstract class SynapseDatabase extends RoomDatabase {
    public abstract SynapseDao synapseDao();
    public abstract UserDao userDao();

    private static volatile SynapseDatabase INSTANCE;

    public static SynapseDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (SynapseDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            SynapseDatabase.class, "synapse_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
