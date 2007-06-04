package org.ops4j.pax.web.service.internal.ng;

public interface RegistrationsCluster
{
    void remove( Registrations registrations );
    HttpTarget getByAlias( String alias );
    Registrations create();
}
