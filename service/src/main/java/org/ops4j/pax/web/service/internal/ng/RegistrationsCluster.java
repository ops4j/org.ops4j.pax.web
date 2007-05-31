package org.ops4j.pax.web.service.internal.ng;

public interface RegistrationsCluster
{
    void add( final Registrations repository );
    void remove( final Registrations repository );
    HttpTarget getByAlias( String alias );
}
