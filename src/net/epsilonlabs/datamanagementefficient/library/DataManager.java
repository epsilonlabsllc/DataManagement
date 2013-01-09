package net.epsilonlabs.datamanagementefficient.library;

import java.util.ArrayList;
import java.util.Queue;

import net.epsilonlabs.datamanagementefficient.directive.CreateDirective;
import net.epsilonlabs.datamanagementefficient.directive.CreateReferenceDirective;
import net.epsilonlabs.datamanagementefficient.directive.DeleteDirective;
import net.epsilonlabs.datamanagementefficient.directive.DeleteReferenceDirective;
import net.epsilonlabs.datamanagementefficient.directive.Directive;
import net.epsilonlabs.datamanagementefficient.directive.UpdateDirective;
import net.epsilonlabs.datamanagementefficient.exception.DatabaseNotOpenExpection;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.SparseArray;

public class DataManager {

	private static DataManager instance;

	private SQLiteDatabase db = null;
	private PersistenceContext pc = null;
	private PersistenceManager pm = null;
	private boolean isOpen = false;
	private SQLHelper helper;

	public static DataManager getInstance(Context context) {
		if (instance == null) {
			instance = new DataManager(context);
		}
		return instance;
	}

	private DataManager(Context context){
		this.helper = new SQLHelper(context);
	}

	public void open(){
		db = helper.getWritableDatabase();
		pm = new PersistenceManager(db);
		pc = new PersistenceContext(pm);
		isOpen = true;
	}

	public void close(){
		commit();
		db.close();
		isOpen = false;
	}

	public void commit(){
		if(!isOpen) throw new DatabaseNotOpenExpection();
		Queue<Directive> pendingDirectives = pc.getPendingDirectivesQueue();
		for(Directive directive : pendingDirectives){
			if(directive instanceof CreateDirective){
				pm.create((CreateDirective)directive);
			}else if(directive instanceof DeleteDirective){
				pm.delete((DeleteDirective)directive);
			}else if(directive instanceof UpdateDirective){
				pm.update((UpdateDirective)directive);
			}else if(directive instanceof CreateReferenceDirective){
				pm.createReference((CreateReferenceDirective)directive);
			}else if(directive instanceof DeleteReferenceDirective){
				pm.deleteReference((DeleteReferenceDirective)directive);
			}
		}
		pc.clearPendingDirectivesQueue();
	}

	public int add(Object obj){
		if(!isOpen) throw new DatabaseNotOpenExpection();
		pc.create(obj);
		return DataUtil.getId(obj);
	}

	public void delete(Class<?> cls, int id){
		if(!isOpen) throw new DatabaseNotOpenExpection();
		pc.delete(cls, id);
	}

	public <T> void update(T obj){
		if(!isOpen) throw new DatabaseNotOpenExpection();
		if(obj == null) throw new NullPointerException();
		pc.update(obj);
	}

	public <T> T get(Class<T> cls, int id){
		if(!isOpen) throw new DatabaseNotOpenExpection();
		T object = pc.getFromCache(cls, id);
		if(object != null) return object;
		return pc.fetchToCache(cls, id);
	}

	public <T> ArrayList<T> getAll(Class<T> cls){
		if(!isOpen) throw new DatabaseNotOpenExpection();
		ArrayList<T> list = new ArrayList<T>();

		SparseArray<T> cachedObjects = pc.getAllFromCache(cls);
		for(int i=0; i<cachedObjects.size(); i++){
			list.add(cachedObjects.get(cachedObjects.keyAt(i)));
		}
		Cursor cursor = null;
		try{
			cursor = db.query(cls.getSimpleName(), null, null, null, null, null, null);
		}catch(SQLException e){
			return list;
		}
		if(!cursor.moveToFirst()) return list;
		while(!cursor.isAfterLast()){
			int id = cursor.getInt(cursor.getColumnIndex(DataUtil.getIdField(cls).getName()));
			if(cachedObjects.get(id) == null){
				T object = pc.getFromCache(cls, id);
				if(object != null) list.add(object);
				list.add(pc.fetchToCache(cls, cursor));
				cursor.moveToNext();
			}
		}
		cursor.close();
		return list;
	}

	public <T> int size(Class<T> cls){
		return pm.size(cls);
	}

	public void dropRecords(String recordName){
		pm.dropRecords(recordName);
	}

	public void setDefaultUpgradeValue(int value){
		pm.setDefaultUpgradeValue(value);
	}
	public PersistenceContext getPc() {
		return pc;
	}

	public boolean isOpen(){
		return isOpen;
	}
}
