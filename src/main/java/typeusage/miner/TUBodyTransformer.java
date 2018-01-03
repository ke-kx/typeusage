package typeusage.miner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.NullType;
import soot.RefType;
import soot.SootClass;
import soot.SootField;
import soot.Type;
import soot.Unit;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceLnPosTag;
import soot.toolkits.graph.ExceptionalUnitGraph;

/**
 * is a typical Soot BodyTransformer. Sends the type-usages to an
 * IMethodCallCollector
 */
public class TUBodyTransformer extends BodyTransformer {

	/** Reference to parent collector */
	private IMethodCallCollector collector;

	/** Constructor */
	public TUBodyTransformer(IMethodCallCollector m) {
		this.collector = m;
	}

	/** Used to determine if two local variables point to the same object? */
	LocalMustAliasAnalysis aliasInfo;

	/** Used to determine if two locals point to the same instance field */ 
	MayPointToTheSameInstanceField instanceFieldDetector;

	HashMap<SootField, TypeUsage> crossMethodData = new HashMap<SootField, TypeUsage>();

	/** Actual worker function, is applied to each method body **/
	@Override
	protected void internalTransform(Body body, String phase, @SuppressWarnings("rawtypes") Map options) {
		aliasInfo = new LocalMustAliasAnalysis(new ExceptionalUnitGraph(body));
		instanceFieldDetector = new MayPointToTheSameInstanceField(body);

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

	//TODO improve this function more
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
			// TypeUsage already exists, add currentCall
			if (correspondingTypeUsage != null &&
					// if there is a cast this test avoids unsound data (e.g. two method calls of different
					//classes in the same type-usage)
					correspondingTypeUsage.type.equals(type.toString())) {

				//TODO shouldn't this be a function in typeUsage on stuff? (especially together as well!)
				correspondingTypeUsage.underlyingLocals.add(currentCall);
				correspondingTypeUsage.addMethodCall(collector.translateCallSignature(currentCall.getMethod()));
				collector.debug("adding " + currentCall + " to " + correspondingTypeUsage);
			} else {
				// Type usage doesn't exist yet, create object and add to typeUsages List
				
				//TODO all this stuff below should be in TypeUsage?!
				String methodContext = collector.translateContextSignature(body.getMethod());
				TypeUsage newTypeUsage = new TypeUsage(methodContext);

				collector.debug("creating " + newTypeUsage + " with " + currentCall.local);

				String location = body.getMethod().getDeclaringClass().toString();
				SourceLnPosTag tag = (SourceLnPosTag) currentCall.stmt.getTag("SourceLnPosTag");
				if (tag != null) {
					location += ":" + tag.startLn();
				}
				LineNumberTag tag2 = (LineNumberTag) currentCall.stmt.getTag("LineNumberTag");
				if (tag2 != null) {
					location += ":" + tag2.getLineNumber();
				}

				newTypeUsage.location = location;

				newTypeUsage.underlyingLocals.add(currentCall);

				newTypeUsage.addMethodCall(collector.translateCallSignature(currentCall.getMethod()));

				if (type instanceof NullType) {
					newTypeUsage.type = currentCall.getMethod().getDeclaringClass().getType().toString();
					newTypeUsage.sootType = currentCall.getMethod().getDeclaringClass().getType();
				} else {
					newTypeUsage.type = type.toString();
					newTypeUsage.sootType = type;
				}
				setExtends(type, newTypeUsage);

				// adding the link to the field
				SootField sootField = instanceFieldDetector.pointsTo.get(currentCall.local);
				if (sootField != null) {
					crossMethodData.put(sootField, newTypeUsage);
				}

				typeUsages.add(newTypeUsage);
			}
		}
		return typeUsages;
	}
	
	private TypeUsage findTypeUsage(MethodCall call, List<TypeUsage> lVariables) {

		SootField sootField = instanceFieldDetector.pointsTo.get(call.local);
		if (sootField != null) {
			return crossMethodData.get(sootField);
		}

		for (TypeUsage aTypeUsage : lVariables) {
			for (MethodCall e : aTypeUsage.underlyingLocals) {
				if (call.local == e.local) {
					collector.debug(call.local + " is same as " + e.local);
					collector.debug(aTypeUsage.type + " <-> " + e.stmt.getInvokeExpr().getMethod().getDeclaringClass());
					return aTypeUsage;
				}

				if (aliasInfo.mustAlias(call.local, call.stmt, e.local, e.stmt)) {
					collector.debug(call.local + " alias to " + e.local);
					return aTypeUsage;
				}
				if (instanceFieldDetector.mayPointsToTheSameInstanceField(call.local, e.local)) {
					return aTypeUsage;
				}
			}

		}
		return null;
	}

	/**
	 * recursive method to get the real type
	 */
	private void setExtends(Type type, TypeUsage aVariable) {
		if (type instanceof RefType) {
			SootClass sc = ((RefType) type).getSootClass();
			// adding the current type:
			if (!sc.toString().equals("java.lang.Object")) {
				aVariable._extends.add("extend:" + sc.toString());
			}
			if (sc.hasSuperclass()) {
				setExtends(sc.getSuperclass().getType(), aVariable);
			}
		}
	}

}
