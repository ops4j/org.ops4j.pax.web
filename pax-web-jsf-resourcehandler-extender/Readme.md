# JSF-ResourceHandler-Extender

## Summary

This module enables Servlet 3.0 Resources for JSF-OSGi-Bundles. 

## Configuration

### Add resources

According to the Servlet specification, resources outside a webmodule must be located unter `META-INF/resources`. This folder will be the root with regards to the resource-path.


### Enable Resource-Bundle

For the sake of performance, the extender is not scanning every bundle in your container. You have to mark a
bundle as resource-bundle by adding the header `WebResources: true` to your bundles manifest.

```
Bundle-SymbolicName: jsf-resourcehandler-resourcebundle
Include-Resource: src/main/resources/
WebResources: true
```

### Configure JSF

The implementation makes use of the standard JSF-ResourceHandler-API. Just enable the OsgiResourceHandler class in your `faces-config.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<faces-config xmlns="http://xmlns.jcp.org/xml/ns/javaee"
 		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facesconfig_2_2.xsd"
		version="2.2">
	<application>
 		<resource-handler>org.ops4j.pax.web.jsf.resourcehandler.extender.OsgiResourceHandler</resource-handler>
	</application>
</faces-config>
```

## Beware the dynamics

Since resource-bundles, like every bundle in OSGi, can become unavailable, the implementation will block read-requests when the internal index is updated. This trade-off was made because usually the reads greatly outnumber the writes.

### Example with a unavailable resource
In JSF, and other web-frameworks, there are two phases: in the first, the HTML will be created and served to the browser. Here a resource-bundle is available and JSF can retrieve the resource-URL from the ResourceHandler.

Now, the resource-bundle gets uninstalled.
 
During HTML-parsing, the browser will issue a dedicated resource-request with the given URL, but due to network-latency the resource-bundle has already gone. JSF will try to open a stream to the given URL, in order to serve the actual bytes. This will cause an IOException because the resource under the given URL has vanished.
