# Webresources-Extender

## Summary

This module provides an extender-based implementation of  the `org.ops4j.pax.web.resource.api.OsgiResourceLocator` interface

The extender-pattern is used to be notified about every bundle-event in the framework.

For the sake of performance, the extender is not scanning every bundle in your container. You have to mark a
bundle as resource-bundle by adding the header `WebResources: true` to your bundles manifest.

If a resource-bundle has been found, all files under `META-INF/resources` are stored as URL in a map for indexing with the key being the lookup-path.

## Example configuration for a resource-bundle

```
Bundle-SymbolicName: jsf-resourcebundle
Include-Resource: src/main/resources/
WebResources: true
```

## Features

The resource-lookup is fast due to the used index. A bundle is only scanned during startup.

A resource can be overriden, by another bundle, and will be visible again when the overriding bundle is stopped again. This feature is limited to one-level of overrides, so if a resource is already overriden and another bundle has a resource under the same lookup-path, an exception will be raised.

## Customization

Customization might be necessary if more levels of overriding resources are required.

To do this, another implementation for `org.ops4j.pax.web.resource.api.OsgiResourceLocator` must be provided. There are two options to do so:

1. Create a separate bundle using the provided api-bundle
2. Just provide a additional service for `org.ops4j.pax.web.resource.api.OsgiResourceLocator`
	- The default implementation in this bundle registers a service with a ranking of -1. By registering a service without a ranking the framework will give it a ranking of 0 which will cause the custom implementation to be picked up
	- This way, the exteder-pattern from this module is still active and will notify your custom-service about new and removed bundles.





