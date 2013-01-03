package net.epsilonlabs.datamanagementefficient.test;

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
		dm.open();
		
		int id = dm.add(new DataSample());
		DataSample ds = dm.get(DataSample.class, id);
		ds.getDs2().add(new DataSample2());
		dm.update(ds);
		dm.commit();
		DataSample sameDs = dm.get(DataSample.class, id);
		
		tv.setText(sameDs.toString());
		
		dm.close();
		setContentView(tv);
	}
}
