package net.epsilonlabs.datamanagementefficient.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.util.SparseArray;

public class Cache {
	
	private Map<Class<?>, SparseArray<Object>> cache;
	
	public Cache(){
		this.cache = new HashMap<Class<?>, SparseArray<Object>>();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> cls, int id){
		SparseArray<Object> classCache = cache.get(cls);
		if(classCache == null) return null;
		return (T) classCache.get(id);
	}
	
	@SuppressWarnings("unchecked")
	public <T> SparseArray<T> getAll(Class<T> cls){
		SparseArray<T> classMap = (SparseArray<T>) cache.get(cls);
		if(classMap == null) return new SparseArray<T>();
		return classMap;
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
	
	public ArrayList<Object> getAllCachedObjects(){
		ArrayList<Object> allObjectList = new ArrayList<Object>();
		for(Class<?> cls : cache.keySet()){
			SparseArray<Object> classList =  cache.get(cls);
			for(int i=0; i<classList.size(); i++){
				allObjectList.add(classList.get(classList.keyAt(i)));
			}
		}
		return allObjectList;
	}
}
