package net.epsilonlabs.datamanagementefficient.test;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class TestActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TextView tv = new TextView(this);
		
		for(int i=0; i<40; i++){
			TestThread tt = new TestThread(this);
			tt.run();
		}
		
		setContentView(tv);
	}
}
