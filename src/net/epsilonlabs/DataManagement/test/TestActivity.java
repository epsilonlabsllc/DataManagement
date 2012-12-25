package net.epsilonlabs.DataManagement.test;

import net.epsilonlabs.DataManagement.library.DataManager;
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
		
		DataSample ds = new DataSample();
		int id = dm.add(ds);
		DataSample sameDs = dm.get(ds.getClass(), id);
		
		dm.close();
		tv.setText(sameDs.toString());
		setContentView(tv);
	}
}
