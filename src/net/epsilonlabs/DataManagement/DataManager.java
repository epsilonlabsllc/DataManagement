package net.epsilonlabs.DataManagement;

import java.util.ArrayList;
import java.util.HashMap;

import net.epsilonlabs.DataManagement.exception.DatabaseNotOpenedException;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * The DataManager class allows for easy storage and retrieval of any Object. The only requirement for stored classes is that they must have 1 int field dedicated to their id number.
 * This is denoted by the @Id Annotation.
 * 
 * This is the only class that should be used by outside programs. It allows the user to open/close the database, create, get/find, update, and delete objects in the database.
 * Also allows user to open and close the database.
 * @author Tom Caputi
 *
 */
public class DataManager {

	public static final String DATABASE_NAME = "Database";
	public static final int DATABASE_VERSION = 1;
	public static final String ID_NAME = "ID";

	private Context context; // The context calling the database
	private SQLHelper helper; // An SQLHelper used to open the database
	private SQLiteDatabase database; // The database
	private HashMap<Class<?>, Table<?>> tables; // A HashMap of tables, arranged by the class they represent.

	/**
	 * Constructor. Instantiates global variables.
	 * @param context - the context that is using the database
	 */
	public DataManager(Context context){
		this.context = context;
		this.tables = new HashMap<Class<?>, Table<?>>();
		this.helper = new SQLHelper(context, DATABASE_NAME, DATABASE_VERSION);
	}

	/**
	 * Used internally within DataManager to return the table associated with a given class. If none exists it creates one, adds it to the HashMap and returns it.
	 * @param cls - the Class stored by the table that is being searched for. 
	 * @return a table associated with the given Class
	 */
//	 Note: If a Table is not present in the HashMap it may still exist in the database. This just means that this instance of the DataManager has not yet used it.
//	 Therefore, the database.execSQL(newTable.getCreateStatement()) call must use a "create table if not exists" to ensure that a Table is not rewritten.
	@SuppressWarnings("unchecked")
	private <T> Table<T> getTable(Class<T> cls){
		Table<T> newTable = (Table<T>) tables.get(cls);
		if(newTable == null){
			newTable = new Table<T>(context, cls);
			tables.put(cls, newTable);
			try{
				database.execSQL(newTable.getCreateStatement());
			}catch(NullPointerException e){
				throw new DatabaseNotOpenedException();
			}
		}
		return newTable;
	}

	/**
	 * Returns the number of Objects of a given class that are currently being stored.
	 * @param cls - the class being stored
	 * @return the number of objects of that class
	 */
	public <T> int size(Class<T> cls){
		Table<T> table = getTable(cls);
		Cursor c;
		try{
			c = database.query(table.getTableName(), null, null, null, null, null, null);
		}catch(NullPointerException e){
			throw new DatabaseNotOpenedException();
		}
		int returnValue = c.getCount();
		c.close();
		return returnValue;
	}

	/**
	 * Returns an object, given its id number.
	 * @param cls - the class being stored 
	 * @param id - the id of the object being retrieved
	 * @return the requested object, or null if it is not found
	 */
	public <T> T get(Class<T> cls, int id){
		Table<T> table = getTable(cls);
		Cursor c;
		try{
			c = database.query(table.getTableName(), null, ID_NAME + " = " + String.valueOf(id), null, null, null, null);
		}catch(NullPointerException e){
			throw new DatabaseNotOpenedException();
		}
		if (c != null && c.getCount() == 1){
			c.moveToFirst();
			Storable<T> s = new Storable<T>(c, table.getDataType(), table.getIdField());
			c.close();
			close();
			return s.getObject();
		}
		c.close();
		return null;
	}

	/**
	 * Returns all the objects of a given class currently being stored as an ArrayList.
	 * @param cls - the class being stored 
	 * @return an ArrayList of all the objects of the given class being stored
	 */
	public <T> ArrayList<T> getAll(Class<T> cls){
		Table<T> table = getTable(cls);
		ArrayList<T> list = new ArrayList<T>();
		Cursor c;
		try{
			c = database.query(table.getTableName(), null, null, null, null, null, null);
		}catch(NullPointerException e){
			throw new DatabaseNotOpenedException();
		}
		if (c != null && c.getCount() > 0){
			c.moveToFirst();
			while(!c.isAfterLast()){
				Storable<T> s = new Storable<T>(c, table.getDataType(), table.getIdField());
				list.add(s.getObject());
				c.moveToNext();
			}
		}
		c.close();
		return list;
	}

