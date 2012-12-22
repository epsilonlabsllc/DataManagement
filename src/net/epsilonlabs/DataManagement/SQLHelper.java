/*
DataManagement Android Library for Storing Objects Permanently
Copyright (C) 2012 Thomas Caputi

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

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
