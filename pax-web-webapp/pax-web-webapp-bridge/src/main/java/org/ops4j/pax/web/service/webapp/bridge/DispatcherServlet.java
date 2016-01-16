package org.ops4j.pax.web.service.webapp.bridge;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.ops4j.pax.web.service.webapp.bridge.internal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Created by loom on 13.01.16.
 */
public class DispatcherServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(DispatcherServlet.class);
    private BridgeServer bridgeServer;

    public void setBridgeServer(BridgeServer bridgeServer) {
        this.bridgeServer = bridgeServer;
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        logger.debug("service({},{})", req, res);
        String proxyContextPath = (String) req.getAttribute("org.ops4j.pax.web.service.webapp.bridge.proxyContextPath");
        String proxyServletPath = (String) req.getAttribute("org.ops4j.pax.web.service.webapp.bridge.proxyServletPath");
        String requestURI = req.getRequestURI();
        String newRequestURI = requestURI;
        if (requestURI.startsWith(proxyContextPath + proxyServletPath)) {
            newRequestURI = requestURI.substring(proxyContextPath.length() + proxyServletPath.length());
        }
        ContextModel contextModel = bridgeServer.getServerModel().matchPathToContext(newRequestURI);
        BridgeServletContext bridgeServletContext = bridgeServer.getContextModel(contextModel.getContextName());

        final String newContextPath = contextModel.getContextName();
        BridgeServerModel.UrlPattern urlPattern = BridgeServerModel.matchPathToContext(bridgeServer.getBridgeServerModel().getServletUrlPatterns(), newRequestURI);
        if (urlPattern.getModel() instanceof ServletModel) {
            ServletModel servletModel = (ServletModel) urlPattern.getModel();
            BridgeServletModel bridgeServletModel = bridgeServletContext.findServlet(servletModel);
            if (!bridgeServletModel.isInitialized()) {
                bridgeServletModel.init();
            }
            String matchedUrlPattern = urlPattern.getUrlPattern();
            String servletPath = matchedUrlPattern.substring(newContextPath.length());
            if (servletPath.endsWith("/*")) {
                servletPath = servletPath.substring(0, servletPath.length()-2);
            }
            final String finalServletPath = servletPath;
            final String pathInfo = newRequestURI.substring(newContextPath.length() + finalServletPath.length());
            final StringHttpServletResponseWrapper stringResponse = new StringHttpServletResponseWrapper(res);
            servletModel.getServlet().service(new HttpServletRequestWrapper(req) {
                @Override
                public String getPathInfo() {
                    return pathInfo;
                }

                @Override
                public String getContextPath() {
                    return newContextPath;
                }

                @Override
                public String getServletPath() {
                    return finalServletPath;
                }

                @Override
                public String getRequestURI() {
                    return getContextPath() + getServletPath() + getPathInfo();
                }

                @Override
                public StringBuffer getRequestURL() {
                    return super.getRequestURL();
                }
            }, stringResponse);

            if (stringResponse.getStringWriter() != null) {
                Document doc = Jsoup.parse(stringResponse.getStringWriter().toString());
                Elements linkElements = doc.select("a[href]");
                rewriteUrls(proxyContextPath, proxyServletPath, linkElements, "href");
                Elements mediaElements = doc.select("[src]");
                rewriteUrls(proxyContextPath, proxyServletPath, mediaElements, "src");
                Elements importElements = doc.select("link[href]");
                rewriteUrls(proxyContextPath, proxyServletPath, importElements, "href");
                String newHtml = doc.html();
                res.getWriter().append(newHtml);
            }
        }
    }

    private void rewriteUrls(String proxyContextPath, String proxyServletPath, Elements mediaElements, String attributeName) {
        for (Element mediaElement : mediaElements) {
            String src = mediaElement.attr(attributeName);
            String proxyPart = proxyContextPath + proxyServletPath;
            if (src.startsWith("/") && !src.startsWith(proxyPart)) {
                mediaElement.attr(attributeName, proxyPart + src);
            }
        }
    }

}
