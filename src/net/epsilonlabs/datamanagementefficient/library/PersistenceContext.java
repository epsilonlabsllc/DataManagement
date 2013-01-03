package net.epsilonlabs.datamanagementefficient.library;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import net.epsilonlabs.datamanagementefficient.directive.CreateDirective;
import net.epsilonlabs.datamanagementefficient.directive.CreateReferenceDirective;
import net.epsilonlabs.datamanagementefficient.directive.DeleteDirective;
import net.epsilonlabs.datamanagementefficient.directive.DeleteReferenceDirective;
import net.epsilonlabs.datamanagementefficient.directive.Directive;
import net.epsilonlabs.datamanagementefficient.directive.UpdateDirective;
import net.epsilonlabs.datamanagementefficient.exception.IdFieldIsInaccessibleException;
import net.epsilonlabs.datamanagementefficient.exception.InaccessableObjectException;
import net.epsilonlabs.datamanagementefficient.exception.InstanceCloneFailedException;
import net.epsilonlabs.datamanagementefficient.exception.InstanceDoesNotExistException;
import net.epsilonlabs.datamanagementefficient.exception.InternalDatabaseException;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.SparseArray;

public class PersistenceContext {

	private Cache cache;
	private Queue<Directive> pendingDirectivesQueue;
	private Map<Class<?>, Integer> nextIdMap;
	private SQLiteDatabase db;

	public PersistenceContext(SQLiteDatabase db) {
		this.db = db;
		this.cache = new Cache();
		this.pendingDirectivesQueue = new LinkedList<Directive>();
		this.nextIdMap = new HashMap<Class<?>, Integer>();
	}

	public int create(Object newInstance) {
		assert (newInstance != null) : "The new instance was null";

		Class<?> instanceType = newInstance.getClass();
		Field idField = DataUtil.getIdField(instanceType);
		Field[] instanceFields = DataUtil.getFields(instanceType);

		Integer rowId = nextIdMap.get(instanceType);
		if (rowId == null) {
			try{
				Cursor cursor = db.rawQuery("SELECT MAX(" + idField.getName() + ") FROM " + instanceType.getSimpleName(), null);
				if (!cursor.moveToFirst()) rowId = 1;
				else rowId = cursor.getInt(0) + 1;
				cursor.close();
			}catch(SQLException e){
				rowId = 1;
			}
		}
		nextIdMap.put(instanceType, rowId + 1);
		try {
			idField.setInt(newInstance, rowId);
		} catch (IllegalAccessException e) {
			throw new IdFieldIsInaccessibleException();
		}

		for(Field field : instanceFields){
			switch (DataUtil.getFieldTypeId(field)) {
			case DataUtil.FIELD_TYPE_INT:
			case DataUtil.FIELD_TYPE_DOUBLE:
			case DataUtil.FIELD_TYPE_FLOAT:
			case DataUtil.FIELD_TYPE_LONG:
			case DataUtil.FIELD_TYPE_STRING:
			case DataUtil.FIELD_TYPE_BOOLEAN:
				break;
			case DataUtil.FIELD_TYPE_NON_PRIMITIVE:
				try{
					create(field.get(newInstance));
				}catch(IllegalAccessException e){
					throw new InaccessableObjectException();
				}
				break;
			case DataUtil.FIELD_TYPE_COLLECTION:
				try{
					Class<?> containedType = DataUtil.getStoredClassOfCollection(field);
					for(Object containedObject : (Collection<?>)field.get(newInstance)){
						int containedObjId = create(containedObject);
						pendingDirectivesQueue.offer(new CreateReferenceDirective(instanceType, containedType, rowId, containedObjId));
					}
				}catch(IllegalAccessException e){
					throw new InaccessableObjectException();
				}
				break;
			}
		}

		Object newInstanceCopy = shallowCopy(newInstance);
		cache.put(newInstanceCopy);	
		pendingDirectivesQueue.offer(new CreateDirective(newInstanceCopy));
		return rowId;
	}

