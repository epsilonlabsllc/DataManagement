package net.epsilonlabs.datamanagementefficient.library;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import net.epsilonlabs.datamanagementefficient.directive.CreateDirective;
import net.epsilonlabs.datamanagementefficient.directive.CreateReferenceDirective;
import net.epsilonlabs.datamanagementefficient.directive.DeleteDirective;
import net.epsilonlabs.datamanagementefficient.directive.DeleteReferenceDirective;
import net.epsilonlabs.datamanagementefficient.directive.UpdateDirective;
import net.epsilonlabs.datamanagementefficient.exception.InternalDatabaseException;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * The Persistence Manager class acts as DataManagement's direct liaison to the database. It handles all queries to the database.
 * @author Tom Caputi
 *
 */
public class PersistenceManager {

	public static final int COLLECTION_EMPTY_VALUE = -1;
	public static final String PARENT_REFERENCE_NAME = "PARENT";
	public static final String CHILD_REFERENCE_NAME = "CHILD";
	private SQLHelper helper;
	private SQLiteDatabase db;
	private Set<Class<?>> upToDateClasses;
	private int defaultUpgradeValue = -1;

	/**
	 * Constructor. Instantiates a new Set that will hold Classes that are confirmed to be up to date.
	 * @param db the SQLite database instance that is being used
	 */
	public PersistenceManager(Context context){
		this.helper = new SQLHelper(context);
		this.upToDateClasses = new HashSet<Class<?>>();
	}
	
	/**
	 * Opens the database for reading and writing.
	 */
	public void open(){
		db = helper.getWritableDatabase();
	}
	
	/**
	 * Closes the database for reading and writing.
	 */
	public void close(){
		db.close();
	}
	
	/**
	 * Returns a Cursor with all columns from the database based on a given Class and a given SQL where clause.
	 * @param cls the class
	 * @param whereString the SQL where clause
	 * @return a cursor with all columns
	 */
	public Cursor getCursor(Class<?> cls, String whereString){
		if(!upToDateClasses.contains(cls)){
			upToDateClasses.add(cls);
			executeCreateIfNotExistsSQLStatement(DataUtil.getTableName(cls), createSQLStatementsFromFields(DataUtil.getFields(cls)));
			performTableUpgrade(cls);
		}
		
		try{
			Cursor cursor = db.query(DataUtil.getTableName(cls), null, whereString, null, null, null, null);
			return cursor;
		}catch(SQLException e){
			throw new InternalDatabaseException();
		}
	}

	/**
	 * Creates an object in the database.
	 * @param cd the CreateDirective that holds the data to be added to the database.
	 */
	public void create(CreateDirective cd) {
		try {
			Object obj = cd.getInstance();
			Class<?> type = obj.getClass();
			String tableName = DataUtil.getTableName(type);
			Field idField = DataUtil.getIdField(type);
			Field[] instanceFields = DataUtil.getFields(type);

			if(!upToDateClasses.contains(type)){
				upToDateClasses.add(type);
				executeCreateIfNotExistsSQLStatement(tableName, createSQLStatementsFromFields(DataUtil.getFields(type)));
				performTableUpgrade(type);
			}

			int rowId = (int) db.insert(tableName, idField.getName(), null);
			ContentValues cv = new ContentValues();
			for (Field field : instanceFields) {
				switch (DataUtil.getFieldTypeId(field)) {
				case DataUtil.FIELD_TYPE_INT:
					cv.put(field.getName(), field.getInt(obj));
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
					cv.put(field.getName(), (String) field.get(obj));
					break;
				case DataUtil.FIELD_TYPE_BOOLEAN:
					if (field.getBoolean(obj)) cv.put(field.getName(), 1);
					else cv.put(field.getName(), 0);
					break;
				case DataUtil.FIELD_TYPE_NON_PRIMITIVE:
					if(field.get(obj) == null) cv.put(field.getName() + "_ref", (Integer) null);
					else cv.put(field.getName() + "_ref", DataUtil.getId(field.get(obj)));
					break;
				}
			}
			db.update(tableName, cv, "ROWID = " + String.valueOf(rowId), null);
		} catch (IllegalAccessException e) {
			throw new InternalDatabaseException();
		}
	}

