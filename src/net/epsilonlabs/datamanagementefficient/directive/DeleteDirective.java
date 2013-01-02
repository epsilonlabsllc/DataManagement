package net.epsilonlabs.datamanagementefficient.directive;

public class DeleteDirective extends Directive {
	
	private int rowId;
	private Class<?> cls;

	public DeleteDirective(Class<?> cls, int rowId) {
		this.rowId = rowId;
		this.cls = cls;
	}
	
	public Class<?> getCls() {
		return cls;
	}
	
	public int getRowId() {
		return rowId;
	}
}
