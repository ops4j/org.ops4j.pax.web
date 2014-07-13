package org.ops4j.pax.web.itest.asset;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;


public class BundleResourceHttpContext implements HttpContext {
    
    
    private Bundle bundle;

    public BundleResourceHttpContext(Bundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        return true;
    }

    @Override
    public URL getResource(String name) {
        return bundle.getEntry(name);
    }

    @Override
    public String getMimeType(String name) {
        return null;
    }

}
