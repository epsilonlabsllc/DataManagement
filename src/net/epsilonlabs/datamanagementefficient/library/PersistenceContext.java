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
import android.util.SparseArray;

public class PersistenceContext {

	private Cache cache;
	private Queue<Directive> pendingDirectivesQueue;
	private Map<Class<?>, Integer> nextIdMap;
	private PersistenceManager pm;

	public PersistenceContext(PersistenceManager pm) {
		this.pm = pm;
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
			rowId = pm.fetchMaxRowId(instanceType);
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
						if(((Collection<?>)field.get(newInstance)).isEmpty()){
							pendingDirectivesQueue.offer(new CreateReferenceDirective(instanceType, field.getName(), rowId, PersistenceManager.COLLECTION_EMPTY_VALUE));
						}else{
							for(Object containedObject : (Collection<?>)field.get(newInstance)){
								int containedObjId = create(containedObject);
								pendingDirectivesQueue.offer(new CreateReferenceDirective(instanceType, field.getName(), rowId, containedObjId));
							}
						}
					}
				}catch(IllegalAccessException e){
					throw new InaccessableObjectException();
				}
				break;
			}
		}

		Object newInstanceCopy = DataUtil.shallowCopy(newInstance);
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
							pendingDirectivesQueue.offer(new DeleteReferenceDirective(instanceType, field.getName(), rowId, key));
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
							pendingDirectivesQueue.offer(new CreateReferenceDirective(instanceType, field.getName(), rowId, childId));
						}
					}

					if(((Collection<?>) updatedValue).isEmpty()){
						pendingDirectivesQueue.offer(new CreateReferenceDirective(instanceType, field.getName(), rowId, PersistenceManager.COLLECTION_EMPTY_VALUE));
					}
					break;
				}
			}

			cache.put(DataUtil.shallowCopy(updatedInstance));
			if(!updateMap.isEmpty()) pendingDirectivesQueue.offer(new UpdateDirective(instanceType, rowId, updateMap));
		}
	}

	public void delete(Class<?> instanceType, int rowId){
		delete(instanceType, rowId, new Cache());
	}
	
	public <T> T fetchToCache(Class<T> cls, int rowId){
		T obj = pm.fetch(cls, rowId);
		if(obj != null) cache.put(obj);
		return obj;
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
									pendingDirectivesQueue.offer(new DeleteReferenceDirective(instanceType, field.getName(), rowId, containedObjId));
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

	public <T> T getFromCache(Class<T> cls, int id){
		return DataUtil.shallowCopy(cache.get(cls, id));
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
