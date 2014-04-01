package org.ops4j.pax.web.itest.base.support;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet(value = "/multipartest", name = "multipartest")
@MultipartConfig(fileSizeThreshold=1024*1024*10,    // 10 MB 
				 maxFileSize=1024*1024*50,          // 50 MB
				 maxRequestSize=1024*1024*100)      // 100 MB
public class AnnotatedMultipartTestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		final Part filePart = request.getPart("exampleFile");
		response.getWriter().write("Part of file: " + filePart);
	}
}