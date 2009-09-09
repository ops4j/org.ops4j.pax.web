/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.war.internal.parser.dom;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.ops4j.pax.web.extender.war.internal.WebXmlParser;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppErrorPage;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilter;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilterMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppInitParam;
import org.ops4j.pax.web.extender.war.internal.model.WebAppListener;
import org.ops4j.pax.web.extender.war.internal.model.WebAppMimeMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServlet;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletMapping;
import static org.ops4j.util.xml.ElementHelper.*;

/**
 * Web xml parserer implementation using DOM.
 * // TODO parse and use session-config
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
public class DOMWebXmlParser
    implements WebXmlParser
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( DOMWebXmlParser.class );

    /**
     * @see WebXmlParser#parse(InputStream)
     */
    public WebApp parse( final InputStream inputStream )
    {
        WebApp webApp = null;
        try
        {
            final Element rootElement = getRootElement( inputStream );
            if( rootElement != null )
            {
                webApp = new WebApp();
                // web app attributes
                webApp.setDisplayName( getTextContent( getChild( rootElement, "display-name" ) ) );
                parseContextParams( rootElement, webApp );
                parseSessionConfig( rootElement, webApp );
                parseServlets( rootElement, webApp );
                parseFilters( rootElement, webApp );
                parseListeners( rootElement, webApp );
                parseErrorPages( rootElement, webApp );
                parseWelcomeFiles( rootElement, webApp );
                parseMimeMappings( rootElement, webApp );
            }
            else
            {
                LOG.warn( "The parsed web.xml does not have a root element" );
            }
        }
        catch( ParserConfigurationException ignore )
        {
            LOG.error( "Cannot parse web.xml", ignore );
        }
        catch( IOException ignore )
        {
            LOG.error( "Cannot parse web.xml", ignore );
        }
        catch( SAXException ignore )
        {
            LOG.error( "Cannot parse web.xml", ignore );
        }
        return webApp;
    }

    /**
     * Parses context params out of web.xml.
     *
     * @param rootElement web.xml root element
     * @param webApp      web app for web.xml
     */
    private static void parseContextParams( final Element rootElement, final WebApp webApp )
    {
        final Element[] elements = getChildren( rootElement, "context-param" );
        if( elements != null && elements.length > 0 )
        {
            for( Element element : elements )
            {
                final WebAppInitParam initParam = new WebAppInitParam();
                initParam.setParamName( getTextContent( getChild( element, "param-name" ) ) );
                initParam.setParamValue( getTextContent( getChild( element, "param-value" ) ) );
                webApp.addContextParam( initParam );
            }
        }
    }

    /**
     * Parses session config out of web.xml.
     *
     * @param rootElement web.xml root element
     * @param webApp      web app for web.xml
     */
    private static void parseSessionConfig( final Element rootElement, final WebApp webApp )
    {
        final Element scElement = getChild( rootElement, "session-config" );
        if( scElement != null )
        {
            final Element stElement = getChild( scElement, "session-config" );
            if( stElement != null )
            {
                webApp.setSessionTimeout( getTextContent( stElement ) );
            }
        }
    }

    /**
     * Parses servlets and servlet mappings out of web.xml.
     *
     * @param rootElement web.xml root element
     * @param webApp      web app for web.xml
     */
    private static void parseServlets( final Element rootElement, final WebApp webApp )
    {
        final Element[] elements = getChildren( rootElement, "servlet" );
        if( elements != null && elements.length > 0 )
        {
            for( Element element : elements )
            {
                final WebAppServlet servlet = new WebAppServlet();
                servlet.setServletName( getTextContent( getChild( element, "servlet-name" ) ) );
                servlet.setServletClass( getTextContent( getChild( element, "servlet-class" ) ) );
                webApp.addServlet( servlet );
                final Element[] initParamElements = getChildren( element, "init-param" );
                if( initParamElements != null && initParamElements.length > 0 )
                {
                    for( Element initParamElement : initParamElements )
                    {
                        final WebAppInitParam initParam = new WebAppInitParam();
                        initParam.setParamName( getTextContent( getChild( initParamElement, "param-name" ) ) );
                        initParam.setParamValue( getTextContent( getChild( initParamElement, "param-value" ) ) );
                        servlet.addInitParam( initParam );
                    }
                }
            }
        }
        final Element[] mappingElements = getChildren( rootElement, "servlet-mapping" );
        if( mappingElements != null && mappingElements.length > 0 )
        {
            for( Element mappingElement : mappingElements )
            {
                // starting with servlet 2.5 url-patern can be specified more times
                // for the earlier version only one entry will be returned
                final String servletName = getTextContent( getChild( mappingElement, "servlet-name" ) );
                final Element[] urlPatternsElements = getChildren( mappingElement, "url-pattern" );
                if( urlPatternsElements != null && urlPatternsElements.length > 0 )
                {
                    for( Element urlPatternElement : urlPatternsElements )
                    {
                        final WebAppServletMapping servletMapping = new WebAppServletMapping();
                        servletMapping.setServletName( servletName );
                        servletMapping.setUrlPattern( getTextContent( urlPatternElement ) );
                        webApp.addServletMapping( servletMapping );
                    }
                }
            }
        }
    }

    /**
     * Parses filters and filter mappings out of web.xml.
     *
     * @param rootElement web.xml root element
     * @param webApp      web app for web.xml
     */
    private static void parseFilters( final Element rootElement, final WebApp webApp )
    {
        final Element[] elements = getChildren( rootElement, "filter" );
        if( elements != null && elements.length > 0 )
        {
            for( Element element : elements )
            {
                final WebAppFilter filter = new WebAppFilter();
                filter.setFilterName( getTextContent( getChild( element, "filter-name" ) ) );
                filter.setFilterClass( getTextContent( getChild( element, "filter-class" ) ) );
                webApp.addFilter( filter );
                final Element[] initParamElements = getChildren( element, "init-param" );
                if( initParamElements != null && initParamElements.length > 0 )
                {
                    for( Element initParamElement : initParamElements )
                    {
                        final WebAppInitParam initParam = new WebAppInitParam();
                        initParam.setParamName( getTextContent( getChild( initParamElement, "param-name" ) ) );
                        initParam.setParamValue( getTextContent( getChild( initParamElement, "param-value" ) ) );
                        filter.addInitParam( initParam );
                    }
                }
            }
        }
        final Element[] mappingElements = getChildren( rootElement, "filter-mapping" );
        if( mappingElements != null && mappingElements.length > 0 )
        {
            for( Element mappingElement : mappingElements )
            {
                // starting with servlet 2.5 url-patern / servlet-names can be specified more times
                // for the earlier version only one entry will be returned
                final String filterName = getTextContent( getChild( mappingElement, "filter-name" ) );
                final Element[] urlPatternsElements = getChildren( mappingElement, "url-pattern" );
                if( urlPatternsElements != null && urlPatternsElements.length > 0 )
                {
                    for( Element urlPatternElement : urlPatternsElements )
                    {
                        final WebAppFilterMapping filterMapping = new WebAppFilterMapping();
                        filterMapping.setFilterName( filterName );
                        filterMapping.setUrlPattern( getTextContent( urlPatternElement ) );
                        webApp.addFilterMapping( filterMapping );
                    }
                }
                final Element[] servletNamesElements = getChildren( mappingElement, "servlet-name" );
                if( servletNamesElements != null && servletNamesElements.length > 0 )
                {
                    for( Element servletNameElement : servletNamesElements )
                    {
                        final WebAppFilterMapping filterMapping = new WebAppFilterMapping();
                        filterMapping.setFilterName( filterName );
                        filterMapping.setServletName( getTextContent( servletNameElement ) );
                        webApp.addFilterMapping( filterMapping );
                    }
                }
            }
        }
    }

    /**
     * Parses listsners out of web.xml.
     *
     * @param rootElement web.xml root element
     * @param webApp      web app for web.xml
     */
    private static void parseListeners( final Element rootElement, final WebApp webApp )
    {
        final Element[] elements = getChildren( rootElement, "listener" );
        if( elements != null && elements.length > 0 )
        {
            for( Element element : elements )
            {
                final WebAppListener listener = new WebAppListener();
                listener.setListenerClass( getTextContent( getChild( element, "listener-class" ) ) );
                webApp.addListener( listener );
            }
        }
    }

    /**
     * Parses error pages out of web.xml.
     *
     * @param rootElement web.xml root element
     * @param webApp      web app for web.xml
     */
    private static void parseErrorPages( final Element rootElement, final WebApp webApp )
    {
        final Element[] elements = getChildren( rootElement, "error-page" );
        if( elements != null && elements.length > 0 )
        {
            for( Element element : elements )
            {
                final WebAppErrorPage errorPage = new WebAppErrorPage();
                errorPage.setErrorCode( getTextContent( getChild( element, "error-code" ) ) );
                errorPage.setExceptionType( getTextContent( getChild( element, "exception-type" ) ) );
                errorPage.setLocation( getTextContent( getChild( element, "location" ) ) );
                webApp.addErrorPage( errorPage );
            }
        }
    }

    /**
     * Parses welcome files out of web.xml.
     *
     * @param rootElement web.xml root element
     * @param webApp      web app for web.xml
     */
    private static void parseWelcomeFiles( final Element rootElement, final WebApp webApp )
    {
        final Element listElement = getChild( rootElement, "welcome-file-list" );
        if( listElement != null )
        {
            final Element[] elements = getChildren( listElement, "welcome-file" );
            if( elements != null && elements.length > 0 )
            {
                for( Element element : elements )
                {
                    webApp.addWelcomeFile( getTextContent( element ) );
                }
            }
        }
    }

    /**
     * Parses mime mappings out of web.xml.
     *
     * @param rootElement web.xml root element
     * @param webApp      web app for web.xml
     */
    private static void parseMimeMappings( final Element rootElement, final WebApp webApp )
    {
        final Element[] elements = getChildren( rootElement, "mime-mapping" );
        if( elements != null && elements.length > 0 )
        {
            for( Element element : elements )
            {
                final WebAppMimeMapping mimeMapping = new WebAppMimeMapping();
                mimeMapping.setExtension( getTextContent( getChild( element, "extension" ) ) );
                mimeMapping.setMimeType( getTextContent( getChild( element, "mime-type" ) ) );
                webApp.addMimeMapping( mimeMapping );
            }
        }
    }

    /**
     * Returns the text content of an element or null if the element is null.
     *
     * @param element the som elemet form which the contet should be retrieved
     *
     * @return text content of element
     */
    private static String getTextContent( final Element element )
    {
        if( element != null )
        {
            String content = element.getTextContent();
            if( content != null )
            {
                content = content.trim();
            }
            return content;
        }
        return null;
    }

}
