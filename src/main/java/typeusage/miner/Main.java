package typeusage.miner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class Main {

    private static final Logger logger = LogManager.getLogger();

    public final static String DEFAULT_DIR = "./target/test-classes/";

    public static void main(String[] args) throws Exception {
        TypeUsageCollector c = new FileTypeUsageCollector("output/jabref.dat");
        String toBeAnalyzed = "/home/tesuji/jabref/bin";
        c = new DatabaseTypeUsageCollector("output/jabref");

        boolean ts = false;
        if (ts) {
            c = new DatabaseTypeUsageCollector("output/teamscale");
            String teamscaleHome = "/home/tesuji/secure/teamscale";
            String teamscaleEngine = teamscaleHome + "/engine";
            String teamscaleLib = teamscaleHome + "/lib";
            toBeAnalyzed = teamscaleEngine;

            setupTSClasspaths(c, toBeAnalyzed);
            setupTSClasspaths(c, teamscaleLib);
        }

        //toBeAnalyzed = null;

        if (args.length > 0) {
            toBeAnalyzed = args[0];
            if (args.length > 1) {
                c.setPrefixToKeep(args[1]);
            }
        } else if (toBeAnalyzed == null) {
            toBeAnalyzed = DEFAULT_DIR;
        }
        c.setDirToProcess(toBeAnalyzed);

        logger.debug("Shouldn't appear");
        logger.warn("STARTING NOW!!!");
        c.run();
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
                        collector.addToClassPath(pathToAdd);
                    }
                }
            }
        }
    }
}
