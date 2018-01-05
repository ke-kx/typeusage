package de.tud.stg.analysis.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.tud.stg.analysis.DegradedObjectTrace;
import de.tud.stg.analysis.ObjectTrace;

public class EcoopEngine implements IMissingCallEngine {

	List<ObjectTrace> dataset;

	public boolean option_filterIsEnabled = true;
	public double option_filter_threshold = 0.9;

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

		// in this case, there could not be almost equals almost by construction
		// which mechanically degrades the results
		// so we discard them

		// however, this does give a better overview of the results thanks to
		// the if (o2.equals(o1)) continue; above
		// mechanically improve
		// * the precision (.84-> .86)
		// * and especially the answered/recall .78 -> 1
		// so it's better to actually consider the precision
		// beacause we don't consider hard cases
		// or in other terms it removes all queries that could not be answered directly
		// if (option_norarecase == true && probe==0) {
		// nspecialcase++;
		// continue;
		// }

		/*
		 * if (nalmostequals2 == 0) {
		 * System.out.println(nequals2+" "+nalmostequals2+" "+probe+" "+degradedRecord);
		 * }
		 */
		// possible filter on the missing calls with a second threshold
		// if (true){
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
