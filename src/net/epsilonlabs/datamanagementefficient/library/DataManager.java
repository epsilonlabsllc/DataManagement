package net.epsilonlabs.datamanagementefficient.library;

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
import android.database.sqlite.SQLiteDatabase;

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

	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> cls, int id){
		if(!isOpen) throw new DatabaseNotOpenExpection();
		Object object = pc.getFromCache(cls, id);
		if(object != null) return (T) object;
		return (T) pc.fetchToCache(cls, id);
	}

	public <T> int size(Class<T> cls){
		if(!isOpen) throw new DatabaseNotOpenExpection();
		Cursor c = db.query(cls.getSimpleName(), null, null , null, null, null, null);
		int size = c.getCount();
		c.close();
		return size;
	}
	
	public void setDefaultUpgradeValue(int value){
		pm.setDefaultUpgradeValue(value);
	}

	public SQLiteDatabase getDb() {
		return db;
	}

	public PersistenceContext getPc() {
		return pc;
	}

	public boolean isOpen(){
		return isOpen;
	}
}
