package net.epsilonlabs.datamanagementefficient.library;

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
	
	public static final String DATABASE_NAME = "Database";
	public static final int DATABASE_VERSION = 1;
	
	public SQLHelper(Context context){
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

}
