package org.ops4j.pax.web.extender.war.internal.model;

public class WebAppCookieConfig {

	private String domain;
	private Boolean httpOnly;
	private Integer maxAge;
	private String name;
	private String path;
	private Boolean secure;
	
	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	public void setHttpOnly(Boolean httpOnly) {
		this.httpOnly = httpOnly;
	}
	
	public void setMaxAge(Integer maxAge) {
		this.maxAge = maxAge;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public void setSecure(Boolean secure) {
		this.secure = secure;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public Boolean getHttpOnly() {
		return httpOnly;
	}
	
	public Integer getMaxAge() {
		return maxAge;
	}
	
	public String getName() {
		return name;
	}
	
	public String getPath() {
		return path;
	}
	
	public Boolean getSecure() {
		return secure;
	}
	
}
