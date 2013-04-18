package net.epsilonlabs.datamanagementefficient.test;

import net.epsilonlabs.datamanagementefficient.annotations.Id;

public class CircularSample {
	
	@Id
	private int id;
	private CircularSample cs;
	
	public CircularSample(){
		this.cs = this;
	}
	
	public CircularSample getCs(){
		return cs;
	}
	
	public void setCs(CircularSample cs){
		this.cs = cs;
	}
	
	public String toString(){
		String string = "Circular Sample\n";
		if(cs != null) string += "     circular";
		else string += "     null";
		return string;
	}
	
	public int getId() {
		return id;
	}
}
