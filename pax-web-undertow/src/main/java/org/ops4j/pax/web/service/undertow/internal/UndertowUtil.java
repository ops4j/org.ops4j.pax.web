package org.ops4j.pax.web.service.undertow.internal;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import io.undertow.Undertow;

public class UndertowUtil {
    
    private static final Logger LOG = LoggerFactory.getLogger(UndertowUtil.class);

    public static XnioWorker createWorker(ClassLoader loader) {
        try {
            if (loader == null) {
                loader = Undertow.class.getClassLoader();
            }
            Xnio xnio = Xnio.getInstance(loader);
            return xnio.createWorker(OptionMap.builder().set(Options.THREAD_DAEMON, true).getMap());
        } catch (IOException ignore) {
            LOG.warn("Xnio Worker failed to be created!", ignore);
            return null;
        }
    }

}
