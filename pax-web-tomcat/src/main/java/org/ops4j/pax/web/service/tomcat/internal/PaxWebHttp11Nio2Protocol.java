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
package org.ops4j.pax.web.service.tomcat.internal;

import java.util.concurrent.TimeUnit;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.CoyoteAdapter;
import org.apache.coyote.Adapter;
import org.apache.coyote.http11.AbstractHttp11JsseProtocol;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.net.Nio2Channel;
import org.apache.tomcat.util.net.Nio2Endpoint;
import org.apache.tomcat.util.threads.TaskQueue;
import org.apache.tomcat.util.threads.TaskThreadFactory;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

/**
 * Almost like {@link org.apache.coyote.http11.Http11Nio2Protocol}, but with a slightly changed {@link Nio2Endpoint}.
 */
public class PaxWebHttp11Nio2Protocol extends AbstractHttp11JsseProtocol<Nio2Channel> {

	private static final Log LOG = LogFactory.getLog(PaxWebHttp11Nio2Protocol.class);

	private String name;

	/** {@link org.apache.catalina.connector.Connector} needed to recreate {@link CoyoteAdapter}. */
	private Connector connector;

	public PaxWebHttp11Nio2Protocol() {
		super(new Nio2Endpoint() {
			@Override
			public void createExecutor() {
				TaskQueue taskqueue = new TaskQueue();
				TaskThreadFactory tf = new PaxWebTaskThreadFactory(getName() + "-exec-", getDaemon(), getThreadPriority());
				ThreadPoolExecutor executor = new ThreadPoolExecutor(getMinSpareThreads(), getMaxThreads(), 60, TimeUnit.SECONDS, taskqueue, tf);
				setExecutor(executor);
				internalExecutor = true;
				taskqueue.setParent(executor);
			}
		});
	}

	@Override
	protected Log getLog() {
		return LOG;
	}

	@Override
	protected String getNamePrefix() {
		if (isSSLEnabled()) {
			return "https-" + getSslImplementationShortName() + "-nio2";
		} else {
			return "http-nio2";
		}
	}

	public String getPaxWebConnectorName() {
		return name;
	}

	public void setPaxWebConnectorName(String name) {
		this.name = name;
	}

	@Override
	public void setAdapter(Adapter adapter) {
		// knowing that adapter is set after the adapter, we can create better adapter and skip the passed one
		super.setAdapter(new PaxWebCoyoteAdapter(connector));
	}

	public void setConnector(Connector connector) {
		this.connector = connector;
	}

}
