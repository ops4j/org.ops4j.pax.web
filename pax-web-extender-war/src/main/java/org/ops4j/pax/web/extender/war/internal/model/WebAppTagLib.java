package org.ops4j.pax.web.extender.war.internal.model;

public class WebAppTagLib {

	private String tagLibLocation;
	private String tagLibUri; 
	
	public void addTagLibLocation(String tagLibLocation) {
		this.tagLibLocation = tagLibLocation;
	}
	
	public String getTagLibLocation() {
		return tagLibLocation;
	}

	public void addTagLibUri(String tagLibUri) {
		this.tagLibUri = tagLibUri;
	}

	public String getTagLibUri() {
		return tagLibUri;
	}
	
}
