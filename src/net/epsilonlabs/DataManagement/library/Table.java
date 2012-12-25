package net.epsilonlabs.DataManagement.library;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import net.epsilonlabs.DataManagement.annotations.Id;
import net.epsilonlabs.DataManagement.exception.InternalDatabaseException;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * The Table class holds all information about a table in the database. It provides basic functionality for objects to be stored, retrieved, updated, and deleted.
 * This class can also hold other Tables so that objects held by other objects may be stored recursively.
 * @author Tom Caputi
 *
 * @param <T> The class of objects this table will store
 */
public class Table<T> {

	private Class<T> cls = null;
	private String tableName = null;
	private String parentTableName = null;
	private HashMap<Class<?>, Table<?>> references = null;
	private Field[] fields = null;
	private Field idField = null;

	/**
	 * Constructor for use by the DataManager class.
	 * @param cls the class being stored
	 * @param db the database being used (so that SQL statements can be executed)
	 */
	public Table(Class<T> cls, SQLiteDatabase db){
		this.cls = cls;
		this.fields = DataUtil.removeFinalFields(cls.getDeclaredFields());
		this.idField = DataUtil.getIdField(cls);
		Field.setAccessible(fields, true);
		idField.setAccessible(true);
		this.tableName = cls.getCanonicalName().replace(".", "_");
		this.references = getReferencesFromClass(cls, db);
		executeCreateStatement(db);
	}

	/**
	 * Constructor for use by Table.getReferences(). Same as other constructor, but also gets stores the name of the parent Table.
	 * @param cls the class being stored
	 * @param db the database being used (so that SQL statements can be executed)
	 * @param parentTableName the name of the parent table (used for referencing purposes)
	 */
	public Table(Class<T> cls, SQLiteDatabase db, String parentTableName){
		this.cls = cls;
		this.fields = DataUtil.removeFinalFields(cls.getDeclaredFields());
		this.idField = DataUtil.getIdField(cls);
		Field.setAccessible(fields, true);
		idField.setAccessible(true);
		this.parentTableName = parentTableName;
		this.tableName = cls.getCanonicalName().replace(".", "_");
		this.references = getReferencesFromClass(cls, db);
		executeCreateStatement(db);
	}

	/**
	 * Used to populate the references HashMap with a list of every Class stored in cls
	 * @param type The class being stored by this Table
	 * @param db the database being used (so that the create statement can be called)
	 * @return the HashMap to be stored as references
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private HashMap<Class<?>, Table<?>> getReferencesFromClass(Class<T> type, SQLiteDatabase db) {
		HashMap<Class<?>, Table<?>> map = new HashMap<Class<?>, Table<?>>();
		for(Field field : DataUtil.removeFinalFields(type.getDeclaredFields())){
			int typeId = DataUtil.getFieldTypeId(field);
			if(typeId == DataUtil.FIELD_TYPE_COLLECTION){
				Class<?> storedCls = DataUtil.getStoredClassOfCollection(field);
				map.put(storedCls, new Table(storedCls, db, tableName));
			}else if(typeId == DataUtil.FIELD_TYPE_OTHER){
				map.put(field.getType(), new Table(field.getType(), db, tableName));
			}
		}
		return map;
	}

	/**
	 * Executes the createStatement of this Table and all Tables contained in references
	 * @param db the database being used (so that the create statement can be called)
	 */
	private void executeCreateStatement(SQLiteDatabase db) {
		ArrayList<String> createStrings = getCreateStringsFromClass();
		String createStatement = "CREATE TABLE IF NOT EXISTS " + tableName + "(";
		for(int i=0; i<createStrings.size();i++){
			if(i != createStrings.size()-1) createStatement += createStrings.get(i) + ", ";
			else createStatement +=  createStrings.get(i) + ");";
		}	
		db.execSQL(createStatement);
	}

	/**
	 * Gets an ArrayList of Strings to be used by executeCreateStatement() to generate the SQL createStatement
	 * @return a list of Strings in the format "<field name> <field type> <additional constraints>" as defined by SQLite
	 */
	private ArrayList<String> getCreateStringsFromClass() {
		ArrayList<String> createStrings = new ArrayList<String>();
		for(Field field : fields){
			String createString = "";
			switch(DataUtil.getFieldTypeId(field)){
			case DataUtil.FIELD_TYPE_INT:
				createString = field.getName() + " ";
				createString += DataUtil.INT_FIELD;
				if(field.isAnnotationPresent(Id.class)) createString += " " + DataUtil.AUTO_PRIMARY_KEY;
				createStrings.add(createString);
				break;
			case DataUtil.FIELD_TYPE_DOUBLE:
				createString = field.getName() + " ";
				createString += DataUtil.REAL_FIELD;
				createStrings.add(createString);
				break;
			case DataUtil.FIELD_TYPE_FLOAT:
				createString = field.getName() + " ";
				createString += DataUtil.REAL_FIELD;
				createStrings.add(createString);
				break;
			case DataUtil.FIELD_TYPE_LONG:
				createString = field.getName() + " ";
				createString += DataUtil.REAL_FIELD;
				createStrings.add(createString);
				break;
			case DataUtil.FIELD_TYPE_STRING:
				createString = field.getName() + " ";
				createString += DataUtil.TEXT_FIELD;
				createStrings.add(createString);
				break;
			case DataUtil.FIELD_TYPE_BOOLEAN:
				createString = field.getName() + " ";
				createString += DataUtil.INT_FIELD;
				createStrings.add(createString);
				break;
			case DataUtil.FIELD_TYPE_OTHER:
				break;
			case DataUtil.FIELD_TYPE_COLLECTION:
				break;
			}
		}

		if(parentTableName != null) createStrings.add(parentTableName + " " + DataUtil.INT_FIELD);
		return createStrings;
	}

