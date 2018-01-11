package typeusage.miner;

import soot.SootMethod;

/** defines an object that can can discuss with TUBodyTransformer */
public interface IMethodCallCollector {
    /** Run the analysis */
    IMethodCallCollector run();

	/** Receive a type usage, that is save it in the way which is decided by the implementing object */
	void receive(TypeUsage t);

	/**
	 * Compute a signature at the required granularity: name name(paramType_1)
	 * returnType name etc.
	 */
	String translateCallSignature(SootMethod meth);

	/** Change the context signature to the required format */
	String translateContextSignature(SootMethod meth);

	/** Keep a type-usage if it's class' fully-qualified name starts with this prefix */
	String getPrefixToKeep();

	/** Poor man's applicative debug */
	void debug(String msg);
}
