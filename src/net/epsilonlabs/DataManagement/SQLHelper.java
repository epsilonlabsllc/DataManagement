package net.epsilonlabs.DataManagement;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Extension of the abstract class SQLiteOpenHelper. onCreate() is not used because DataManager already defines when tables should be created.
 * onUpgrade not used in current implementation.
 * @author Tom Caputi
 *
 */
public class SQLHelper extends SQLiteOpenHelper {
	
	public SQLHelper(Context context, String dbName, int dbVersion){
		super(context, dbName, null, dbVersion);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

}
