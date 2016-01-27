package org.ops4j.pax.web.service.webapp.bridge;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

        BridgePathRequestDispatcher bridgePathRequestDispatcher = new BridgePathRequestDispatcher(newRequestURI, req.getQueryString(), bridgeServer);
        final StringHttpServletResponseWrapper stringResponse = new StringHttpServletResponseWrapper(res);
        bridgePathRequestDispatcher.service(req, stringResponse, "REQUEST");
        rewriteLinksForProxying(res, proxyContextPath, proxyServletPath, stringResponse);

    }

    private void rewriteLinksForProxying(HttpServletResponse res, String proxyContextPath, String proxyServletPath, StringHttpServletResponseWrapper stringResponse) throws IOException {
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
