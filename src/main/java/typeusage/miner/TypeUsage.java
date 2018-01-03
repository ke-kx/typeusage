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

	/** Class in which the TypeUsage occurs */
	private String location = "!";

	/** Type of the used object in string form */
	protected String type = "!";

	/** Reference to sootType of used object */
	private Type sootType = null;

	/** Method in which the type usage occurs ("context" in the DMMC paper) */
	private String methodContext = "!";

	/** List of methodCalls belonging to this TypeUsage */
	private List<MethodCall> underlyingLocals = new ArrayList<MethodCall>();
	
	//TODO this could potentially be replaced by just the arrayList, correct?
	/** String form of methodCalls belonging to this TU */
	protected final Set<String> methodCalls = new HashSet<String>();

	private Set<String> _extends = new HashSet<String>();

	protected TypeUsage() {
	}

	public TypeUsage(Body body, MethodCall call, Type type, IMethodCallCollector collector) {
		methodContext = collector.translateContextSignature(body.getMethod());
		collector.debug(String.format("Creating type usage for %s with %s", methodContext, call.getLocal()));

		location = body.getMethod().getDeclaringClass().toString();
		SourceLnPosTag sourceLnTag = (SourceLnPosTag) call.getStmt().getTag("SourceLnPosTag");
		if (sourceLnTag != null) {
			location += ":" + sourceLnTag.startLn();
		}
		LineNumberTag lineNumberTag = (LineNumberTag) call.getStmt().getTag("LineNumberTag");
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

	//TODO only do string work in for string method o.O - or easier like this?
	public void addMethodCall(MethodCall call, IMethodCallCollector collector) {
		underlyingLocals.add(call);
		methodCalls.add("call:" + collector.translateCallSignature(call.getMethod()));
	}

	public String getLocation() {
		return location;
	}
	
	public List<MethodCall> getUnderlyingLocals() {
		return underlyingLocals;
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
