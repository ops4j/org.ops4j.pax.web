package org.ops4j.pax.web.service.internal.ng;

public interface Registration
{
    void register( HttpServiceServer httpServiceServer );
    String getAlias();
}
