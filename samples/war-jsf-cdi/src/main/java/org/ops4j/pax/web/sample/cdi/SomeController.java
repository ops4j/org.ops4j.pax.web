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

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

//import javax.enterprise.context.RequestScoped;
//import javax.inject.Named;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

@RequestScoped
@ManagedBean(name = "controller")
//@RequestScoped
//@Named("controller")
public class SomeController {

  private String logMessage;

//  private void executeWithCdiService(Consumer<CdiService> x){
//    final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
//    // get-service, execute function, and unget-service
//    ServiceReference<CdiService> serviceRef = context.getServiceReference(CdiService.class);
//    if (serviceRef != null) {
//      CdiService service = context.getService(serviceRef);
//      if (service != null) {
//        x.accept(service);
//        service = null;
//      }
//      context.ungetService(serviceRef);
//    }
//  }

  public void submit(){
//    executeWithCdiService(service -> service.logMessage(logMessage));
    final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    // get-service, execute function, and unget-service
    ServiceReference<CdiService> serviceRef = context.getServiceReference(CdiService.class);
    if (serviceRef != null) {
      CdiService service = context.getService(serviceRef);
      if (service != null) {
        service.logMessage(logMessage);
      }
      context.ungetService(serviceRef);
    }
  }
  
  public String getHello() {
    //    executeWithCdiService(service -> service.logMessage(logMessage));
    final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    // get-service, execute function, and unget-service
    ServiceReference<CdiService> serviceRef = context.getServiceReference(CdiService.class);
    if (serviceRef != null) {
      CdiService service = context.getService(serviceRef);
      if (service != null) {
        return service.helloFromSession();
      }
      context.ungetService(serviceRef);
    }
    return "ERROR";
  }


  public String getLogMessage() {
    return logMessage;
  }

  public void setLogMessage(String logMessage) {
    this.logMessage = logMessage;
  }
}
