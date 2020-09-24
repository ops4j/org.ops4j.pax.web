/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.jsp;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Pax Web extension of the original {@link org.apache.jasper.servlet.JspServlet} to set proper TCCL, so Jasper
 * can correctly create {@link javax.el.ExpressionFactory}.
 */
public class JspServlet extends org.apache.jasper.servlet.JspServlet {

	@Override
	public void init(ServletConfig config) throws ServletException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(JspServlet.class.getClassLoader());
			super.init(config);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(JspServlet.class.getClassLoader());
			super.service(req, res);
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

}
