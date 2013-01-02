package net.epsilonlabs.datamanagementefficient.library;

import java.util.HashMap;
import java.util.Map;

import android.util.SparseArray;

public class Cache {
	
	private Map<Class<?>, SparseArray<Object>> cache;
	
	public Cache(){
		this.cache = new HashMap<Class<?>, SparseArray<Object>>();
	}
	
	public Object get(Class<?> cls, int id){
		SparseArray<Object> classCache = cache.get(cls);
		if(classCache == null) return null;
		return classCache.get(id);
	}
	
	public void put(Object obj){
		Class<?> cls = obj.getClass();
		int id = DataUtil.getId(obj);
		SparseArray<Object> classCache = cache.get(cls);
		if(classCache == null) classCache = new SparseArray<Object>();
		classCache.put(id, obj);
		cache.put(cls, classCache);
	}
	
	public boolean remove(Class<?> cls, int id){
		SparseArray<Object> classCache = cache.get(cls);
		if(classCache == null) return false;
		if(classCache.get(id) == null) return false;
		classCache.remove(id);
		return true;
	}
}
