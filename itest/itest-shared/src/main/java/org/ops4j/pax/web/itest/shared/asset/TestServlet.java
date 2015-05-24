package org.ops4j.pax.web.itest.shared.asset;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by anierbeck on 24.05.15.
 */
public class TestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private boolean initCalled;

    @Override
    public void init(ServletConfig config) throws ServletException {
        this.initCalled = true;
        super.init(config);
    }

    public boolean isInitCalled() {
        return initCalled;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().println("SimpleServlet: TEST OK");
    }
}
