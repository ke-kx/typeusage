package typeusage.miner;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileTypeUsageCollector extends TypeUsageCollector {

    private BufferedWriter output;

    public FileTypeUsageCollector(String file) throws Exception {
        super();
        output = new BufferedWriter(new FileWriter(file));
    }

    final public List<TypeUsage> data = new ArrayList<TypeUsage>();

    @Override
    public FileTypeUsageCollector run() {
        super.run();
        close();
        return this;
    }

    @Override
    public void receive(TypeUsage t) {
        try {
            output.write(t.toString() + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void close() {
        try {
            output.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
