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

	
}
