package org.ops4j.pax.web.jsf.resourcehandler.internal;

import javax.faces.application.Resource;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.osgi.framework.Bundle;

public class OsgiResourceMatcher extends TypeSafeMatcher<Resource> {

	private long bundleId;
	private String resourcepath;
	
	public OsgiResourceMatcher(Bundle bundle , String resourcepath){
		if(bundle == null || resourcepath == null){
			throw new IllegalArgumentException("OsgiResourceMatcher: all values must be set!");
		}
		this.bundleId = bundle.getBundleId();
		if(resourcepath.charAt(0) == '/'){
			this.resourcepath = resourcepath.substring(1);
		}else{
			this.resourcepath = resourcepath;
		}
	}
	
	@Override
	public void describeTo(Description description) {
		description
			.appendText("expected result from getUrl(): ")
			.appendValue("file://" + bundleId + ".0:0/META-INF/resources/" + resourcepath);
	}
	
	@Override
	protected void describeMismatchSafely(Resource item, Description mismatchDescription) {
		mismatchDescription
			.appendText("was ")
			.appendValue(item.getURL());
	}

	@Override
	protected boolean matchesSafely(Resource item) {
		return item.getURL().toString().equals("file://" + bundleId + ".0:0/META-INF/resources/" + resourcepath);
	}
	
	public static OsgiResourceMatcher isBundleResource(Bundle bundle , String resourcepath){
		return new OsgiResourceMatcher(bundle, resourcepath);
	}
}


