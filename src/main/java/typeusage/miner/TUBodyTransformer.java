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

	/**TODO REFACTOR!!! (way too long...) */
	@Override
	protected void internalTransform(Body body, String phase, @SuppressWarnings("rawtypes") Map options) {


		aliasInfo = new LocalMustAliasAnalysis(new ExceptionalUnitGraph(body));
		instanceFieldDetector = new MayPointToTheSameInstanceField(body);

		List<MethodCall> methodCalls = extractMethodCalls(body);
		List<TypeUsage> lVariables = extractTypeUsages(methodCalls, body);

		// output the variables
		for (TypeUsage aVariable : lVariables) {
			if ((aVariable.type.startsWith(collector.getPackagePrefixToKeep()))) {
				collector.receive(aVariable);
			}
		}

	} // end internalTransform
	
	/** Iterate through all statements in method body and collect method calls */
	private List<MethodCall> extractMethodCalls(Body body) {
		List<MethodCall> calls = new ArrayList<MethodCall>();

		// for each statement in method body
		for (Unit u : body.getUnits()) { 
			Stmt s = (Stmt) u;
			collector.debug(s + "-" + s.getClass());

			if (s.containsInvokeExpr()) {
				InvokeExpr invokeExpr = s.getInvokeExpr();
				collector.debug(invokeExpr.toString());
				if (invokeExpr instanceof InstanceInvokeExpr
				// && ! (invokeExpr instanceof SpecialInvokeExpr)
				) {
					Local local = (Local) ((InstanceInvokeExpr) invokeExpr).getBase();
					MethodCall elem = new MethodCall(local, s);
					calls.add(elem);
					collector.debug(elem + " " + invokeExpr.getMethod().getDeclaringClass().getName());
				}
			}
		}
		return calls;
	}

	private List<TypeUsage> extractTypeUsages(List<MethodCall> methodCalls, Body body) {
		List<TypeUsage> typeUsages = new ArrayList<TypeUsage>();
		String methodContext = collector.translateContextSignature(body.getMethod());

		for (MethodCall currentCall : methodCalls) {
			Type type = currentCall.local.getType();
			if (type instanceof NullType) {
				type = currentCall.stmt.getInvokeExpr().getMethod().getDeclaringClass().getType();
				// still couldn't determine type, skip this call
				if (type instanceof NullType) continue;
			}
			collector.debug("v: " + type);

			TypeUsage correspondingTypeUsage = findTypeUsage(currentCall, typeUsages);
			if (correspondingTypeUsage != null

					// if there is a cast this test avoids unsound data (e.g. two method calls of
					// different classes in the same type-usage)
					&& correspondingTypeUsage.type.equals(type.toString())

			) {
				correspondingTypeUsage.underlyingLocals.add(currentCall);
				InvokeExpr invokeExpr = currentCall.stmt.getInvokeExpr();
				correspondingTypeUsage.addMethodCall(collector.translateCallSignature(invokeExpr.getMethod()));
				collector.debug("adding " + currentCall + " to " + correspondingTypeUsage);
			} else {

				TypeUsage aNewTypeUsage = new TypeUsage(methodContext);

				collector.debug("creating " + aNewTypeUsage + " with " + currentCall.local);

				String location = body.getMethod().getDeclaringClass().toString();
				SourceLnPosTag tag = (SourceLnPosTag) currentCall.stmt.getTag("SourceLnPosTag");
				if (tag != null) {
					location += ":" + tag.startLn();
				}
				LineNumberTag tag2 = (LineNumberTag) currentCall.stmt.getTag("LineNumberTag");
				if (tag2 != null) {
					location += ":" + tag2.getLineNumber();
				}

				aNewTypeUsage.location = location;

				aNewTypeUsage.underlyingLocals.add(currentCall);

				InvokeExpr invokeExpr = currentCall.stmt.getInvokeExpr();
				aNewTypeUsage.addMethodCall(collector.translateCallSignature(invokeExpr.getMethod()));

				if (type instanceof NullType) {
					aNewTypeUsage.type = invokeExpr.getMethod().getDeclaringClass().getType().toString();
					aNewTypeUsage.sootType = invokeExpr.getMethod().getDeclaringClass().getType();
				} else {
					aNewTypeUsage.type = type.toString();
					aNewTypeUsage.sootType = type;
				}
				setExtends(type, aNewTypeUsage);

				// adding the link to the field
				SootField sootField = instanceFieldDetector.pointsTo.get(currentCall.local);
				if (sootField != null) {
					crossMethodData.put(sootField, aNewTypeUsage);
				}

				typeUsages.add(aNewTypeUsage);
			}
		}
		return typeUsages;
	}
	
	private TypeUsage findTypeUsage(MethodCall call1, List<TypeUsage> lVariables) {

		SootField sootField = instanceFieldDetector.pointsTo.get(call1.local);
		if (sootField != null) {
			return crossMethodData.get(sootField);
		}

		for (TypeUsage aTypeUsage : lVariables) {
			for (MethodCall e : aTypeUsage.underlyingLocals) {
				if (call1.local == e.local) {
					collector.debug(call1.local + " is same as " + e.local);
					collector.debug(aTypeUsage.type + " <-> " + e.stmt.getInvokeExpr().getMethod().getDeclaringClass());
					return aTypeUsage;
				}

				if (aliasInfo.mustAlias(call1.local, call1.stmt, e.local, e.stmt)) {
					collector.debug(call1.local + " alias to " + e.local);
					return aTypeUsage;
				}
				if (instanceFieldDetector.mayPointsToTheSameInstanceField(call1.local, e.local)) {
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
