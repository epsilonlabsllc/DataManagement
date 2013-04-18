package net.epsilonlabs.datamanagementefficient.user;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Queue;

import net.epsilonlabs.datamanagementefficient.directive.CreateDirective;
import net.epsilonlabs.datamanagementefficient.directive.CreateReferenceDirective;
import net.epsilonlabs.datamanagementefficient.directive.DeleteDirective;
import net.epsilonlabs.datamanagementefficient.directive.DeleteReferenceDirective;
import net.epsilonlabs.datamanagementefficient.directive.Directive;
import net.epsilonlabs.datamanagementefficient.directive.UpdateDirective;
import net.epsilonlabs.datamanagementefficient.exception.DatabaseNotOpenExpection;
import net.epsilonlabs.datamanagementefficient.exception.FieldDoesNotExistException;
import net.epsilonlabs.datamanagementefficient.exception.MisMatchedFieldValueTypeException;
import net.epsilonlabs.datamanagementefficient.library.DataUtil;
import net.epsilonlabs.datamanagementefficient.library.DatabaseAsyncTask;
import net.epsilonlabs.datamanagementefficient.library.PersistenceContext;
import net.epsilonlabs.datamanagementefficient.library.PersistenceManager;
import android.content.Context;
import android.database.Cursor;

public class AsyncDataManager {

	private static AsyncDataManager instance;
	private PersistenceContext pc = null;
	private PersistenceManager pm = null;
	private boolean isOpen = false;

	/**
	 * Singleton instantiation method for getting a DataManager instance
	 * @param context the context that is instantiating the DataManager object
	 * @return a DataManager instance
	 */
	public static AsyncDataManager getInstance(Context context) {
		if (instance == null) instance = new AsyncDataManager(context);
		return instance;
	}

	/**
	 * Singleton private constructor for use by the getInstance() method.
	 * @param context the context that is instantiating the DataManager object
	 */
	private AsyncDataManager(Context context){
		pm = new PersistenceManager(context);
		pc = new PersistenceContext(pm);
	}

	/**
	 * Opens DataManager for writing.
	 */
	public void open(DatabaseCallback<Void> listener){
		DatabaseAsyncTask<Void> task = new DatabaseAsyncTask<Void>(listener) {
			@Override
			protected Void doInBackground(Void... params) {
				pm.open();
				isOpen = true;
				return null;
			}
		};
		task.execute();
	}

	/**
	 * Closes DataManager. This method calls commit() finalizing any changes before closing.
	 */
	public void close(DatabaseCallback<Void> listener){
		DatabaseAsyncTask<Void> task = new DatabaseAsyncTask<Void>(listener) {
			@Override
			protected Void doInBackground(Void... params) {
				if(!isOpen) throw new DatabaseNotOpenExpection();
				commit();
				pm.close();
				isOpen = false;
				return null;
			}
		};
		task.execute();
	}

	/**
	 * Commits all cached changes to the database.
	 */
	public void commit(DatabaseCallback<Void> listener){
		DatabaseAsyncTask<Void> task = new DatabaseAsyncTask<Void>(listener) {
			@Override
			protected Void doInBackground(Void... params) {
				commit();
				return null;
			}
		};
		task.execute();
	}
	
	private void commit(){
		if(!isOpen) throw new DatabaseNotOpenExpection();
		Queue<Directive> pendingDirectives = pc.getPendingDirectivesQueue();
		for(Directive directive : pendingDirectives){
			if(directive instanceof CreateDirective) pm.create((CreateDirective)directive);
			else if(directive instanceof DeleteDirective) pm.delete((DeleteDirective)directive);
			else if(directive instanceof UpdateDirective) pm.update((UpdateDirective)directive);
			else if(directive instanceof CreateReferenceDirective) pm.createReference((CreateReferenceDirective)directive);
			else if(directive instanceof DeleteReferenceDirective) pm.deleteReference((DeleteReferenceDirective)directive);
		}
		pc.clearPendingDirectivesQueue();
	}