	/**
	 * Deletes an object from the database.
	 * @param dd the DeleteDirective that holds the data to be deleted from the database.
	 */
	public void delete(DeleteDirective dd) {
		Class<?> type = dd.getCls();
		int rowId = dd.getRowId();
		String tableName = DataUtil.getTableName(type);;
		Field idField = DataUtil.getIdField(type);

		if(!upToDateClasses.contains(type)){
			upToDateClasses.add(type);
			executeCreateIfNotExistsSQLStatement(tableName, createSQLStatementsFromFields(DataUtil.getFields(type)));
			performTableUpgrade(type);
		}

		db.delete(tableName, idField.getName() + " = " + rowId, null);
	}

	/**
	 * Updates an object in the database.
	 * @param ud the UpdateDirective that holds the data to be updated in the database.
	 */
	public void update(UpdateDirective ud) {
		Class<?> type = ud.getCls();
		Map<Field, Object> fieldValueMap = ud.getValues();
		int rowId = ud.getRowId();
		String tableName = DataUtil.getTableName(type);
		Field idField = DataUtil.getIdField(type);

		if(!upToDateClasses.contains(type)){
			upToDateClasses.add(type);
			executeCreateIfNotExistsSQLStatement(tableName, createSQLStatementsFromFields(DataUtil.getFields(type)));
			performTableUpgrade(type);
		}

		ContentValues cv = new ContentValues();
		for (Field field : fieldValueMap.keySet()) {
			switch (DataUtil.getFieldTypeId(field)) {
			case DataUtil.FIELD_TYPE_INT:
				cv.put(field.getName(), (Integer)fieldValueMap.get(field));
				break;
			case DataUtil.FIELD_TYPE_DOUBLE:
				cv.put(field.getName(), (Double)fieldValueMap.get(field));
				break;
			case DataUtil.FIELD_TYPE_FLOAT:
				cv.put(field.getName(), (Float)fieldValueMap.get(field));
				break;
			case DataUtil.FIELD_TYPE_LONG:
				cv.put(field.getName(), (Long)fieldValueMap.get(field));
				break;
			case DataUtil.FIELD_TYPE_STRING:
				cv.put(field.getName(), (String)fieldValueMap.get(field));
				break;
			case DataUtil.FIELD_TYPE_BOOLEAN:
				if ((Boolean)fieldValueMap.get(field)) cv.put(field.getName(), 1);
				else cv.put(field.getName(), 0);
				break;
			case DataUtil.FIELD_TYPE_NON_PRIMITIVE:
				cv.put(field.getName() + "_ref", (Integer)fieldValueMap.get(field));
				break;
			}
		}
		db.update(tableName, cv, idField.getName() + " = " + String.valueOf(rowId), null);
	}

	/**
	 * Fetches an object from the database based on its Class a Cursor.
	 * @param type the Class of the object to be returned
	 * @param cursor the Cursor, positioned at the correct row, that represents the object to be fetched
	 * @return the object from the database
	 */
	public <T> T fetch(Class<T> type, Cursor cursor){
		return fetch(type, cursor, new Cache());
	}

