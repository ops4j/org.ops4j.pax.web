/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ops4j.pax.web.resources.jsf.internal;

/**
 * Represents a mapping entry of the FacesServlet in the web.xml
 * configuration file.
 * <p>
 * DISCLAIMER: this code has been taken from MyFaces and was slightly modified
 */
public final class FacesServletMapping {

	/**
	 * Identifies the FacesServlet mapping in the current request map.
	 */
	static final String CACHED_SERVLET_MAPPING = FacesServletMapping.class.getName() + ".CACHED_SERVLET_MAPPING";

	/**
	 * The path ("/faces", for example) which has been specified in the
	 * url-pattern of the FacesServlet mapping.
	 */
	private String prefix;

	/**
	 * The extension (".jsf", for example) which has been specified in the
	 * url-pattern of the FacesServlet mapping.
	 */
	private String extension;

	/**
	 * Creates a new FacesServletMapping object using prefix mapping.
	 *
	 * @param path The path ("/faces", for example) which has been specified
	 *             in the url-pattern of the FacesServlet mapping.
	 * @return a newly created FacesServletMapping
	 */
	private static FacesServletMapping createPrefixMapping(String path) {
		FacesServletMapping mapping = new FacesServletMapping();
		mapping.setPrefix(path);
		return mapping;
	}

	/**
	 * Creates a new FacesServletMapping object using extension mapping.
	 *
	 * @param extension The extension (".xhtml", for example) which has been
	 *                  specified in the url-pattern of the FacesServlet mapping.
	 * @return a newly created FacesServletMapping
	 */
	private static FacesServletMapping createExtensionMapping(
			String extension) {
		FacesServletMapping mapping = new FacesServletMapping();
		mapping.setExtension(extension);
		return mapping;
	}

	/**
	 * Returns the path ("/faces", for example) which has been specified in
	 * the url-pattern of the FacesServlet mapping. If this mapping is based
	 * on an extension, <code>null</code> will be returned. Note that this
	 * path is not the same as the specified url-pattern as the trailing
	 * "/*" is omitted.
	 *
	 * @return the path which has been specified in the url-pattern
	 */
	private String getPrefix() {
		return prefix;
	}

	/**
	 * Sets the path ("/faces/", for example) which has been specified in
	 * the url-pattern.
	 *
	 * @param path The path which has been specified in the url-pattern
	 */
	private void setPrefix(String path) {
		this.prefix = path;
	}

	/**
	 * Returns the extension (".jsf", for example) which has been specified
	 * in the url-pattern of the FacesServlet mapping. If this mapping is
	 * not based on an extension, <code>null</code> will be returned.
	 *
	 * @return the extension which has been specified in the url-pattern
	 */
	private String getExtension() {
		return extension;
	}

	/**
	 * Sets the extension (".jsf", for example) which has been specified in
	 * the url-pattern of the FacesServlet mapping.
	 *
	 * @param extension The extension which has been specified in the url-pattern
	 */
	private void setExtension(String extension) {
		this.extension = extension;
	}

	/**
	 * Indicates whether this mapping is based on an extension (e.g.
	 * "*.xhtml").
	 *
	 * @return <code>true</code>, if this mapping is based is on an
	 * extension, <code>false</code> otherwise
	 */
	public boolean isExtensionMapping() {
		return extension != null;
	}

	public String getMapping() {
		if (isExtensionMapping()) {
			return getExtension();
		} else {
			return getPrefix();
		}
	}

	/**
	 * Determines the mapping of the FacesServlet in the web.xml configuration
	 * file. However, there is no need to actually parse this configuration file
	 * as runtime information is sufficient.
	 *
	 * @param servletPath The servletPath of the current request
	 * @param pathInfo    The pathInfo of the current request
	 * @return the mapping of the FacesServlet in the web.xml configuration file
	 */
	static FacesServletMapping calculateFacesServletMapping(
			String servletPath, String pathInfo) {
		if (pathInfo != null) {
			// If there is a "extra path", it's definitely no extension mapping.
			// Now we just have to determine the path which has been specified
			// in the url-pattern, but that's easy as it's the same as the
			// current servletPath. It doesn't even matter if "/*" has been used
			// as in this case the servletPath is just an empty string according
			// to the Servlet Specification (SRV 4.4).
			return FacesServletMapping.createPrefixMapping(servletPath);
		} else {
			// In the case of extension mapping, no "extra path" is available.
			// Still it's possible that prefix-based mapping has been used.
			// Actually, if there was an exact match no "extra path"
			// is available (e.g. if the url-pattern is "/faces/*"
			// and the request-uri is "/context/faces").
			int slashPos = servletPath.lastIndexOf('/');
			int extensionPos = servletPath.lastIndexOf('.');
			if (extensionPos > -1 && extensionPos > slashPos) {
				String extension = servletPath.substring(extensionPos);
				return FacesServletMapping.createExtensionMapping(extension);
			} else {
				// There is no extension in the given servletPath and therefore
				// we assume that it's an exact match using prefix-based mapping.
				return FacesServletMapping.createPrefixMapping(servletPath);
			}
		}
	}
}