	/**
	 * Adds an object to the database. This method will assign the object an id number and store it in the object's id field.
	 * @param obj the object to be stored.
	 * @return the assigned id number
	 */
	public void add(final Object obj, DatabaseCallback<Integer> listener){
		DatabaseAsyncTask<Integer> task = new DatabaseAsyncTask<Integer>(listener) {
			@Override
			protected Integer doInBackground(Void... params) {
				if(!isOpen) throw new DatabaseNotOpenExpection();
				pc.create(obj);
				return DataUtil.getId(obj);
			}
		};
		task.execute();
	}

	/**
	 * Deletes an object from the database. 
	 * @param cls the class of the object
	 * @param id the id number of the object
	 */
	public void delete(final Class<?> cls, final int id, DatabaseCallback<Void> listener){
		DatabaseAsyncTask<Void> task = new DatabaseAsyncTask<Void>(listener) {
			@Override
			protected Void doInBackground(Void... params) {
				if(!isOpen) throw new DatabaseNotOpenExpection();
				pc.delete(cls, id);
				return null;
			}
		};
		task.execute();
	}

	/**
	 * Updates an object in the database with a matching Class and id number.
	 * @param obj the object to be updated
	 */
	public <T> void update(final T obj, DatabaseCallback<Void> listener){
		DatabaseAsyncTask<Void> task = new DatabaseAsyncTask<Void>(listener) {
			@Override
			protected Void doInBackground(Void... params) {
				if(!isOpen) throw new DatabaseNotOpenExpection();
				if(obj == null) throw new NullPointerException();
				pc.update(obj);
				return null;
			}
		};
		task.execute();
	}

	/**
	 * Retrieves an object from the database.
	 * @param cls the class of the object
	 * @param id the id number of the object
	 * @return
	 */
	public <T> void get(final Class<T> cls, final int id, DatabaseCallback<T> listener){
		DatabaseAsyncTask<T> task = new DatabaseAsyncTask<T>(listener) {
			@Override
			protected T doInBackground(Void... params) {
				if(!isOpen) throw new DatabaseNotOpenExpection();
				T object = pc.getCopyFromCache(cls, id);
				if(object != null) return object;
				return pc.fetchToCache(cls, id);
			}
		};
		task.execute();
	}

	/**
	 * Retrieves copies of all stored objects of a given class in an ArrayList.
	 * @param cls the class
	 * @return an ArrayList of all stored objects of a given class
	 */
	public <T> void getAll(final Class<T> cls, DatabaseCallback<ArrayList<T>> listener){
		DatabaseAsyncTask<ArrayList<T>> task = new DatabaseAsyncTask<ArrayList<T>>(listener) {
			@Override
			protected ArrayList<T> doInBackground(Void... params) {
				if(!isOpen) throw new DatabaseNotOpenExpection();
				commit();
				ArrayList<T> list = new ArrayList<T>();
				
				Cursor cursor = pm.getCursor(cls, null);
				if(!cursor.moveToFirst()) return list;
				while(!cursor.isAfterLast()){
					int id = cursor.getInt(cursor.getColumnIndex(DataUtil.getIdField(cls).getName()));
					T object = pc.getCopyFromCache(cls, id);
					if(object != null) list.add(object);
					else list.add(pc.fetchToCache(cls, cursor));
					cursor.moveToNext();
				}
				cursor.close();
				return list;
			}
		};
		task.execute();
	}

