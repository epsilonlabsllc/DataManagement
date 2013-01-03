package net.epsilonlabs.datamanagementefficient.debug;

import java.util.ArrayList;

import android.database.Cursor;

import net.epsilonlabs.datamanagementefficient.exception.DatabaseNotOpenExpection;
import net.epsilonlabs.datamanagementefficient.library.Cache;
import net.epsilonlabs.datamanagementefficient.library.DataManager;

public class Debugger {

	public DataManager dm;

	public Debugger(DataManager dm){
		this.dm = dm;
	}

	public String printTable(ArrayList<Class<?>> classes){
		if(!dm.isOpen()) throw new DatabaseNotOpenExpection();
		String tableString = "";

		for(Class<?> cls : classes){
			tableString += cls.getSimpleName() + "\n";
			tableString += "================================\n";

			Cursor cursor = dm.getDb().query(cls.getSimpleName(), null, null, null, null, null, null);
			cursor.moveToFirst();

			for(int i=0; i<cursor.getColumnCount(); i++){
				tableString += cursor.getColumnNames()[i] + "   ";
			}

			tableString += "\n--------------------------------\n";

			while(!cursor.isAfterLast()){
				for(int i=0; i<cursor.getColumnCount(); i++){
					switch(cursor.getType(i)){
					case Cursor.FIELD_TYPE_NULL:
						tableString += "null" + "   ";
						break;
					case Cursor.FIELD_TYPE_FLOAT:
						tableString += cursor.getFloat(i) + "   ";
						break;
					case Cursor.FIELD_TYPE_INTEGER:
						tableString += cursor.getInt(i) + "   ";
						break;
					case Cursor.FIELD_TYPE_STRING:
						tableString += cursor.getString(i) + "   ";
						break;
					}
				}
				tableString += "\n";
				cursor.moveToNext();
			}
		}

		return tableString;
	}

	public String printCache(){
		if(!dm.isOpen()) throw new DatabaseNotOpenExpection();
		Cache cache = dm.getPc().getCache();
		String cacheString = "";
		ArrayList<Object> allObjects = cache.getAllCachedObjects();
		for(Object obj : allObjects){
			cacheString += obj.getClass().getSimpleName() + "\n";
			cacheString += obj.toString() + "\n\n";
		}
		return cacheString;
	}
}
