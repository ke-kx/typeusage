package typeusage.miner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import soot.Body;
import soot.NullType;
import soot.RefType;
import soot.SootClass;
import soot.Type;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceLnPosTag;

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
	
	public TypeUsage(Body body, MethodCall call, Type type, IMethodCallCollector collector) {
		methodContext = collector.translateContextSignature(body.getMethod());
		collector.debug(String.format("Creating type usage for %s with %s", methodContext, call.local));

		location = body.getMethod().getDeclaringClass().toString();
		SourceLnPosTag sourceLnTag = (SourceLnPosTag) call.stmt.getTag("SourceLnPosTag");
		if (sourceLnTag != null) {
			location += ":" + sourceLnTag.startLn();
		}
		LineNumberTag lineNumberTag = (LineNumberTag) call.stmt.getTag("LineNumberTag");
		if (lineNumberTag != null) {
			location += ":" + lineNumberTag.getLineNumber();
		}

		addMethodCall(call, collector);

		if (type instanceof NullType) {
			this.type = call.getMethod().getDeclaringClass().getType().toString();
			sootType = call.getMethod().getDeclaringClass().getType();
		} else {
			this.type = type.toString();
			sootType = type;
		}
		setExtends(type);
	}

	//TODO only do string work in for string method o.O - or easier like this?
	public void addMethodCall(MethodCall call, IMethodCallCollector collector) {
		underlyingLocals.add(call);
		methodCalls.add("call:" + collector.translateCallSignature(call.getMethod()));
	}

	public String getLocation() {
		return location;
	}

	/**
	 * recursive method to get the complete type hierarchy type
	 */
	private void setExtends(Type type) {
		if (type instanceof RefType) {
			SootClass sc = ((RefType) type).getSootClass();
			// adding the current type:
			if (!sc.toString().equals("java.lang.Object")) {
				_extends.add("extend:" + sc.toString());
			}
			if (sc.hasSuperclass()) {
				setExtends(sc.getSuperclass().getType());
			}
		}
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
