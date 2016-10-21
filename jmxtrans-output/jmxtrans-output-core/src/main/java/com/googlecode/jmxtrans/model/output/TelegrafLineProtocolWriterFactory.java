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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.ResultAttribute;
import com.googlecode.jmxtrans.model.output.support.UdpOutputWriterBuilder;
import com.googlecode.jmxtrans.model.output.support.WriterPoolOutputWriter;
import com.googlecode.jmxtrans.model.output.support.pool.FlushStrategy;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.immutableEnumSet;
import static com.googlecode.jmxtrans.model.output.support.pool.FlushStrategyUtils.createFlushStrategy;

@EqualsAndHashCode
@ToString
public class TelegrafLineProtocolWriterFactory implements OutputWriterFactory {

	private static final Logger LOG = LoggerFactory.getLogger(TelegrafLineProtocolWriterFactory.class);

	@Nonnull
	private final ImmutableList<String> typeNames;
	@Nonnull
	private final String bucketType;
	@Nonnull
	private final InetSocketAddress server;
	@Nonnull
	private final FlushStrategy flushStrategy;
	private final int poolSize;
	private final ImmutableSet<ResultAttribute> resultAttributesToWriteAsTags;
	private final ImmutableMap<String, String> tags;

	public TelegrafLineProtocolWriterFactory(
		@JsonProperty("typeNames") ImmutableList<String> typeNames,
		@JsonProperty("bucketType") String bucketType,
		@JsonProperty("host") String host,
		@JsonProperty("port") Integer port,
		@JsonProperty("tags") ImmutableMap<String, String> tags,
		@JsonProperty("resultTags") List<String> resultTags,
		@JsonProperty("flushStrategy") String flushStrategy,
		@JsonProperty("flushDelayInSeconds") Integer flushDelayInSeconds,
		@JsonProperty("poolSize") Integer poolSize) {
		this.typeNames = firstNonNull(typeNames, ImmutableList.<String>of());
		this.bucketType = firstNonNull(bucketType, "c");
		//this.stringValueDefaultCount = firstNonNull(stringValueDefaultCount, 1L);
		this.server = new InetSocketAddress(
			checkNotNull(host, "Host cannot be null."),
			checkNotNull(port, "Port cannot be null."));
		this.resultAttributesToWriteAsTags = initResultAttributesToWriteAsTags(resultTags);
		this.tags = initCustomTagsMap(tags);
		this.flushStrategy = createFlushStrategy(flushStrategy, flushDelayInSeconds);
		this.poolSize = firstNonNull(poolSize, 1);
	}

	/**
	 * Copied from InfluxDbWriterFactory
	 * @param tags
	 * @return
	 */
	private ImmutableMap<String, String> initCustomTagsMap(ImmutableMap<String, String> tags) {
		return ImmutableMap.copyOf(firstNonNull(tags, Collections.<String, String>emptyMap()));
	}

	/**
	 * Copied from InfluxDbWriterFactory
	 * @param resultTags
	 * @return
	 */
	private ImmutableSet<ResultAttribute> initResultAttributesToWriteAsTags(List<String> resultTags) {
		EnumSet<ResultAttribute> resultAttributes = EnumSet.noneOf(ResultAttribute.class);
		if (resultTags != null) {
			for (String resultTag : resultTags) {
				resultAttributes.add(ResultAttribute.fromAttribute(resultTag));
			}
		} else {
			resultAttributes = EnumSet.allOf(ResultAttribute.class);
		}

		ImmutableSet<ResultAttribute> result = immutableEnumSet(resultAttributes);
		LOG.debug("Result Tags to write set to: {}", result);
		return result;
	}

	@Override
	public WriterPoolOutputWriter<TelegrafWriter> create() {
		return UdpOutputWriterBuilder.builder(
			server,
			new TelegrafWriter(typeNames, bucketType, tags, resultAttributesToWriteAsTags))
			.setCharset(UTF_8)
			.setFlushStrategy(flushStrategy)
			.setPoolSize(poolSize)
			.build();
	}
}
