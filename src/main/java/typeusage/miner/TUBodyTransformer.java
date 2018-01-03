package typeusage.miner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.NullType;
import soot.SootField;
import soot.Type;
import soot.Unit;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.toolkits.graph.ExceptionalUnitGraph;

/**
 * is a typical Soot BodyTransformer. Sends the type-usages to an
 * IMethodCallCollector
 */
public class TUBodyTransformer extends BodyTransformer {

	/** Reference to parent collector */
	private IMethodCallCollector collector;

	/** Used to determine if two local variables point to the same object? */
	private LocalMustAliasAnalysis aliasInfo;

	/** Used to determine if two locals point to the same instance field */ 
	private InstanceFieldDetector instanceFieldDetector;

	/** Keep data across methods to correctly collect type usage data on instance fields */
	private HashMap<SootField, TypeUsage> crossMethodData = new HashMap<SootField, TypeUsage>();

	/** Constructor */
	public TUBodyTransformer(IMethodCallCollector m) {
		collector = m;
	}

	/** Actual worker function, is applied to each method body **/
	@Override
	protected void internalTransform(Body body, String phase, @SuppressWarnings("rawtypes") Map options) {
		aliasInfo = new LocalMustAliasAnalysis(new ExceptionalUnitGraph(body));
		instanceFieldDetector = new InstanceFieldDetector(body, crossMethodData);

		List<MethodCall> methodCalls = extractMethodCalls(body);
		List<TypeUsage> lVariables = extractTypeUsages(methodCalls, body);

		// output the variables
		for (TypeUsage aVariable : lVariables) {
			if ((aVariable.type.startsWith(collector.getPrefixToKeep()))) {
				collector.receive(aVariable);
			}
		}
	} 
	
	/** Iterate through all statements in method body and collect method calls */
	private List<MethodCall> extractMethodCalls(Body body) {
		List<MethodCall> calls = new ArrayList<MethodCall>();

		// for each statement in method body
		for (Unit u : body.getUnits()) { 
			Stmt statement = (Stmt) u;
			collector.debug(statement + "-" + statement.getClass());

			if (statement.containsInvokeExpr()) {
				InvokeExpr invokeExpr = statement.getInvokeExpr();
				collector.debug(invokeExpr.toString());
				if (invokeExpr instanceof InstanceInvokeExpr
				// && ! (invokeExpr instanceof SpecialInvokeExpr)
				) {
					Local local = (Local) ((InstanceInvokeExpr) invokeExpr).getBase();
					MethodCall elem = new MethodCall(local, statement);
					calls.add(elem);
					collector.debug(elem + " " + invokeExpr.getMethod().getDeclaringClass().getName());
				}
			}
		}
		return calls;
	}

	/** Go over list of methodCalls and extract typeUsages by grouping together calls on the same object */
	private List<TypeUsage> extractTypeUsages(List<MethodCall> methodCalls, Body body) {
		List<TypeUsage> typeUsages = new ArrayList<TypeUsage>();

		for (MethodCall currentCall : methodCalls) {
			Type type = currentCall.local.getType();
			if (type instanceof NullType) {
				type = currentCall.getMethod().getDeclaringClass().getType();
				// still couldn't determine type, skip this call
				if (type instanceof NullType) continue;
			}
			collector.debug("v: " + type);

			TypeUsage correspondingTypeUsage = findTypeUsage(currentCall, typeUsages);
			if (correspondingTypeUsage != null &&
					// if there is a cast this test avoids unsound data (e.g. two method calls of different
					//classes in the same type-usage)
					correspondingTypeUsage.type.equals(type.toString())) {
				// TypeUsage already exists, add currentCall
				correspondingTypeUsage.addMethodCall(currentCall, collector);
				collector.debug("adding " + currentCall + " to " + correspondingTypeUsage);
			} else {
				// Type usage doesn't exist yet, create object and add to typeUsages List
				TypeUsage newTypeUsage = new TypeUsage(body, currentCall, type, collector);
				typeUsages.add(newTypeUsage);

				// add link to instance field if it exists
				instanceFieldDetector.addIfAppropiate(currentCall, newTypeUsage);
			}
		}
		return typeUsages;
	}
	
	/** Go through list of typeUsages and find the one belonging to call */
	private TypeUsage findTypeUsage(MethodCall call, List<TypeUsage> variables) {
		//TODO this should be part of MayPointToTheSameInstanceField! + crossMethodData variable as well
		SootField sootField = instanceFieldDetector.getField(call);
		if (sootField != null) {
			return crossMethodData.get(sootField);
		}

		for (TypeUsage typeUsage : variables) {
			for (MethodCall e : typeUsage.underlyingLocals) {
				if (call.local == e.local) {
					collector.debug(call.local + " is same as " + e.local);
					collector.debug(typeUsage.type + " <-> " + e.getMethod().getDeclaringClass());
					return typeUsage;
				}

				if (aliasInfo.mustAlias(call.local, call.stmt, e.local, e.stmt)) {
					collector.debug(call.local + " alias to " + e.local);
					return typeUsage;
				}
				if (instanceFieldDetector.mayPointToSameInstanceField(call.local, e.local)) {
					return typeUsage;
				}
			}

		}
		return null;
	}
}
