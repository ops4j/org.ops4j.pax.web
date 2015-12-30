package org.ops4j.pax.web.jsf.resourcehandler.itest.application.myfaces;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

@ManagedBean
@RequestScoped
public class SomeBean {
    public String getHello(){
        return "Hello Bean";
    }
}
