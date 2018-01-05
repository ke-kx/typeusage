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
	
	/** Also consider context for almostEqual check */
	private static boolean USE_CONTEXT = true;
	
	/** Also consider type for almostEqual check */
	private static boolean USE_TYPE = true;
	
	/** How many methodcalls can be missing to be considered almostEqual */
	private static int ALMOST_EQUAL_K = 1; 
	
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
	
	/** The set of calls present in other but not in this */
	public Set<String> getMissingCalls(TypeUsage other) { 
		Set<String> missing = new HashSet<String>(other.calls);
		missing.removeAll(calls);
		return missing;
	}
	
	public boolean isWeaklyEqual(TypeUsage other) {
		if (USE_CONTEXT && !context.equals(other.context)) 
			return false;
		if (USE_TYPE && !type.equals(other.type))
			return false;
		return true;
	}
	
	/** TypeUsages are considered equal if context, type and calls are all identical */
	public boolean isEqual(TypeUsage other) {
		if (!isWeaklyEqual(other))
			return false;
		if (calls.size() != other.calls.size())
			return false;

		return calls.equals(other.calls);
	}

	/** 
	 * True when other is almost equal to this.
	 * 	
	 * Can use the method implemented in the available code or the one described in the papers.
	 */
	public boolean isAlmostEqual(TypeUsage other) {
		if (!isWeaklyEqual(other))
			return false;
		return isAlmostEqualCode(other);
	}
	
	/**
	 * This is the behavior found in the original code.
	 * It differs with respect to the behavior explained in the papers, in so far
	 * that it only takes into account methods which are in other but missing in this,
	 * without regards to additional methods in this.
	 * 
	 * As an example: this has methods 1,2 and other has methods 2,3.
	 * This variant calculates the set of added methods as {3} and thus declares them almost equal
	 * even though method 1 is not present in other at all!
	 * 
	 * The same size check is not enough to prevent this kind of situations from happening!
	 */
	private boolean isAlmostEqualCode(TypeUsage other) {
		if (calls.size() >= other.calls.size())
			return false;
		
		Set<String> missing = getMissingCalls(other);
		int missingCount = missing.size(); 
		return missingCount > 0 && missingCount <= ALMOST_EQUAL_K;
	}

	/**
	 * This is the behavior as explained in the papers.
	 * other is almost equal to this, iff M(this) contained in M(other) and
	 * |M(other)| = |M(this)| + k
	 * In other words, other is almost equal to this, if it calls the same methods
	 * plus up to k additional ones.
	 */
	private boolean isAlmostEqualPaper(TypeUsage other) {
		return (other.calls.size() == (calls.size()+1)) && other.calls.containsAll(calls);
	}
	
	@Override
	public String toString() {
		return context + " " + type + " " + StringUtils.join(calls, " ");
	}
}
