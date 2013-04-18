package net.epsilonlabs.datamanagementefficient.test;

import java.util.ArrayList;

import net.epsilonlabs.datamanagementefficient.annotations.Id;

public class CircularSample2 {

	@Id
	private int id;
	private ArrayList<CircularSample2> csList;
	
	public CircularSample2(){
		this.csList = new ArrayList<CircularSample2>();
		csList.add(this);
	}

	public ArrayList<CircularSample2> getCsList() {
		return csList;
	}

	public void setCsList(ArrayList<CircularSample2> csList) {
		this.csList = csList;
	}
	
	public String toString(){
		String string = "Circular Sample 2\n";
		if(csList.get(0) != null) string += "     circular";
		else string += "     null";
		return string;
	}
	
	public int getId() {
		return id;
	}
}
