package net.epsilonlabs.datamanagementefficient.user;

public interface DatabaseCallback<T>{
	public void onCallback(T result);
}