	/**
	 * Retrieves copies of all stored objects of a given class with a given value for a field with the given name in an ArrayList
	 * @param cls the class
	 * @param fieldName the name of the field
	 * @param value the value 
	 * @return an ArrayList of all objects that match the search criteria
	 */
	public <T> void find(final Class<T> cls, final String fieldName, final int value, DatabaseCallback<ArrayList<T>> listener){
		DatabaseAsyncTask<ArrayList<T>> task = new DatabaseAsyncTask<ArrayList<T>>(listener) {
			@Override
			protected ArrayList<T> doInBackground(Void... params) {
				if(!isOpen) throw new DatabaseNotOpenExpection();
				
				Field field = null;
				for(Field containedField : DataUtil.getFields(cls)){
					if(containedField.getName().equals(fieldName)) field = containedField;
				}
				if(field == null) throw new FieldDoesNotExistException();
				if(DataUtil.getFieldTypeId(field) != DataUtil.FIELD_TYPE_INT) throw new MisMatchedFieldValueTypeException();

				commit();
				ArrayList<T> list = new ArrayList<T>();
				String SQLWhereStatement = fieldName + " = " + String.valueOf(value);
				Cursor cursor = pm.getCursor(cls, SQLWhereStatement);
				if(!cursor.moveToFirst()) return list;
				while(!cursor.isAfterLast()){
					int id = cursor.getInt(cursor.getColumnIndex(DataUtil.getIdField(cls).getName()));
					T object = pc.getCopyFromCache(cls, id);
					if(object != null) list.add(object);
					else list.add(pc.fetchToCache(cls, cursor));
					cursor.moveToNext();
				}
				cursor.close();
				return list;
			}
		};
		task.execute();
	}
	
	/**
	 * Retrieves copies of all stored objects of a given class with a given value for a field with the given name in an ArrayList
	 * @param cls the class
	 * @param fieldName the name of the field
	 * @param value the value 
	 * @return an ArrayList of all objects that match the search criteria
	 */
	public <T> void find(final Class<T> cls, final String fieldName, final float value, DatabaseCallback<ArrayList<T>> listener){
		DatabaseAsyncTask<ArrayList<T>> task = new DatabaseAsyncTask<ArrayList<T>>(listener) {
			@Override
			protected ArrayList<T> doInBackground(Void... params) {
				if(!isOpen) throw new DatabaseNotOpenExpection();
				
				Field field = null;
				for(Field containedField : DataUtil.getFields(cls)){
					if(containedField.getName().equals(fieldName)) field = containedField;
				}
				if(field == null) throw new FieldDoesNotExistException();
				if(DataUtil.getFieldTypeId(field) != DataUtil.FIELD_TYPE_INT) throw new MisMatchedFieldValueTypeException();

				commit();
				ArrayList<T> list = new ArrayList<T>();
				String SQLWhereStatement = fieldName + " = " + String.valueOf(value);
				Cursor cursor = pm.getCursor(cls, SQLWhereStatement);
				if(!cursor.moveToFirst()) return list;
				while(!cursor.isAfterLast()){
					int id = cursor.getInt(cursor.getColumnIndex(DataUtil.getIdField(cls).getName()));
					T object = pc.getCopyFromCache(cls, id);
					if(object != null) list.add(object);
					else list.add(pc.fetchToCache(cls, cursor));
					cursor.moveToNext();
				}
				cursor.close();
				return list;
			}
		};
		task.execute();
	}
	
