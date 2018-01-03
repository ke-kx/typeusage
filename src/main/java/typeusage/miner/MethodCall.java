package typeusage.miner;

import soot.Local;
import soot.SootMethod;
import soot.jimple.Stmt;

/** represents a method call on top of Soot's Local and Stmt */
public class MethodCall {

	private Local local;
	private Stmt stmt;
	
	public MethodCall(Local l, Stmt s) {
		this.local = l;
		this.stmt = s;
	}
	
	public SootMethod getMethod() {
		return stmt.getInvokeExpr().getMethod();
	}

	public Local getLocal() {
		return local;
	}

	public Stmt getStmt() {
		return stmt;
	}

	@Override
	public String toString() {
		return local.toString() + " " + getMethod().getName();
	}
}
