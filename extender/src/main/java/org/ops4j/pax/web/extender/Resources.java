package org.ops4j.pax.web.extender;
/*
 * Copyright 2007 Damian Golda.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Class used for registering resources for HttpService using white-board approach.
 * To register resources using Pax Web Extender:
 * <pre>
 * import org.ops4j.pax.web.extender.Resources;
 * 
 * ...
 * 
 * Dictionary props = new Hashtable();
 * props.put( "alias", "/whiteboardresources" );
 * m_registration = bundleContext.registerService( Resources.class.getName(), "/mypathtoresourcesinbundle", props );
 * </pre>
 * 
 * @author Damian Golda
 * @since August 22, 2007
 */
public class Resources
{

  private final String resources;

  public Resources(String resources)
  {
    if(resources == null) 
    {
      throw new NullPointerException("resources");
    }
    this.resources = resources;
  }

  public String getResources()
  {
    return resources;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (!(obj instanceof Resources))
    {
      return false;
    }
    Resources other = ((Resources) obj);
    return resources.equals(other.resources);
  }

  @Override
  public int hashCode()
  {
    return resources.hashCode();
  }
}
