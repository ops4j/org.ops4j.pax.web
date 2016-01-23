package org.ops4j.pax.web.webapp.samples.war;

import org.apache.karaf.main.Main;
import org.osgi.framework.BundleContext;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.File;

/**
 * A simple WebListener that will start Karaf.
 */
@WebListener
public class WebAppListener implements ServletContextListener {

    private Main main;

    public void contextInitialized(ServletContextEvent sce) {
        try {
            System.err.println("Apache Karaf WebAppListener.contextInitialized");
            String root = new File(sce.getServletContext().getRealPath("/WEB-INF/karaf")).getAbsolutePath();
            System.err.println("Root: " + root);
            System.setProperty("karaf.home", root);
            System.setProperty("karaf.base", root);
            System.setProperty("karaf.data", root + "/data");
            System.setProperty("karaf.etc", root + "/etc");
            System.setProperty("karaf.history", root + "/data/history.txt");
            System.setProperty("karaf.instances", root + "/instances");
            System.setProperty("karaf.startLocalConsole", "false");
            System.setProperty("karaf.startRemoteShell", "true");
            System.setProperty("karaf.lock", "false");
            main = new Main(new String[0]);
            main.launch();
            sce.getServletContext().setAttribute(BundleContext.class.getName(), main.getFramework().getBundleContext());
            System.err.println("Apache Karaf BundleContext now available in ServletContext attribute");
        } catch (Exception e) {
            main = null;
            e.printStackTrace();
        }
    }

    @Produces
    public Main getMain() {
        return main;
    }

    public void contextDestroyed(ServletContextEvent sce) {
        try {
            System.err.println("Apache Karaf WebAppListener.contextDestroyed");
            if (main != null) {
                main.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}