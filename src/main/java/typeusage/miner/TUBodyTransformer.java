package typeusage.miner;

import soot.*;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * is a typical Soot BodyTransformer. Sends the type-usages to an IMethodCallCollector
 */
public class TUBodyTransformer extends BodyTransformer {

    /** Reference to parent collector */
    private IMethodCallCollector collector;

    /** Used to determine if two local variables point to the same object? */
    private LocalMustAliasAnalysis aliasInfo;

    /** Keep data across methods to correctly collect type usage data on instance fields */
    private HashMap<SootField, TypeUsage> crossMethodData = new HashMap<>();

    /** Constructor */
    public TUBodyTransformer(IMethodCallCollector m) {
        collector = m;
    }

    /** Actual worker function, is applied to each method body **/
    @Override
    protected void internalTransform(Body body, String phase, @SuppressWarnings("rawtypes") Map options) {
        aliasInfo = new LocalMustAliasAnalysis(new ExceptionalUnitGraph(body));
        InstanceFieldDetector ifd = new InstanceFieldDetector(crossMethodData);
        ifd.readMethod(body);

        List<MethodCall> methodCalls = extractMethodCalls(body, ifd);
        List<TypeUsage> lVariables = extractTypeUsages(methodCalls, body, ifd);

        // output the variables
        for (TypeUsage aVariable : lVariables) {
            if ((aVariable.type.startsWith(collector.getPrefixToKeep()))) {
                collector.receive(aVariable);
            }
        }
    }

    /** Iterate through all statements in method body and collect method calls */
    private List<MethodCall> extractMethodCalls(Body body, InstanceFieldDetector ifd) {
        List<MethodCall> calls = new ArrayList<MethodCall>();

        // for each statement in method body
        for (Unit u : body.getUnits()) {
            Stmt statement = (Stmt) u;
            collector.debug("%s - %s\n", statement, statement.getClass());

            if (statement.containsInvokeExpr()) {
                InvokeExpr invokeExpr = statement.getInvokeExpr();
                collector.debug("%s\n", invokeExpr);
                if (invokeExpr instanceof InstanceInvokeExpr
                    // && ! (invokeExpr instanceof SpecialInvokeExpr)
                        ) {
                    Local local = (Local) ((InstanceInvokeExpr) invokeExpr).getBase();
                    MethodCall elem = new MethodCall(local, statement);
                    calls.add(elem);
                    collector.debug("%s %s\n", elem, invokeExpr.getMethod().getDeclaringClass().getName());
                }
            }
        }
        return calls;
    }

    /** Go over list of methodCalls and extract typeUsages by grouping together calls on the same object */
    private List<TypeUsage> extractTypeUsages(List<MethodCall> methodCalls, Body body, InstanceFieldDetector ifd) {
        List<TypeUsage> typeUsages = new ArrayList<TypeUsage>();

        for (MethodCall currentCall : methodCalls) {
            Type type = currentCall.getLocal().getType();
            if (type instanceof NullType) {
                type = currentCall.getMethod().getDeclaringClass().getType();
                // still couldn't determine type, skip this call
                if (type instanceof NullType) continue;
            }
            collector.debug("v: %s\n", type);

            TypeUsage correspondingTypeUsage = findTypeUsage(currentCall, typeUsages, ifd);
            if (correspondingTypeUsage != null &&
                    // if there is a cast this test avoids unsound data (e.g. two method calls of different
                    //classes in the same type-usage)
                    correspondingTypeUsage.type.equals(type.toString())) {
                // TypeUsage already exists, add currentCall
                correspondingTypeUsage.addMethodCall(currentCall, collector);
                collector.debug("adding %s to %s\n", currentCall, correspondingTypeUsage);
            } else {
                // Type usage doesn't exist yet, create object and add to typeUsages List
                TypeUsage newTypeUsage = new TypeUsage(body, currentCall, type, collector);
                typeUsages.add(newTypeUsage);

                // add link to instance field if it exists
                ifd.addIfAppropiate(currentCall, newTypeUsage);
            }
        }
        return typeUsages;
    }

    /** Go through list of typeUsages and find the one belonging to call */
    private TypeUsage findTypeUsage(MethodCall call, List<TypeUsage> variables, InstanceFieldDetector ifd) {
        if (ifd.pointsToInstanceField(call)) {
            return ifd.getTypeUsage(call);
        }

        for (TypeUsage typeUsage : variables) {
            for (MethodCall e : typeUsage.getUnderlyingLocals()) {
                if (call.getLocal() == e.getLocal()) {
                    collector.debug("%s is same as %s\n", call.getLocal(), e.getLocal());
                    collector.debug("%s <-> %s\n", typeUsage.type, e.getMethod().getDeclaringClass());
                    return typeUsage;
                }

                if (aliasInfo.mustAlias(call.getLocal(), call.getStmt(), e.getLocal(), e.getStmt())) {
                    collector.debug("%s alias to %s\n", call.getLocal(), e.getLocal());
                    return typeUsage;
                }
                if (ifd.mayPointToSameInstanceField(call.getLocal(), e.getLocal())) {
                    return typeUsage;
                }
            }

        }
        return null;
    }
}
