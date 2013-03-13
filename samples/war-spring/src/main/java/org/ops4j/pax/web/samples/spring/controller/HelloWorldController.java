package org.ops4j.pax.web.samples.spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HelloWorldController
{
 
    @RequestMapping("/helloWorld")
    public ModelAndView helloWorld()
    {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("helloWorld");
        String message = "Done! Spring MVC works like a charm! : - ) ";
        modelAndView.addObject("message", message);
        return modelAndView;
    }
 
}
