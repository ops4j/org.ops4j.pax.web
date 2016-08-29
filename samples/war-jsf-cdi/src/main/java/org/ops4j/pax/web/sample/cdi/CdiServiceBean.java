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

import javax.inject.Inject;

import org.ops4j.pax.cdi.api.OsgiService;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.osgi.service.log.LogService;

// FIXME JSF cannot access CdiBean https://ops4j1.jira.com/projects/PAXCDI/issues/PAXCDI-210
@OsgiServiceProvider
public class CdiServiceBean implements CdiService {

  @Inject
  @OsgiService
  private LogService logService;

  @Inject
  private SessionBean session;

  @Override
  public String helloFromSession() {
    return session.getState();
  }

  @Override
  public void logMessage(String message) {
    if(logService != null){
      logService.log(LogService.LOG_INFO, message);
    }
  }

  public String hello(){
    return session.getState();
  }
}
