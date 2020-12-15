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
package org.ops4j.pax.web.service.spi.model.elements;

import java.util.List;

import org.ops4j.pax.web.annotations.Review;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.events.WebElementEventData;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;

@Review("Not refactored yet")
public class SecurityConstraintMappingModel extends ElementModel {

	private String url;
	private String mapping;
	private String constraintName;
	private List<String> roles;
	private boolean authentication;
	private String dataConstraint;

	public SecurityConstraintMappingModel(OsgiContextModel contextModel,
										  String constraintName, String mapping, String url,
										  String dataConstraint, boolean authentication, List<String> roles) {
//		super(contextModel);
		this.constraintName = constraintName;
		this.mapping = mapping;
		this.url = url;
		this.dataConstraint = dataConstraint;
		this.authentication = authentication;
		this.roles = roles;
	}

	@Override
	public void register(WhiteboardWebContainerView view) {
	}

	@Override
	public void unregister(WhiteboardWebContainerView view) {
	}

	@Override
	public WebElementEventData asEventData() {
		return null;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param mapping the mapping to set
	 */
	public void setMapping(String mapping) {
		this.mapping = mapping;
	}

	/**
	 * @return the mapping
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

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	public boolean isAuthentication() {
		return authentication;
	}

	public void setAuthentication(boolean authentication) {
		this.authentication = authentication;
	}

	public String getDataConstraint() {
		return dataConstraint;
	}

	public void setDataConstraint(String dataConstraint) {
		this.dataConstraint = dataConstraint;
	}
	@Override
	public Boolean performValidation() {
		return Boolean.TRUE;
	}

}
