package typeusage.miner;

import org.apache.commons.lang.StringUtils;
import soot.*;
import soot.options.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class makes a static anaylsis of a directory containing Java bytecode using the Soot toolkit
 * <p>
 * It extracts all variables and their method calls
 *
 * @author Martin Monperrus
 */
public abstract class TypeUsageCollector implements IMethodCallCollector {

    /**
     * Keep a type-usage if it's class' fully-qualified name starts with this prefix
     */
    private String prefixToKeep = "";

    /** Directory which shall be processed by Soot */
    private String dirToProcess;

    /** Contains all relavant classpaths */
    final List<String> classpaths = new ArrayList<String>();

    /** Constructor */
    public TypeUsageCollector() {
        // TODO blogpost claims this option is not actually recommended?
        // http://www.bodden.de/2008/08/21/soot-command-line/
        Options.v().set_allow_phantom_refs(true);

        //TODO: do i actually want to analyse the src or the classfiles?
        //Options.v().set_src_prec(Options.src_prec_c);
        Scene.v().addBasicClass("java.lang.invoke.LambdaMetafactory", SootClass.SIGNATURES);
    }

    /** Register Transform, set options and start analysis */
    @Override
    public TypeUsageCollector run() {

        PackManager.v().getPack("jtp").add(new Transform("jtp.myTransform", new TUBodyTransformer(this)));

        Options.v().set_keep_line_number(true);
        Options.v().set_output_format(Options.output_format_none);

        String[] myArgs = buildSootArgs();

        soot.Main.main(myArgs);
        return this;
    }

    /** Assemble arguments by setting classpath and processed directory */
    protected String[] buildSootArgs() {
        String[] myArgs = {"-soot-classpath", getClassPath(), "-pp", // prepend is not required
                "-process-dir", dirToProcess,};
        return myArgs;
    }

    @Override
    public String translateCallSignature(SootMethod meth) {
        // can also be meth.getSignature
        return meth.getName() + "()";
        // or aVariable.addMethodCall(invokeExpr.getMethod().getName());
    }

    //TODO replace with string.format ...
    @Override
    public String translateContextSignature(SootMethod meth) {
        StringBuilder sb = new StringBuilder();
        sb.append(meth.getName()).append("(");
        boolean firstParam = true;
        for (Type pType : ((List<Type>) meth.getParameterTypes())) {
            if (!firstParam) {
                sb.append(",");
            }
            String typeName = pType.toString();
            int lastIndexOfDot = typeName.lastIndexOf('.');
            if (lastIndexOfDot > -1) {
                typeName = typeName.substring(lastIndexOfDot + 1);
            }
            sb.append(typeName);
            firstParam = false;
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public void debug(String msg) {
        // subclasses may override
    }

    /** @see #prefixToKeep */
    @Override
    public String getPrefixToKeep() {
        return prefixToKeep;
    }

    /** @see #prefixToKeep */
    public void setPrefixToKeep(String prefix) {
        this.prefixToKeep = prefix;
    }

    /**
     * Return classpath array joined by ":" and including the directory to process
     */
    public String getClassPath() {
        if (!classpaths.contains(dirToProcess))
            classpaths.add(dirToProcess);
        return StringUtils.join(classpaths, ":");
    }

    /** Adds a new file to the classpath as long as it exists */
    public void addToClassPath(String jar) {
        if (!new File(jar).exists()) {
            throw new IllegalArgumentException(jar + " must be a valid file");
        }
        classpaths.add(jar);
    }

    /** @see #dirToProcess */
    public void setDirToProcess(String dirToProcess) {
        this.dirToProcess = dirToProcess;
    }
}