/*
 * Copyright 2007 Alin Dreghiciu.
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

//import org.ops4j.lang.NullArgumentException;

/**
 * Models init param element in web.xml.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
public class WebAppInitParam {

	/**
	 * Param name.
	 */
	private String paramName;
	/**
	 * Param value.
	 */
	private String paramValue;

	/**
	 * Getter.
	 *
	 * @return param name
	 */
	public String getParamName() {
		return paramName;
	}

	/**
	 * Setter.
	 *
	 * @param paramName value to set. Cannot be null
	 * @throws NullArgumentException if param name is null
	 */
	public void setParamName(final String paramName) {
//		NullArgumentException.validateNotNull(paramName, "Param name");
		this.paramName = paramName;
	}

	/**
	 * Getter.
	 *
	 * @return param value
	 */
	public String getParamValue() {
		return paramValue;
	}

	/**
	 * Setter.
	 *
	 * @param paramValue value to set.Cannot be null
	 * @throws NullArgumentException if param value is null
	 */
	public void setParamValue(final String paramValue) {
//		NullArgumentException.validateNotNull(paramValue, "Param value");
		this.paramValue = paramValue;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() +
				"{" + "paramName=" + paramName +
				",paramValue=" + paramValue + "}";
	}

}