package net.epsilonlabs.datamanagementefficient.test;

import java.util.ArrayList;

import net.epsilonlabs.datamanagementefficient.exception.DatabaseNotOpenExpection;
import net.epsilonlabs.datamanagementefficient.user.DataManager;
import android.content.Context;

public class TestLib {

    private static final Object LOCK = new Object();
    private static DataManager database;
    private static boolean initialized;

    public static void init(final Context context) {        
        if (initialized) return;        
        database = DataManager.getInstance(context);
        initialized = true;
    }

    /**
     * 
     * This function is always called from other classes, sometimes is called
     * multiple times concurrently.
     * 
     */
    public static void refreshData(final Context context) {

        // This is always called.
        getClosestCode();

        // This might be called sometimes depending on the inputs.
        String some_new_code = "x";
        saveLocationCache(some_new_code);
    }

    /**
     * The crashes seemed to point here.
     */
    private static String getClosestCode() {
        String code = null;

        synchronized (LOCK) {
            database.open();
            ArrayList<LocationCache> rcdb = database.getAll(LocationCache.class);

            for (LocationCache lc : rcdb) {
                // Some process here
                code = lc.code;
            }

            try {
                database.close();
            } catch (DatabaseNotOpenExpection e) { }
        }

        return code;
    }

    /**
     * This function is called less frequently.
     */
    private static void saveLocationCache(String code) {
        LocationCache lc = new LocationCache();
        lc.code = code;

        synchronized (LOCK) {
            database.open();
            database.add(lc);
            database.close();
        }       
    }

}