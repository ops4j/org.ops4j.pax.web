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
package org.ops4j.pax.web.service.whiteboard;

/**
 * <p>Registers an error page to customize the response sent back to the web client
 * in case that an exception or error propagates back to the web container, or
 * the servlet/filter calls sendError() on the response object for a specific
 * status code.</p>
 *
 * <p>This is Pax Web specific <em>Whiteboard mapping</em> approach, where all the details are
 * passed directly and not as service registration properties. The problem with CMPN Whitboard
 * specification is that <em>error pages</em> are not registered directly, but as service
 * registration properties when registering a {@link javax.servlet.Servlet}. Also, such servlet is
 * not required to be associated with any URL pattern. Pax Web on the other hand matches the
 * {@code web.xml} model, where <em>error page</em> is a mapping from error codes/exception names
 * to an URI (not a {@link javax.servlet.Servlet}). Thus in Pax Web we're forced to use artificial,
 * generated URI locations. With <em>mapping</em> approach, error mapping is registered together
 * with some servlet with can <strong>always</strong> be accessed directly (just as in {@code web.xml}.</p>
 *
 * @author dsklyut
 * @since 0.7.0 Jun 23, 2009
 */
public interface ErrorPageMapping extends ContextRelated {

	/**
	 * Returns a list of "error spacifications", which may be error codes, FQCN of Exception classes or
	 * special {@code 4xx} or {@code 5xx} values
	 * @return
	 */
	String[] getErrors();

	/**
	 * URI mapping (must start with {@code /}) that'll be used to handle the exception/error. In case of standard
	 * OSGi CMPN Whiteboard specification, the location is passed implicitly - the associated
	 * {@link javax.servlet.Servlet} being registered will handle the problem. In Pax Web <em>Whiteboard mapping</em>,
	 * actual servlet should be registered separately (by registering a {@link javax.servlet.Servlet} or
	 * {@link ServletMapping}).
	 * @return
	 */
	String getLocation();

}
