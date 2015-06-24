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
