DataManagement
==============

Android Local Database Library for Easily Storing Objects
---------------------------------------------------------------

DataManagement is a Java Android library designed to help easily and efficiently store aggregate classes to an SQLite database.  It eliminates the need to write separate classes to manage database – object interactions and allows developers to use simple methods to store, query, update, and delete objects. The library is capable of storing all objects of classes whose instance variables are either primitive data types or are themselves objects of another storable class.
The DataManagement Library condenses many standard database features into several simple methods:

Examples:
---------------------------------------------------------------
###Creating a Storable Class:

	public class StorableClass{
		@Id
		private int ident;
		private int num1;
		private double num2;
		private String num3;
		private boolean num4;
		public static final int num5 = 3;
		private Collection<OtherStorableClass> col;
	}
A storable class must meet three requirements. First, the class must have a private instance variable of type int that will be used as the id number of the object. This variable may be read by the application, but the application should not have the capability to write to or change this variable in anyway. This variable is identified by the system with an @Id annotation. In addition, the class should not have any instance variables that are not either primitive types, strings, or other storable objects. Finally, the class must have an empty constructor.

###Instantiating a DataManager Object:

	DataManager dm = DataManager.newInstance(context);
The static constructor accepts the calling Context that is going to use the database. Usually this should be the calling Activity.

###Opening a Database for Use:

	dm.open();
This method must be called before the database is used in any way.

###Closing a Database After Use:

	dm.close();
This method should be called after all database operations have been performed.

###Adding an Object to the Database:

	StorableClass myObj = new StorableClass();
	int id = dm.add(myObj);
The add method accepts an object of a storable class as its only parameter and adds it to the database. It returns its id in the database for future use.

###Retrieving a Specific Item from the Database by ID:

	StorableClass storableObject = dm.get(StorableClass.class, id);
The get method accepts two parameters: the data type of the stored object and the Id number of the object (the return value of the add method).

###Retrieving All Objects of a Given Type Stored in the Database as a Collection:

	storableObjectCollection = dm.getAll(StorableClass.class);
The getAll method’s only parameter is the class of the objects that should be retrieved.

###Retrieving a Collection of Storable Objects that match a given criteria:

	Collection<StorableClass> storableObjectCollection = dm.find(StorableClass.class, 5, "num1");
The find method accepts three parameters: the data type of the stored object, the value that is being searched for, and the name of the instance variable as a string. This method is overloaded in such a way that the second parameter may be any primitive value or a string.

###Updating an Object in the Database:

	dm.update(updatedObject);
The update method accepts  the updated object that will replace the existing one I the database. For safest use, updatedObject should be an object retrieved from the database using dm.get(). This will ensure that the correct object is updated in the database.

###Deleting an Object by its Id number:

	dm.delete(StorableClass.class, id);
The delete method accepts two parameters: The data type and id number of the object to be deleted.

###Upgrading Tables in the Database

DataManagement is meant to allow users with little or no SQLite experience to be able to store and retreive Objects without dealing with tables directly. As a result, DataManagement handles table upgrades automatically. Table alterations are performed at runtime he next time that DataManagement is asked to use that class.
Note: When adding and removing variables, all unaltered variable data will remain intact. When adding a variable, all existing Objects in the database will be given a default value for that variable. If that variable is numeric, you may change that value via setDefaultValue(). otherwise the data will be defaulted to null. At this time, DataManagement can only handle variable additions and removals; renaming a variable will delete all the data for that variable in the database.
 

Additional Notes:
-----------------------------------------
Id numbers are used by the database to ensure that objects are put in the correct place and to allow the program to access these objects. It is important that programs using this library do not attempt to set these variables as they will be initialized and managed by the library. These id numbers are unique for objects of a given type; objects of different types may have the same id number. In addition, if objects are deleted from the database their id numbers are left empty and are not reused. Another important note is that Strings are considered primitves for the purposes of this library. Thus, for instance, an ArrayList of Strings cannot currently be stored directly since they do not have id numbers. This can be accomplished by creating a simple wrapper class.

Contact Us:
-----------------------------------------
If you have any questions or comments about this library feel free to email us at contactus@epsilonlabsllc.com

Licensing:
-----------------------------------------
DataManagement is Currently Licensed under the GNU General Public License, version 3 (GPL-3.0). It is intended for open source use by anyone who would like to use it.