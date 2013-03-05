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
package org.ops4j.pax.web.extender.war.internal.model;

import org.ops4j.lang.NullArgumentException;

/**
 * Models an error page element in web.xml.
 * 
 * @author Alin Dreghiciu
 * @since 0.3.0, January 12, 2007
 */
public class WebAppErrorPage {

	/**
	 * Error code.
	 */
	private String m_errorCode;
	/**
	 * Error type.
	 */
	private String m_exceptionType;
	/**
	 * Location.
	 */
	private String m_location;

	/**
	 * Getter.
	 * 
	 * @return error code
	 */
	public String getErrorCode() {
		return m_errorCode;
	}

	/**
	 * Setter.
	 * 
	 * @param errorCode
	 *            value to set
	 */
	public void setErrorCode(String errorCode) {
		if (errorCode != null) {
			NullArgumentException.validateNotEmpty(errorCode, "Error code");
		}
		m_errorCode = errorCode;
	}

	/**
	 * Getter.
	 * 
	 * @return exception type
	 */
	public String getExceptionType() {
		return m_exceptionType;
	}

	/**
	 * Setter.
	 * 
	 * @param exceptionType
	 *            value to set
	 */
	public void setExceptionType(String exceptionType) {
		if (exceptionType != null) {
			NullArgumentException.validateNotEmpty(exceptionType, "Error code");
		}
		m_exceptionType = exceptionType;
	}

	/**
	 * Getter.
	 * 
	 * @return location
	 */
	public String getLocation() {
		return m_location;
	}

	/**
	 * Setter.
	 * 
	 * @param location
	 *            value to set. Cannot be null.
	 * 
	 * @throws org.ops4j.lang.NullArgumentException
	 *             if location is null or empty
	 */
	public void setLocation(String location) {
		NullArgumentException.validateNotEmpty(location, "Location");
		m_location = location;
	}

	/**
	 * Returns the exception type or error code dependeing on which one is set.
	 * Exception type has priority.
	 * 
	 * @return error
	 */
	public String getError() {
		if (m_exceptionType != null) {
			return m_exceptionType;
		}
		return m_errorCode;
	}

	@Override
	public String toString() {
		return new StringBuffer().append(this.getClass().getSimpleName())
				.append("{").append("errorCode=").append(m_errorCode)
				.append(",exceptionType=").append(m_exceptionType)
				.append(",location=").append(m_location).append("}").toString();
	}

}