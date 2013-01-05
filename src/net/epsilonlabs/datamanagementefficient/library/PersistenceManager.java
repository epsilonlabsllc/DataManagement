package net.epsilonlabs.datamanagementefficient.library;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

import net.epsilonlabs.datamanagementefficient.directive.CreateDirective;
import net.epsilonlabs.datamanagementefficient.directive.CreateReferenceDirective;
import net.epsilonlabs.datamanagementefficient.directive.DeleteDirective;
import net.epsilonlabs.datamanagementefficient.directive.DeleteReferenceDirective;
import net.epsilonlabs.datamanagementefficient.directive.UpdateDirective;
import net.epsilonlabs.datamanagementefficient.exception.InternalDatabaseException;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

public class PersistenceManager {

	public static final String PARENT_REFERENCE_NAME = "parent";
	public static final String CHILD_REFERENCE_NAME = "child";
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

		executeCreateReferenceTableStatement(tableName);

		String SQLSelectionString = PARENT_REFERENCE_NAME + " = " + String.valueOf(parentValue) + " AND " + CHILD_REFERENCE_NAME + " = " + String.valueOf(childValue);
		db.delete(tableName, SQLSelectionString, null);
	}

	public void createReference(CreateReferenceDirective crd){
		String parentName = crd.getParentType().getSimpleName();
		String childName = crd.getChildType().getSimpleName();
		int parentValue = crd.getParentId();
		int childValue = crd.getChildId();
		String tableName = parentName + "_" + childName;

		executeCreateReferenceTableStatement(tableName);

		ContentValues cv = new ContentValues();
		cv.put(PARENT_REFERENCE_NAME, parentValue);
		cv.put(CHILD_REFERENCE_NAME, childValue);
		db.insert(tableName, null, cv);
	}
	
	private void executeCreateReferenceTableStatement(String tableName){
		String createStatement = "CREATE TABLE IF NOT EXISTS " + tableName + "(" + PARENT_REFERENCE_NAME + " " + DataUtil.INT_FIELD + ", " + CHILD_REFERENCE_NAME + " " + DataUtil.INT_FIELD + ");";
		db.execSQL(createStatement);
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
