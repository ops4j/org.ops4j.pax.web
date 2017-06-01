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
package org.ops4j.pax.web.itest.jetty.support;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AsyncServlet extends HttpServlet {

	public static final int SIZE = 1024 + 32;
	private static final int PART = 128;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	/**
	 * Regardless of request path/headers/params, this method will slowly return 1kB of data.
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		switch (req.getDispatcherType()) {
			case REQUEST:
				startResponding(req, resp);
				break;
			case ASYNC:
				continueResponding(req, resp);
				break;
		}
	}

	private void startResponding(final HttpServletRequest req, HttpServletResponse resp) {
		final AsyncContext ac = req.startAsync();
		executor.execute(new Runnable() {
			@Override
			public void run() {
				req.setAttribute("_position", 0);
				req.setAttribute("_read", 0);
				ac.dispatch();
			}
		});
	}

	private void continueResponding(final HttpServletRequest req, final HttpServletResponse resp) {
		final AsyncContext ac = req.startAsync();
		int read = (Integer) req.getAttribute("_read");
		int position = (Integer) req.getAttribute("_position");
		if (read > 0) {
			// return current part
			byte[] buf = new byte[read];
			Arrays.fill(buf, (byte) 0x42);
			try {
				resp.getOutputStream().write(buf);
				resp.flushBuffer();
				position += read;
				req.setAttribute("_position", position);
			} catch (IOException e) {
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				ac.complete();
			}
		}
		if (position == SIZE) {
			ac.complete();
		} else {
			// schedule reading next part
			final int _position = position;
			executor.execute(new Runnable() {
				@Override
				public void run() {
					// read next part/chunk
					int _read = Math.min(PART, SIZE - _position);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					req.setAttribute("_read", _read);
					ac.dispatch();
				}
			});
		}
	}

}
