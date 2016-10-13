/*
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
package org.ops4j.pax.web.sample.cdi;


import org.ops4j.pax.cdi.api.OsgiService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;


@RequestScoped
@Named("controller")
public class SomeController {

  private String logMessage;

  @Inject
  // must be dynamic because during CDI-scanning the service (which lies in the bundle) is not yet active
  @OsgiService(dynamic = true)
  private CdiService service;

  @Inject
  private SomeSessionBean session;

  public void submit(){
    service.logMessage(logMessage);
  }

  public String getHelloSession(){
    return session.getState();
  }

  public String getHelloService(){
    return service.helloFromService();
  }

  public String getLogMessage() {
    return logMessage;
  }

  public void setLogMessage(String logMessage) {
    this.logMessage = logMessage;
  }
}
