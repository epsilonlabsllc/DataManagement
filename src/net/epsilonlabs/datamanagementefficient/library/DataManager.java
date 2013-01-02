package net.epsilonlabs.datamanagementefficient.library;

import java.util.Queue;

import net.epsilonlabs.datamanagementefficient.directive.CreateDirective;
import net.epsilonlabs.datamanagementefficient.directive.CreateReferenceDirective;
import net.epsilonlabs.datamanagementefficient.directive.DeleteDirective;
import net.epsilonlabs.datamanagementefficient.directive.DeleteReferenceDirective;
import net.epsilonlabs.datamanagementefficient.directive.Directive;
import net.epsilonlabs.datamanagementefficient.directive.UpdateDirective;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DataManager {
	private SQLHelper helper;
	private SQLiteDatabase db = null;
	private PersistenceContext pc = null;
	private PersistenceManager pm = null;
	
	public DataManager(Context context){
		this.helper = new SQLHelper(context);
	}
	
	public void open(){
		db = helper.getWritableDatabase();
		pc = new PersistenceContext(db);
		pm = new PersistenceManager(db);
	}
	
	public void close(){
		commit();
		db.close();
	}
	
	public void commit(){
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
		pc.create(obj);
		return DataUtil.getId(obj);
	}
	
	public void delete(Class<?> cls, int id){
		pc.delete(cls, id);
	}
	
	public <T> void update(T obj){
		pc.update(obj);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> cls, int id){
		T object = (T) pc.getFromCache(cls, id);
		if(object != null) return object;
		String tableName = cls.getSimpleName();
		String SQLSelectionStatement = DataUtil.getIdField(cls).getName() + " = " + String.valueOf(id);
		Cursor cursor = db.query(tableName, null, SQLSelectionStatement, null, null, null, null);
		if(!cursor.moveToFirst()) return null;
		object = pm.fetch(cls, cursor);
		cursor.close();
		return object;
	}
	
	public <T> int size(Class<T> cls){
		Cursor c = db.query(cls.getSimpleName(), null, null , null, null, null, null);
		int size = c.getCount();
		c.close();
		return size;
	}
}
