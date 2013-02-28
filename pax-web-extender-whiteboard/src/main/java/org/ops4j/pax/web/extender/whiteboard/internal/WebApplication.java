/*
 * Copyright 2007 Damian Golda.
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.element.WebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.util.DictionaryUtils;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.WebContainerConstants;

/**
 * TODO Add JavaDoc
 * 
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class WebApplication implements HttpServiceListener {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(WebApplication.class);

	/**
	 * List of web elements that makes up this context.
	 */
	private final List<WebElement> m_webElements;
	/**
	 * Registerers lock.
	 */
	private final ReadWriteLock m_webElementsLock;
	/**
	 * Http service lock.
	 */
	private final ReadWriteLock m_httpServiceLock;

	/**
	 * Current http context mapping.
	 */
	private HttpContextMapping m_httpContextMapping;
	/**
	 * Active http service;
	 */
	private HttpService m_httpService;
	/**
	 * Provided or created http context.
	 */
	private HttpContext m_httpContext;

	/**
	 * Constructor.
	 */
	public WebApplication() {
		m_webElements = new ArrayList<WebElement>();
		m_httpServiceLock = new ReentrantReadWriteLock();
		m_webElementsLock = new ReentrantReadWriteLock();
	}

	public void addWebElement(final WebElement webElement) {
		NullArgumentException.validateNotNull(webElement, "Registerer");
		//FIX for PAXWEB-485 changing order of registration. 
		m_httpServiceLock.readLock().lock();
		try {
			registerWebElement(webElement);
		} finally {
			m_httpServiceLock.readLock().unlock();
		}
		m_webElementsLock.writeLock().lock();
		try {
			m_webElements.add(webElement);
		} finally {
			m_webElementsLock.writeLock().unlock();
		}
	}

	public void removeWebElement(final WebElement webElement) {
		NullArgumentException.validateNotNull(webElement, "Registerer");
		m_webElementsLock.writeLock().lock();
		try {
			m_webElements.remove(webElement);
		} finally {
			m_webElementsLock.writeLock().unlock();
		}
		m_httpServiceLock.readLock().lock();
		try {
			unregisterWebElement(webElement);
		} finally {
			m_httpServiceLock.readLock().unlock();
		}
	}

	public void available(final HttpService httpService) throws Exception {
		NullArgumentException.validateNotNull(httpService, "Http service");
		m_httpServiceLock.writeLock().lock();
		try {
			if (m_httpService != null) {
				unavailable(m_httpService);
			}
			m_httpService = httpService;
			registerHttpContext();
		} finally {
			m_httpServiceLock.writeLock().unlock();
		}
	}

	public boolean hasHttpContextMapping() {
		return m_httpContextMapping != null;
	}

	public void setHttpContextMapping(
			final HttpContextMapping httpContextMapping) {
		m_httpServiceLock.writeLock().lock();
		try {
			if (hasHttpContextMapping()) {
				unregisterHttpContext();
			}
			m_httpContextMapping = httpContextMapping;
			registerHttpContext();
		} finally {
			m_httpServiceLock.writeLock().unlock();
		}
	}

	private void unregisterHttpContext() {
		if (m_httpContext != null) {
			unregisterWebElements();
			m_httpContext = null;
		}
	}

	private void registerHttpContext() {
		if (m_httpContextMapping != null && m_httpService != null) {
			m_httpContext = m_httpContextMapping.getHttpContext();
			if (m_httpContext == null) {
				m_httpContext = m_httpService.createDefaultHttpContext();
			}
			if (WebContainerUtils.isWebContainer(m_httpService)) {
				final Map<String, String> contextparams = new HashMap<String, String>();
				if (m_httpContextMapping.getPath() != null) {
					contextparams.put(WebContainerConstants.CONTEXT_NAME,
							m_httpContextMapping.getPath());
				}
				if (m_httpContextMapping.getParameters() != null) {
					contextparams.putAll(m_httpContextMapping.getParameters());
				}
				((WebContainer) m_httpService).setContextParam(
						DictionaryUtils.adapt(contextparams), m_httpContext);
			}
			registerWebElements();
		}
	}

	private void registerWebElements() {
		m_webElementsLock.readLock().lock();
		try {
			if (m_httpService != null && m_httpContext != null) {
				for (WebElement registerer : m_webElements) {
					registerWebElement(registerer);
				}
			}
		} finally {
			m_webElementsLock.readLock().unlock();
		}
	}

	private void registerWebElement(final WebElement registerer) {
		try {
			if (m_httpService != null && m_httpContext != null) {
				registerer.register(m_httpService, m_httpContext);
			}
		} catch (Exception ignore) {
			LOG.error("Registration skipped for [" + registerer
					+ "] due to error during registration", ignore);
		}
	}

	public void unavailable(final HttpService httpService) {
		NullArgumentException.validateNotNull(httpService, "Http service");
		m_httpServiceLock.writeLock().lock();
		try {
			if (httpService != m_httpService) {
				throw new IllegalStateException("Unavailable http service ["
						+ httpService
						+ "] is not equal with prior available http service ["
						+ m_httpService + "]");
			}
			unregisterWebElements();
			m_httpService = null;
			m_httpContext = null;
		} finally {
			m_httpServiceLock.writeLock().unlock();
		}
	}

	private void unregisterWebElements() {
		m_webElementsLock.readLock().lock();
		try {
			if (m_httpService != null && m_httpContext != null) {
				for (WebElement registerer : m_webElements) {
					unregisterWebElement(registerer);
				}
			}
		} finally {
			m_webElementsLock.readLock().unlock();
		}
	}

	private void unregisterWebElement(final WebElement registerer) {
		if (m_httpService != null && m_httpContext != null) {
			registerer.unregister(m_httpService, m_httpContext);
		}
	}

	@Override
	public String toString() {
		return new StringBuffer().append(this.getClass().getSimpleName())
				.append("{").append("mapping=").append(m_httpContextMapping)
				.append("}").toString();
	}

}
