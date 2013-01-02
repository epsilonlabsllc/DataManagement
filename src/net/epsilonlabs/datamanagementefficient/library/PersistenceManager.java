package net.epsilonlabs.datamanagementefficient.library;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import net.epsilonlabs.datamanagementefficient.directive.CreateDirective;
import net.epsilonlabs.datamanagementefficient.directive.CreateReferenceDirective;
import net.epsilonlabs.datamanagementefficient.directive.DeleteDirective;
import net.epsilonlabs.datamanagementefficient.directive.DeleteReferenceDirective;
import net.epsilonlabs.datamanagementefficient.directive.UpdateDirective;
import net.epsilonlabs.datamanagementefficient.exception.InternalDatabaseException;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class PersistenceManager {

	private SQLiteDatabase db;

	public PersistenceManager(SQLiteDatabase db){
		this.db = db;
	}

	public void create(CreateDirective cd) {
		try {
			Object obj = cd.getInstance();

			Class<?> type = obj.getClass();
			String tableName = type.getSimpleName();
			Field idField = DataUtil.getIdField(type);
			Field[] instanceFields = DataUtil.getFields(type);

			executeCreateIfNotExistsSQLStatement(tableName, createSQLStatementsFromFields(DataUtil.getFields(type)));

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
					cv.put(field.getName() + "_ref", DataUtil.getId(field.get(obj)));
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
		db.delete(tableName, idField.getName() + " = " + rowId, null);
	}

	public void update(UpdateDirective ud) {
		Class<?> type = ud.getCls();
		Map<Field, Object> fieldValueMap = ud.getValues();
		int rowId = ud.getRowId();

		String tableName = type.getSimpleName();
		Field idField = DataUtil.getIdField(type);

		executeCreateIfNotExistsSQLStatement(tableName, createSQLStatementsFromFields(DataUtil.getFields(type)));

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

	public void deleteReference(DeleteReferenceDirective drd){
		String parentName = drd.getParentType().getSimpleName();
		String childName = drd.getChildType().getSimpleName();
		int parentValue = drd.getParentId();
		int childValue = drd.getChildId();
		String tableName = parentName + "_" + childName;

		executeCreateIfNotExistsSQLStatement(tableName, createReferenceTableSQLStatements(parentName, childName));

		String SQLSelectionString = parentName + " = " + String.valueOf(parentValue) + " AND " + childName + " = " + String.valueOf(childValue);
		db.delete(tableName, SQLSelectionString, null);
	}

	public void createReference(CreateReferenceDirective crd){
		String parentName = crd.getParentType().getSimpleName();
		String childName = crd.getChildType().getSimpleName();
		int parentValue = crd.getParentId();
		int childValue = crd.getChildId();
		String tableName = parentName + "_" + childName;

		executeCreateIfNotExistsSQLStatement(tableName, createReferenceTableSQLStatements(parentName, childName));

		ContentValues cv = new ContentValues();
		cv.put(parentName, parentValue);
		cv.put(childName, childValue);
		db.insert(tableName, null, cv);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> T fetch(Class<T> type, Cursor cursor){
		Queue<Field> nonPrimitveFieldQueue = new LinkedList<Field>();
		Queue<Field> nonPrimitveCollectionFieldQueue = new LinkedList<Field>();
		
		try{
			T newObj = type.newInstance();
			Field idField = DataUtil.getIdField(type);
			Field[] fields = DataUtil.getFields(type);
			
			for(Field field: fields){
				switch(DataUtil.getFieldTypeId(field)){
				case DataUtil.FIELD_TYPE_INT:
					field.setInt(newObj, cursor.getInt(cursor.getColumnIndex(field.getName())));
					break;
				case DataUtil.FIELD_TYPE_DOUBLE:
					field.setDouble(newObj, cursor.getDouble(cursor.getColumnIndex(field.getName())));
					break;
				case DataUtil.FIELD_TYPE_FLOAT:
					field.setFloat(newObj, cursor.getFloat(cursor.getColumnIndex(field.getName())));
					break;
				case DataUtil.FIELD_TYPE_LONG:
					field.setLong(newObj, cursor.getLong(cursor.getColumnIndex(field.getName())));
					break;
				case DataUtil.FIELD_TYPE_STRING:
					field.set(newObj, cursor.getString(cursor.getColumnIndex(field.getName())));
					break;
				case DataUtil.FIELD_TYPE_BOOLEAN:
					if(cursor.getInt(cursor.getColumnIndex(field.getName())) == 1) field.setBoolean(newObj, true);
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
			
			for(Field field : nonPrimitveFieldQueue){
				int nonPrimitiveReferenceId = cursor.getInt(cursor.getColumnIndex(field.getName() + "_ref"));
				String nonPrimitiveReferenceSQLStatement = DataUtil.getIdField(field.getType()).getName() + " = " + nonPrimitiveReferenceId;
				Cursor nonPrimitiveReferenceCursor = db.query(field.getType().getSimpleName(), null, nonPrimitiveReferenceSQLStatement, null, null, null, null);
				field.set(newObj, fetch(field.getType(), nonPrimitiveReferenceCursor));
				nonPrimitiveReferenceCursor.close();
			}
			
			for(Field field : nonPrimitveCollectionFieldQueue){
				Class<?> containedClass = DataUtil.getStoredClassOfCollection(field);
				int rowId = idField.getInt(newObj);
				String containedObjTableName = containedClass.getSimpleName();
				Field containedObjIdField = DataUtil.getIdField(containedClass);
				String collectionReferenceTableName = type.getSimpleName() + "_" + containedClass.getSimpleName();
				String collectionReferenceSQLStatement = type.getSimpleName() + " = " + String.valueOf(rowId);
				Cursor collectionReferenceCursor = db.query(collectionReferenceTableName, new String[]{containedClass.getSimpleName()}, collectionReferenceSQLStatement, null, null, null, null);
				collectionReferenceCursor.moveToFirst();
				Collection newCollection = (Collection) field.getType().newInstance();
				while(!collectionReferenceCursor.isAfterLast()){
					int containedObjId = collectionReferenceCursor.getInt(collectionReferenceCursor.getColumnIndex(containedClass.getSimpleName()));
					String containedObjSQLStatement = containedObjIdField.getName() + " = " + String.valueOf(containedObjId);
					Cursor containedObjCursor = db.query(containedObjTableName, null, containedObjSQLStatement, null, null, null, null);
					containedObjCursor.moveToFirst();
					newCollection.add(fetch(containedClass, containedObjCursor));
					collectionReferenceCursor.moveToNext();
				}
				collectionReferenceCursor.close();
			}
			
			return newObj;
		}catch(IllegalAccessException e){
			throw new InternalDatabaseException();
		}catch(InstantiationException e){
			throw new InternalDatabaseException();
		}
	}

	private void executeCreateIfNotExistsSQLStatement(String tableName, ArrayList<String> createStrings) {
		String createStatement = "CREATE TABLE IF NOT EXISTS " + tableName + "(";
		for (int i = 0; i < createStrings.size(); i++) {
			if (i != createStrings.size() - 1)
				createStatement += createStrings.get(i) + ", ";
			else
				createStatement += createStrings.get(i) + ");";
		}
		db.execSQL(createStatement);
	}

	private ArrayList<String> createReferenceTableSQLStatements(String parentTypeName, String childTypeName) {
		ArrayList<String> stringList = new ArrayList<String>();
		stringList.add(parentTypeName + " " + DataUtil.INT_FIELD);
		stringList.add(childTypeName + " " + DataUtil.INT_FIELD);
		return stringList;
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
}
