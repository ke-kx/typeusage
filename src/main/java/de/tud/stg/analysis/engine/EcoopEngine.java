package de.tud.stg.analysis.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.tud.stg.analysis.DatasetReader;
import de.tud.stg.analysis.DegradedObjectTrace;
import de.tud.stg.analysis.ObjectTrace;

public class EcoopEngine implements IMissingCallEngine {

	List<ObjectTrace> dataset;

	public boolean option_filterIsEnabled = true;
	public double option_filter_threshold = 0.9;

	/** Quick and dirty missing method call detector */
	public static void main(String[] args) throws Exception {
		double minStrangeness = 0.9;
		String datasetFileName = "datasets/eclipse-soot-swt-v5.dat";
		List<ObjectTrace> input = new DatasetReader().readObjects(datasetFileName);
		EcoopEngine engine = new EcoopEngine(input);
		
		List<ObjectTrace> violations = new ArrayList<>();
		
		int i = 0;
		for (ObjectTrace ot : input) {
			i ++;
			if (i % 1000 == 0) System.out.printf("%d / %d\n", i, input.size());
			
			engine.query(ot);
			double strangeness = ot.strangeness();
			
			if (strangeness >= minStrangeness) {
				System.out.println("Violation!");
				violations.add(ot);
			}
		}
		
		violations.sort((f1, f2) -> Double.compare(f2.strangeness(), f1.strangeness()));

		System.out.printf("%d instances found with strangeness above %f!\n", violations.size(), minStrangeness);
		System.out.printf("Highest strangeness score: %f\n", violations.get(0).strangeness());

		for (ObjectTrace ot : violations) {
			System.out.println(ot);
		}
	}

	public EcoopEngine(List<ObjectTrace> l) {
		//TODO is this a deep or shallow copy?
		dataset = new ArrayList<ObjectTrace>(l);
	}

	@Override
	public HashMap<String, Integer> query(ObjectTrace query) {
		// reset
		//TODO doesnt missingcalls also have to be reseted?
		query.reset();

		for (ObjectTrace o2 : dataset) {
			if (query.isEqual(o2))
				query.incEqualCount();
			if (query.isAlmostEqual(o2)) {
				query.incAlmostEqualCount();
				for (String missingCall : query.getMissingCalls(o2)) {
					query.incMissingCallCount(missingCall);
				}
			}
		}

		//TODO what is the point of this exactly?
		// possible filter on the missing calls with a second threshold
		if (option_filterIsEnabled) {
			List<String> callsToBeFiltered = new ArrayList<String>();
			int nmissing = query.missingCallStatistics.size();
			for (String cs : query.missingCallStatistics.keySet()) {
				if ((query.missingCallStatistics.get(cs) * 1.0) / nmissing < option_filter_threshold) {
					callsToBeFiltered.add(cs);
				}
			}
			for (String cs : callsToBeFiltered) {
				query.missingCallStatistics.remove(cs);
			}
		}

		return query.missingCallStatistics;
	}

	@Override
	public HashMap<String, Integer> simulateQuery(DegradedObjectTrace degraded) {

		if (degraded.original != null) {
			if (!dataset.remove(degraded.original))
				throw new RuntimeException();
		}
		HashMap<String, Integer> result = query(degraded);
		if (degraded.original != null) {
			dataset.add(degraded.original);
		}
		return result;
	}
}
