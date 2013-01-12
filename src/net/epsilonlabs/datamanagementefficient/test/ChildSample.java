package net.epsilonlabs.datamanagementefficient.test;

public class ChildSample extends DataSample3{

	private int extendedValue1;
	
	public ChildSample(){
		extendedValue1 = 42;
	}

	public int getExtendedValue1() {
		return extendedValue1;
	}

	public void setExtendedValue1(int extendedValue1) {
		this.extendedValue1 = extendedValue1;
	}
}
