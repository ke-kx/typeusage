package typeusage.miner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import soot.Type;

/**
 * is an abstraction over Soot locals: Variable: 1 ----- 1..n Local
 *
 */
public class TypeUsage {
	// TODO comments for member variables!!! && correct visibility!
	String location = "!";
	protected String type = "!";
	Type sootType = null;
	String methodContext = "!";
	List<MethodCall> underlyingLocals = new ArrayList<MethodCall>();
	protected final Set<String> methodCalls = new HashSet<String>();
	Set<String> _extends = new HashSet<String>();

	public TypeUsage() {
	}

	public TypeUsage(String _methodContext) {
		methodContext = _methodContext;
	}

	public void addMethodCall(String s) {
		methodCalls.add("call:" + s);
	}

	public String getLocation() {
		return location;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((methodCalls == null) ? 0 : methodCalls.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	};

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TypeUsage))
			return false;
		TypeUsage other = (TypeUsage) obj;
		return other.type.equals(this.type) && other.methodCalls.equals(this.methodCalls);
	};

	@Override
	public String toString() {
		return String.format("location:%s context:%s type:%s %s %s", 
				location, methodContext, type, StringUtils.join(methodCalls, " "), StringUtils.join(_extends, " "));
	}
}
