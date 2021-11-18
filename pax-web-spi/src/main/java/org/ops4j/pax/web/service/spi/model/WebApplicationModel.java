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
package org.ops4j.pax.web.service.spi.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpService;

/**
 * <p>A model class representing the details of a <em>Web Application</em>.</p>
 *
 * <p>Even if the most important <em>Web Application</em> is the one installed as WAB and deployed using
 * pax-web-extender-war, web applications are also crated by pax-web-extender-whiteboard and even directly using
 * {@link HttpService}.</p>
 *
 * <p>This model collects <em>all</em> information regardless of the web application deployment mechanism.</p>
 */
public class WebApplicationModel {

	private String contextPath;
	private Bundle bundle;
	private boolean wab;
	private String deploymentState;
	private final List<String> servletContainerInitializers = new ArrayList<>();
	private final List<URL> metaInfResources = new ArrayList<>();
	private final List<URL> descriptors = new ArrayList<>();
	private final List<URL> wabClassPath = new ArrayList<>();
	private final List<Bundle> containerFragmentBundles = new ArrayList<>();
	private final List<Bundle> applicationFragmentBundles = new ArrayList<>();

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

	public List<Bundle> getContainerFragmentBundles() {
		return containerFragmentBundles;
	}

	public List<Bundle> getApplicationFragmentBundles() {
		return applicationFragmentBundles;
	}

}
