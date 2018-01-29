package typeusage.miner;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.*;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceLnPosTag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * is an abstraction over Soot locals: Variable: 1 ----- 1..n Local
 */
public class TypeUsage {

    private static final Logger logger = LogManager.getLogger();

    /** Class in which the TypeUsage occurs */
    private String location = "!";

    /** Line nr where the TypeUsage occurs if available */
    private Integer lineNr = null;

    /** Type of the used object in string form */
    protected String type = "!";

    /** Reference to sootType of used object */
    private Type sootType = null;

    /** Method in which the type usage occurs ("context" in the DMMC paper) */
    private String methodContext = "!";

    /** List of methodCalls belonging to this TypeUsage */
    private List<MethodCall> underlyingLocals = new ArrayList<>();

    //TODO this could potentially be replaced by just the arrayList, correct?
    /** String form of methodCalls belonging to this TU */
    protected final Set<String> methodCalls = new HashSet<>();
    protected List<String> methodCallsInOrder = new ArrayList<>();

    /** The type hierarchy of the type  */
    private List<String> _extends = new ArrayList<>();

    protected TypeUsage() {
    }

    public TypeUsage(Body body, MethodCall call, Type type, IMethodCallCollector collector) {
        methodContext = collector.translateContextSignature(body.getMethod());
        logger.debug("Creating type usage for {0} with {0}", methodContext, call.getLocal());

        location = body.getMethod().getDeclaringClass().toString();
        SourceLnPosTag sourceLnTag = (SourceLnPosTag) call.getStmt().getTag("SourceLnPosTag");
        if (sourceLnTag != null) {
            lineNr = sourceLnTag.startLn();
        }
        LineNumberTag lineNumberTag = (LineNumberTag) call.getStmt().getTag("LineNumberTag");
        if (lineNumberTag != null) {
            lineNr = lineNumberTag.getLineNumber();
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

    /** Start going up the type hierarchy */
    private void setExtends(Type type) {
        if (type instanceof RefType) {
            SootClass sc = ((RefType) type).getSootClass();
            if (sc.hasSuperclass()) {
                setExtendsRecursive(sc.getSuperclass().getType());
            }
        }
    }

    /** recursive method to get the complete type hierarchy type */
    private void setExtendsRecursive(Type type) {
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

    /** Get list of all types that the original type extends, as well as the original type */
    public List<String> getTypeHierarchy() {
        List<String> ret = _extends.stream()
                .map(s -> s.replace("extend:", ""))
                .collect(Collectors.toList());
        ret.add(0, type);
        return ret;
    }

    /** Get set of all method calls applied in this TU */
    public Set<String> getMethodCalls() {
        return methodCalls.stream()
                .map(s -> s.replace("call:", ""))
                .collect(Collectors.toSet());
    }

    //TODO only do string work in for string method o.O - or easier like this?
    public void addMethodCall(MethodCall call, IMethodCallCollector collector) {
        underlyingLocals.add(call);
        methodCalls.add("call:" + collector.translateCallSignature(call.getMethod()));
        methodCallsInOrder.add(collector.translateCallSignature(call.getMethod()));
    }

    public String getLocation() {
        return location;
    }

    public Integer getLineNr() {
        return lineNr;
    }

    public String getContext() {
        return methodContext;
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
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TypeUsage))
            return false;
        TypeUsage other = (TypeUsage) obj;
        return other.type.equals(this.type) && other.methodCalls.equals(this.methodCalls);
    }

    @Override
    public String toString() {
        return String.format("location:%s:%d context:%s type:%s %s %s",
                location, lineNr, methodContext, type, StringUtils.join(methodCalls, " "), StringUtils.join(_extends, " "));
    }
}
