package typeusage.miner;

public class Main {

	public final static String DEFAULT_DIR = "./target/test-classes/";

	public static void main(String[] args) throws Exception {
		FileTypeUsageCollector c = new FileTypeUsageCollector("output/output.dat");
		String toBeAnalyzed = "/home/tesuji/jabref";
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
		c.close();
	}
}
