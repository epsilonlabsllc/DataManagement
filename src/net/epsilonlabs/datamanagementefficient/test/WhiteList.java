package net.epsilonlabs.datamanagementefficient.test;

import java.util.ArrayList;

import net.epsilonlabs.datamanagementefficient.annotations.Id;

public class WhiteList {
	
	public static final int NOT_INITIALIZED = -1;
	public static final int SILENT_MODE = -2;
	public static final int VIBRATE_MODE = -3;
	
	@Id
	private int id;
	private ArrayList<Contact> whitelist = new ArrayList<Contact>();
	private int currentVolume = NOT_INITIALIZED;
	
	public WhiteList(){
	}

	public ArrayList<Contact> getWhitelist() {
		return whitelist;
	}

	public int getCurrentVolume() {
		return currentVolume;
	}

	public void setCurrentVolume(int currentVolume) {
		this.currentVolume = currentVolume;
	}
	
	public int getId() {
		return id;
	}
}