	/**
	 * Retrieves copies of all stored objects of a given class with a given value for a field with the given name in an ArrayList
	 * @param cls the class
	 * @param fieldName the name of the field
	 * @param value the value 
	 * @return an ArrayList of all objects that match the search criteria
	 */	
	public <T> void find(final Class<T> cls, final String fieldName, final double value, DatabaseCallback<ArrayList<T>> listener){
		DatabaseAsyncTask<ArrayList<T>> task = new DatabaseAsyncTask<ArrayList<T>>(listener) {
			@Override
			protected ArrayList<T> doInBackground(Void... params) {
				if(!isOpen) throw new DatabaseNotOpenExpection();
				
				Field field = null;
				for(Field containedField : DataUtil.getFields(cls)){
					if(containedField.getName().equals(fieldName)) field = containedField;
				}
				if(field == null) throw new FieldDoesNotExistException();
				if(DataUtil.getFieldTypeId(field) != DataUtil.FIELD_TYPE_INT) throw new MisMatchedFieldValueTypeException();

				commit();
				ArrayList<T> list = new ArrayList<T>();
				String SQLWhereStatement = fieldName + " = " + String.valueOf(value);
				Cursor cursor = pm.getCursor(cls, SQLWhereStatement);
				if(!cursor.moveToFirst()) return list;
				while(!cursor.isAfterLast()){
					int id = cursor.getInt(cursor.getColumnIndex(DataUtil.getIdField(cls).getName()));
					T object = pc.getCopyFromCache(cls, id);
					if(object != null) list.add(object);
					else list.add(pc.fetchToCache(cls, cursor));
					cursor.moveToNext();
				}
				cursor.close();
				return list;
			}
		};
		task.execute();
	}
	
	/**
	 * Retrieves copies of all stored objects of a given class with a given value for a field with the given name in an ArrayList
	 * @param cls the class
	 * @param fieldName the name of the field
	 * @param value the value 
	 * @return an ArrayList of all objects that match the search criteria
	 */
	public <T> void find(final Class<T> cls, final String fieldName, final long value, DatabaseCallback<ArrayList<T>> listener){
		DatabaseAsyncTask<ArrayList<T>> task = new DatabaseAsyncTask<ArrayList<T>>(listener) {
			@Override
			protected ArrayList<T> doInBackground(Void... params) {
				if(!isOpen) throw new DatabaseNotOpenExpection();
				
				Field field = null;
				for(Field containedField : DataUtil.getFields(cls)){
					if(containedField.getName().equals(fieldName)) field = containedField;
				}
				if(field == null) throw new FieldDoesNotExistException();
				if(DataUtil.getFieldTypeId(field) != DataUtil.FIELD_TYPE_INT) throw new MisMatchedFieldValueTypeException();

				commit();
				ArrayList<T> list = new ArrayList<T>();
				String SQLWhereStatement = fieldName + " = " + String.valueOf(value);
				Cursor cursor = pm.getCursor(cls, SQLWhereStatement);
				if(!cursor.moveToFirst()) return list;
				while(!cursor.isAfterLast()){
					int id = cursor.getInt(cursor.getColumnIndex(DataUtil.getIdField(cls).getName()));
					T object = pc.getCopyFromCache(cls, id);
					if(object != null) list.add(object);
					else list.add(pc.fetchToCache(cls, cursor));
					cursor.moveToNext();
				}
				cursor.close();
				return list;
			}
		};
		task.execute();
	}
	
	/**
	 * Retrieves copies of all stored objects of a given class with a given value for a field with the given name in an ArrayList
	 * @param cls the class
	 * @param fieldName the name of the field
	 * @param value the value 
	 * @return an ArrayList of all objects that match the search criteria
	 */	
	public <T> void find(final Class<T> cls, final String fieldName, final String value, DatabaseCallback<ArrayList<T>> listener){
		DatabaseAsyncTask<ArrayList<T>> task = new DatabaseAsyncTask<ArrayList<T>>(listener) {
			@Override
			protected ArrayList<T> doInBackground(Void... params) {
				if(!isOpen) throw new DatabaseNotOpenExpection();
				
				Field field = null;
				for(Field containedField : DataUtil.getFields(cls)){
					if(containedField.getName().equals(fieldName)) field = containedField;
				}
				if(field == null) throw new FieldDoesNotExistException();
				if(DataUtil.getFieldTypeId(field) != DataUtil.FIELD_TYPE_INT) throw new MisMatchedFieldValueTypeException();

				commit();
				ArrayList<T> list = new ArrayList<T>();
				String SQLWhereStatement = fieldName + " = " + value;
				Cursor cursor = pm.getCursor(cls, SQLWhereStatement);
				if(!cursor.moveToFirst()) return list;
				while(!cursor.isAfterLast()){
					int id = cursor.getInt(cursor.getColumnIndex(DataUtil.getIdField(cls).getName()));
					T object = pc.getCopyFromCache(cls, id);
					if(object != null) list.add(object);
					else list.add(pc.fetchToCache(cls, cursor));
					cursor.moveToNext();
				}
				cursor.close();
				return list;
			}
		};
		task.execute();
	}
	
