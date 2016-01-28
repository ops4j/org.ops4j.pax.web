package org.ops4j.pax.web.service.webapp.bridge.internal;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by loom on 15.01.16.
 */
public class StringHttpServletResponseWrapper extends HttpServletResponseWrapper {

    StringWriter stringWriter = null;
    PrintWriter stringPrintWriter = null;

    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @param response
     * @throws IllegalArgumentException if the response is null
     */
    public StringHttpServletResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (stringPrintWriter == null) {
            stringWriter = new StringWriter();
            stringPrintWriter = new PrintWriter(stringWriter);
        }
        return stringPrintWriter;
    }

    /**
     * Returns null if getWriter was never called (for example if getOutputStream was used instead)
     * @return
     */
    public StringWriter getStringWriter() {
        return stringWriter;
    }

    @Override
    public void resetBuffer() {
        super.resetBuffer();
        if (stringWriter != null) {
            stringWriter.getBuffer().setLength(0);
        }
    }

    @Override
    public void reset() {
        super.reset();
        if (stringWriter != null) {
            stringWriter.getBuffer().setLength(0);
        }
    }
}
