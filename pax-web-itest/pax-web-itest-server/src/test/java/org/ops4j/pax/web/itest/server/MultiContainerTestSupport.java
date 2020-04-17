/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.server;

import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiContainerTestSupport {

	public static Logger LOG = LoggerFactory.getLogger(ServerControllerScopesTest.class);

	protected int port;

	@Parameterized.Parameter
	public Runtime runtime;

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ Runtime.JETTY },
				{ Runtime.TOMCAT },
				{ Runtime.UNDERTOW }
		});
	}

	@Before
	public void init() throws Exception {
		ServerSocket serverSocket = new ServerSocket(0);
		port = serverSocket.getLocalPort();
		serverSocket.close();
	}

}
