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
        String run = "signal";
        TypeUsageCollector collector = new FileTypeUsageCollector("output/jabref.dat");
        String toBeAnalyzed = null;

        switch (run) {
            case "jabref":
                toBeAnalyzed = "/home/tesuji/jabref/bin";
                collector = new DatabaseTypeUsageCollector("output/jabref");
                break;
            case "ts":
                collector = new DatabaseTypeUsageCollector("output/teamscale");
                String teamscaleHome = "/home/tesuji/secure/teamscale";
                String teamscaleEngine = teamscaleHome + "/engine";
                String teamscaleLib = teamscaleHome + "/lib";
                toBeAnalyzed = teamscaleEngine;
                //c = new FileTypeUsageCollector("output/teamscale.dat");

                setupTSClasspaths(collector, toBeAnalyzed);
                setupTSClasspaths(collector, teamscaleLib);
                break;
            case "signal":
                String path = "/home/tesuji/Dropbox/Uni/MA/workspace/typeusage/apks/";
                toBeAnalyzed = path + "Signal-play-release-unsigned-4.16.7.apk";
                collector = new DatabaseTypeUsageCollector("output/signal");
                String[] androidSettings = {"-app", "-src-prec", "apk-c-j", "-android-jars", "/home/tesuji/Android/Sdk/platforms", "-process-multiple-dex"};
                collector.setAdditionalOptions(androidSettings);

                setAndroidExcludeOptions();
                break;
            default:
                break;
        }

        collector.addDirToProcess(toBeAnalyzed);

        logger.debug("Shouldn't appear");
        logger.warn("STARTING NOW!!!");
        collector.run();
    }

    /** As copied from:
     * https://github.com/secure-software-engineering/FlowDroid/blob/a1438c2b38a6ba453b91e38b2f7927b6670a2702/soot-infoflow-android/src/soot/jimple/infoflow/android/config/SootConfigForAndroid.java
     */
    public static void setAndroidExcludeOptions() {
        // explicitly include packages for shorter runtime:
        List<String> excludeList = new LinkedList<String>();
        excludeList.add("java.*");
        excludeList.add("sun.*");
        excludeList.add("android.*");
        excludeList.add("org.apache.*");
        excludeList.add("org.eclipse.*");
        excludeList.add("soot.*");
        excludeList.add("javax.*");
        Options.v().set_exclude(excludeList);
        Options.v().set_no_bodies_for_excluded(true);
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
