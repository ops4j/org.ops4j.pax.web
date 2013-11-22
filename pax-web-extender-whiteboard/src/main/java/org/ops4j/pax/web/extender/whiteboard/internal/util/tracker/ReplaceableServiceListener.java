/**
 *
 */
package org.ops4j.pax.web.extender.whiteboard.internal.util.tracker;

/**
 * Listener for events related to replaceable service.
 */
public interface ReplaceableServiceListener<T> {

    /**
     * Called when the backing service gets changed.
     *
     * @param oldService old service or null if there was no service
     * @param newService new service or null if there is no new service
     */
    void serviceChanged(T oldService, T newService);

}
