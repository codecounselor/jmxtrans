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

import com.google.common.collect.ImmutableList;
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

public class TelegrafWriter implements WriterBasedOutputWriter {

	private static final Logger log = LoggerFactory.getLogger(TelegrafWriter.class);

	private final ValueTransformer valueTransformer = new CPrecisionValueTransformer();
	private final ImmutableList<String> typeNames;
	private final String bucketType;
	private final ImmutableMap<String, String> tags;
	private final ImmutableSet<ResultAttribute> resultAttributesToWriteAsTags;

	public TelegrafWriter(ImmutableList<String> typeNames, String bucketType, ImmutableMap<String, String> tags, ImmutableSet<ResultAttribute> resultAttributesToWriteAsTags) {
		this.typeNames = typeNames;
		this.bucketType = bucketType;
		this.tags = tags;
		this.resultAttributesToWriteAsTags = resultAttributesToWriteAsTags;
	}

	@Override
	public void write(@Nonnull Writer writer, @Nonnull Server server, @Nonnull Query query, @Nonnull Iterable<Result> results) throws IOException {

		for (Result result : results) {
			for (Map.Entry<String, Object> values : result.getValues().entrySet()) {

				String attributeName = values.getKey();
				Object value = values.getValue();
				if (isNotValidValue(value)) {
					log.debug("Skipping message key[{}] with value: {}.", attributeName, value);
					continue;
				}

				List<String> tagList = new ArrayList<>();
				tagList.add(",jmxport=" + server.getPort());
				tagList.add("attribute=" + attributeName);
				for (Map.Entry e : tags.entrySet()) {
					tagList.add(e.getKey() + "=" + e.getValue());
				}

				StringBuilder sb = new StringBuilder(result.getKeyAlias())
					.append(StringUtils.join(tagList, ","))
					.append(":").append(computeActualValue(value))
					.append("|").append(bucketType).append("\n");

				//.append(result.getEpoch())

				writer.write(sb.toString());
			}
		}
	}

	private boolean isNotValidValue(Object value) {
		return !(isNumeric(value) /*|| stringsValuesAsKey*/);
	}

	private String computeActualValue(Object value) {
		Object transformedValue = valueTransformer.apply(value);
		if (isNumeric(transformedValue)) {
			return transformedValue.toString();
		}
		// TODO: Revisit this, cant' just append ".string:1" with tags
		// Will have to precalculate it when adding the measurement, is it necessary?
		return "0";
	}

}