	/**
	 * Helper method used by fetch(Class, Cursor) that recursively fetches an object of a given class and all contained objects within it from the database.
	 * @param type the Class of the object to be retrieved
	 * @param cursor the Cursor, positioned at the correct row, that represents the object to be fetched
	 * @param cache a map of all objects that have already been retrieved so far
	 * @return The object from the database
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> T fetch(Class<T> type, Cursor cursor, Cache cache){
		Queue<Field> nonPrimitveFieldQueue = new LinkedList<Field>();
		Queue<Field> nonPrimitveCollectionFieldQueue = new LinkedList<Field>();

		if(!upToDateClasses.contains(type)){
			upToDateClasses.add(type);
			executeCreateIfNotExistsSQLStatement(DataUtil.getTableName(type), createSQLStatementsFromFields(DataUtil.getFields(type)));
			performTableUpgrade(type);
		}

		try{
			T newObj = type.newInstance();
			Field idField = DataUtil.getIdField(type);
			Field[] fields = DataUtil.getFields(type);

			for(Field field: fields){
				int columnNumber = cursor.getColumnIndex(field.getName());
				switch(DataUtil.getFieldTypeId(field)){
				case DataUtil.FIELD_TYPE_INT:
					if(cursor.isNull(columnNumber)) field.setInt(newObj, defaultUpgradeValue);
					else field.setInt(newObj, cursor.getInt(columnNumber));
					break;
				case DataUtil.FIELD_TYPE_DOUBLE:
					if(cursor.isNull(columnNumber)) field.setDouble(newObj, defaultUpgradeValue);
					else field.setDouble(newObj, cursor.getDouble(columnNumber));
					break;
				case DataUtil.FIELD_TYPE_FLOAT:
					if(cursor.isNull(columnNumber)) field.setFloat(newObj, defaultUpgradeValue);
					else field.setFloat(newObj, cursor.getFloat(columnNumber));
					break;
				case DataUtil.FIELD_TYPE_LONG:
					if(cursor.isNull(columnNumber)) field.setLong(newObj, defaultUpgradeValue);
					else field.setLong(newObj, cursor.getLong(columnNumber));
					break;
				case DataUtil.FIELD_TYPE_STRING:
					field.set(newObj, cursor.getString(columnNumber));
					break;
				case DataUtil.FIELD_TYPE_BOOLEAN:
					if(cursor.getInt(columnNumber) == 1) field.setBoolean(newObj, true);
					else field.setBoolean(newObj, false);
					break;
				case DataUtil.FIELD_TYPE_NON_PRIMITIVE:
					nonPrimitveFieldQueue.offer(field);
					break;
				case DataUtil.FIELD_TYPE_COLLECTION:
					nonPrimitveCollectionFieldQueue.offer(field);
					break;
				}
			}

			cache.put(newObj);

			for(Field field : nonPrimitveFieldQueue){
				if(cursor.isNull(cursor.getColumnIndex(field.getName() + "_ref"))){
					field.set(newObj, null);
				}else{
					int nonPrimitiveReferenceId = cursor.getInt(cursor.getColumnIndex(field.getName() + "_ref"));
					Object cachedObject = cache.get(field.getType(), nonPrimitiveReferenceId);
					if(cachedObject != null){
						field.set(newObj, DataUtil.copy(cachedObject));
					}else{
						String nonPrimitiveReferenceSQLStatement = DataUtil.getIdField(field.getType()).getName() + " = " + nonPrimitiveReferenceId;
						Cursor nonPrimitiveReferenceCursor = db.query(DataUtil.getTableName(field.getType()), null, nonPrimitiveReferenceSQLStatement, null, null, null, null);
						nonPrimitiveReferenceCursor.moveToFirst();
						field.set(newObj, fetch(field.getType(), nonPrimitiveReferenceCursor, cache));
						nonPrimitiveReferenceCursor.close();
					}
				}
			}

			for(Field field : nonPrimitveCollectionFieldQueue){
				Class<?> containedClass = DataUtil.getStoredClassOfCollection(field);
				int rowId = idField.getInt(newObj);
				String containedObjTableName = DataUtil.getTableName(containedClass);
				Field containedObjIdField = DataUtil.getIdField(containedClass);
				String collectionReferenceTableName = DataUtil.getTableName(type) + "_" + field.getName();
				String collectionReferenceSQLStatement = PARENT_REFERENCE_NAME + " = " + String.valueOf(rowId);
				try{
					Cursor collectionReferenceCursor = db.query(collectionReferenceTableName, new String[]{CHILD_REFERENCE_NAME}, collectionReferenceSQLStatement, null, null, null, null);
					if(!collectionReferenceCursor.moveToFirst()){
						field.set(newObj, null);
					}else{
						Collection newCollection = (Collection) field.getType().newInstance();
						while(!collectionReferenceCursor.isAfterLast()){
							int containedObjId = collectionReferenceCursor.getInt(collectionReferenceCursor.getColumnIndex(CHILD_REFERENCE_NAME));
							if(containedObjId != COLLECTION_EMPTY_VALUE){
								Object cachedObject = cache.get(containedClass, containedObjId);
								if(cachedObject != null){
									newCollection.add(DataUtil.copy(cachedObject));
								}else{
									String containedObjSQLStatement = containedObjIdField.getName() + " = " + String.valueOf(containedObjId);
									Cursor containedObjCursor = db.query(containedObjTableName, null, containedObjSQLStatement, null, null, null, null);
									containedObjCursor.moveToFirst();
									newCollection.add(fetch(containedClass, containedObjCursor, cache));
								}
							}
							collectionReferenceCursor.moveToNext();
						}
						field.set(newObj, newCollection);
						collectionReferenceCursor.close();
					}
				}catch(SQLException e){
					field.set(newObj, null);
				}
			}
			return newObj;
		}catch(IllegalAccessException e){
			throw new InternalDatabaseException();
		}catch(InstantiationException e){
			throw new InternalDatabaseException();
		}
	}

	/**
	 * Fetches an object from the database based on its Class and id number.
	 * @param cls the Class of the object to be returned
	 * @param id the id number of the object to be fetched from the database
	 * @return the object from the database
	 */
	public <T> T fetch(Class<T> cls, int id){
		if(!upToDateClasses.contains(cls)){
			upToDateClasses.add(cls);
			executeCreateIfNotExistsSQLStatement(DataUtil.getTableName(cls), createSQLStatementsFromFields(DataUtil.getFields(cls)));
			performTableUpgrade(cls);
		}
		
		String tableName = DataUtil.getTableName(cls);
		String SQLSelectionStatement = DataUtil.getIdField(cls).getName() + " = " + String.valueOf(id);
		Cursor cursor = null;
		try{
			cursor = db.query(tableName, null, SQLSelectionStatement, null, null, null, null);
		}catch(SQLException e){
			return null;
		}
		if(!cursor.moveToFirst()) return null;
		T object = fetch(cls, cursor);
		cursor.close();
		return DataUtil.copy(object);
	}

