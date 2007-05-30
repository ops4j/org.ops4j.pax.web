package org.ops4j.pax.web.service.internal.ng;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class HttpServiceImplTest
{
    private HttpServiceImpl m_underTest;
    private Bundle m_bundle;
    private Servlet m_servlet;
    private HttpContext m_context;
    private Dictionary m_initParams;
    private RegistrationRepository m_registrationRepository;
    private Registration m_servletRegistration;
    private HttpServiceServer m_httpServiceServer;

    @Before
    public void setUp()
    {
        m_bundle = createMock( Bundle.class );
        m_servlet = createMock( Servlet.class );
        m_context = createMock( HttpContext.class );
        m_registrationRepository = createMock( RegistrationRepository.class );
        m_servletRegistration = createMock( Registration.class );
        m_httpServiceServer = createMock( HttpServiceServer.class );
        m_initParams = new Hashtable();
        m_underTest = new HttpServiceImpl( m_bundle, m_registrationRepository, m_httpServiceServer );
        reset( m_bundle, m_servlet, m_context, m_registrationRepository, m_servletRegistration, m_httpServiceServer);
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullBundle()
        throws ServletException
    {
        new HttpServiceImpl( null, m_registrationRepository, m_httpServiceServer );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullRegistrationRepository()
        throws ServletException
    {
        new HttpServiceImpl( m_bundle, null, m_httpServiceServer );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullHttpServiceServer()
        throws ServletException
    {
        new HttpServiceImpl( m_bundle, m_registrationRepository, null );
    }

    @Test
    public void stateChangedOnServerStarted() {
        // prepare
        List<Registration> registrations = new ArrayList<Registration>();
        registrations.add( m_servletRegistration );
        expect( m_registrationRepository.get() ).andReturn( registrations );
        m_servletRegistration.register( m_httpServiceServer );
        replay( m_registrationRepository, m_servletRegistration );
        // execute
        m_underTest.stateChanged( HttpServiceServerEvent.STARTED );
        // verify
        verify( m_registrationRepository, m_servletRegistration );
    }

    // expect to not do anything
    @Test
    public void stateChangedOnServerConfigured() {
        // prepare
        replay( m_registrationRepository, m_servletRegistration );
        // execute
        m_underTest.stateChanged( HttpServiceServerEvent.CONFIGURED );
        // verify
        verify( m_registrationRepository, m_servletRegistration );
    }

    // expect to not do anything
    @Test
    public void stateChangedOnServerStoped() {
        // prepare
        replay( m_registrationRepository, m_servletRegistration );
        // execute
        m_underTest.stateChanged( HttpServiceServerEvent.STOPPED );
        // verify
        verify( m_registrationRepository, m_servletRegistration );
    }

    @Test
    public void registerServlet()
        throws NamespaceException, ServletException
    {
        // prepare
        expect( m_registrationRepository.registerServlet( "/alias", m_servlet, m_initParams, m_context ) ).andReturn( m_servletRegistration);
        m_servletRegistration.register( m_httpServiceServer );
        replay( m_registrationRepository, m_servletRegistration );
        // execute
        m_underTest.registerServlet( "/alias", m_servlet, m_initParams, m_context );
        // verify
        verify( m_registrationRepository, m_servletRegistration );
    }

    @Test
    public void registerServletWithNullHttpContext()
        throws NamespaceException, ServletException
    {
        // prepare
        expect( m_registrationRepository.registerServlet( eq( "/alias" ), eq( m_servlet) , eq( m_initParams) , (HttpContext) notNull() )).andReturn( m_servletRegistration);
        replay( m_registrationRepository );
        // execute
        m_underTest.registerServlet( "/alias", m_servlet, m_initParams, null );
        // verify
        verify( m_registrationRepository );
    }

    @Test
    public void createDefaultContext()
        throws NamespaceException, ServletException
    {
        assertNotNull( "not null", m_underTest.createDefaultHttpContext() );   
    }

    @Test
    public void checkRegistrationAsHttpServiceServerListener() {
        // prepare
        m_httpServiceServer.addListener( (HttpServiceServerListener) notNull() );
        replay( m_httpServiceServer );
        // execute
        new HttpServiceImpl( m_bundle, m_registrationRepository, m_httpServiceServer );
        // verify
        verify( m_httpServiceServer );
    }

}