	/**
	 * Retrieves copies of all stored objects of a given class with a given value for a field with the given name in an ArrayList
	 * @param cls the class
	 * @param fieldName the name of the field
	 * @param value the value 
	 * @return an ArrayList of all objects that match the search criteria
	 */
	public <T> void find(final Class<T> cls, final String fieldName, final boolean value, DatabaseCallback<ArrayList<T>> listener){
		DatabaseAsyncTask<ArrayList<T>> task = new DatabaseAsyncTask<ArrayList<T>>(listener) {
			@Override
			protected ArrayList<T> doInBackground(Void... params) {
				if(!isOpen) throw new DatabaseNotOpenExpection();
				
				Field field = null;
				for(Field containedField : DataUtil.getFields(cls)){
					if(containedField.getName().equals(fieldName)) field = containedField;
				}
				if(field == null) throw new FieldDoesNotExistException();
				if(DataUtil.getFieldTypeId(field) != DataUtil.FIELD_TYPE_INT) throw new MisMatchedFieldValueTypeException();

				commit();
				ArrayList<T> list = new ArrayList<T>();
				String SQLWhereStatement = fieldName + " = " + String.valueOf(value);
				Cursor cursor = pm.getCursor(cls, SQLWhereStatement);
				if(!cursor.moveToFirst()) return list;
				while(!cursor.isAfterLast()){
					int id = cursor.getInt(cursor.getColumnIndex(DataUtil.getIdField(cls).getName()));
					T object = pc.getCopyFromCache(cls, id);
					if(object != null) list.add(object);
					else list.add(pc.fetchToCache(cls, cursor));
					cursor.moveToNext();
				}
				cursor.close();
				return list;
			}
		};
		task.execute();
	}

	/**
	 * Returns the number of objects of a given class that are stored in the database.
	 * @param cls the class
	 * @return the number of stored objects of the given class
	 */
	public <T> void size(final Class<T> cls, DatabaseCallback<Integer> listener){
		DatabaseAsyncTask<Integer> task = new DatabaseAsyncTask<Integer>(listener) {
			@Override
			protected Integer doInBackground(Void... params) {
				if(!isOpen) throw new DatabaseNotOpenExpection();
				commit();
				return pm.size(cls);
			}
		};
		task.execute();
	}

	/**
	 * Drops all records of a given class from the database.
	 * @param recordName the name of the class to be deleted
	 */
	public void dropRecords(final String recordName, DatabaseCallback<Void> listener){
		DatabaseAsyncTask<Void> task = new DatabaseAsyncTask<Void>(listener) {
			@Override
			protected Void doInBackground(Void... params) {
				if(!isOpen) throw new DatabaseNotOpenExpection();
				pm.dropRecords(recordName);
				return null;
			}
		};
		task.execute();
	}

	/**
	 * Sets the default value to be given to added numerical fields when an class is changed. When fields are added to a stored class, DataManagement automatically
	 * adds these fields to existing objects in the database. If these fields are numerical, they can be given a default value when the objects are retrieved. If the
	 * added fields are not primitive, they are defaulted to null.
	 * @param value
	 */
	public void setDefaultUpgradeValue(int value){
		if(!isOpen) throw new DatabaseNotOpenExpection();
		pm.setDefaultUpgradeValue(value);
	}

	/**
	 * Returns true if the database is open.
	 * @return true if database is open
	 */
	public boolean isOpen(){
		return isOpen;
	}
}
