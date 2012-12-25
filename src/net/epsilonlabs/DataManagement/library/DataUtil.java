package net.epsilonlabs.DataManagement.library;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;

import net.epsilonlabs.DataManagement.annotations.Id;
import net.epsilonlabs.DataManagement.exception.IdFieldDoesNotExistException;
import net.epsilonlabs.DataManagement.exception.IdFieldIsNotIntException;

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
	public static final int FIELD_TYPE_OTHER = 6;
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
		else return FIELD_TYPE_OTHER;
	}

	/**
	 * Removes final fields from an Array of fields and returns it. This prevents predetermined fields from being stored unnecessarily.
	 * @param allFields - An array of Fields
	 * @return - The fields in allFields that are not final
	 */
	public static Field[] removeFinalFields(Field[] allFields){
		ArrayList<Field> newFieldList = new ArrayList<Field>();
		for(Field field: allFields){
			if (!java.lang.reflect.Modifier.isFinal(field.getModifiers())) newFieldList.add(field);
		}
		return (Field[]) newFieldList.toArray(new Field[newFieldList.size()]);
	}

	/**
	 * Returns the field of a given class which has the @Id notation
	 * @param cls the class to be searched for an id field
	 * @return the id field of the class
	 */
	public static Field getIdField(Class<?> cls){
		for(Field field : removeFinalFields(cls.getDeclaredFields())){
			if(field.getAnnotation(Id.class) != null){
				if(field.getType().getName().equals("int")) return field;
				throw new IdFieldIsNotIntException();
			}
		}
		throw new IdFieldDoesNotExistException();
	}

	/**
	 * Returns the class stored within a Collection
	 * @param field the Collection as a field of a given class
	 * @return the stored class within the collection
	 */
	public static Class<?> getStoredClassOfCollection(Field field){
		ParameterizedType listType = (ParameterizedType) field.getGenericType();
		return (Class<?>) listType.getActualTypeArguments()[0];
	}
}