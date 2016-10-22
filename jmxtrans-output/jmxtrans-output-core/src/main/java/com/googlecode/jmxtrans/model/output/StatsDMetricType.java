package com.googlecode.jmxtrans.model.output;

/**
 * Created by Nate Good on 10/21/16.
 */
public enum StatsDMetricType {

    COUNTER("c"), 
    GAUGE("g"), 
    SET("s"), 
    TIMING_MS("ms");


    private final String key;

    StatsDMetricType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
