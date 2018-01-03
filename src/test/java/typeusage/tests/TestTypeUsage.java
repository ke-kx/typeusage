package typeusage.tests;

import typeusage.miner.TypeUsage;

public class TestTypeUsage extends TypeUsage {

	public TestTypeUsage type(String type) {
		this.type = type;
		return this;
	}

	public TestTypeUsage call(String call) {
		methodCalls.add("call:" + call);
		return this;
	}

}
