package de.tud.stg.analysis;

public class Main {

	public static void main(String[] args) throws Exception {

		ComputePrecisionAndRecall analysis;
		analysis = new AnalysisDegraded();
		analysis.setDatasetFileName("eclipse-soot-swt-v5.dat");
		analysis.setOption_k(1);
		System.out.println(analysis.run());

	}
}
