/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.itest.utils.web;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(value = "/test", name = "test")
public class AnnotatedTestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(AnnotatedTestServlet.class);

	private final CountDownLatch initCalled = new CountDownLatch(1);
	private final CountDownLatch destroyCalled = new CountDownLatch(1);

	@Override
	public void init(ServletConfig config) throws ServletException {
		this.initCalled.countDown();
		LOG.info("init called");
		super.init(config);
	}

	@Override
	public void destroy() {
		this.destroyCalled.countDown();
		LOG.info("destroy called");
		super.destroy();
	}

	public boolean isInitCalled() throws InterruptedException {
		return initCalled.await(5, TimeUnit.SECONDS);
	}

	public boolean isDestroyCalled() throws InterruptedException {
		return destroyCalled.await(5, TimeUnit.SECONDS);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.getWriter().write("TEST OK");
	}

}
