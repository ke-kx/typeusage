package typeusage.miner;

public class ConsoleTypeUsageCollector extends TypeUsageCollector {
    @Override
    public void receive(TypeUsage t) {
        System.out.println(t);
    }
}
