package org.ops4j.pax.web.resources.api;

import java.net.URL;
import java.time.LocalDateTime;

public class ResourceInfo {
	private URL url;
	private LocalDateTime lastModified;
	private long bundleId;
	
	public ResourceInfo(URL url, LocalDateTime lastModified, long bundleId) {
		this.url = url;
		this.lastModified = lastModified;
		this.bundleId = bundleId;
	}

	public URL getUrl() {
		return url;
	}

	public LocalDateTime getLastModified() {
		return lastModified;
	}
	
	public long getBundleId() {
		return bundleId;
	}
}