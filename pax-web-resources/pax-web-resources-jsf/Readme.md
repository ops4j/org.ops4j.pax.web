# Webresources-JSF-ResourceHandler

## Summary

This module enables Servlet 3.0 Resources for JSF-OSGi-Bundles. 

### Configure JSF

The implementation makes use of the standard JSF-ResourceHandler-API. Just enable the `org.ops4j.pax.web.resources.jsf.OsgiResourceHandler` class in your `faces-config.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<faces-config xmlns="http://xmlns.jcp.org/xml/ns/javaee"
 		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facesconfig_2_2.xsd"
		version="2.2">
	<application>
 		<resource-handler>org.ops4j.pax.web.resources.jsf.OsgiResourceHandler</resource-handler>
	</application>
</faces-config>
```

### Dependencies

A service with the interface `org.ops4j.pax.web.resource.api.OsgiResourceLocator` must be available in the framework. The default implementation is provided via [pax-web-resources-extender](../pax-web-resources-extender).


There is no coupling to Myfaces, nor Mojarra. Only `jakarta.faces` is used.