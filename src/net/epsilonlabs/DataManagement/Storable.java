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
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * This class is used to move objects from Java Objects to ContentValues and from Cursors back to their original objects.
 * It relies heavily on Reflection to make this conversion.
 * @author Tom Caputi
 *
 * @param <T> The class of the object being stored
 */
public class Storable<T> {

	private T object; // The object being stored
	private Class<?> typeT; // The class of the object being stored
	private int id; // The id number of the object
	private Field idField; // The Field that represents the id of the object

	/**
	 * Constructor used by {@link DataManager} for creating new Storable objects when calling add() or update().
	 * @param object - The object to be stored
	 * @param idField - The field that represents the id number
	 */
	public Storable(T object, Field idField){
		this.object = object;
		this.typeT = object.getClass();
		this.idField = idField;
	}

	/**
	 * Constructor using Cursor objects returned by {@link SQLiteDatabase} queries. This generates the original object from values in the Cursor.
	 * @param c - The Cursor returned by a {@link SQLiteDatabase} query
	 * @param typeT - The class of the object to be stored
	 * @param idField - The field that represents the id number
	 */
	public Storable(Cursor c, Class<T> typeT, Field idField){
		this.typeT = typeT;
		this.idField = idField;

		try{
			this.object = typeT.newInstance();
			Field[] fields = DataUtil.removeFinalFields(typeT.getDeclaredFields());
			Field.setAccessible(fields, true);
			for(Field field : fields){
				switch(DataUtil.getFieldTypeId(field)){
				case DataUtil.FIELD_TYPE_INT:
					field.set(object, c.getInt(c.getColumnIndex(field.getName())));
					break;
				case DataUtil.FIELD_TYPE_DOUBLE:
					field.set(object, c.getDouble(c.getColumnIndex(field.getName())));
					break;
				case DataUtil.FIELD_TYPE_FLOAT:
					field.set(object, c.getFloat(c.getColumnIndex(field.getName())));
					break;
				case DataUtil.FIELD_TYPE_LONG:
					field.set(object, c.getLong(c.getColumnIndex(field.getName())));
					break;
				case DataUtil.FIELD_TYPE_STRING:
					field.set(object, c.getString(c.getColumnIndex(field.getName())));
					break;
				case DataUtil.FIELD_TYPE_BOOLEAN:
					if(c.getInt(c.getColumnIndex(field.getName())) == 1) field.set(object, true);
					else field.set(object, false);
					break;
				}

				if(field.getName().equals(idField.getName())) id = field.getInt(object);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * Generates and returns a {@link ContentValues} object from the stored object. These values are then used to store the object in an {@link SQLiteDatabase}.
	 * @return {@link ContentValues} with values and keys representing each declared field in the object
	 */
	public ContentValues getContentValues(){
		try{
			ContentValues cv = new ContentValues();
			Field[] fields = DataUtil.removeFinalFields(typeT.getDeclaredFields());
			Field.setAccessible(fields, true);
			for(Field field : fields){
				if(!field.getName().equals(idField.getName())){
					switch(DataUtil.getFieldTypeId(field)){
					case DataUtil.FIELD_TYPE_INT:
						cv.put(field.getName(), field.getInt(object));
						break;
					case DataUtil.FIELD_TYPE_DOUBLE:
						cv.put(field.getName(), field.getDouble(object));
						break;
					case DataUtil.FIELD_TYPE_FLOAT:
						cv.put(field.getName(), field.getFloat(object));
						break;
					case DataUtil.FIELD_TYPE_LONG:
						cv.put(field.getName(), field.getLong(object));
						break;
					case DataUtil.FIELD_TYPE_STRING:
						cv.put(field.getName(),(String) field.get(object));
						break;
					case DataUtil.FIELD_TYPE_BOOLEAN:
						if(field.getBoolean(object) == true) cv.put(field.getName(), 1);
						else cv.put(field.getName(), 0);
						break;
					}
				}
			}
			return cv;	
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public T getObject() {
		return object;
	}
}
