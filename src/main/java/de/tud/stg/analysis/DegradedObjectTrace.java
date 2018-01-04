package de.tud.stg.analysis;

public class DegradedObjectTrace extends ObjectTrace {

	/** The original ObjectTrace from which this DegradedObjectTrace was created */
	public ObjectTrace original;
	
	/** The call which was removed to create this DegradedObjectTrace */
	String removedMethodCall;

	/** 
	 * Degraded Trace is created by cloning a parent completely (besides the location) and removing one method call.
	 */
	public DegradedObjectTrace(ObjectTrace parent, String removedCall) {
		super(null, parent.context, parent.type);
		calls.addAll(parent.calls);
		original = parent;
		remove(removedCall);
	}

	private void remove(String removedCall) {
		removedMethodCall = removedCall;
		calls.remove(removedCall);
	}
}
