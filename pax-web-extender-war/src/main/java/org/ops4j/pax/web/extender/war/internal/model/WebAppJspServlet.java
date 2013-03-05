package org.ops4j.pax.web.extender.war.internal.model;

public class WebAppJspServlet extends WebAppServlet {

	private String jspFile;

	public void setJspPath(String jspFile) {
		this.jspFile = jspFile;
	}

	public String getJspPath() {
		return jspFile;
	}
}
