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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WebAppSecurityConstraint implements Cloneable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5891881031735140829L;

	private boolean authenticate;

	private final List<String> roles;

	private String dataConstraint;

	public WebAppSecurityConstraint() {
		roles = new ArrayList<String>();
	}

	public void setAuthenticate(boolean authenticate) {
		this.authenticate = authenticate;
	}

	public boolean getAuthenticate() {
		return authenticate;
	}

	public void addRole(String role) {
		roles.add(role);
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setDataConstraint(String constraint) {
		this.dataConstraint = constraint;
	}

	public String getDataConstraint() {
		return dataConstraint;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public String toString() {
		return new StringBuffer().append(this.getClass().getSimpleName())
				.append("{").append("authenticate=").append(authenticate)
				.append("dataConstraint=").append(dataConstraint).append("}")
				.toString();
	}
}