	/**
	 * Stores an object to the database. Recursively stores contained objects (as long as they fit storing criteria). 
	 * @param obj The object to be stored
	 * @param parentId the id number of the object that is storing this one. (If this is the root object, this is not used)
	 * @param db the database being used 
	 * @return the id number of the newly stored object 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public int add(T obj, int parentId, SQLiteDatabase db){
		try{
			if(obj == null) return -1;
			int rowId = (int) db.insert(tableName, idField.getName(), null);
			ContentValues cv = new ContentValues();
			for(Field field: fields){
				switch(DataUtil.getFieldTypeId(field)){
				case DataUtil.FIELD_TYPE_INT:
					if(field.getAnnotation(Id.class) == null) cv.put(field.getName(), field.getInt(obj));
					break;
				case DataUtil.FIELD_TYPE_DOUBLE:
					cv.put(field.getName(), field.getDouble(obj));
					break;
				case DataUtil.FIELD_TYPE_FLOAT:
					cv.put(field.getName(), field.getFloat(obj));
					break;
				case DataUtil.FIELD_TYPE_LONG:
					cv.put(field.getName(), field.getLong(obj));
					break;
				case DataUtil.FIELD_TYPE_STRING:
					cv.put(field.getName(),(String) field.get(obj));
					break;
				case DataUtil.FIELD_TYPE_BOOLEAN:
					if(field.getBoolean(obj)) cv.put(field.getName(), 1);
					else cv.put(field.getName(), 0);
					break;
				case DataUtil.FIELD_TYPE_OTHER:
					Table refTable = references.get(field.getType());
					refTable.add(field.get(obj), rowId, db);
					break;
				case DataUtil.FIELD_TYPE_COLLECTION:
					Table refTable2 = references.get(DataUtil.getStoredClassOfCollection(field));
					for(Object containedObj : (Collection<?>) field.get(obj)){
						refTable2.add(containedObj, rowId, db);
					}
					break;
				}
			}
			if(parentId != 0) cv.put(parentTableName, parentId);
			db.update(tableName, cv, idField.getName() + " = " + String.valueOf(rowId), null);
			return rowId;

		}catch(IllegalAccessException e){
			throw new InternalDatabaseException();
		}
	}

	/**
	 * Updates an object already stored in the database. Recursively updates contained objects (as long as they fit storing criteria).
	 * @param obj The object to be updated. Must have the same id number as object that is being updated.
	 * @param parentId parentId the id number of the object that is storing this one. (If this is the root object, this is not used)
	 * @param db the database being used 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void update(T obj, int parentId, SQLiteDatabase db){
		try{
			if(obj != null){
				int rowId = idField.getInt(obj);
				if(rowId == 0) rowId = (int) db.insert(tableName, idField.getName(), null); 
				ContentValues cv = new ContentValues();
				for(Field field: fields){
					switch(DataUtil.getFieldTypeId(field)){
					case DataUtil.FIELD_TYPE_INT:
						if(field.getAnnotation(Id.class) == null) cv.put(field.getName(), field.getInt(obj));
						break;
					case DataUtil.FIELD_TYPE_DOUBLE:
						cv.put(field.getName(), field.getDouble(obj));
						break;
					case DataUtil.FIELD_TYPE_FLOAT:
						cv.put(field.getName(), field.getFloat(obj));
						break;
					case DataUtil.FIELD_TYPE_LONG:
						cv.put(field.getName(), field.getLong(obj));
						break;
					case DataUtil.FIELD_TYPE_STRING:
						cv.put(field.getName(),(String) field.get(obj));
						break;
					case DataUtil.FIELD_TYPE_BOOLEAN:
						if(field.getBoolean(obj)) cv.put(field.getName(), 1);
						else cv.put(field.getName(), 0);
						break;
					case DataUtil.FIELD_TYPE_OTHER:
						Table refTable = references.get(field.getType());
						refTable.update(field.get(obj), rowId, db);
						break;
					case DataUtil.FIELD_TYPE_COLLECTION:
						Table refTable2 = references.get(DataUtil.getStoredClassOfCollection(field));
						for(Object containedObj : (Collection<?>) field.get(obj)){
							refTable2.update(containedObj, rowId, db);
						}
						break;
					}
				}
				if(parentId != 0) cv.put(parentTableName, parentId);
				db.update(tableName, cv, idField.getName() + " = " + String.valueOf(rowId), null);
			}
		}catch(IllegalAccessException e){
			throw new InternalDatabaseException();
		}
	}

	/**
	 * Gets the first object listed in a Cursor from the database
	 * @param c an SQL query
	 * @param db the database being used
	 * @return the first object stored in the Cursor
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public T get(Cursor c, SQLiteDatabase db){
		try{
			T newObj = cls.newInstance();
			for(Field field: fields){
				switch(DataUtil.getFieldTypeId(field)){
				case DataUtil.FIELD_TYPE_INT:
					field.setInt(newObj, c.getInt(c.getColumnIndex(field.getName())));
					break;
				case DataUtil.FIELD_TYPE_DOUBLE:
					field.setDouble(newObj, c.getDouble(c.getColumnIndex(field.getName())));
					break;
				case DataUtil.FIELD_TYPE_FLOAT:
					field.setFloat(newObj, c.getFloat(c.getColumnIndex(field.getName())));
					break;
				case DataUtil.FIELD_TYPE_LONG:
					field.setLong(newObj, c.getLong(c.getColumnIndex(field.getName())));
					break;
				case DataUtil.FIELD_TYPE_STRING:
					field.set(newObj, c.getString(c.getColumnIndex(field.getName())));
					break;
				case DataUtil.FIELD_TYPE_BOOLEAN:
					if(c.getInt(c.getColumnIndex(field.getName())) == 1) field.setBoolean(newObj, true);
					else field.setBoolean(newObj, false);
					break;
				case DataUtil.FIELD_TYPE_OTHER:
					Table refTable = references.get(field.getType());
					Cursor refCursor = db.query(refTable.getTableName(), null, tableName + " = " + String.valueOf(c.getInt(c.getColumnIndex(idField.getName()))), null, null, null, null);
					refCursor.moveToFirst();
					field.set(newObj, refTable.get(refCursor, db));
					break;
				case DataUtil.FIELD_TYPE_COLLECTION:
					Table refTable2 = references.get(DataUtil.getStoredClassOfCollection(field));
					Cursor refCursor2 = db.query(refTable2.getTableName(), null, tableName + " = " + String.valueOf(c.getInt(c.getColumnIndex(idField.getName()))), null, null, null, null);
					refCursor2.moveToFirst();
					Collection col = (Collection) field.getType().newInstance();
					while(!refCursor2.isAfterLast()){
						col.add(refTable2.get(refCursor2, db));
						refCursor2.moveToNext();
					}
					field.set(newObj, col);
					break;
				}
			}
			return newObj;
		}catch(IllegalAccessException e){
			throw new InternalDatabaseException();
		} catch (InstantiationException e) {
			throw new InternalDatabaseException();
		}
	}

	/**
	 * Deletes an object from the database. Recursively deletes every object stored within this one.
	 * @param deleteId the id number of the item to be deleted. If this is the root object, this is its id number. If not, this is the id of its parent.
	 * @param db the database being used
	 */
	public void delete(int deleteId, SQLiteDatabase db){

		for(Field field : fields){
			switch(DataUtil.getFieldTypeId(field)){
			case DataUtil.FIELD_TYPE_INT:
				break;
			case DataUtil.FIELD_TYPE_DOUBLE:
				break;
			case DataUtil.FIELD_TYPE_FLOAT:
				break;
			case DataUtil.FIELD_TYPE_LONG:
				break;
			case DataUtil.FIELD_TYPE_STRING:
				break;
			case DataUtil.FIELD_TYPE_BOOLEAN:
				break;
			case DataUtil.FIELD_TYPE_OTHER:
				Cursor refCursor;
				if(parentTableName == null) refCursor= db.query(tableName, new String[]{idField.getName()}, idField.getName() + " = " + String.valueOf(deleteId), null, null, null, null);
				else refCursor = db.query(tableName,  new String[]{idField.getName()}, parentTableName + " = " + String.valueOf(deleteId), null, null, null, null);
				refCursor.moveToFirst();
				references.get(field.getType()).delete(refCursor.getInt(0), db);
				break;
			case DataUtil.FIELD_TYPE_COLLECTION:
				Cursor refCursor2;
				if(parentTableName == null) refCursor2 = db.query(tableName, new String[]{idField.getName()}, idField.getName() + " = " + String.valueOf(deleteId), null, null, null, null);
				else refCursor2 = db.query(tableName,  new String[]{idField.getName()}, parentTableName + " = " + String.valueOf(deleteId), null, null, null, null);
				refCursor2.moveToFirst();
				references.get(DataUtil.getStoredClassOfCollection(field)).delete(refCursor2.getInt(0), db);
				break;
			}
		}

		if(parentTableName == null) db.delete(tableName, idField.getName() + " = " + String.valueOf(deleteId), null);
		else db.delete(tableName, parentTableName + " = " + String.valueOf(deleteId), null);
	}

	/**
	 * returns the table name
	 * @return tableName
	 */
	public String getTableName(){
		return tableName;
	}

	public Field getIdField(){
		return idField;
	}

}