	/**
	 * Deletes a reference from a reference table.
	 * @param drd the DeleteReferenceDirective that holds the data to be deleted from the database
	 */
	public void deleteReference(DeleteReferenceDirective drd){
		String parentName = DataUtil.getTableName(drd.getParentType());
		String childName = drd.getChildName();
		int parentValue = drd.getParentId();
		int childValue = drd.getChildId();
		String tableName = parentName + "_" + childName;

		executeCreateReferenceTableStatement(tableName);

		String SQLSelectionString = PARENT_REFERENCE_NAME + " = " + String.valueOf(parentValue) + " AND " + CHILD_REFERENCE_NAME + " = " + String.valueOf(childValue);
		db.delete(tableName, SQLSelectionString, null);
	}

	/**
	 * Creates a reference in a reference table.
	 * @param crd The CreateReferenceDirective that holds the data to be added to the database
	 */
	public void createReference(CreateReferenceDirective crd){
		String parentName = DataUtil.getTableName(crd.getParentType());
		String childName = crd.getChildName();
		int parentValue = crd.getParentId();
		int childValue = crd.getChildId();
		String tableName = parentName + "_" + childName;

		executeCreateReferenceTableStatement(tableName);

		ContentValues cv = new ContentValues();
		cv.put(PARENT_REFERENCE_NAME, parentValue);
		cv.put(CHILD_REFERENCE_NAME, childValue);
		db.insert(tableName, null, cv);
	}

