package org.ops4j.pax.web.service.internal;

public class Assert
{

    private Assert()
    {
        // utility class
    }

    public static void notNull( final String message, final Object object )
    {
        if ( object == null)
        {
            throw new IllegalArgumentException( message );
        }
    }

    public static void notEmpty( final String message, final String object )
    {
        if ( object != null && "".equals( object))
        {
            throw new IllegalArgumentException( message );
        }
    }
}
