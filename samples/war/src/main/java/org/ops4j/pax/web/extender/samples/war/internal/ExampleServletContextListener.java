package org.ops4j.pax.web.extender.samples.war.internal;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ExampleServletContextListener implements ServletContextListener {

	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		System.out.println("Context Initialized with event: " +sce);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		System.out.println("Context destroyed with event: "+sce);
	}

}
