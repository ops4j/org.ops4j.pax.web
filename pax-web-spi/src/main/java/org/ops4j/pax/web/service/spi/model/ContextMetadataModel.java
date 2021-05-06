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

/**
 * Some meta information about the "context", attached to {@link OsgiContextModel}, but only in the target runtime.
 */
public class ContextMetadataModel {

	private String publicId;
	private int majorVersion;
	private int minorVersion;
	private boolean metadataComplete;
	private boolean distributable;
	private String displayName;
	private String requestCharacterEncoding;
	private String responseCharacterEncoding;
	private boolean denyUncoveredHttpMethods;

	public void setPublicId(String publicId) {
		this.publicId = publicId;
	}

	public String getPublicId() {
		return publicId;
	}

	public void setMajorVersion(int majorVersion) {
		this.majorVersion = majorVersion;
	}

	public int getMajorVersion() {
		return majorVersion;
	}

	public void setMinorVersion(int minorVersion) {
		this.minorVersion = minorVersion;
	}

	public int getMinorVersion() {
		return minorVersion;
	}

	public void setMetadataComplete(boolean metadataComplete) {
		this.metadataComplete = metadataComplete;
	}

	public boolean isMetadataComplete() {
		return metadataComplete;
	}

	public void setDistributable(boolean distributable) {
		this.distributable = distributable;
	}

	public boolean getDistributable() {
		return distributable;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setRequestCharacterEncoding(String requestCharacterEncoding) {
		this.requestCharacterEncoding = requestCharacterEncoding;
	}

	public String getRequestCharacterEncoding() {
		return requestCharacterEncoding;
	}

	public void setResponseCharacterEncoding(String responseCharacterEncoding) {
		this.responseCharacterEncoding = responseCharacterEncoding;
	}

	public String getResponseCharacterEncoding() {
		return responseCharacterEncoding;
	}

	public void setDenyUncoveredHttpMethods(boolean denyUncoveredHttpMethods) {
		this.denyUncoveredHttpMethods = denyUncoveredHttpMethods;
	}

	public boolean isDenyUncoveredHttpMethods() {
		return denyUncoveredHttpMethods;
	}

}
