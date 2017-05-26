/* Copyright 2010 Achim Nierbeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.war.internal.model;

public class WebAppConstraintMapping {

	private String constraintName;
	private String mapping;
	private String url;
	private WebAppSecurityConstraint securityConstraints;

	/**
	 * @param httpMapping
	 */
	public void setMapping(String httpMapping) {
		this.mapping = httpMapping;
	}

	/**
	 * @return mapping
	 */
	public String getMapping() {
		return mapping;
	}

	/**
	 * @param constraintName the constraintName to set
	 */
	public void setConstraintName(String constraintName) {
		this.constraintName = constraintName;
	}

	/**
	 * @return the constraintName
	 */
	public String getConstraintName() {
		return constraintName;
	}

	/**
	 * @param url
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param sc
	 */
	public void setSecurityConstraints(WebAppSecurityConstraint sc) {
		this.securityConstraints = sc;
	}

	public WebAppSecurityConstraint getSecurityConstraint() {
		return securityConstraints;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("constraintName=").append(constraintName)
				.append(",url=").append(url).append(",mapping=")
				.append(mapping).append("}").toString();
	}

}
