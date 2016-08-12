/*
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

import java.util.ArrayList;
import java.util.List;

public class WebAppJspPropertyGroup {
	private List<String> displayNames = new ArrayList<>();
	private List<String> urlPatterns = new ArrayList<>();
	private List<String> includeCodes = new ArrayList<>();
	private List<String> includePreludes = new ArrayList<>();
	private Boolean elIgnored;
	private Boolean scriptingInvalid;
	private Boolean isXml;

	public void addDisplayName(String displayName) {
		displayNames.add(displayName);
	}

	public void addUrlPattern(String urlPattern) {
		urlPatterns.add(urlPattern);
	}

	public void addIncludeCode(String includeCode) {
		includeCodes.add(includeCode);
	}

	public void addIncludePrelude(String includePrelude) {
		includePreludes.add(includePrelude);
	}

	public void addElIgnored(boolean elIgnored) {
		this.elIgnored = elIgnored;
	}

	public void addScrptingInvalid(boolean scriptingInvalid) {
		this.scriptingInvalid = scriptingInvalid;
	}

	public void addIsXml(boolean isXml) {
		this.isXml = isXml;
	}

	public List<String> getDisplayNames() {
		return displayNames;
	}

	public void setDisplayNames(List<String> displayNames) {
		this.displayNames = displayNames;
	}

	public List<String> getUrlPatterns() {
		return urlPatterns;
	}

	public void setUrlPatterns(List<String> urlPatterns) {
		this.urlPatterns = urlPatterns;
	}

	public List<String> getIncludeCodes() {
		return includeCodes;
	}

	public void setIncludeCodes(List<String> includeCodes) {
		this.includeCodes = includeCodes;
	}

	public List<String> getIncludePreludes() {
		return includePreludes;
	}

	public void setIncludePreludes(List<String> includePreludes) {
		this.includePreludes = includePreludes;
	}

	public Boolean getElIgnored() {
		return elIgnored;
	}

	public void setElIgnored(Boolean elIgnored) {
		this.elIgnored = elIgnored;
	}

	public Boolean getScriptingInvalid() {
		return scriptingInvalid;
	}

	public void setScriptingInvalid(Boolean scriptingInvalid) {
		this.scriptingInvalid = scriptingInvalid;
	}

	public Boolean getIsXml() {
		return isXml;
	}

	public void setIsXml(Boolean isXml) {
		this.isXml = isXml;
	}

}
