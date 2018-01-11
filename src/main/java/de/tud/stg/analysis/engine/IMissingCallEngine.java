package de.tud.stg.analysis.engine;

import de.tud.stg.analysis.DegradedObjectTrace;
import de.tud.stg.analysis.ObjectTrace;

import java.util.HashMap;

public interface IMissingCallEngine {
    /** returns a list of method calls */
    HashMap<String, Integer> query(ObjectTrace o);

    /** returns a list of method calls */
    HashMap<String, Integer> simulateQuery(DegradedObjectTrace degraded);
}
