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
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class PersistenceManager {

	public static final int COLLECTION_EMPTY_VALUE = -1;
	public static final String PARENT_REFERENCE_NAME = "PARENT";
	public static final String CHILD_REFERENCE_NAME = "CHILD";
	private SQLiteDatabase db;
	private Set<Class<?>> upToDateClasses;
	private int defaultUpgradeValue = -1;

	public PersistenceManager(SQLiteDatabase db){
		this.db = db;
		this.upToDateClasses = new HashSet<Class<?>>();
	}

	public void create(CreateDirective cd) {
		try {
			Object obj = cd.getInstance();
			Class<?> type = obj.getClass();
			String tableName = type.getSimpleName();
			Field idField = DataUtil.getIdField(type);
			Field[] instanceFields = DataUtil.getFields(type);

			executeCreateIfNotExistsSQLStatement(tableName, createSQLStatementsFromFields(DataUtil.getFields(type)));
			performTableUpgradeIfNeeded(type);

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

	public void delete(DeleteDirective dd) {
		Class<?> type = dd.getCls();
		int rowId = dd.getRowId();
		String tableName = type.getSimpleName();
		Field idField = DataUtil.getIdField(type);
		executeCreateIfNotExistsSQLStatement(tableName, createSQLStatementsFromFields(DataUtil.getFields(type)));
		performTableUpgradeIfNeeded(type);
		db.delete(tableName, idField.getName() + " = " + rowId, null);
	}

	public void update(UpdateDirective ud) {
		Class<?> type = ud.getCls();
		Map<Field, Object> fieldValueMap = ud.getValues();
		int rowId = ud.getRowId();
		String tableName = type.getSimpleName();
		Field idField = DataUtil.getIdField(type);
		executeCreateIfNotExistsSQLStatement(tableName, createSQLStatementsFromFields(DataUtil.getFields(type)));
		performTableUpgradeIfNeeded(type);

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

	public <T> T fetch(Class<T> type, Cursor cursor){
		return fetch(type, cursor, new Cache());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> T fetch(Class<T> type, Cursor cursor, Cache cache){
		Queue<Field> nonPrimitveFieldQueue = new LinkedList<Field>();
		Queue<Field> nonPrimitveCollectionFieldQueue = new LinkedList<Field>();
		executeCreateIfNotExistsSQLStatement(type.getSimpleName(), createSQLStatementsFromFields(DataUtil.getFields(type)));
		performTableUpgradeIfNeeded(type);

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
						field.set(newObj, DataUtil.shallowCopy(cachedObject));
					}else{
						String nonPrimitiveReferenceSQLStatement = DataUtil.getIdField(field.getType()).getName() + " = " + nonPrimitiveReferenceId;
						Cursor nonPrimitiveReferenceCursor = db.query(field.getType().getSimpleName(), null, nonPrimitiveReferenceSQLStatement, null, null, null, null);
						nonPrimitiveReferenceCursor.moveToFirst();
						field.set(newObj, fetch(field.getType(), nonPrimitiveReferenceCursor, cache));
						nonPrimitiveReferenceCursor.close();
					}
				}
			}

			for(Field field : nonPrimitveCollectionFieldQueue){
				Class<?> containedClass = DataUtil.getStoredClassOfCollection(field);
				int rowId = idField.getInt(newObj);
				String containedObjTableName = containedClass.getSimpleName();
				Field containedObjIdField = DataUtil.getIdField(containedClass);
				String collectionReferenceTableName = type.getSimpleName() + "_" + field.getName();
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
									newCollection.add(DataUtil.shallowCopy(cachedObject));
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

	public Object fetch(Class<?> cls, int id){
		String tableName = cls.getSimpleName();
		String SQLSelectionStatement = DataUtil.getIdField(cls).getName() + " = " + String.valueOf(id);
		Cursor cursor = null;
		try{
			cursor = db.query(tableName, null, SQLSelectionStatement, null, null, null, null);
		}catch(SQLException e){
			return null;
		}
		if(!cursor.moveToFirst()) return null;
		Object object = fetch(cls, cursor);
		cursor.close();
		return DataUtil.shallowCopy(object);
	}

	public void deleteReference(DeleteReferenceDirective drd){
		String parentName = drd.getParentType().getSimpleName();
		String childName = drd.getChildName();
		int parentValue = drd.getParentId();
		int childValue = drd.getChildId();
		String tableName = parentName + "_" + childName;

		executeCreateReferenceTableStatement(tableName);

		String SQLSelectionString = PARENT_REFERENCE_NAME + " = " + String.valueOf(parentValue) + " AND " + CHILD_REFERENCE_NAME + " = " + String.valueOf(childValue);
		db.delete(tableName, SQLSelectionString, null);
	}

	public void createReference(CreateReferenceDirective crd){
		String parentName = crd.getParentType().getSimpleName();
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

	private void performTableUpgradeIfNeeded(Class<?> cls){
		if(!upToDateClasses.contains(cls)){
			upToDateClasses.add(cls);

			HashSet<String> existingNonCollectionFieldList = new HashSet<String>();
			HashSet<String> newNonCollectionFieldList = new HashSet<String>();
			HashSet<String> existingCollectionFieldList = new HashSet<String>();
			HashSet<String> newCollectionFieldList = new HashSet<String>();

			Cursor cursor = db.rawQuery("PRAGMA table_info(" + cls.getSimpleName() + ")", null);
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
				if(tableName.startsWith(cls.getSimpleName() + "_")){
					String collectionName = tableName.split("_")[1];
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

				String tableName = cls.getSimpleName();
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
				String tableName = cls.getSimpleName();
				for(String existingCollectionName : existingCollectionFieldList){
					if(!newCollectionFieldList.contains(existingCollectionName)){
						db.execSQL("DROP TABLE " + tableName + "_" + existingCollectionName + ";");
					}
				}
			}
		}
	}

	private void executeCreateReferenceTableStatement(String tableName){
		String createStatement = "CREATE TABLE IF NOT EXISTS " + tableName + "(" + PARENT_REFERENCE_NAME + " " + DataUtil.INT_FIELD + ", " + CHILD_REFERENCE_NAME + " " + DataUtil.INT_FIELD + ");";
		db.execSQL(createStatement);
	}

	private void executeCreateIfNotExistsSQLStatement(String tableName, ArrayList<String> createStrings) {
		String createStatement = "CREATE TABLE IF NOT EXISTS " + tableName + "(";
		for (int i = 0; i < createStrings.size(); i++) {
			if (i != createStrings.size() - 1) createStatement += createStrings.get(i) + ", ";
			else createStatement += createStrings.get(i) + ");";
		}
		db.execSQL(createStatement);
	}

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

	public void setDefaultUpgradeValue(int value){
		this.defaultUpgradeValue = value;
	}

	public int fetchMaxRowId(Class<?> instanceType){
		int rowId;
		try{
			Cursor cursor = db.rawQuery("SELECT MAX(" + DataUtil.getIdField(instanceType).getName() + ") FROM " + instanceType.getSimpleName(), null);
			if (!cursor.moveToFirst()) rowId = 1;
			else rowId = cursor.getInt(0) + 1;
			cursor.close();
		}catch(SQLException e){
			rowId = 1;
		}
		return rowId;
	}
}
