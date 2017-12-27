package typeusage.miner;

import soot.Local;
import soot.SootMethod;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

/** represents a method call on top of Soot's Local and Stmt */
public class MethodCall {

	public Local local;
	public Stmt stmt;
	
	public MethodCall(Local l, Stmt s) {
		this.local = l;
		this.stmt = s;
	}
	
	public SootMethod getMethod() {
		return stmt.getInvokeExpr().getMethod();
	}

	@Override
	public String toString() {
		InvokeExpr invokeExpr = stmt.getInvokeExpr();
		return local.toString() + " " + invokeExpr.getMethod().getName();
	}

}
