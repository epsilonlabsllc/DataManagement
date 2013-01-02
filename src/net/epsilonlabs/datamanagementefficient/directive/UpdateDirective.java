package net.epsilonlabs.datamanagementefficient.directive;

import java.lang.reflect.Field;
import java.util.Map;

public class UpdateDirective extends Directive {
	
	private Map<Field, Object> values;
	private int rowId;
	private Class<?> cls;
	
	public UpdateDirective(Class<?> cls, int rowId, Map<Field, Object> values) {
		this.values = values;
		this.rowId = rowId;
		this.cls = cls;
	}

	public Class<?> getCls() {
		return cls;
	}

	public Map<Field, Object> getValues() {
		return values;
	}

	public int getRowId() {
		return rowId;
	}
}
