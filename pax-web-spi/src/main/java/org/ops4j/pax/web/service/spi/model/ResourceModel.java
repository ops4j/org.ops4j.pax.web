/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.spi.model;

import javax.servlet.Servlet;

import org.ops4j.lang.NullArgumentException;

public class ResourceModel extends ServletModel {

	private String name;

	public ResourceModel(final ContextModel contextModel,
						 final Servlet servlet, final String alias, final String name) {
		super(contextModel, servlet, alias, null, null, null);
		NullArgumentException.validateNotNull(name, "Name");
		if (!"/".equals(name) && name.endsWith("/")) {
			throw new IllegalArgumentException("name ends with slash (/)");
		}
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * There should be one resource model for given name/alias registered in web context
	 * @param o
	 * @return whether the resource models are equal
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ResourceModel that = (ResourceModel) o;

		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (getAlias() != null ? !getAlias().equals(that.getAlias()) : that.getAlias() != null) return false;

		return true;
	}

}
