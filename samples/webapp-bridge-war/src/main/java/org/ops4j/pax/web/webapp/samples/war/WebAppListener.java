package org.ops4j.pax.web.webapp.samples.war;

import org.apache.karaf.main.Main;

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
            System.err.println("contextInitialized");
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
        } catch (Exception e) {
            main = null;
            e.printStackTrace();
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        try {
            System.err.println("contextDestroyed");
            if (main != null) {
                main.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}