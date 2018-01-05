package de.tud.stg.analysis;

public class Main {

	public static void main(String[] args) throws Exception {

		ComputePrecisionAndRecall analysis = new AnalysisDegraded("datasets/eclipse-soot-swt-v5.dat");
		System.out.println(analysis.run());

	}
}
