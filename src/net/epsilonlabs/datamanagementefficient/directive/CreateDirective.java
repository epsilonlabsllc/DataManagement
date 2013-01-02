package net.epsilonlabs.datamanagementefficient.directive;

public class CreateDirective extends Directive {
	
	private Object instance;

	public CreateDirective(Object instance) {
		this.instance = instance;
	}

	public Object getInstance() {
		return instance;
	}
}
