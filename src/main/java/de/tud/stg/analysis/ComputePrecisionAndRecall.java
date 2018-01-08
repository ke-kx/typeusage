package de.tud.stg.analysis;

import java.io.*;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import de.tud.stg.analysis.engine.EcoopEngine;
import de.tud.stg.analysis.engine.IMissingCallEngine;

/**
 * Computes the performance of the system
 * 
 * @author martin
 */
public abstract class ComputePrecisionAndRecall {

	private List<ObjectTrace> l;

	// default engine
	private EcoopEngine ecoopEngine;
	protected IMissingCallEngine engine;

	private String suffix = ""; // default value

	public abstract List<DegradedObjectTrace> createEvaluationData(List<ObjectTrace> input);

	boolean display_tmp = false;

	protected String datasetFileName;
	
	private static final int OUTPUT_STEP_SIZE = 1000;

	public static void main(String[] args) throws Exception {

		ComputePrecisionAndRecall analysis = new AnalysisDegraded("datasets/eclipse-soot-swt-v5.dat");
		System.out.println(analysis.run());

	}

	/** Constructor */
	public ComputePrecisionAndRecall(String datasetFileName) {
		super();
		this.datasetFileName = datasetFileName;
	}

	public List<ObjectTrace> getEvaluationDataSet() {
		if (l == null)
			throw new RuntimeException();
		return l;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	protected IMissingCallEngine getEngine() throws Exception {
		if (engine != null)
			return engine;
		if (ecoopEngine == null) {
			ecoopEngine = new EcoopEngine(defaultDataset());
		}
		return ecoopEngine;
	}

	public void setEvaluationDataSet(List<ObjectTrace> list) {
		if (list == null)
			throw new RuntimeException();
		l = list;
	}

	public String run() throws Exception {

		int nanalyzed = 0;
		int nanswered = 0;

		// everytinh is float in order not to ennoyed by euclidean division
		double nperfect = 0;
		double ntoomuch = 0;
		
		// Number of specialcases without any almost equal or equal TUs (shouldn't exist?)
		double nspecialcase = 0;
		double nkilled = 0;
		double nfalse = 0;
		double precision_score = 0;
		double recall_score = 0;
		double strangeness_score = 0;
		double strangeness_score2 = 0;
		double strangeness_score3 = 0;
		double strangeness_score4 = 0;
		List<String> overview = new ArrayList<String>();

		IMissingCallEngine engine = getEngine();

		System.out.println("creating evaluation data...");
		List<DegradedObjectTrace> testCases = createEvaluationData(getEvaluationDataSet());

		Writer output = getOutputWriter();

		List<Double> medianlist = new ArrayList<Double>();

		System.out.println("\ncomputing precision and recall...");
		for (DegradedObjectTrace degradedRecord : testCases) {
			{
				if ((nanalyzed % OUTPUT_STEP_SIZE) == 0) System.out.print("\r" + nanalyzed + "/" + testCases.size());
				engine.simulateQuery(degradedRecord);

				double strangeness = degradedRecord.strangeness();
				medianlist.add(strangeness);
				strangeness_score += strangeness;

				if (strangeness < 0.1) {
					strangeness_score2++;
				}
				if (strangeness > 0.9) {
					strangeness_score3++;
				}
				if (strangeness > 0.5) {
					strangeness_score4++;
				}

				// there is no equals and no almost equals
				// we really can not say anything
				if (degradedRecord.isSpecial()) {
					nspecialcase++;
				}

				nanalyzed++;
				output.write(strangeness + " " + degradedRecord.getCounts() + "\n");

				if (strangeness > 0.8) {
					nkilled++;
				}
				// else {
				// degradedRecord.missingcalls.clear();
				// }

				if (degradedRecord.missingCallStatistics.size() > 0) {
					nanswered++;
				} else {
					// System.out.println("I am in ");
				}

				if (degradedRecord.missingCallStatistics.keySet().contains(degradedRecord.removedMethodCall)) {

					// System.out.println(degradedRecord.missingcalls.keySet().size());
					precision_score += 1.0 / (degradedRecord.missingCallStatistics.keySet().size());
					recall_score += 1;

					if (degradedRecord.missingCallStatistics.keySet().size() == 1) {
						nperfect++;
					} else {
						ntoomuch++;
					}
				} else if (degradedRecord.missingCallStatistics.keySet().size() > 0) {
					// answered but false
					// we increase neither the precision nor the recall
					nfalse++;
				}

				overview.clear();
				overview.add("#queries:" + Integer.toString(nanalyzed));
				overview.add(String.format("killed:%2.2f", ((nkilled * 1.0) / nanalyzed)));
				// overview.add(String.format("found:%2.2f",
				// (((ntoomuch+nperfect)*1.0)/nanalyzed))
				overview.add(String.format("specialcase:%2.2f", ((nspecialcase * 1.0) / testCases.size())));
				overview.add(String.format("answered:%2.2f", ((nanswered * 1.0) / nanalyzed)));

				// nanswered block
				overview.add(String.format("false:%2.2f", (nfalse / nanswered)));
				overview.add(String.format("correct:%2.2f", ((ntoomuch + nperfect) / nanswered)));
				// overview.add(String.format("found2:%2.2f", (recall_score/nanswered)));

				overview.add(String.format("perfect:%2.2f", (nperfect / nanswered)));
				overview.add(String.format("toomuch:%2.2f", (ntoomuch / nanswered)));

				overview.add(String.format("precision:%2.2f", (precision_score / (nanswered))));
				overview.add(String.format("recall:%2.2f", (recall_score / nanalyzed)));
				overview.add(String.format("mean-sscore:%2.2f", ((strangeness_score * 1.0) / nanalyzed)));
				overview.add(String.format("sscore<.1:%2.4f", ((strangeness_score2 * 1.0) / nanalyzed)));
				overview.add(String.format("sscore>.9:%2.4f", ((strangeness_score3 * 1.0) / nanalyzed)));
				overview.add(String.format("sscore>.5:%2.4f", ((strangeness_score4 * 1.0) / nanalyzed)));
				// overview.add(String.format("sscoretmp:%2.2f", strangeness));
				if (display_tmp)
					System.out.println(StringUtils.join(overview, " "));

			}

		}
		output.close();

		// computing and adding the median
		Collections.sort(medianlist);
		if (medianlist.size() > 1) {
			overview.add(String.format("median-sscore:%2.4f", medianlist.get(medianlist.size() / 2)));
		}

		IOUtils.copy(new StringBufferInputStream(StringUtils.join(overview, "\n")),
				new FileOutputStream(datasetFileName.replace('/', '-') + getClass().getName() + suffix));
		return "\n" + StringUtils.join(overview, "\n");
	}

	protected BufferedWriter getOutputWriter() throws IOException {
		return new BufferedWriter(new StringWriter());
	}

	public String getExperimentParameters() throws Exception {

		List<String> result = new ArrayList<String>();
		result.add("dataset(" + datasetFileName + ")");
		return StringUtils.join(result, " ");
	}

	/** default dataset, noth a getter and a setter */
	protected List<ObjectTrace> defaultDataset() throws Exception {
		List<ObjectTrace> defaultDataSet = new DatasetReader().readObjects(datasetFileName);
		setEvaluationDataSet(defaultDataSet);
		return defaultDataSet;
	}

	public void setEcoopEngineThreshold(double d) throws Exception {
		if (((EcoopEngine) getEngine()).option_filterIsEnabled == false) {
			throw new RuntimeException("option_filterIsEnabled is false");
		}
		((EcoopEngine) getEngine()).option_filter_threshold = d;

	}

	public void enableFiltering() throws Exception {
		((EcoopEngine) getEngine()).option_filterIsEnabled = true;
	}

	protected void disableFiltering() throws Exception {
		((EcoopEngine) getEngine()).option_filterIsEnabled = false;
	}
}
