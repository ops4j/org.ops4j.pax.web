/*
 * Copyright 2009 Dmitry Sklyut
 * Copyright 2009 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.spi.model;

import java.util.Arrays;
import java.util.Hashtable;
import javax.servlet.Filter;
import static org.easymock.EasyMock.*;
import org.junit.Test;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.service.WebContainerConstants;

/**
 * @author Dmitry Sklyut
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 0.7.0, June 18, 2009
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
                             new Hashtable<String,Object>()
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
                             new Hashtable<String, String>()
                             {
            	               /**
                                 * 
                                 */
				 private static final long serialVersionUID = 6291128067491953259L;

                                 {
                                     put( WebContainerConstants.FILTER_MAPPING_DISPATCHER, "REQUEST, FORWARD" );
                                 }
                             }
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
                             new Hashtable<String, String>()
                             {
            	               /**
                                 * 
                                 */
				 private static final long serialVersionUID = 4025173284250768044L;

				 {
                                     put( WebContainerConstants.FILTER_MAPPING_DISPATCHER,
                                          "REQUEST, FORWARD, ERROR , include"
                                     );
                                 }
                             }
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
                         new Hashtable<String, String>()
                         {
        	               /**
        	                * 
        	                */
                                private static final long serialVersionUID = -7408477134593679742L;

                                {
                                    put( WebContainerConstants.FILTER_MAPPING_DISPATCHER,
                                      "REQuEST, ZFORWARD, , , include");
                                }
                         }
        );

    }
}
