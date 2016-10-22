/**
 * The MIT License
 * Copyright © 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.ResultAttribute;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.support.WriterBasedOutputWriter;
import com.googlecode.jmxtrans.model.results.CPrecisionValueTransformer;
import com.googlecode.jmxtrans.model.results.ValueTransformer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.googlecode.jmxtrans.util.NumberUtils.isNumeric;

public class StatsDTelegrafWriter implements WriterBasedOutputWriter {

	private static final Logger log = LoggerFactory.getLogger(StatsDTelegrafWriter.class);

	private final ValueTransformer valueTransformer = new CPrecisionValueTransformer();
	private final String[] bucketTypes;
	private double sampleRate;
	private final ImmutableMap<String, String> tags;
	private final ImmutableSet<ResultAttribute> resultAttributesToWriteAsTags;

	public StatsDTelegrafWriter(String bucketType, String sampleRate, ImmutableMap<String, String> tags, ImmutableSet<ResultAttribute> resultAttributesToWriteAsTags) {
		this.bucketTypes = StringUtils.split(bucketType, ",");
		setSampleRate(sampleRate);
		this.tags = tags;
		this.resultAttributesToWriteAsTags = resultAttributesToWriteAsTags;
	}

	@Override
	public void write(@Nonnull Writer writer, @Nonnull Server server, @Nonnull Query query, @Nonnull Iterable<Result> results) throws IOException {

		int resultIndex = -1;
		for (Result result : results) {
			resultIndex++;
			String bucketType = getBucketType(resultIndex);
			for (Map.Entry<String, Object> values : result.getValues().entrySet()) {

				String attributeName = values.getKey();
				Object value = values.getValue();
				if (isNotValidValue(value)) {
					log.debug("Skipping message key[{}] with value: {}.", attributeName, value);
					continue;
				}

				List<String> tagList = new ArrayList<>();
				tagList.add(",jmxport=" + server.getPort());
				tagList.add("objectName=" + query.getObjectName());
				tagList.add("attribute=" + attributeName);
				for (Map.Entry e : tags.entrySet()) {
					tagList.add(e.getKey() + "=" + e.getValue());
				}

				Integer actualValue = computeActualValue(value);
				StringBuilder sb = new StringBuilder(result.getKeyAlias())
					.append(StringUtils.join(tagList, ","))
					.append(":").append(actualValue)
					.append("|").append(bucketType)
					.append("|@").append(sampleRate).append("\n");

				if( actualValue < 0 && !StatsDMetricType.GAUGE.getKey().equals(bucketType) )
				{
					log.debug("Negative values are only supported for gauges, not sending: {}.", sb.toString());
				}
				else{
					writer.write(sb.toString());
				}
			}
		}
	}

	private String getBucketType(int resultIndex) {
		if( this.bucketTypes.length > resultIndex ){
			return bucketTypes[resultIndex];
		}
		return bucketTypes[bucketTypes.length - 1];
	}

	private boolean isNotValidValue(Object value) {
		return !(isNumeric(value) /*|| stringsValuesAsKey*/);
	}

	private Integer computeActualValue(Object value) {
		Object transformedValue = valueTransformer.apply(value);
		if (isNumeric(transformedValue)) {
			return transformedValue instanceof Number ?
				((Number) transformedValue).intValue() :
			    Integer.parseInt(transformedValue.toString());
		}
		// TODO: Revisit this, cant' just append ".string:1" with tags
		// Will have to precalculate it when adding the measurement, is it necessary?
		return null;
	}

	void setSampleRate(String sampleRate) {
		this.sampleRate = 1.0;
		if( isNumeric(sampleRate) ){
			Double d = Double.parseDouble(sampleRate);
			if( d < 0 || d > 1 ){
				log.warn("Sample rate was specified as '{}', it must be between 0 and 1, setting to '1'.", sampleRate);
			} else {
				this.sampleRate = d;
			}
		} else {
			log.warn("Sample rate was specified as '{}', it must be between 0 and 1, setting to '1'.", sampleRate);
		}
	}
}
