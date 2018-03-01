package typeusage.miner;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.*;
import soot.options.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class makes a static anaylsis of a directory containing Java bytecode using the Soot toolkit
 * <p>
 * It extracts all variables and their method calls
 *
 * @author Martin Monperrus
 */
public abstract class TypeUsageCollector implements IMethodCallCollector {

    private static final Logger logger = LogManager.getLogger();

    /**
     * Keep a type-usage if it's class' fully-qualified name starts with this prefix
     */
    private String prefixToKeep = "";

    /** Contains all relavant classpaths */
    private final List<String> classpaths = new ArrayList<>();

    /** Contains all relavant dirs */
    private final List<String> processDirs = new ArrayList<>();

    /** Contains excluded packages */
    private String[] additionalOptions;

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
    public TypeUsageCollector run() {

        PackManager.v().getPack("jtp").add(new Transform("jtp.myTransform", new TUBodyTransformer(this)));

        Options.v().set_keep_line_number(true);
        Options.v().set_output_format(Options.output_format_none);

        String[] myArgs = buildSootArgs();

        logger.warn("Soot Commandline Arguments: " + Arrays.toString(myArgs));

        soot.Main.main(myArgs);
        return this;
    }

    public void setAdditionalOptions(String[] additionalOptions) {
        this.additionalOptions = additionalOptions;
    }

    /** Assemble arguments by setting classpath and processed directory */
    protected String[] buildSootArgs() {
        String[] myArgs = {//"-soot-classpath", getClassPath(), "-pp", // prepend is not required
                };
        String[] processDirsStrings = new String[2 * processDirs.size()];

        int i = 0;
        for (String path : processDirs) {
            processDirsStrings[i * 2] = "-process-dir";
            processDirsStrings[i * 2 + 1] = path;
            i++;
        }

        return Stream.concat(Stream.concat(Arrays.stream(additionalOptions), Arrays.stream(myArgs)), Arrays.stream(processDirsStrings))
                .toArray(String[]::new);
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
    public void debug(String format, Object... args) {
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
        List<String> cp = new ArrayList<>(classpaths);
        // cp.add(dirToProcess);
        return StringUtils.join(cp, ":");
    }

    /** Adds a new file to the classpath as long as it exists */
    public void addToClassPath(String jar) {
        if (!new File(jar).exists()) {
            throw new IllegalArgumentException(jar + " must be a valid file");
        }
        classpaths.add(jar);
    }

    /** @see #processDirs */
    public void addDirToProcess(String dirToProcess) {
        processDirs.add(dirToProcess);
    }
}