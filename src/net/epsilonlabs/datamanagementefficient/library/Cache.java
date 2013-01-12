package net.epsilonlabs.datamanagementefficient.library;

import java.util.HashMap;
import java.util.Map;

import android.util.SparseArray;

/**
 * The Cache class is used to store objects in a map based on their class and id number.
 * @author Tom Caputi
 *
 */
public class Cache {
	
	private Map<Class<?>, SparseArray<Object>> cache; //The actual map that stores all the cached Objects
	
	/**
	 * Instantiates the cache map
	 */
	public Cache(){
		this.cache = new HashMap<Class<?>, SparseArray<Object>>();
	}
	
	/**
	 * Gets an object from the map based on its Class and id number. Returns null if there is no such object
	 * @param cls the class of the object
	 * @param id the id number of the object
	 * @return the cached object of the given class
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> cls, int id){
		SparseArray<Object> classCache = cache.get(cls);
		if(classCache == null) return null;
		return (T) classCache.get(id);
	}
	
	/**
	 * Places an object into the cache. Replaces existing objects with the same class and id number.
	 * @param obj the object to be placed into the cache
	 */
	public void put(Object obj){
		Class<?> cls = obj.getClass();
		int id = DataUtil.getId(obj);
		SparseArray<Object> classCache = cache.get(cls);
		if(classCache == null) classCache = new SparseArray<Object>();
		classCache.put(id, obj);
		cache.put(cls, classCache);
	}
	
	/**
	 * Removes an object from the cache.
	 * @param cls the class of the object
	 * @param id the id number of the object
	 * @return true if an object existed and was successfully removed
	 */
	public boolean remove(Class<?> cls, int id){
		SparseArray<Object> classCache = cache.get(cls);
		if(classCache == null) return false;
		if(classCache.get(id) == null) return false;
		classCache.remove(id);
		return true;
	}
}
