/*
DataManagement Android Library for Storing Objects Permanently
Copyright (C) 2012 Thomas Caputi

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.epsilonlabs.DataManagement;

import java.lang.reflect.Field;

import net.epsilonlabs.DataManagement.annotations.Id;
import net.epsilonlabs.DataManagement.exception.IllegalIdFieldException;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * This class generates and stores all information that {@link SQLiteDatabase} requires to create a table for a given class of objects to be stored.
 * It also stores information that {@link DataManager} uses to query the table, add to the table, update an object in the table, or delete an object from the table.
 * @author Tom Caputi
 *
 * @param <T> - the class being stored
 */
public class Table<T> {

	private String dropStatement; // SQL drop statement. currently unused (dropping tables not currently supported)
	private String createStatement; // SQL create statement
	private String tableName; // Name of this table
	private Class<T> typeT; // the class being stored
	private Field idField; // the id number field

	/**
	 * Constructor for Table class. Instantiates global variables.
	 * @param context - the context that is using this database
	 * @param typeT - the class being stored in this table
	 */
	public Table(Context context, Class<T> typeT) {
		populateValuesFromClass(typeT);
		this.typeT = typeT;
		for(Field field : typeT.getDeclaredFields()){
			if(field.isAnnotationPresent(Id.class)){
				if(!field.getType().getName().equals("int")){
					throw new IllegalIdFieldException();
				}
				idField = field;
			}
		}
		if(idField == null) {
			throw new IllegalIdFieldException();
		}
	}

	/**
	 * This generates values for dropStatement, createStatement, and tableName from the class
	 * @param cls
	 */
	private void populateValuesFromClass(Class<?> cls){
		this.tableName = cls.getCanonicalName().replace(".", "_");
		this.dropStatement = "DROP TABLE IF EXISTS " + tableName;
		Field[] declaredFields = DataUtil.removeFinalFields(cls.getDeclaredFields());
		String[][] fields = new String[declaredFields.length][3];

		for(int i=0; i<declaredFields.length; i++){
			fields[i][0] = declaredFields[i].getName();

			switch(DataUtil.getFieldTypeId(declaredFields[i])){
			case 0:
				fields[i][1] = DataUtil.INT_FIELD;
				break;
			case 1:
				fields[i][1] = DataUtil.REAL_FIELD;
				break;
			case 2:
				fields[i][1] = DataUtil.REAL_FIELD;
				break;
			case 3:
				fields[i][1] = DataUtil.REAL_FIELD;
				break;
			case 4:
				fields[i][1] = DataUtil.TEXT_FIELD;
				break;
			case 5:
				fields[i][1] = DataUtil.INT_FIELD;
				break;
			}

			if(declaredFields[i].isAnnotationPresent(Id.class)) fields[i][2] = DataUtil.AUTO_PRIMARY_KEY;
			else fields[i][2] = DataUtil.NO_PROPERTY;
		}

		createStatement = "CREATE TABLE IF NOT EXISTS " + tableName + "(";
		for(int i=0; i<fields.length;i++){
			if(i != fields.length-1) createStatement += fields[i][0] + " " + fields[i][1] + fields[i][2] + ", ";
			else createStatement +=  fields[i][0] + " " + fields[i][1] + fields[i][2] + ");";
		}
	}

	/**
	 * Returns the Field that holds the id number, as denoted by the @Id annotation in that class.
	 * @return the Field that holds the id number
	 */
	public Field getIdField(){
		return idField;
	}

	/**
	 * Returns the class being stored in this table.
	 * @return the class being stored in this table
	 */
	public Class<T> getDataType(){
		return typeT;
	}

	/**
	 * Returns the name of this table.
	 * @return the table name
	 */
	public String getTableName(){
		return this.tableName;
	}

	/**
	 * Returns the SQL drop statement for this table. (Currently unused)
	 * @return dropStatement
	 */
	public String getDropStatement(){
		return this.dropStatement;
	}

	/**
	 * Returns the SQL create statement for this table. (Currently unused)
	 * @return createStatement
	 */
	public String getCreateStatement(){
		return this.createStatement;
	}
}
