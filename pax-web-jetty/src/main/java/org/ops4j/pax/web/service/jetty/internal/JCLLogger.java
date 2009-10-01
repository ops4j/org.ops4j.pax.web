/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.jetty.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.log.Logger;

/**
 * A jetty6 logger implementation on top of commons-logging.
 * This class has been copied from ServiceMix HTTP Binding Component.
 *
 * @author gnodet
 */
public class JCLLogger
    implements Logger
{

    private static final char DELIM_START = '{';
    private static final char DELIM_STOP = '}';

    private final String name;
    private final Log log;

    public JCLLogger()
    {
        this( "org.mortbay.jetty" );
    }

    public JCLLogger( String name )
    {
        this.name = name;
        this.log = LogFactory.getLog( name );
    }

    public static void init()
    {
        // TODO: use org.mortbay.log.Log#setLog when available (beta18)
        String old = System.getProperty( "org.mortbay.log.class" );
        try
        {
            System.setProperty( "org.mortbay.log.class", JCLLogger.class.getName() );
            // For the class to be loaded by invoking a public static method
            Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass( "org.mortbay.log.Log" );
            cl.getMethod( "isDebugEnabled" ).invoke( null );
        } catch( Exception e )
        {
            e.printStackTrace();
        } finally
        {
            if( old != null )
            {
                System.setProperty( "org.mortbay.log.class", old );
            }
            else
            {
                System.getProperties().remove( "org.mortbay.log.class" );
            }
        }
    }

    public void debug( String msg, Throwable th )
    {
        log.debug( msg, th );
    }

    public void debug( String msg, Object arg0, Object arg1 )
    {
        if( log.isDebugEnabled() )
        {
            log.debug( arrayFormat( msg, new Object[]{ arg0, arg1 } ) );
        }
    }

    public Logger getLogger( String loggerName )
    {
        if( loggerName == null )
        {
            return null;
        }
        return new JCLLogger( this.name + "." + loggerName );
    }

    public void info( String msg, Object arg0, Object arg1 )
    {
        debug( msg, arg0, arg1 );
    }

    public boolean isDebugEnabled()
    {
        return log.isDebugEnabled();
    }

    public void setDebugEnabled( boolean enabled )
    {
        log.warn( "setDebugEnabled not supported" );
    }

    public void warn( String msg, Throwable th )
    {
        log.warn( msg, th );
    }

    public void warn( String msg, Object arg0, Object arg1 )
    {
        if( log.isWarnEnabled() )
        {
            log.warn( arrayFormat( msg, new Object[]{ arg0, arg1 } ) );
        }
    }

    private static String arrayFormat( String messagePattern, Object[] argArray )
    {
        if( messagePattern == null )
        {
            return null;
        }
        int i = 0;
        int len = messagePattern.length();

        StringBuffer sbuf = new StringBuffer( messagePattern.length() + 50 );

        for( int index = 0; index < argArray.length; index++ )
        {

            char escape = 'x';

            int j = messagePattern.indexOf( DELIM_START, i );

            if( j == -1 || ( j + 1 == len ) )
            {
                // no more variables
                if( i == 0 )
                { // this is a simple string
                    return messagePattern;
                }
                else
                { // add the tail string which contains no variables
                    // and return the result.
                    sbuf.append( messagePattern.substring( i, messagePattern.length() ) );
                    return sbuf.toString();
                }
            }
            else
            {
                char delimStop = messagePattern.charAt( j + 1 );
                if( j > 0 )
                {
                    escape = messagePattern.charAt( j - 1 );
                }

                if( escape == '\\' )
                {
                    index--; // DELIM_START was escaped, thus should not be
                    // incremented
                    sbuf.append( messagePattern.substring( i, j - 1 ) );
                    sbuf.append( DELIM_START );
                    i = j + 1;
                }
                else if( delimStop != DELIM_STOP )
                {
                    // invalid DELIM_START/DELIM_STOP pair
                    sbuf.append( messagePattern.substring( i, messagePattern.length() ) );
                    return sbuf.toString();
                }
                else
                {
                    // normal case
                    sbuf.append( messagePattern.substring( i, j ) );
                    sbuf.append( argArray[ index ] );
                    i = j + 2;
                }
            }
        }
        // append the characters following the second {} pair.
        sbuf.append( messagePattern.substring( i, messagePattern.length() ) );
        return sbuf.toString();
    }
}
