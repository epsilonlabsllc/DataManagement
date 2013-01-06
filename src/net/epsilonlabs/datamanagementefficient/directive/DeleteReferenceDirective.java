package net.epsilonlabs.datamanagementefficient.directive;

public class DeleteReferenceDirective extends Directive {

	private Class<?> parentType;
	private String childName;
	private int parentId;
	private int childId;
	
	public DeleteReferenceDirective(Class<?> parentType, String childName, int parentId, int childId) {
		super();
		this.parentType = parentType;
		this.childName = childName;
		this.parentId = parentId;
		this.childId = childId;
	}
	public Class<?> getParentType() {
		return parentType;
	}
	public String getChildName() {
		return childName;
	}
	public int getParentId() {
		return parentId;
	}
	public int getChildId() {
		return childId;
	}
}
