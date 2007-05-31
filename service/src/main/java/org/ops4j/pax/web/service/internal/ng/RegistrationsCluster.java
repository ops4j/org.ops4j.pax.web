package org.ops4j.pax.web.service.internal.ng;

public interface RegistrationsCluster
{
    void add( Registrations registrations );
    void remove( Registrations registrations );
    HttpTarget getByAlias( String alias );
}
