package org.ops4j.pax.web.extender.war;

/**
 * Used to manage deployments of WARs discovered by
 * OPS4J Pax Web - Extender - WAR.
 *
 * @author Hiram Chirino <hiram@hiramchirino.com>
 */
public interface WarManager {

    static final int SUCCESS = 0;
    static final int WAR_NOT_FOUND = 2;
    static final int ALREADY_STARTED = 3;
    static final int ALREADY_STOPPED = 4;

    /**
     * Starts a war bundle under an optional configurable content name.
     *
     * @param bundleId The bundle id that contains the war.
     * @param contextName an optional context name to host the war under, if null
     *        it will use the context name configured in the war OSGi metadata.
     * @return {@link #SUCCESS} if the war was started, or {@link #WAR_NOT_FOUND}
     *         if the bundle is not a war bundle, or {@link #ALREADY_STARTED} if the
     *         war had already been started.
     */
    public int start(long bundleId, String contextName);

    /**
     * Stops a war bundle.
     *
     * @param bundleId The bundle id that contains the war.
     * @return
     */
    public int stop(long bundleId);

}