	/**
	 * Finds a list of objects of a given class that have a given String variable of a given value.
	 * @param cls - the class being stored
	 * @param target - the value being searched for
	 * @param column - the variable being searched for the target
	 * @return an ArrayList of all objects of class cls that match the search criteria
	 */
	public <T> ArrayList<T> find(Class<T> cls, String target, String column){
		Table<T> table = getTable(cls);
		ArrayList<T> list = new ArrayList<T>();
		String whereClause = column + " = '" + target + "'";
		Cursor c;
		try{
			c = database.query(table.getTableName(), null, whereClause, null, null, null, null);
		}catch(NullPointerException e){
			throw new DatabaseNotOpenedException();
		}
		if (c != null && c.getCount() > 0){
			c.moveToFirst();
			while(!c.isAfterLast()){
				Storable<T> s = new Storable<T>(c, table.getDataType(), table.getIdField());
				list.add(s.getObject());
				c.moveToNext();
			}
		}
		c.close();
		return list;
	}

	/**
	 * Finds a list of objects of a given class that have a given int variable of a given value.
	 * @param cls - the class being stored
	 * @param target - the value being searched for
	 * @param column - the variable being searched for the target
	 * @return an ArrayList of all objects of class cls that match the search criteria
	 */
	public <T> ArrayList<T> find(Class<T> cls, int target, String column){
		Table<T> table = getTable(cls);
		ArrayList<T> list = new ArrayList<T>();
		String whereClause = column + " = " + target;
		Cursor c;
		try{
			c = database.query(table.getTableName(), null, whereClause, null, null, null, null);
		}catch(NullPointerException e){
			throw new DatabaseNotOpenedException();
		}
		if (c != null && c.getCount() > 0){
			c.moveToFirst();
			while(!c.isAfterLast()){
				Storable<T> s = new Storable<T>(c, table.getDataType(), table.getIdField());
				list.add(s.getObject());
				c.moveToNext();
			}
		}
		c.close();
		return list;
	}

	/**
	 * Finds a list of objects of a given class that have a given double variable of a given value.
	 * @param cls - the class being stored
	 * @param target - the value being searched for
	 * @param column - the variable being searched for the target
	 * @return an ArrayList of all objects of class cls that match the search criteria
	 */
	public <T> ArrayList<T> find(Class<T> cls, double target, String column){
		Table<T> table = getTable(cls);
		ArrayList<T> list = new ArrayList<T>();
		String whereClause = column + " = " + target;
		Cursor c;
		try{
			c = database.query(table.getTableName(), null, whereClause, null, null, null, null);
		}catch(NullPointerException e){
			throw new DatabaseNotOpenedException();
		}
		if (c != null && c.getCount() > 0){
			c.moveToFirst();
			while(!c.isAfterLast()){
				Storable<T> s = new Storable<T>(c, table.getDataType(), table.getIdField());
				list.add(s.getObject());
				c.moveToNext();
			}
		}
		c.close();
		return list;
	}

	/**
	 * Finds a list of objects of a given class that have a given float variable of a given value.
	 * @param cls - the class being stored
	 * @param target - the value being searched for
	 * @param column - the variable being searched for the target
	 * @return an ArrayList of all objects of class cls that match the search criteria
	 */
	public <T> ArrayList<T> findObject(Class<T> cls, float target, String column){
		Table<T> table = getTable(cls);
		ArrayList<T> list = new ArrayList<T>();
		String whereClause = column + " = " + target;
		Cursor c;
		try{
			c = database.query(table.getTableName(), null, whereClause, null, null, null, null);
		}catch(NullPointerException e){
			throw new DatabaseNotOpenedException();
		}
		if (c != null && c.getCount() > 0){
			c.moveToFirst();
			while(!c.isAfterLast()){
				Storable<T> s = new Storable<T>(c, table.getDataType(), table.getIdField());
				list.add(s.getObject());
				c.moveToNext();
			}
		}
		c.close();
		return list;
	}

