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
package com.googlecode.jmxtrans.model;

public final class ServerFixtures {

	public static final String DEFAULT_HOST = "host.example.net";
	public static final String DEFAULT_PORT = "4321";

	private ServerFixtures() {}

	public static Server createServerWithOneQuery(String host, String port, String queryObject) {
		return getBuilder(host, port, queryObject).build();
	}

	private static Server.Builder getBuilder(String host, String port, String queryObject) {
		return Server.builder()
				.setHost(host)
				.setPort(port)
				.addQuery(Query.builder()
					.setObj(queryObject)
					.build());
	}

	public static Server serverWithNoQuery() {
		return Server.builder()
				.setHost(DEFAULT_HOST)
				.setPort(DEFAULT_PORT)
				.build();
	}

	public static Server dummyServer() {
		return createServerWithOneQuery(DEFAULT_HOST, DEFAULT_PORT, "myQuery:key=val");
	}

	public static Server.Builder dummyServerBuilder() {
		return getBuilder(DEFAULT_HOST, DEFAULT_PORT, "myQuery:key=val");
	}

	public static Server localServer() {
		return Server.builder()
				.setHost("host.example.net")
				.setPort("4321")
				.setLocal(true)
				.build();
	}
}
