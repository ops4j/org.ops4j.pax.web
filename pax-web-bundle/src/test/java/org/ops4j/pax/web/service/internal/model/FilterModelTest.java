package org.ops4j.pax.web.service.internal.model;

import java.util.Hashtable;
import java.util.Arrays;
import javax.servlet.Filter;
import static org.easymock.EasyMock.*;
import org.junit.Test;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.service.WebContainerConstants;

/**
 * Created by IntelliJ IDEA.
 * User: dsklyut
 * Date: Jun 18, 2009
 * Time: 3:09:36 PM
 */
public class FilterModelTest
{

    @Test
    public void registerWithNoDispatcherInitParams()
    {
        FilterModel fm =
            new FilterModel( new ContextModel( createMock( HttpContext.class ), null, getClass().getClassLoader() ),
                             createMock( Filter.class ),
                             new String[]{ "/*" },
                             null,
                             new Hashtable()
            );

        System.out.println( Arrays.asList( fm.getDispatcher() ) );
    }

    @Test
    public void registerWithCorrectSubsetOfDispatcherInitParams()
    {
        FilterModel fm =
            new FilterModel( new ContextModel( createMock( HttpContext.class ), null, getClass().getClassLoader() ),
                             createMock( Filter.class ),
                             new String[]{ "/*" },
                             null,
                             new Hashtable()
                             {{
                                     put( WebContainerConstants.FILTER_MAPPING_DISPATCHER, "REQUEST, FORWARD" );
                                 }}
            );

        System.out.println( Arrays.asList( fm.getDispatcher() ) );
    }

    @Test
    public void registerWithFullComplimentOfDispatcherInitParams()
    {
        FilterModel fm =
            new FilterModel( new ContextModel( createMock( HttpContext.class ), null, getClass().getClassLoader() ),
                             createMock( Filter.class ),
                             new String[]{ "/*" },
                             null,
                             new Hashtable()
                             {{
                                     put( WebContainerConstants.FILTER_MAPPING_DISPATCHER,
                                          "REQUEST, FORWARD, ERROR , include"
                                     );
                                 }}
            );

        System.out.println( Arrays.asList( fm.getDispatcher() ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerWithErrorInDispatcherInitParams()
    {
        new FilterModel( new ContextModel( createMock( HttpContext.class ), null, getClass().getClassLoader() ),
                         createMock( Filter.class ),
                         new String[]{ "/*" },
                         null,
                         new Hashtable()
                         {{
                                 put( WebContainerConstants.FILTER_MAPPING_DISPATCHER,
                                      "REQuEST, ZFORWARD, , , include"
                                 );
                             }}
        );

    }
}