	/**
	 * Finds a list of objects of a given class that have a given long variable of a given value.
	 * @param cls - the class being stored
	 * @param target - the value being searched for
	 * @param column - the variable being searched for the target
	 * @return an ArrayList of all objects of class cls that match the search criteria
	 */
	public <T> ArrayList<T> find(Class<T> cls, long target, String column){
		Table<T> table = getTable(cls);
		ArrayList<T> list = new ArrayList<T>();
		String whereClause = column + " = " + target;
		Cursor c;
		try{
			c = database.query(table.getTableName(), null, whereClause, null, null, null, null);
		}catch(NullPointerException e){
			throw new DatabaseNotOpenedException();
		}
		if (c != null && c.getCount() > 0){
			c.moveToFirst();
			while(!c.isAfterLast()){
				Storable<T> s = new Storable<T>(c, table.getDataType(), table.getIdField());
				list.add(s.getObject());
				c.moveToNext();
			}
		}
		c.close();
		return list;
	}

	/**
	 * Finds a list of objects of a given class that have a given boolean variable of a given value.
	 * @param cls - the class being stored
	 * @param target - the value being searched for
	 * @param column - the variable being searched for the target
	 * @return an ArrayList of all objects of class cls that match the search criteria
	 */
	public <T> ArrayList<T> find(Class<T> cls, boolean target, String column){
		Table<T> table = getTable(cls);
		ArrayList<T> list = new ArrayList<T>();
		String whereClause;
		if(target) whereClause = column + " = " + 1;
		else whereClause = column + " = " + 0;
		Cursor c;
		try{
			c = database.query(table.getTableName(), null, whereClause, null, null, null, null);
		}catch(NullPointerException e){
			throw new DatabaseNotOpenedException();
		}
		if (c != null && c.getCount() > 0){
			c.moveToFirst();
			while(!c.isAfterLast()){
				Storable<T> s = new Storable<T>(c, table.getDataType(), table.getIdField());
				list.add(s.getObject());
				c.moveToNext();
			}
		}
		c.close();
		return list;
	}

	/**
	 * Adds an object to the database and returns its given id number.
	 * Note: the returned id number is unique for its class; objects of different classes may have the same id number.
	 * @param object - the object being stored
	 * @return the id number of the object being stored
	 */
	@SuppressWarnings("unchecked")
	public <T> int add(T object){
		Table<T> table = (Table<T>) getTable(object.getClass());
		Storable<T> s = new Storable<T>(object, table.getIdField());
		try{
			return (int) database.insert(table.getTableName(), null, s.getContentValues());
		}catch(NullPointerException e){
			throw new DatabaseNotOpenedException();
		}
	}

	/**
	 * Updates an existing object in the database.
	 * @param id - the id number of the object being updated
	 * @param object the object being updated
	 */
	@SuppressWarnings("unchecked")
	public <T> void update(int id, T object){
		open();
		Table<T> table = (Table<T>) getTable(object.getClass());
		Storable<T> s = new Storable<T>(object, table.getIdField());
		try{
			database.update(table.getTableName(), s.getContentValues(), ID_NAME + " = " + String.valueOf(id), null);
		}catch(NullPointerException e){
			throw new DatabaseNotOpenedException();
		}
	}

	/**
	 * Deletes an object from the database.
	 * @param cls - the class of the object being deleted
	 * @param id - the id number of the object being deleted
	 */
	public <T> void delete(Class<T> cls, int id){
		Table<T> table = (Table<T>) getTable(cls);
		try{
			database.delete(table.getTableName(), ID_NAME + " = " + String.valueOf(id), null);
		}catch(NullPointerException e){
			throw new DatabaseNotOpenedException();
		}
	}

	/**
	 * Opens the database for writing and reading.
	 */
	public void open() {
		database = helper.getWritableDatabase();
	}

	/**
	 * Closes the database. It can not be written to or read from until it is reopened using open().
	 */
	public void close(){
		database.close();
	}

}
