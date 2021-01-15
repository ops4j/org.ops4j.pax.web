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

//import org.ops4j.lang.NullArgumentException;

/**
 * Models mime mapping element in web.xml.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 01, 2008
 */
public class WebAppMimeMapping {

	/**
	 * Extension.
	 */
	private String extension;
	/**
	 * Mime type.
	 */
	private String mimeType;

	/**
	 * Getter.
	 *
	 * @return extension
	 */
	public String getExtension() {
		return extension;
	}

	/**
	 * Setter.
	 *
	 * @param extension value to set. Cannot be null.
	 * @throws NullArgumentException if extension is null
	 */
	public void setExtension(final String extension) {
//		NullArgumentException.validateNotNull(extension, "Extension");
		this.extension = extension;
	}

	/**
	 * Getter.
	 *
	 * @return mime type
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Setter.
	 *
	 * @param mimeType value to set. Cannot be null.
	 * @throws NullArgumentException if mime type is null
	 */
	public void setMimeType(final String mimeType) {
//		NullArgumentException.validateNotNull(mimeType, "Mime type");
		this.mimeType = mimeType;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("extension=").append(extension)
				.append(",mimeType=").append(mimeType).append("}").toString();
	}

}