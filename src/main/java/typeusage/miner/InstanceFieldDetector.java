package typeusage.miner;

import java.util.HashMap;

import soot.Body;
import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Stmt;

/**
 * answers the question: do two locals point to the same instance field?
 * 
 * @author Martin Monperrus
 */
public class InstanceFieldDetector {

	/** Maps from a local to an instance field reference */
	private HashMap<Value, SootField> pointsTo = new HashMap<Value, SootField>();
	
	/** Reference to crossMethodData from TUBodyTransformer */
	private HashMap<SootField, TypeUsage> crossMethodData;

	/** Constructor. Populates pointsTo map by iterating over all statements */
	public InstanceFieldDetector(Body body, HashMap<SootField, TypeUsage> cmd) {
		crossMethodData = cmd;
		
		// simple resolving
		for (Unit u : body.getUnits()) { // for each statement
			Stmt stmt = (Stmt) u;
			if (stmt instanceof AssignStmt) {
				AssignStmt ass = (AssignStmt) stmt;
				Value left = ass.getLeftOp();
				Value right = ass.getRightOp();
				if (right instanceof InstanceFieldRef) {
					pointsTo.put(left, // a local (e.g. JimpleLocal)
							((InstanceFieldRef) right).getField() // an InstanceFieldRef (e.g. JInstanceFieldRef)
					);
				}

				//TODO is this relevant for me? chains of locals = instanceField = local = ... ?
				
				// not necessary according to the specification as given by the test suite
				// if (left instanceof InstanceFieldRef) {
				// pointsTo.put(
				// right, // a local (e.g. JimpleLocal)
				// ((InstanceFieldRef) left).getField() // an InstanceFieldRef (e.g.
				// JInstanceFieldRef)
				// );
				// }

			}
		}
	}

	/** Return true if two locals reference the same instance field */
	public boolean mayPointToSameInstanceField(Value v1, Value v2) {
		if (!pointsTo.keySet().contains(v1)) {
			return false;
		}
		if (!pointsTo.keySet().contains(v2)) {
			return false;
		}
		return pointsTo.get(v1).equals(pointsTo.get(v2));
	}

	/** Add type usage to cross method data if it belongs to an instance field */
	public void addIfAppropiate(MethodCall call, TypeUsage newTypeUsage) {
		SootField sootField = pointsTo.get(call.local);
		if (sootField != null) {
			crossMethodData.put(sootField, newTypeUsage);
		}
	}

	/** Return corresponding field or NULL if the call doesn't relate to an instance field */
	public SootField getField(MethodCall call) {
		return pointsTo.get(call.local);
	}
}
