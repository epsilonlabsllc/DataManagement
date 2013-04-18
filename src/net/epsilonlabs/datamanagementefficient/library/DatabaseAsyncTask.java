package net.epsilonlabs.datamanagementefficient.library;

import net.epsilonlabs.datamanagementefficient.user.DatabaseCallback;
import android.os.AsyncTask;

public abstract class DatabaseAsyncTask<T> extends AsyncTask<Void, Void, T>{
	
	private DatabaseCallback<T> listener;

	public DatabaseAsyncTask(DatabaseCallback<T> listener){
		this.listener = listener;
	}
	
	@Override
	protected void onPostExecute(T result) {
		super.onPostExecute(result);
		listener.onCallback(result);
	}
}
