package de.tud.stg.analysis;

import java.util.*;

import org.apache.commons.lang.StringUtils;

public abstract class TypeUsage {
	
	/** Class in which the TU occured */
	protected String location;

	/** Context in which TU occured (Method name) */
	protected String context;
	
	/** Type on which calls are made */
	protected String type;
	
	/** Calls made on the object */
	protected Set<String> calls = new HashSet<String>();
	
	public TypeUsage (String location, String context, String type) {
		this.location = location;
		this.context = context;
		this.type = type;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getContext() {
		return context;
	}

	public TypeUsage setContext(String _context) {
		context = _context;
		return this;
	}

	public String getType() {
		return type;
	}

	public TypeUsage setType(String _type) {
		type = _type;
		return this;
	}

	//TODO is this really necessary? only used in DMMCRunner, purpose not yet clear
	public Set<String> getCalls() {
		return calls;
	}

	/** Add another call to this TypeUsage */
	public void addCall(String call) {
		calls.add(call);
	}
	
	@Override
	public String toString() {
		return context + " " + type + " " + StringUtils.join(calls, " ");
	}
}
