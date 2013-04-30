package org.ops4j.pax.web.samples.spring.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

//@Controller
public class HelloWorldController implements Controller {

	@Override
	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("helloWorld");
		String message = "Done! Spring MVC works like a charm! : - ) ";
		modelAndView.addObject("message", message);
		return modelAndView;
	}

}
