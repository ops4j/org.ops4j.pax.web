/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.service.spi.model.info;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * <p>A model class representing the details of a <em>Web Application</em>.</p>
 *
 * <p>Even if the most important <em>Web Application</em> is the one installed as WAB and deployed using
 * pax-web-extender-war, web applications are also crated by pax-web-extender-whiteboard and even directly using
 * {@link HttpService}.</p>
 *
 * <p>This model collects <em>all</em> information regardless of the web application deployment mechanism.</p>
 */
public class WebApplicationInfo implements Comparable<WebApplicationInfo> {

	private String contextPath;
	private Bundle bundle;
	private boolean wab;
	private boolean whiteboard;
	private boolean httpService;
	private String deploymentState;
	private final List<String> servletContainerInitializers = new ArrayList<>();
	private final List<URL> metaInfResources = new ArrayList<>();
	private final List<URL> descriptors = new ArrayList<>();
	private final List<URL> wabClassPath = new ArrayList<>();
	private final Set<URL> wabClassPathSkipped = new HashSet<>();
	private final List<Bundle> containerFragmentBundles = new ArrayList<>();
	private final List<Bundle> applicationFragmentBundles = new ArrayList<>();
	private boolean replaced;

	private WebContextInfo contextModel;

	public WebApplicationInfo() {
	}

	public WebApplicationInfo(OsgiContextModel ocm) {
		if (ocm != null) {
			contextModel = new WebContextInfo(ocm);
			contextPath = ocm.getContextPath();
			bundle = ocm.getOwnerBundle();
			wab = ocm.isWab();
			whiteboard = ocm.isWhiteboard();
			httpService = !(wab || whiteboard);
		}
	}

	public WebApplicationInfo(OsgiContextModel ocm, boolean replaced) {
		this(ocm);
		if (replaced) {
			// this context is 1) bundle-scoped created in HttpService, 2) shadowed by Whiteboard-registered
			// instance obtained from HttpService
			this.replaced = replaced;
		}
	}

	public Bundle getBundle() {
		return bundle;
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	public boolean isWab() {
		return wab;
	}

	public void setWab(boolean wab) {
		this.wab = wab;
	}

	public boolean isWhiteboard() {
		return whiteboard;
	}

	public boolean isHttpService() {
		return httpService;
	}

	public String getDeploymentState() {
		return deploymentState;
	}

	public void setDeploymentState(String deploymentState) {
		this.deploymentState = deploymentState;
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public List<String> getServletContainerInitializers() {
		return servletContainerInitializers;
	}

	public List<URL> getMetaInfResources() {
		return metaInfResources;
	}

	public List<URL> getDescriptors() {
		return descriptors;
	}

	public List<URL> getWabClassPath() {
		return wabClassPath;
	}

	public Set<URL> getWabClassPathSkipped() {
		return wabClassPathSkipped;
	}

	public List<Bundle> getContainerFragmentBundles() {
		return containerFragmentBundles;
	}

	public List<Bundle> getApplicationFragmentBundles() {
		return applicationFragmentBundles;
	}

	public void setContextModel(WebContextInfo contextModel) {
		this.contextModel = contextModel;
	}

	public int getServiceRank() {
		return contextModel == null ? 0 : contextModel.getModel().getServiceRank();
	}

	public long getServiceId() {
		return contextModel == null ? 0L : contextModel.getModel().getServiceId();
	}

	public String getName() {
		return contextModel == null ? "-" : contextModel.getModel().getName();
	}

	public String getScope() {
		if (contextModel == null) {
			return "?";
		}
		OsgiContextModel ocm = contextModel.getModel();
		if (ocm.getContextReference() == null) {
			return "static*";
		}
		return (String) ocm.getContextReference().getProperty(Constants.SERVICE_SCOPE);
	}

	public List<String> getContextRegistrationIdProperties() {
		if (contextModel == null) {
			return Collections.emptyList();
		}
		Set<String> props = new TreeSet<>();
		contextModel.getModel().getContextRegistrationProperties().forEach((k, v) -> {
			if (PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID.equals(k)
					|| PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH.equals(k)
					|| HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY.equals(k)
					|| HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME.equals(k)
					|| HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH.equals(k)) {
				props.add(String.format("%s=%s", k, v));
			}
		});
		return new ArrayList<>(props);
	}

	public boolean isReplaced() {
		return replaced;
	}

	@Override
	public int compareTo(WebApplicationInfo other) {
		// first - by context path
		if (!contextPath.equals(other.contextPath)) {
			int s1 = contextPath.split("/").length;
			int s2 = other.contextPath.split("/").length;
			if (s1 == s2) {
				return contextPath.compareTo(other.contextPath);
			} else {
				return s1 < s2 ? -1 : 1;
			}
		}

		// then by rank/serviceId of OsgiContextModel
		if (contextModel != null && other.contextModel != null) {
			return contextModel.getModel().compareTo(other.contextModel.getModel());
		}

		return bundle.compareTo(other.bundle);
	}

}
