package de.tud.stg.analysis;

import java.util.*;

/**
 * //Note that since the relationship holds for the identity, i.e. xEx is
 * always- //valid, E(x) always contains x itself, and |E(x)| 1.
 * 
 * // well, this reuce the average S-score significantly // we remove the 1
 * again
 * 
 * 
 * @author martin
 *
 */
public class ObjectTrace extends TypeUsage {
	
	/** Number of EXACTLY EQUAL Traces */
	private int equalCount;
	
	/** Number of ALMOST EQUAL Traces */
	private int almostEqualCount;

	public HashMap<String, Integer> missingcalls = new HashMap<String, Integer>();

	/** Coefficient to use for default strangeness2 */
	private static final double conservative_coef = 2;

	public ObjectTrace() {
		super(null, null, null);
	}
	
	public ObjectTrace(String location, String context, String type) {
		super(location, context, type);
		reset();
	}
	
	public void incEqualCount() {
		equalCount++;
	}
	
	public void incAlmostEqualCount() {
		almostEqualCount++;
	}
	
	public boolean isSpecial() {
		return equalCount == 0 && almostEqualCount == 0;
	}

	public void reset() {
		equalCount = 0;
		almostEqualCount = 0;
	}

	public double strangeness() {
		if ((equalCount + almostEqualCount) == 0) return 0;
		return ((double) almostEqualCount) / (equalCount + almostEqualCount);
	}

	public double strangeness2() {
		return strangeness2(conservative_coef);
	}

	public double strangeness2(double coef) {
		return (almostEqualCount) / (Math.pow(equalCount, coef) + almostEqualCount);
	}

	public double strangeness3() {
		return ((double) almostEqualCount) / (1 + equalCount + almostEqualCount);
	}

	public String getCounts() {
		return equalCount + " " + almostEqualCount;
	}

	//TODO does this make sense? probably not
	@Override
	public String toString() {
		String ret = "\n\n\n -------------------- \n";
		ret += "Strange: " + strangeness() + "\n";
		ret += "Strangev2: " + strangeness2() + "\n";
		ret += "Equals: " + equalCount + "\n";
		ret += "Almost: " + almostEqualCount + "\n";
		ret += "\tlocation: " + getLocation() + "\n";
		ret += "\tcontext: " + getContext() + "\n";
		ret += "\ttype: " + getType() + "\n";
		ret += "\t\tpresent" + calls + "\n";
		ret += "\t\tmissing" + missingcalls + "\n";
		return ret;
	}
}
