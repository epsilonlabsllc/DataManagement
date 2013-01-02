package net.epsilonlabs.datamanagementefficient.directive;

public class DeleteReferenceDirective extends Directive {

	private Class<?> parentType;
	private Class<?> childType;
	private int parentId;
	private int childId;
	
	public DeleteReferenceDirective(Class<?> parentType, Class<?> childType, int parentId, int childId) {
		super();
		this.parentType = parentType;
		this.childType = childType;
		this.parentId = parentId;
		this.childId = childId;
	}
	public Class<?> getParentType() {
		return parentType;
	}
	public Class<?> getChildType() {
		return childType;
	}
	public int getParentId() {
		return parentId;
	}
	public int getChildId() {
		return childId;
	}
}
