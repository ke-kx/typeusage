package typeusage.miner;

import java.io.File;

public class Main {

    public final static String DEFAULT_DIR = "./target/test-classes/";

    public static void main(String[] args) throws Exception {
        TypeUsageCollector c = new FileTypeUsageCollector("output/jabref.dat");
        String toBeAnalyzed = "/home/tesuji/jabref/bin";

        boolean ts = true;
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
                        System.out.printf("Adding %s\n", pathToAdd);
                        collector.addToClassPath(pathToAdd);
                    }
                }
            }
        }
    }
}
