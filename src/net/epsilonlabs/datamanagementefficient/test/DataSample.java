package net.epsilonlabs.datamanagementefficient.test;

import net.epsilonlabs.datamanagementefficient.annotations.Id;

public class DataSample{

	@Id
	private int ident;
	private int num1;
	private double numderp;
	private String num3;
	private boolean num4;
	public static final int num5 = 3;
//	private ArrayList<DataSample2> ds2depier;

	public DataSample(){
		num1 = 3;
		numderp = 3.0;
		num3 = "three";
		num4 = true;
//		ds2depier = new ArrayList<DataSample2>();
//		ds2depier.add(new DataSample2());
//		ds2depier.add(new DataSample2());
	}

	public int getNum1() {
		return num1;
	}

	public void setNum1(int num1) {
		this.num1 = num1;
	}

	public double getNumDERP() {
		return numderp;
	}

	public void setNum2(double numDERP) {
		this.numderp = numDERP;
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

//	public ArrayList<DataSample2> getDs2() {
//		return ds2depier;
//	}
//
//	public void setDs2(ArrayList<DataSample2> ds2) {
//		this.ds2depier = ds2;
//	}
//
//	public String toString(){
//		if(ds2depier != null){
//			String string =  ident + " " + num1 + " " + numderp + " " + num3 + " " + num4 + "\n";
//			for(DataSample2 containedDs2 : ds2depier){
//				string += "     " + containedDs2.toString() + "\n";
//			}
//			return string;
//		}
//		
//		String string =  ident + " " + num1 + " " + numderp + " " + num3 + " " + num4 + "\n";
//		string += "     " + " null " + "\n";
//		return string;
//	}

}
