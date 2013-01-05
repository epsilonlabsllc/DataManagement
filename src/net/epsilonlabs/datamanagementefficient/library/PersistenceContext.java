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
import net.epsilonlabs.datamanagementefficient.exception.InternalDatabaseException;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.SparseArray;

public class PersistenceContext {

	public static final int COLLECTION_EMPTY_VALUE = -1;

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
		if(newInstance == null) return 0;
		Class<?> instanceType = newInstance.getClass();
		Field idField = DataUtil.getIdField(instanceType);
		Field[] instanceFields = DataUtil.getFields(instanceType);

		Integer rowId = DataUtil.getId(newInstance);
		if(rowId != 0) return rowId;

		rowId = nextIdMap.get(instanceType);
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
					if(field.get(newInstance) != null){
						Class<?> containedType = DataUtil.getStoredClassOfCollection(field);
						if(((Collection<?>)field.get(newInstance)).isEmpty()){
							pendingDirectivesQueue.offer(new CreateReferenceDirective(instanceType, containedType, rowId, COLLECTION_EMPTY_VALUE));
						}else{
							for(Object containedObject : (Collection<?>)field.get(newInstance)){
								int containedObjId = create(containedObject);
								pendingDirectivesQueue.offer(new CreateReferenceDirective(instanceType, containedType, rowId, containedObjId));
							}
						}
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
		update(updatedInstance, new Cache());
	}

