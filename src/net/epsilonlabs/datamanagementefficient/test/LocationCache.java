package net.epsilonlabs.datamanagementefficient.test;

import net.epsilonlabs.datamanagementefficient.annotations.Id;

public class LocationCache {
	@Id
	private int id;
	public String code;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
}