	public void update(Object updatedInstance){
		assert (updatedInstance != null) : "The new instance was null";

		Map<Field, Object> updateMap = new HashMap<Field, Object>();
		Class<?> instanceType = updatedInstance.getClass();
		Field[] instanceFields = DataUtil.getFields(instanceType);
		int rowId = DataUtil.getId(updatedInstance);

		Object storedInstance = cache.get(instanceType, rowId);
		if(storedInstance == null){
			fetchToCache(instanceType, rowId);
		}

		for(Field field : instanceFields){

			Object storedValue = null;
			Object updatedValue = null;
			try{
				storedValue = field.get(storedInstance);
				updatedValue = field.get(updatedInstance);
			}catch(IllegalAccessException e){
				throw new InaccessableObjectException();
			}
			if(!areEqual(storedValue, updatedValue)){
				switch (DataUtil.getFieldTypeId(field)) {
				case DataUtil.FIELD_TYPE_INT:
				case DataUtil.FIELD_TYPE_DOUBLE:
				case DataUtil.FIELD_TYPE_FLOAT:
				case DataUtil.FIELD_TYPE_LONG:
				case DataUtil.FIELD_TYPE_STRING:
				case DataUtil.FIELD_TYPE_BOOLEAN:
					updateMap.put(field, updatedValue);
					break;
				case DataUtil.FIELD_TYPE_NON_PRIMITIVE:
					if(storedValue == null) updateMap.put(field, create(updatedValue)); //places an int in which will be the reference
					else update(updatedValue);
					break;
				case DataUtil.FIELD_TYPE_COLLECTION:
					Class<?> containedType = DataUtil.getStoredClassOfCollection(field);

					if(storedValue == null) storedValue = new ArrayList<Object>();
					if(updatedValue == null) updatedValue = new ArrayList<Object>();

					SparseArray<Object> storedObjectsMap = new SparseArray<Object>();
					SparseArray<Object> updatedObjectsMap = new SparseArray<Object>();
					for(Object storedContainedObj : (Collection<?>) storedValue){
						storedObjectsMap.put(DataUtil.getId(storedContainedObj), storedContainedObj);
					}
					for(Object updatedContainedObj : (Collection<?>) updatedValue){
						updatedObjectsMap.put(DataUtil.getId(updatedContainedObj), updatedContainedObj);
					}

					for(int i=0; i<storedObjectsMap.size(); i++) {
						int key = storedObjectsMap.keyAt(i);
						if(updatedObjectsMap.get(key) == null){
							//check for objects that no longer exist, delete those (delete reference)
							delete(containedType, key);
							pendingDirectivesQueue.offer(new DeleteReferenceDirective(instanceType, containedType, rowId, key));
						}else{
							//check for objects that already exist, update those (don't change reference)
							update(updatedObjectsMap.get(key));
						}
					}

					for(int i=0; i<updatedObjectsMap.size(); i++) {
						int key = updatedObjectsMap.keyAt(i);
						if(storedObjectsMap.get(key) == null){
							//check for objects that do not exist yet, create those (add reference)
							int childId = create(updatedObjectsMap.get(key));
							pendingDirectivesQueue.offer(new CreateReferenceDirective(instanceType, containedType, rowId, childId));
						}
					}
					break;
				}
			}
		}

		cache.put(shallowCopy(updatedInstance));
		if(!updateMap.isEmpty()) pendingDirectivesQueue.offer(new UpdateDirective(instanceType, rowId, updateMap));
	}

	private <T> boolean areEqual(T obj1, T obj2){
		
		if(obj1 == null && obj2 == null) return true;
		if((obj1 == null && obj2 != null) || (obj1 != null && obj2 == null)) return false;
		
		if(obj1 instanceof Integer || obj1 instanceof Double || obj1 instanceof Float || obj1 instanceof Long || obj1 instanceof String || obj1 instanceof Boolean){
			return obj1.equals(obj2);
		}
		
		Class<?> type = obj1.getClass();
		Object obj1value = null;
		Object obj2value = null;
		for(Field field : DataUtil.getFields(type)){
			try {
				obj1value = field.get(obj1);
				obj2value = field.get(obj2);
			}catch (IllegalAccessException e) {
				throw new InaccessableObjectException();
			}
			
			if(!areEqual(obj1value, obj2value)) return false;
		}
		return true;
	}

