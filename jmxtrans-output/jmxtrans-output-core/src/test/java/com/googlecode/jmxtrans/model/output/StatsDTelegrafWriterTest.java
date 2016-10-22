package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.googlecode.jmxtrans.model.ResultAttribute;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static com.googlecode.jmxtrans.model.QueryFixtures.dummyQuery;
import static com.googlecode.jmxtrans.model.ResultFixtures.numericResult;
import static com.googlecode.jmxtrans.model.ResultFixtures.singleNumericBelowCPrecisionResult;
import static com.googlecode.jmxtrans.model.ResultFixtures.singleNumericResult;
import static com.googlecode.jmxtrans.model.ServerFixtures.dummyServer;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Nate Good on 10/21/16.
 */
public class StatsDTelegrafWriterTest {

    private StringWriter writer = new StringWriter();
    String expectedMeasureAndTags = "MemoryAlias,jmxport=4321,objectName=myQuery:key=val,attribute=ObjectPendingFinalizationCount:";

    @Test
    public void countAttribute() throws IOException {
        StatsDTelegrafWriter w = new StatsDTelegrafWriter(StatsDMetricType.COUNTER.getKey(), null, ImmutableMap.<String, String>of(), ImmutableSet.<ResultAttribute>of());
        w.write(writer, dummyServer(), dummyQuery(), singleNumericResult());
        assertThat(writer.toString()).isEqualTo(expectedMeasureAndTags + "10|c|@1.0\n");
    }

    @Test
    public void countAttribute_negativeValueIsIgnored() throws IOException {
        StatsDTelegrafWriter w = new StatsDTelegrafWriter(StatsDMetricType.COUNTER.getKey(), null, ImmutableMap.<String, String>of(), ImmutableSet.<ResultAttribute>of());
        w.write(writer, dummyServer(), dummyQuery(), ImmutableList.of(numericResult(-10)));
        assertThat(writer.toString()).isEqualTo("");
    }

    @Test
    public void gaugeAttribute() throws IOException {
        StatsDTelegrafWriter w = new StatsDTelegrafWriter(StatsDMetricType.GAUGE.getKey(), null, ImmutableMap.<String, String>of(), ImmutableSet.<ResultAttribute>of());
        w.write(writer, dummyServer(), dummyQuery(), ImmutableList.of(numericResult(-10)));
        assertThat(writer.toString()).isEqualTo(expectedMeasureAndTags + "-10|g|@1.0\n");
    }

    @Test
    public void multipleBucketTypes() throws IOException {
        StatsDTelegrafWriter w = new StatsDTelegrafWriter("c,ms,g", null, ImmutableMap.<String, String>of(), ImmutableSet.<ResultAttribute>of());
        w.write(writer, dummyServer(), dummyQuery(), ImmutableList.of( numericResult(5), numericResult(250), numericResult(10)));
        assertThat(writer.toString()).isEqualTo(
            expectedMeasureAndTags + "5|c|@1.0\n" +
            expectedMeasureAndTags + "250|ms|@1.0\n" +
            expectedMeasureAndTags + "10|g|@1.0\n");
    }
}