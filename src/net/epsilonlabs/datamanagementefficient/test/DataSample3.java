package net.epsilonlabs.datamanagementefficient.test;

import net.epsilonlabs.datamanagementefficient.annotations.Id;

public class DataSample3{

	@Id
	protected int ident;
	private int num1;
	private double num2;
	private String num3;
	private boolean num4;
	public static final int num5 = 4;

	public DataSample3(){
		num1 = 5;
		num2 = 7.0;
		num3 = "five";
		num4 = false;
	}

	public int getNum1() {
		return num1;
	}

	public void setNum1(int num1) {
		this.num1 = num1;
	}

	public double getNum2() {
		return num2;
	}

	public void setNum2(double num2) {
		this.num2 = num2;
	}

	public String getNum3() {
		return num3;
	}

	public void setNum3(String num3) {
		this.num3 = num3;
	}

	public boolean isNum4() {
		return num4;
	}

	public void setNum4(boolean num4) {
		this.num4 = num4;
	}

	public int getIdent() {
		return ident;
	}

	public void setId(int ident) {
		this.ident = ident;
	}

	public String toString(){
		String string =  ident + " " + num1 + " " + num2 + " " + num3 + " " + num4;
		return string;
	}

}