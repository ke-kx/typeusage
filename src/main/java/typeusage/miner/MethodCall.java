package typeusage.miner;

import soot.Local;
import soot.SootMethod;
import soot.jimple.Stmt;

/** represents a method call on top of Soot's Local and Stmt */
public class MethodCall {

    /** Local variable on which the call is executed */
    private Local local;

    /** Whole statement which contains the methodCall */
    private Stmt stmt;

    public MethodCall(Local l, Stmt s) {
        this.local = l;
        this.stmt = s;
    }

    /** Return only the actual method which is getting called */
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
