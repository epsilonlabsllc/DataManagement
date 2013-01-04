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
		storedClasses.add(CircularSample.class);
		dm.open();
		
		CircularSample cs = dm.get(CircularSample.class, 3);
		cs.setCs(cs);
		dm.update(cs);
		
		dm.commit();
		String debug = debugger.printTable(storedClasses) + "\n\n" + debugger.printCache();
		tv.setText(debug);
		System.out.println(debug);
		dm.close();
		setContentView(tv);
	}
}
