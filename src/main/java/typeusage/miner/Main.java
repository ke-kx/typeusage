package typeusage.miner;

import java.io.File;

public class Main {

    public final static String DEFAULT_DIR = "./target/test-classes/";

    public static void main(String[] args) throws Exception {
        TypeUsageCollector c = new FileTypeUsageCollector("output/output.dat");
        c = new DatabaseTypeUsageCollector("output/test");

        String toBeAnalyzed = "/home/tesuji/jabref/bin";
        //toBeAnalyzed = "/home/tesuji/secure/teamscale/engine";
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

        //TODO does this make sense or rather investigate the not class method again? / generally read into soot a little bit deeper possibly
        File dir = new File(toBeAnalyzed + "/public-package-jars/");
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                System.out.printf("Adding %s\n", child.getAbsolutePath());
                c.addToClassPath(child.getAbsolutePath());
            }
        }

        c.run();
    }
}
