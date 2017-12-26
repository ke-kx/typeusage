package typeusage.miner;

import soot.SootMethod;

/** defines an object that can can discuss with TUBodyTransformer */
public interface IMethodCallCollector {
	/** receives a type usage */
	void receive(TypeUsage t);

	/**
	 * computes a signature at the required granularity: name name(paramType_1)
	 * returnType name etc.
	 */
	String translateCallSignature(SootMethod meth);

	String translateContextSignature(SootMethod meth);

	/**
	 * keep a type-usage if it's class' fully-qualified name starts with this prefix
	 */
	String getPackagePrefixToKeep();

	/** poor man's applicative debug */
	void debug(String msg);
}
