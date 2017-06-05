/*
 * Copyright 2008 Alin Dreghiciu.
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

import java.util.Objects;

import org.ops4j.lang.NullArgumentException;

/**
 * @author Alin Dreghiciu
 * @since 0.3.0, January 12, 2008
 */
public class ErrorPageModel extends Model {

	public static final String ERROR_PAGE = "org.ops4j.pax.web.error.error_page.global";

	/**
	 * Fully qualified class name of the error or an error code.
	 */
	private final String error;
	/**
	 * Request path of the error handler. Starts with a "/".
	 */
	private final String location;

	public ErrorPageModel(final ContextModel contextModel, final String error,
						  final String location) {
		super(contextModel);
		NullArgumentException.validateNotEmpty(error, "Error");
		NullArgumentException.validateNotEmpty(location, "Location");
		if (!location.startsWith("/")) {
			throw new IllegalArgumentException(
					"Location must start with a slash (/)");
		}
		this.error = error;
		this.location = location;
	}

	/**
	 * Getter.
	 *
	 * @return fully qualified class name of the error or an error code.
	 */
	public String getError() {
		return error;
	}

	/**
	 * Getter.
	 *
	 * @return the request path of error handler.
	 */
	public String getLocation() {
		return location;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ErrorPageModel that = (ErrorPageModel) o;
		return Objects.equals(error, that.error) &&
				Objects.equals(getContextModel().getContextName(), that.getContextModel().getContextName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(error, getContextModel().getContextName());
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() +
				"{" + "id=" + getId() + ",error=" +
				error + ",location=" + location +
				",context=" + getContextModel() + "}";
	}

}