# Webresources

## Summary

This module enables Servlet 3.0 Resources for OSGi-Bundles. 

## Modules

* [pax-web-resources-api](pax-web-resources-api)
* [pax-web-resources-extender](pax-web-resources-extender)
* [pax-web-resources-jsf](pax-web-resources-jsf)

## Beware the dynamics

Since resource-bundles, like every bundle in OSGi, can become unavailable.

### Example with a unavailable resource
When creating a HTML page, there are two phases: in the first, the HTML will be created and served to the browser. Here a resource-bundle is available and JSF can retrieve the resource-URL from the ResourceHandler.

Now, the resource-bundle gets uninstalled.
 
During HTML-parsing, the browser will issue a dedicated resource-request with the given URL, but due to network-latency the resource-bundle has already gone. A web-framework (like JSF) will try to open a stream to the given URL, in order to serve the actual bytes. This will cause an IOException because the resource under the given URL has vanished.
