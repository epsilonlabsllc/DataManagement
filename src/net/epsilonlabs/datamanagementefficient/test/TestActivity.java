package net.epsilonlabs.datamanagementefficient.test;

import java.util.ArrayList;

import net.epsilonlabs.datamanagementefficient.library.DataManager;
import net.epsilonlabs.datamanagementefficient.library.DataUtil;
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
		
		DataUtil.getFields(ChildSample.class);
		
//		int id = dm.add(new ChildSample());
		ArrayList<ChildSample> csList = dm.find(ChildSample.class, "num4", true);
		
		dm.close();
		setContentView(tv);
	}
}
