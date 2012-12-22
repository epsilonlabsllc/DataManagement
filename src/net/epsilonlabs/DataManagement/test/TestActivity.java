package net.epsilonlabs.DataManagement.test;

import java.util.Collection;

import net.epsilonlabs.DataManagement.DataManager;
import android.os.Bundle;
import android.widget.TextView;
import android.app.Activity;

public class TestActivity extends Activity {

	@SuppressWarnings("unused")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TextView tv = new TextView(this);
		
		DataManager dm = new DataManager(this);
		dm.open();
		
		int id = dm.add(new DataSample());
		
		DataSample ds = dm.get(DataSample.class, id);
		
		ds.setNum1(5);
		dm.update(id, ds);
		
		Collection<DataSample> dsCollection = dm.find(DataSample.class, 5, "num1");
		
		dsCollection = dm.getAll(DataSample.class);
		
		dm.delete(DataSample.class, id);
		
		dm.close();
		
		tv.setText(ds.getIdent() + " " + ds.getNum1() + " " + ds.getNum2() + " " + ds.getNum3() + " " + ds.isNum4() + "\n");
		setContentView(tv);
	}
}
