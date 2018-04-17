package typeusage.miner;

import java.io.File;
import java.util.Arrays;

public class AndroidCollector {

    private final String databaseLocation;
    private final File apkDir;
    private int count;

    public static void main(String[] args) throws Exception {
        AndroidCollector collector = new AndroidCollector("output/android", "apks", 0);
        collector.run();
    }

    public AndroidCollector(String databaseLocation, String apkFolder, int count) {
        this.databaseLocation = databaseLocation;
        this.apkDir = new File(apkFolder);
        this.count = count;
    }

    public void run() {
        File[] apkList = getApkList();
        System.out.println(Arrays.toString(apkList));

        if (this.count == 0) this.count = apkList.length;

        for (int i=0; i < this.count; i++) {
            TypeUsageCollector collector = new DatabaseTypeUsageCollector(this.databaseLocation, apkList[i].getName());
            setAndroidOptions(collector);
            collector.addDirToProcess(this.apkDir + "/" + apkList[i].getName());
            collector.run();

            // reset soot status for next run
            soot.G.reset();
        }
    }

    private File[] getApkList() {
        File[] ret = this.apkDir.listFiles((dir, name) -> name.endsWith(".apk"));
        if (ret == null) {
            throw new IllegalArgumentException("Apk dir doesn't contain any apks!");
        }
        Arrays.sort(ret);
        return ret;
    }

    /** As copied from:
     * https://github.com/secure-software-engineering/FlowDroid/blob/a1438c2b38a6ba453b91e38b2f7927b6670a2702/soot-infoflow-android/src/soot/jimple/infoflow/android/config/SootConfigForAndroid.java
     */
     static void setAndroidOptions(TypeUsageCollector collector) {
         String[] androidSettings = {"-src-prec", "apk-c-j", "-android-jars", "/home/tesuji/Android/Sdk/platforms", "-process-multiple-dex"};
         collector.setAdditionalOptions(androidSettings);

        // explicitly exclude packages for shorter runtime:
        // List<String> excludeList = new LinkedList<String>();
        collector.addExcludedPackage("java.*");
        collector.addExcludedPackage("sun.*");
        collector.addExcludedPackage("android.*");
        collector.addExcludedPackage("org.apache.*");
        collector.addExcludedPackage("org.eclipse.*");
        collector.addExcludedPackage("soot.*");
        collector.addExcludedPackage("javax.*");
        // Options.v().set_exclude(excludeList);
        // Options.v().set_no_bodies_for_excluded(true);
    }
}
