package net.epsilonlabs.datamanagementefficient.test;

import net.epsilonlabs.datamanagementefficient.annotations.Id;

public class Contact {
	
	@Id
	private int id;
	private String number;
	
	public Contact(){
	}
	
	public Contact(String number){
		this.number = number;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}
	
	public int getId() {
		return id;
	}
}
