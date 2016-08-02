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
package org.ops4j.pax.web.service.tomcat.internal;

/**
 * Created with IntelliJ IDEA. User: romain.gilles Date: 6/11/12 Time: 2:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerStopException extends RuntimeException {
	/**
	 *
	 */
	private static final long serialVersionUID = 7694044273453973884L;

	public ServerStopException(String serverInfo, Throwable cause) {
		super(String.format("cannot stop server: '%s'", serverInfo), cause);
	}
}