	public void update(Object updatedInstance, Cache previosulyUpdatedObjects){
		Map<Field, Object> updateMap = new HashMap<Field, Object>();
		Class<?> instanceType = updatedInstance.getClass();
		Field[] instanceFields = DataUtil.getFields(instanceType);
		int rowId = DataUtil.getId(updatedInstance);

		if(previosulyUpdatedObjects.get(instanceType, rowId) == null){
			Object storedInstance = cache.get(instanceType, rowId);
			if(storedInstance == null){
				storedInstance = fetchToCache(instanceType, rowId);
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
					if(storedValue == null && updatedValue != null){
						//was null before, now has a value
						updateMap.put(field, create(updatedValue)); 
					}else if(storedValue != null && updatedValue == null){
						//had a value before, now is null
						updateMap.put(field, null);
						previosulyUpdatedObjects.put(updatedInstance);
						delete(field.getType(), DataUtil.getId(storedValue), previosulyUpdatedObjects);
					}else if(storedValue != null && updatedValue != null){
						//value is being altered (wasn't and will not be null)
						Field idField = DataUtil.getIdField(storedValue.getClass());
						try {
							idField.set(updatedValue, DataUtil.getId(storedValue));
						} catch (Exception e) {
							throw new IdFieldIsInaccessibleException();
						}
						previosulyUpdatedObjects.put(updatedInstance);
						update(updatedValue, previosulyUpdatedObjects);
					}
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
							previosulyUpdatedObjects.put(updatedInstance);
							delete(containedType, key, previosulyUpdatedObjects);
							pendingDirectivesQueue.offer(new DeleteReferenceDirective(instanceType, containedType, rowId, key));
						}else{
							//check for objects that already exist, update those (don't change reference)
							previosulyUpdatedObjects.put(updatedInstance);
							update(updatedObjectsMap.get(key), previosulyUpdatedObjects);
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

					if(((Collection<?>) updatedValue).isEmpty()){
						pendingDirectivesQueue.offer(new CreateReferenceDirective(instanceType, containedType, rowId, COLLECTION_EMPTY_VALUE));
					}
					break;
				}
			}

			cache.put(shallowCopy(updatedInstance));
			if(!updateMap.isEmpty()) pendingDirectivesQueue.offer(new UpdateDirective(instanceType, rowId, updateMap));
		}
	}

	public void delete(Class<?> instanceType, int rowId){
		delete(instanceType, rowId, new Cache());
	}

	private void delete(Class<?> instanceType, int rowId, Cache previouslyDeletedObjects) {
		if(previouslyDeletedObjects.get(instanceType, rowId) == null){
			Field[] instanceFields = DataUtil.getFields(instanceType);		
			Object storedInstance = cache.get(instanceType, rowId);
			if(storedInstance == null) storedInstance = fetchToCache(instanceType, rowId);
			if(storedInstance != null){
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
							if(field.get(storedInstance) != null) {
								previouslyDeletedObjects.put(storedInstance);
								delete(field.getType(), DataUtil.getId(field.get(storedInstance)), previouslyDeletedObjects);
							}
						} catch (IllegalAccessException e) {
							throw new InaccessableObjectException();
						}
						break;
					case DataUtil.FIELD_TYPE_COLLECTION:
						try{
							if(field.get(storedInstance) != null){
								Class<?> containedType = DataUtil.getStoredClassOfCollection(field);
								previouslyDeletedObjects.put(storedInstance);
								for(Object containedObject : (Collection<?>)field.get(storedInstance)){
									int containedObjId = DataUtil.getId(containedObject);
									delete(containedType, containedObjId, previouslyDeletedObjects);
									pendingDirectivesQueue.offer(new DeleteReferenceDirective(instanceType, containedType, rowId, containedObjId));
								}
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
		}
	}

	public Object fetchToCache(Class<?> cls, int id){
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
		cache.put(object);
		return shallowCopy(object);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
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

			cache.put(newObj);

			for(Field field : nonPrimitveFieldQueue){
				if(cursor.isNull(cursor.getColumnIndex(field.getName() + "_ref"))){
					field.set(newObj, null);
				}else{
					int nonPrimitiveReferenceId = cursor.getInt(cursor.getColumnIndex(field.getName() + "_ref"));
					Object cachedObject = cache.get(field.getType(), nonPrimitiveReferenceId);
					if(cachedObject != null){
						field.set(newObj, shallowCopy(cachedObject));
					}else{
						String nonPrimitiveReferenceSQLStatement = DataUtil.getIdField(field.getType()).getName() + " = " + nonPrimitiveReferenceId;
						Cursor nonPrimitiveReferenceCursor = db.query(field.getType().getSimpleName(), null, nonPrimitiveReferenceSQLStatement, null, null, null, null);
						nonPrimitiveReferenceCursor.moveToFirst();
						field.set(newObj, fetch(field.getType(), nonPrimitiveReferenceCursor));
						nonPrimitiveReferenceCursor.close();
					}
				}
			}

			for(Field field : nonPrimitveCollectionFieldQueue){
				Class<?> containedClass = DataUtil.getStoredClassOfCollection(field);
				int rowId = idField.getInt(newObj);
				String containedObjTableName = containedClass.getSimpleName();
				Field containedObjIdField = DataUtil.getIdField(containedClass);
				String collectionReferenceTableName = type.getSimpleName() + "_" + containedClass.getSimpleName();
				String collectionReferenceSQLStatement = PersistenceManager.PARENT_REFERENCE_NAME + " = " + String.valueOf(rowId);
				try{
					Cursor collectionReferenceCursor = db.query(collectionReferenceTableName, new String[]{PersistenceManager.CHILD_REFERENCE_NAME}, collectionReferenceSQLStatement, null, null, null, null);
					if(!collectionReferenceCursor.moveToFirst()){
						field.set(newObj, null);
					}else{
						Collection newCollection = (Collection) field.getType().newInstance();
						while(!collectionReferenceCursor.isAfterLast()){
							int containedObjId = collectionReferenceCursor.getInt(collectionReferenceCursor.getColumnIndex(PersistenceManager.CHILD_REFERENCE_NAME));
							if(containedObjId != COLLECTION_EMPTY_VALUE){
								Object cachedObject = cache.get(containedClass, containedObjId);
								if(cachedObject != null){
									newCollection.add(shallowCopy(cachedObject));
								}else{
									String containedObjSQLStatement = containedObjIdField.getName() + " = " + String.valueOf(containedObjId);
									Cursor containedObjCursor = db.query(containedObjTableName, null, containedObjSQLStatement, null, null, null, null);
									containedObjCursor.moveToFirst();
									newCollection.add(fetch(containedClass, containedObjCursor));
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

	public <T> T shallowCopy(T instance){
		return shallowCopy(instance, new Cache());
	}

	@SuppressWarnings({ "unchecked" })
	public <T> T shallowCopy(T instance, Cache previosulyClonedObjects){
		if(instance == null) return null;
		try{
			Queue<Field> nonPrimitveFieldQueue = new LinkedList<Field>();
			Queue<Field> nonPrimitveCollectionFieldQueue = new LinkedList<Field>();

			Class<T> instanceType = (Class<T>) instance.getClass();
			Field[] typeFields = DataUtil.getFields(instanceType);

			Object previouslyClonedObject = previosulyClonedObjects.get(instanceType, DataUtil.getId(instance));
			if(previouslyClonedObject != null) return (T) previouslyClonedObject;

			T newInstance = instanceType.newInstance();

			for(Field typeField : typeFields){
				switch(DataUtil.getFieldTypeId(typeField)){
				case DataUtil.FIELD_TYPE_INT:
				case DataUtil.FIELD_TYPE_DOUBLE:
				case DataUtil.FIELD_TYPE_FLOAT:
				case DataUtil.FIELD_TYPE_LONG:
				case DataUtil.FIELD_TYPE_STRING:
				case DataUtil.FIELD_TYPE_BOOLEAN:
					typeField.set(newInstance, typeField.get(instance));
					break;
				case DataUtil.FIELD_TYPE_NON_PRIMITIVE:
					nonPrimitveFieldQueue.offer(typeField);

					break;
				case DataUtil.FIELD_TYPE_COLLECTION:
					nonPrimitveCollectionFieldQueue.offer(typeField);

				}
			}

			previosulyClonedObjects.put(newInstance);

			for(Field typeField : nonPrimitveFieldQueue){
				typeField.set(newInstance, shallowCopy(typeField.get(instance), previosulyClonedObjects));
			}

			for(Field typeField : nonPrimitveCollectionFieldQueue){
				if(typeField.get(instance) == null){
					typeField.set(newInstance, null);
				}else{
					Collection<Object> newCollection = (Collection<Object>) typeField.getType().newInstance();
					for (Object containedObj : (Collection<?>) typeField.get(instance)) {
						newCollection.add(shallowCopy(containedObj, previosulyClonedObjects));
					}
					typeField.set(newInstance, newCollection);
				}
				break;
			}
			return newInstance;
		}catch (Exception e) {
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

	public Cache getCache(){
		return cache;
	}
}
