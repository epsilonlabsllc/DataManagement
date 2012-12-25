package net.epsilonlabs.DataManagement.library;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * This class acts as the user interface for the DataManagement library. It allows the user to safely access the database to perform add, update, delete, and get operations
 * and extensions thereof.
 * @author Tom Caputi
 *
 */
public class DataManager {
	
	private SQLHelper helper;
	private SQLiteDatabase db;
	private HashMap<Class<?>, Table<?>> tables;

	/**
	 * Constructor for the DataManager class.
	 * @param context the context calling the database
	 */
	public DataManager(Context context){
		this.helper = new SQLHelper(context);
		this.tables = new HashMap<Class<?>, Table<?>>();
	}
	
	/**
	 * Opens the database for reading and writing
	 */
	public void open(){
		db = helper.getWritableDatabase();
	}
	
	/**
	 * Closes the database
	 */
	public void close(){
		db.close();
	}
	
	/**
	 * Returns a Table for a given class from the tables HashMap. If no table exists, it creates one.
	 * @param cls the class for which a table is needed.
	 * @return a table representing a given class to be stored in the database
	 */
	private <T> Table<?> getTable(Class<T> cls){
		Table<?> newTable = (Table<?>) tables.get(cls);
		if(newTable == null){
			newTable = new Table<T>(cls, db);
			tables.put(cls, newTable);
		}
		return newTable;
	}
	
	/**
	 * Adds an object to the database.
	 * @param obj the object to be stored
	 * @return the objects id number in the database as an int
	 */
	@SuppressWarnings("unchecked")
	public <T> int add(T obj){
		Table<T> table = (Table<T>) getTable(obj.getClass());
		return table.add(obj, 0, db);
	}
	
	/**
	 * Gets an object from the database. If an object with the specified id number does not exist, null is returned.
	 * @param cls the class of the object which is needed from the database
	 * @param id the id number of the Object needed
	 * @return the object or null
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> cls, int id){
		Table<T> table = (Table<T>) getTable(cls);
		Cursor c = db.query(table.getTableName(), null, DataUtil.getIdField(cls).getName() + " = " + String.valueOf(id) , null, null, null, null);
		if(!c.moveToFirst()) return null;
		return table.get(c, db);
	}
	
	/**
	 * Gets every object of a given class from the database. Returns an empty Collection if none are found.
	 * @param cls The class of objects wanted
	 * @return an Collection of all objects of a given class (empty if none are found)
	 */
	@SuppressWarnings("unchecked")
	public <T> Collection<T> getAll(Class<T> cls){
		Table<T> table = (Table<T>) getTable(cls);
		Cursor c = db.query(table.getTableName(), null, null , null, null, null, null);
		if(!c.moveToFirst()) return new ArrayList<T>();
		return getMultiple(table, c);
	}
	
	/**
	 * Finds all object that have a given value for a given field and returns them in a Collection. Returns an empty Collection if none are found.
	 * @param cls the class of objects to search
	 * @param fieldName the name of the field to search in
	 * @param value the value that is be matched
	 * @return a Collection of all objects that have a field of fieldName with a value of value (empty if none are found)
	 */
	@SuppressWarnings("unchecked")
	public <T> Collection<T> find(Class<T> cls, String fieldName, int value){
		Table<T> table = (Table<T>) getTable(cls);
		Cursor c = db.query(table.getTableName(), null, fieldName + " = " + String.valueOf(value), null, null, null, null);
		if(!c.moveToFirst()) return new ArrayList<T>();
		return getMultiple(table, c);
	}

	/**
	 * Finds all object that have a given value for a given field and returns them in a Collection. Returns an empty Collection if none are found.
	 * @param cls the class of objects to search
	 * @param fieldName the name of the field to search in
	 * @param value the value that is be matched
	 * @return a Collection of all objects that have a field of fieldName with a value of value (empty if none are found)
	 */
	@SuppressWarnings("unchecked")
	public <T> Collection<T> find(Class<T> cls, String fieldName, double value){
		Table<T> table = (Table<T>) getTable(cls);
		Cursor c = db.query(table.getTableName(), null, fieldName + " = " + String.valueOf(value), null, null, null, null);
		if(!c.moveToFirst()) return new ArrayList<T>();
		return getMultiple(table, c);
	}
	
