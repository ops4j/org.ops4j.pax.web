/*  Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.internal;

import java.util.Dictionary;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.framework.Bundle;

public interface ServerManager extends ManagedService
{
    Object PID = "org.org.ops4j.pax.web.service.manager";

    void start()
        throws Exception;

    void updated( Dictionary dictionary )
        throws ConfigurationException;

    void stop()
        throws Exception;

    HttpServiceImpl createHttpService( Bundle bunlde );
}