	/**
	 * Changes a table, if needed, to match new Fields if any have been added or removed. This method should only be called
	 * once per instantiation of a PersistenceManager  per class. Classes that have already been confirmed to be up to date
	 * are stored in upToDateClasses Set.
	 * @param cls the class of the table to be upgraded
	 */
	private void performTableUpgrade(Class<?> cls){
		HashSet<String> existingNonCollectionFieldList = new HashSet<String>();
		HashSet<String> newNonCollectionFieldList = new HashSet<String>();
		HashSet<String> existingCollectionFieldList = new HashSet<String>();
		HashSet<String> newCollectionFieldList = new HashSet<String>();

		Cursor cursor = db.rawQuery("PRAGMA table_info(" + DataUtil.getTableName(cls) + ")", null);
		cursor.moveToFirst();
		while(!cursor.isAfterLast()){
			existingNonCollectionFieldList.add(cursor.getString(1));
			cursor.moveToNext();
		}
		cursor.close();

		Cursor collectionReferenceCursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
		collectionReferenceCursor.moveToFirst();
		while (!collectionReferenceCursor.isAfterLast()){
			String tableName = collectionReferenceCursor.getString(collectionReferenceCursor.getColumnIndex("name"));
			if(tableName.startsWith(DataUtil.getTableName(cls) + "_")){
				String[] nameStrings = tableName.split("_");
				String collectionName = nameStrings[nameStrings.length-1];
				existingCollectionFieldList.add(collectionName);
			}
			collectionReferenceCursor.moveToNext();
		}
		collectionReferenceCursor.close();

		Field[] newFields = DataUtil.getFields(cls);
		for(Field field : newFields){
			switch(DataUtil.getFieldTypeId(field)){
			case DataUtil.FIELD_TYPE_INT:
			case DataUtil.FIELD_TYPE_DOUBLE:
			case DataUtil.FIELD_TYPE_FLOAT:
			case DataUtil.FIELD_TYPE_LONG:
			case DataUtil.FIELD_TYPE_STRING:
			case DataUtil.FIELD_TYPE_BOOLEAN:
				newNonCollectionFieldList.add(field.getName());
				break;
			case DataUtil.FIELD_TYPE_NON_PRIMITIVE:
				newNonCollectionFieldList.add(field.getName() + "_ref");
				break;
			case DataUtil.FIELD_TYPE_COLLECTION:
				newCollectionFieldList.add(field.getName());
				break;
			}
		}

		if(!existingNonCollectionFieldList.containsAll(newNonCollectionFieldList) || !newNonCollectionFieldList.containsAll(existingNonCollectionFieldList)){
			LinkedList<String> sharedNonCollectionColumns = new LinkedList<String>();
			for(String existingField : existingNonCollectionFieldList){
				if(newNonCollectionFieldList.contains(existingField)) sharedNonCollectionColumns.add(existingField);
			}

			String tableName = DataUtil.getTableName(cls);
			executeCreateIfNotExistsSQLStatement(tableName + "_backup", createSQLStatementsFromFields(DataUtil.getFields(cls)));

			String SQLCopyStatement = "INSERT INTO " + tableName + "_backup (";
			String SQLCopyFields = "";
			for(int i=0; i<sharedNonCollectionColumns.size(); i++){
				if(i != sharedNonCollectionColumns.size()-1) SQLCopyFields += sharedNonCollectionColumns.get(i) + ", ";
				else SQLCopyFields += sharedNonCollectionColumns.get(i);
			}
			SQLCopyStatement += SQLCopyFields + ") SELECT " + SQLCopyFields;
			SQLCopyStatement += " FROM " + tableName + ";";
			db.execSQL(SQLCopyStatement);			

			db.execSQL("DROP TABLE " + tableName + ";");
			db.execSQL("ALTER TABLE " + tableName + "_backup " + "RENAME TO " + tableName + ";");

		}

		if(!newCollectionFieldList.containsAll(existingCollectionFieldList)){
			String tableName = DataUtil.getTableName(cls);
			for(String existingCollectionName : existingCollectionFieldList){
				if(!newCollectionFieldList.contains(existingCollectionName)){
					db.execSQL("DROP TABLE " + tableName + "_" + existingCollectionName + ";");
				}
			}
		}
	}

	/**
	 * Creates a reference table from a table name, if it does not already exist.
	 * @param tableName the name of the table to be created
	 */
	private void executeCreateReferenceTableStatement(String tableName){
		String createStatement = "CREATE TABLE IF NOT EXISTS " + tableName + "(" + PARENT_REFERENCE_NAME + " " + DataUtil.INT_FIELD + ", " + CHILD_REFERENCE_NAME + " " + DataUtil.INT_FIELD + ");";
		db.execSQL(createStatement);
	}

