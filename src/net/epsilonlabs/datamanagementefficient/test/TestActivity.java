package net.epsilonlabs.datamanagementefficient.test;

import java.lang.reflect.Field;

import net.epsilonlabs.datamanagementefficient.exception.InaccessableObjectException;
import net.epsilonlabs.datamanagementefficient.library.DataManager;
import net.epsilonlabs.datamanagementefficient.library.DataUtil;
import android.os.Bundle;
import android.widget.TextView;
import android.app.Activity;

public class TestActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TextView tv = new TextView(this);
		DataManager dm = new DataManager(this);
		dm.open();
		
		DataSample ds1 = new DataSample();
		DataSample ds2 = new DataSample();
//		ds2.getDs2().add(new DataSample2());
		
		boolean test = areEqual(ds1, ds2);
		
//		int id = dm.add(new DataSample());
//		DataSample ds = dm.get(DataSample.class, id);
//		ds.getDs2().add(new DataSample2());
//		dm.update(ds);
//		dm.commit();
//		DataSample sameDs = dm.get(DataSample.class, id);
//		
//		tv.setText(sameDs.toString());
		
		dm.close();
		setContentView(tv);
	}
	
	private <T> boolean areEqual(T obj1, T obj2){
		
		if(obj1 == null && obj2 == null) return true;
		if((obj1 == null && obj2 != null) || (obj1 != null && obj2 == null)) return false;
		
		if(obj1 instanceof Integer || obj1 instanceof Double || obj1 instanceof Float || obj1 instanceof Long || obj1 instanceof String || obj1 instanceof Boolean){
			return obj1.equals(obj2);
		}
		
		Class<?> type = obj1.getClass();
		Object obj1value = null;
		Object obj2value = null;
		for(Field field : DataUtil.getFields(type)){
			try {
				obj1value = field.get(obj1);
				obj2value = field.get(obj2);
			}catch (IllegalAccessException e) {
				throw new InaccessableObjectException();
			}
			
			if(!areEqual(obj1value, obj2value)) return false;
		}
		return true;
	}
}
