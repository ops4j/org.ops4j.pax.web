package org.ops4j.pax.web.service.jetty.internal;

public class ConfigureRequestLogParameter {
	//CHECKSTYLE:OFF
	public String format;
	public String retainDays;
	public Boolean append;
	public Boolean extend;
	public Boolean dispatch;
	public String timeZone;
	public String dir;
	public Boolean logLatency;
	public Boolean logCookies;
	public Boolean logServer;
	//CHECKSTYLE:ON

	public ConfigureRequestLogParameter(String format, String retainDays,
			Boolean append, Boolean extend, Boolean dispatch, String timeZone,
			String dir, Boolean logLatency, Boolean logCookies,
			Boolean logServer) {
		this.format = format;
		this.retainDays = retainDays;
		this.append = append;
		this.extend = extend;
		this.dispatch = dispatch;
		this.timeZone = timeZone;
		this.dir = dir;
		this.logLatency = logLatency;
		this.logCookies = logCookies;
		this.logServer = logServer;
	}
}