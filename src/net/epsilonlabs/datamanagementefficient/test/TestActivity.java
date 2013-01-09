package net.epsilonlabs.datamanagementefficient.test;

import java.util.ArrayList;

import net.epsilonlabs.datamanagementefficient.library.DataManager;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class TestActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TextView tv = new TextView(this);
		DataManager dm = DataManager.getInstance(this);
		dm.open();
		dm.add(new DataSample3());
		dm.add(new DataSample3());
		dm.add(new DataSample3());
		ArrayList<DataSample3> list = dm.getAll(DataSample3.class);
		dm.commit();
		dm.close();
		setContentView(tv);
	}
}