	/**
	 * Creates a table from a table name and an ArrayList of Strings that represent the Field names and types.
	 * @param tableName the name of the table
	 * @param createStrings an ArrayList of Strings that represent the Field names and types
	 */
	private void executeCreateIfNotExistsSQLStatement(String tableName, ArrayList<String> createStrings) {
		String createStatement = "CREATE TABLE IF NOT EXISTS " + tableName + "(";
		for (int i = 0; i < createStrings.size(); i++) {
			if (i != createStrings.size() - 1) createStatement += createStrings.get(i) + ", ";
			else createStatement += createStrings.get(i) + ");";
		}
		db.execSQL(createStatement);
	}

	/**
	 * Creates an ArrayList of Strings from a Field[] which are the SQL representation of the Fields and their types.
	 * @param fields a Field[] to be converted to SQL column name-type Strings
	 * @return an ArrayList of Strings representing the column names and types for a new table
	 */
	private ArrayList<String> createSQLStatementsFromFields(Field[] fields) {
		ArrayList<String> createStrings = new ArrayList<String>();
		for (Field field : fields) {
			String createString = "";
			switch (DataUtil.getFieldTypeId(field)) {
			case DataUtil.FIELD_TYPE_INT:
				createString += field.getName() + " ";
				createString += DataUtil.INT_FIELD;
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
			case DataUtil.FIELD_TYPE_NON_PRIMITIVE:
				createString = field.getName() + "_ref ";
				createString += DataUtil.INT_FIELD;
				createStrings.add(createString);
				break;
			case DataUtil.FIELD_TYPE_COLLECTION:
				break;
			}
		}
		return createStrings;
	}

	/**
	 * Returns the number of objects of a given Class currently stored in the database
	 * @param cls the class
	 * @return the number of objects currently being stored in the database
	 */
	public <T> int size(Class<T> cls){
		
		if(!upToDateClasses.contains(cls)){
			executeCreateIfNotExistsSQLStatement(DataUtil.getTableName(cls), createSQLStatementsFromFields(DataUtil.getFields(cls)));
			performTableUpgrade(cls);
		}
		
		Cursor c = null;
		try{
			c = db.query(DataUtil.getTableName(cls), new String[]{DataUtil.getIdField(cls).getName()}, null , null, null, null, null);
		}catch(SQLException e){
			return 0;
		}
		int size = c.getCount();
		c.close();
		return size;
	}
	
	/**
	 * Drops all records of a given class from the database.
	 * @param recordName the name of the class to be deleted
	 */
	public void dropRecords(String recordName){
		db.execSQL("DROP TABLE " + recordName + ";");
	}

	/**
	 * Sets the default value to be given to added numerical fields when an class is changed. When fields are added to a stored class, DataManagement automatically
	 * adds these fields to existing objects in the database. If these fields are numerical, they can be given a default value when the objects are retrieved. If the
	 * added fields are not primitive, they are defaulted to null.
	 * @param value
	 */
	public void setDefaultUpgradeValue(int value){
		this.defaultUpgradeValue = value;
	}

	/**
	 * Gets the highest currently stored id of a given class stored in the database
	 * @param instanceType the Class
	 * @return the highest currently stored id number of all objects of the given class
	 */
	public int fetchMaxRowId(Class<?> instanceType){
		
		if(!upToDateClasses.contains(instanceType)){
			executeCreateIfNotExistsSQLStatement(DataUtil.getTableName(instanceType), createSQLStatementsFromFields(DataUtil.getFields(instanceType)));
			performTableUpgrade(instanceType);
		}
		
		int rowId;
		try{
			Cursor cursor = db.rawQuery("SELECT MAX(" + DataUtil.getIdField(instanceType).getName() + ") FROM " + DataUtil.getTableName(instanceType), null);
			if (!cursor.moveToFirst()) rowId = 1;
			else rowId = cursor.getInt(0) + 1;
			cursor.close();
		}catch(SQLException e){
			rowId = 1;
		}
		return rowId;
	}
}
