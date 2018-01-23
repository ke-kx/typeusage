package typeusage.miner;

import soot.Body;
import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Stmt;

import java.util.HashMap;

/**
 * answers the question: do two locals point to the same instance field?
 *
 * @author Martin Monperrus
 */
public class InstanceFieldDetector {

    /** Keep data across methods to correctly collect type usage data on instance fields */
    private HashMap<SootField, TypeUsage> crossMethodData = new HashMap<SootField, TypeUsage>();

    /** Maps from a local to an instance field reference */
    private HashMap<Value, SootField> pointsTo = new HashMap<Value, SootField>();

    /** Populates pointsTo map by iterating over all statements in current method */
    public void readMethod(Body body) {
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
        SootField sootField = pointsTo.get(call.getLocal());
        if (sootField != null) {
            crossMethodData.put(sootField, newTypeUsage);
        }
    }

    /** Return true if the call is invoked on a local which is referencing an instance field */
    public boolean pointsToInstanceField(MethodCall call) {
        return (pointsTo.get(call.getLocal()) != null);
    }

    /** Return typeUsage belonging to the call */
    public TypeUsage getTypeUsage(MethodCall call) {
        if (!pointsToInstanceField(call))
            throw new IllegalArgumentException("MethodCall must point to instance field!");
        return crossMethodData.get(pointsTo.get(call.getLocal()));
    }
}