	/**
	 * Finds all object that have a given value for a given field and returns them in a Collection. Returns an empty Collection if none are found.
	 * @param cls the class of objects to search
	 * @param fieldName the name of the field to search in
	 * @param value the value that is be matched
	 * @return a Collection of all objects that have a field of fieldName with a value of value (empty if none are found)
	 */
	@SuppressWarnings("unchecked")
	public <T> Collection<T> find(Class<T> cls, String fieldName, float value){
		Table<T> table = (Table<T>) getTable(cls);
		Cursor c = db.query(table.getTableName(), null, fieldName + " = " + String.valueOf(value), null, null, null, null);
		if(!c.moveToFirst()) return new ArrayList<T>();
		return getMultiple(table, c);
	}
	
	/**
	 * Finds all object that have a given value for a given field and returns them in an Collection. Returns an empty Collection if none are found.
	 * @param cls the class of objects to search
	 * @param fieldName the name of the field to search in
	 * @param value the value that is be matched
	 * @return a Collection of all objects that have a field of fieldName with a value of value (empty if none are found)
	 */
	@SuppressWarnings("unchecked")
	public <T> Collection<T> find(Class<T> cls, String fieldName, long value){
		Table<T> table = (Table<T>) getTable(cls);
		Cursor c = db.query(table.getTableName(), null, fieldName + " = " + String.valueOf(value), null, null, null, null);
		if(!c.moveToFirst()) return new ArrayList<T>();
		return getMultiple(table, c);
	}
	
	/**
	 * Finds all object that have a given value for a given field and returns them in a Collection. Returns an empty Collection if none are found.
	 * @param cls the class of objects to search
	 * @param fieldName the name of the field to search in
	 * @param value the value that is be matched
	 * @return a Collection of all objects that have a field of fieldName with a value of value (empty if none are found)
	 */
	@SuppressWarnings("unchecked")
	public <T> Collection<T> find(Class<T> cls, String fieldName, String value){
		Table<T> table = (Table<T>) getTable(cls);
		Cursor c = db.query(table.getTableName(), null, fieldName + " = " + value, null, null, null, null);
		if(!c.moveToFirst()) return new ArrayList<T>();
		return getMultiple(table, c);
	}
	
	/**
	 * Finds all object that have a given value for a given field and returns them in a Collection. Returns an empty Collection if none are found.
	 * @param cls the class of objects to search
	 * @param fieldName the name of the field to search in
	 * @param value the value that is be matched
	 * @return a Collection of all objects that have a field of fieldName with a value of value (empty if none are found)
	 */
	@SuppressWarnings("unchecked")
	public <T> Collection<T> find(Class<T> cls, String fieldName, boolean value){
		Table<T> table = (Table<T>) getTable(cls);
		String valueString;
		if(value) valueString = String.valueOf(1);
		else valueString = String.valueOf(0);
		Cursor c = db.query(table.getTableName(), null, fieldName + " = " + valueString, null, null, null, null);
		if(!c.moveToFirst()) return new ArrayList<T>();
		return getMultiple(table, c);
	}
	
	/**
	 * Convenience method for getting all rows of a Cursor and adding them into an ArrayList. An empty ArrayList is returned if no objects are found.
	 * @param table the table containing the to be queried
	 * @param c the cursor containing query result rows
	 * @return an ArrayList of all objects found in the Cursor (empty if none are found)
	 */
	private <T> ArrayList<T> getMultiple(Table<T> table, Cursor c){
		ArrayList<T> list = new ArrayList<T>();
		while(!c.isAfterLast()){
			list.add(table.get(c, db));
			c.moveToNext();
		}
		return list;
	}
	
	/**
	 * Updates a given object in the database
	 * @param obj the object to update (This must have the same id number as the original object)
	 */
	@SuppressWarnings("unchecked")
	public <T> void update(T obj){
		Table<T> table = (Table<T>) getTable(obj.getClass());
		table.update(obj, 0, db);
	}
	
	/**
	 * Deletes an object from the database, given its rowId
	 * @param cls the class of the object
	 * @param id the objects id number
	 */
	@SuppressWarnings("unchecked")
	public <T> void delete(Class<T> cls, int id){
		Table<T> table = (Table<T>) getTable(cls);
		table.delete(id, db);
	}
	
	/**
	 * Returns the number of objects of a given class currently stored in the database
	 * @param cls the class of the objects
	 * @return the number of objects of type cls stored in the database
	 */
	@SuppressWarnings("unchecked")
	public <T> int size(Class<T> cls){
		Table<T> table = (Table<T>) getTable(cls);
		Cursor c = db.query(table.getTableName(), null, null , null, null, null, null);
		return c.getColumnCount();
	}
}
