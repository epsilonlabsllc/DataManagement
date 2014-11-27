package net.epsilonlabs.datamanagementefficient.library;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import net.epsilonlabs.datamanagementefficient.annotations.Id;
import net.epsilonlabs.datamanagementefficient.exception.IdFieldDoesNotExistException;
import net.epsilonlabs.datamanagementefficient.exception.IdFieldIsInaccessibleException;
import net.epsilonlabs.datamanagementefficient.exception.IdFieldIsNotIntException;
import net.epsilonlabs.datamanagementefficient.exception.InstanceCloneFailedException;

/**
 * This class contains values and methods that are used by several classes in this library. All variables and methods are public and static.
 * @author Tom Caputi
 *
 */
public class DataUtil {

	//Data types
	public static final String NULL_FIELD = "NULL";
	public static final String INT_FIELD = "INTEGER";
	public static final String REAL_FIELD = "REAL";
	public static final String TEXT_FIELD = "TEXT";

	//identifiers for internal method use
	public static final int FIELD_TYPE_INT = 0;
	public static final int FIELD_TYPE_DOUBLE = 1;
	public static final int FIELD_TYPE_FLOAT = 2;
	public static final int FIELD_TYPE_LONG = 3;
	public static final int FIELD_TYPE_STRING = 4;
	public static final int FIELD_TYPE_BOOLEAN = 5;
	public static final int FIELD_TYPE_NON_PRIMITIVE = 6;
	public static final int FIELD_TYPE_COLLECTION = 7;

	//Data properties
	public static final String NO_PROPERTY = "";
	public static final String NOT_NULL = " NOT NULL";
	public static final String UNIQUE = " UNIQUE";
	public static final String PRIMARY_KEY = " PRIMARY KEY";
	public static final String AUTO_PRIMARY_KEY = " PRIMARY KEY AUTOINCREMENT";
	public static final String FOREIGN_KEY = " FOREIGN KEY";

	/**
	 * Returns the type of data a given field contains as an int. This int is used to quickly and efficiently determine how the field should be interpreted (See FIELD_TYPE... variables).
	 * @param field - The field that is checked for type
	 * @return The field type as an int
	 */
	public static int getFieldTypeId(Field field){
		if(field.getType().getName().equals("int")) return FIELD_TYPE_INT;
		else if(field.getType().getName().equals("double")) return FIELD_TYPE_DOUBLE;
		else if(field.getType().getName().equals("float")) return FIELD_TYPE_FLOAT;
		else if(field.getType().getName().equals("long")) return FIELD_TYPE_LONG;
		else if(field.getType().getName().equals("java.lang.String")) return FIELD_TYPE_STRING;
		else if(field.getType().getName().equals("boolean")) return FIELD_TYPE_BOOLEAN;
		else if(Collection.class.isAssignableFrom(field.getType())) return FIELD_TYPE_COLLECTION;
		else return FIELD_TYPE_NON_PRIMITIVE;
	}
	
	/**
	 * Gets the fields of a given class that will be stored. Includes all Fields of superclasses of the given class. Final fields are nor stored.
	 * Before they are returned, they are set to be accessible.
	 * @param cls the class
	 * @return a Field[] holding all of the Fields contained by the class and its superclasses. 
	 */
	public static Field[] getFields(Class<?> cls){
		LinkedList<Field> fieldList = getFields(cls, new LinkedList<Field>());
		Field[] fieldArray = (Field[]) fieldList.toArray(new Field[fieldList.size()]);
		Field.setAccessible(fieldArray, true);
		return fieldArray;
	}
	
	/**
	 * Helper method used by getAll(Class<?> cls) to recursively get all fields of a given class and its superclasses.
	 * @param cls the class
	 * @param fields the current list of Fields that have been collected
	 * @return a LinkedList of all the Fields in the given class and its superclasses
	 */
	private static LinkedList<Field> getFields(Class<?> cls, LinkedList<Field> fields){
		for (Field field: cls.getDeclaredFields()) {
			if (!java.lang.reflect.Modifier.isFinal(field.getModifiers())) fields.add(field);
	    }

		if (cls.getSuperclass() != null && cls.getSuperclass() != java.lang.Object.class) fields = getFields(cls.getSuperclass(), fields);
	    return fields;
	}

	/**
	 * Returns the field of a given class which has the @Id notation.
	 * @param cls the class to be searched for an id field
	 * @return the id field of the class
	 */
	public static Field getIdField(Class<?> cls){
		for(Field field : getFields(cls)){
			if(field.getAnnotation(Id.class) != null){
				if(field.getType().getName().equals("int")){
					field.setAccessible(true);
					return field;
				}
				throw new IdFieldIsNotIntException();
			}
		}
		throw new IdFieldDoesNotExistException();
	}
	
	/**
	 * Returns the id number of a given storable object.
	 * @param obj the object
	 * @return the id number
	 */
	public static int getId(Object obj){
		Class<?> cls = obj.getClass();
		Field idField = getIdField(cls);
		idField.setAccessible(true);
		try {
			return idField.getInt(obj);
		}catch (IllegalAccessException e) {
			throw new IdFieldIsInaccessibleException();
		}
	}

	/**
	 * Returns the class stored within a Collection.
	 * @param field the Collection as a field of a given class
	 * @return the stored class within the collection
	 */
	public static Class<?> getStoredClassOfCollection(Field field){
		ParameterizedType listType = (ParameterizedType) field.getGenericType();
		return (Class<?>) listType.getActualTypeArguments()[0];
	}
	
	/**
	 * Gets the name of the table from a given class
	 * @param cls the class
	 * @return the name of the table that is holding the given class 
	 */
	public static String getTableName(Class<?> cls){
		return cls.getCanonicalName().replace(".", "_");
	}
	
	/**
	 * Returns a copy of the given object.
	 * @param instance the original object 
	 * @return a copy of the object
	 */
	public static <T> T copy(T instance){
		return copy(instance, new Cache());
	}

	/**
	 * Helper method used by copy(T instance) to recursively copy all the Fields of an object to a new object
	 * @param instance the original object
	 * @param previosulyClonedObjects a map of objects that have already been copied
	 * @return the copy of the object
	 */
	@SuppressWarnings({ "unchecked" })
	private static <T> T copy(T instance, Cache previosulyClonedObjects){
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
				typeField.set(newInstance, copy(typeField.get(instance), previosulyClonedObjects));
			}

			for(Field typeField : nonPrimitveCollectionFieldQueue){
				if(typeField.get(instance) == null){
					typeField.set(newInstance, null);
				}else{
					Collection<Object> newCollection = (Collection<Object>) typeField.getType().newInstance();
					for (Object containedObj : (Collection<?>) typeField.get(instance)) {
						newCollection.add(copy(containedObj, previosulyClonedObjects));
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
}