	public void delete(Class<?> instanceType, int rowId) {

		Field[] instanceFields = DataUtil.getFields(instanceType);
		Object storedInstance = cache.get(instanceType, rowId);
		if(storedInstance == null){
			fetchToCache(instanceType, rowId);
		}

		for(Field field : instanceFields){
			switch (DataUtil.getFieldTypeId(field)) {
			case DataUtil.FIELD_TYPE_INT:
			case DataUtil.FIELD_TYPE_DOUBLE:
			case DataUtil.FIELD_TYPE_FLOAT:
			case DataUtil.FIELD_TYPE_LONG:
			case DataUtil.FIELD_TYPE_STRING:
			case DataUtil.FIELD_TYPE_BOOLEAN:
				break;
			case DataUtil.FIELD_TYPE_NON_PRIMITIVE:
				try {
					delete(field.getType(), DataUtil.getId(field.get(storedInstance)));
				} catch (IllegalAccessException e) {
					throw new InaccessableObjectException();
				}
				break;
			case DataUtil.FIELD_TYPE_COLLECTION:
				try{
					Class<?> containedType = DataUtil.getStoredClassOfCollection(field);
					for(Object containedObject : (Collection<?>)field.get(storedInstance)){
						int containedObjId = DataUtil.getId(containedObject);
						delete(containedType, containedObjId);
						pendingDirectivesQueue.offer(new DeleteReferenceDirective(instanceType, containedType, rowId, containedObjId));
					}
				} catch (IllegalAccessException e) {
					throw new InaccessableObjectException();
				}
				break;
			}
		}
		cache.remove(instanceType, rowId);	
		pendingDirectivesQueue.offer(new DeleteDirective(instanceType, rowId));
	}
	
	private void fetchToCache(Class<?> cls, int id){
		String tableName = cls.getSimpleName();
		String SQLSelectionStatement = DataUtil.getIdField(cls).getName() + " = " + String.valueOf(id);
		Cursor cursor = db.query(tableName, null, SQLSelectionStatement, null, null, null, null);
		if(!cursor.moveToFirst()) throw new InstanceDoesNotExistException();
		Object object = fetch(cls, cursor);
		cursor.close();
		cache.put(object);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> T fetch(Class<T> type, Cursor cursor){
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

	@SuppressWarnings({ "unchecked" })
	public <T> T shallowCopy(T instance) {
		try {
			Class<T> instanceType = (Class<T>) instance.getClass();
			Field[] typeFields = DataUtil.removeFinalFields(instanceType.getDeclaredFields());
			T newInstance = instanceType.newInstance();

			Object instanceValue;
			for (Field typeField : typeFields) {
				if (DataUtil.getFieldTypeId(typeField) == DataUtil.FIELD_TYPE_COLLECTION) {
					Collection<Object> newCollection = (Collection<Object>) typeField.getType().newInstance();
					for (Object containedObj : (Collection<?>) typeField.get(instance)) {
						newCollection.add(shallowCopy(containedObj));
					}
					typeField.set(newInstance, newCollection);
				}else if(DataUtil.getFieldTypeId(typeField) == DataUtil.FIELD_TYPE_NON_PRIMITIVE){
					typeField.set(newInstance, shallowCopy(typeField.get(instance)));
				}else{
					instanceValue = typeField.get(instance);
					typeField.set(newInstance, instanceValue);
				}
			}
			return newInstance;
		} catch (Exception e) {
			throw new InstanceCloneFailedException();
		}
	}

	public Object getFromCache(Class<?> cls, int id){
		return shallowCopy(cache.get(cls, id));
	}

	public Queue<Directive> getPendingDirectivesQueue(){
		return pendingDirectivesQueue;
	}

	public void clearPendingDirectivesQueue(){
		pendingDirectivesQueue.clear();
	}
}
