package typeusage.miner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.options.Options;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class Main {

    private static final Logger logger = LogManager.getLogger();

    public final static String DEFAULT_DIR = "./target/test-classes/";

    public static void main(String[] args) throws Exception {
        String run = "jabref";
        TypeUsageCollector collector = new FileTypeUsageCollector("output/jabref.dat");
        String toBeAnalyzed = null;

        switch (run) {
            case "jabref":
                toBeAnalyzed = "/home/tesuji/jabref/bin";
                collector = new DatabaseTypeUsageCollector("output/jabref", "jabref");
                break;
            case "ts":
                collector = new DatabaseTypeUsageCollector("output/teamscale", "teamscale");
                String teamscaleHome = "/home/tesuji/secure/teamscale";
                String teamscaleEngine = teamscaleHome + "/engine";
                String teamscaleLib = teamscaleHome + "/lib";
                toBeAnalyzed = teamscaleEngine;
                //c = new FileTypeUsageCollector("output/teamscale.dat");
                String[] settings = {"-src-prec", "c"};
                collector.setAdditionalOptions(settings);

                setupTSClasspaths(collector, toBeAnalyzed);
                setupTSClasspaths(collector, teamscaleLib);
                setTSExcludeOptions(collector);
                break;
            case "signal":
                String path = "/home/tesuji/Dropbox/Uni/MA/workspace/typeusage/apks/";
                toBeAnalyzed = path + "Signal-play-release-unsigned-4.16.7.apk";
                collector = new DatabaseTypeUsageCollector("output/signal", "signal");

                AndroidCollector.setAndroidOptions(collector);
                break;
            default:
                break;
        }

        collector.addDirToProcess(toBeAnalyzed);

        logger.debug("Shouldn't appear");
        logger.warn("STARTING NOW!!!");
        collector.run();
    }

    private static void setTSExcludeOptions(TypeUsageCollector collector) {
        // explicitly exclude packages for shorter runtime:
        // excludeList.add("eu.cqse.check.framework.scanner.*");
        // excludeList.add("org.conqat.lib.cqddl.parser.*");
        // excludeList.add("org.conqat.lib.simulink.targetlink.*");
        // excludeList.add("org.conqat.lib.simulink.builder.*");
        // excludeList.add("org.conqat.engine.text.comments.analysis.*");
        // excludeList.add("org.conqat.engine.persistence.index.keyed.query.lexer.*");

        collector.addExcludedPackage("$SWITCH_TABLE$eu$cqse$check$framework$scanner$ETokenType");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.JPLToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.CSToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.PLSQLToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.JavaScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.MagikScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.ABAPScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.PythonScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.JavaScriptScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.GroovyToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.CSScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.XtendScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.RubyToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.TSQLScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.HanaSQLScriptScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.CPPScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.KotlinToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.MTextToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.HanaSQLScriptToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.CobolScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.MagikToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.TextToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.GosuToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.PL1Token");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.XtendToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.RustToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.XMLToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.KotlinScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.SwiftToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.TextScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.OScriptScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.OScriptToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.ABAPToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.FortranToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.Iec61131Scanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.PythonToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.GosuScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.VBScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.PLSQLScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.SwiftScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.CPPToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.DelphiToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.OpenCLToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.OpenCLScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.PHPScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.UnicodeEscapes");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.DelphiScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.JavaToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.FortranScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.JPLScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.XMLScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.AdaScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.RustScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.OCamlScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.LineScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.PL1Scanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.Iec61131Token");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.CobolToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.GroovyScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.OCamlToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.MatlabScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.MTextScanner");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.ETokenType");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.JavaScriptToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.MatlabToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.VBToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.LineToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.TSQLToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.AdaToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.PHPToken");
        collector.addExcludedPackage("eu.cqse.check.framework.scanner.RubyScanner");

        collector.addExcludedPackage("org.conqat.lib.cqddl.parser.CQDDLLexer");
        collector.addExcludedPackage("org.conqat.lib.cqddl.parser.CQDDLParser");
        collector.addExcludedPackage("org.conqat.lib.simulink.targetlink.TargetlinkDataParser");
        collector.addExcludedPackage("org.conqat.lib.simulink.targetlink.SymbolConstants");
        collector.addExcludedPackage("org.conqat.lib.simulink.targetlink.TargetlinkDataScanner");
        collector.addExcludedPackage("org.conqat.lib.simulink.builder.SymbolConstants");
        collector.addExcludedPackage("org.conqat.lib.simulink.builder.MDLParser");
        collector.addExcludedPackage("org.conqat.lib.simulink.builder.MDLScanner");

        collector.addExcludedPackage("org.conqat.engine.text.comments.analysis.CppCommentClassifier");
        collector.addExcludedPackage("org.conqat.engine.text.comments.analysis.JavaCommentClassifier");
        collector.addExcludedPackage("org.conqat.engine.text.comments.analysis.CsCommentClassifier");

        collector.addExcludedPackage("org.conqat.engine.persistence.index.keyed.query.lexer.GeneratedQueryLexer");
    }

    private static void setupTSClasspaths(TypeUsageCollector collector, String parentDir) {
        File dir = new File(parentDir);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                if (child.isDirectory()) {
                    String pathToAdd = child.getAbsolutePath() + "/classes";
                    if (new File(pathToAdd).exists()) {
                        logger.info("Adding {}", pathToAdd);
                        collector.addDirToProcess(pathToAdd);
                    }
                }
            }
        }
    }
}
