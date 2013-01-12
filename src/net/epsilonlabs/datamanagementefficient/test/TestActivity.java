package net.epsilonlabs.datamanagementefficient.test;

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
		ChildSample cs = dm.get(ChildSample.class, 3);
		
		dm.close();
		setContentView(tv);
	}
}
