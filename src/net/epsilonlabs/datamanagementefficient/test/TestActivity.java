package net.epsilonlabs.datamanagementefficient.test;

import java.util.ArrayList;

import net.epsilonlabs.datamanagementefficient.debug.Debugger;
import net.epsilonlabs.datamanagementefficient.library.DataManager;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class TestActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TextView tv = new TextView(this);
		DataManager dm = new DataManager(this);
		Debugger debugger = new Debugger(dm);
		ArrayList<Class<?>> storedClasses = new ArrayList<Class<?>>();
		storedClasses.add(CircularSample2.class);
		dm.open();
		
//		dm.add(new CircularSample2());
		CircularSample2 ds = dm.get(CircularSample2.class, 1);
		dm.update(ds);
		
		dm.commit();
		String debug = debugger.printTable(storedClasses) + "\n\n" + debugger.printCache();
		tv.setText(debug);
		System.out.println(debug);
		dm.close();
		setContentView(tv);
	}
}
