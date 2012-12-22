package net.epsilonlabs.DataManagement.test;

import net.epsilonlabs.DataManagement.annotations.Id;

public class DataSample2{

	@Id
	private int ident;
	private int num1;
	private double num2;
	private String num3;
	private boolean num4;
	public static final int num5 = 4;
	private DataSample3 ds3;
	
	public DataSample2(){
		num1 = 4;
		num2 = 4.0;
		num3 = "four";
		num4 = false;
		ident = 500;
		ds3 = new DataSample3();
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

	public DataSample3 getDs3() {
		return ds3;
	}

	public void setDs3(DataSample3 ds3) {
		this.ds3 = ds3;
	}

}