/* Copyright 2007 Alin Dreghiciu.
 *
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
package org.ops4j.pax.web.service.undertow.internal;

import io.undertow.server.handlers.resource.ResourceManager;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

public class ResourceServletTest {

	private ResourceManager resourceManager;
//	private Context context;
	private OsgiContextModel contextModel;

//	public void setUp() {
//		context = createMock(Context.class);
//		resourceManager = createMock(ResourceManager.class);
//		contextModel = createMock(ContextModel.class);
//	}

//	private void checkResourceNameSpaceMapping(String context, String alias, String name,
//											   String uri, String expected) throws IOException, ServletException {
//		setUp();
//		// prepare
//		expect(this.context.getContextModel()).andReturn(contextModel).anyTimes();
//		expect(contextModel.getContextName()).andReturn(context).anyTimes();
//		expect(this.context.getResource(expected)).andReturn(null);
//
//		replay(this.context, resourceManager, contextModel);
//		// execute
//		new ResourceServlet(this.context, alias, name).getResource(uri);
//		// verify
//		verify(this.context, resourceManager, contextModel);
//	}

//	@Test
//	public void checkResourceNameSpaceMapping01() throws IOException,
//			ServletException {
//		checkResourceNameSpaceMapping("", "/", "", "/fudd/bugs", "/fudd/bugs");
//		checkResourceNameSpaceMapping("war", "/", "", "/fudd/bugs", "/fudd/bugs");
//	}
//
//	@Test
//	public void checkResourceNameSpaceMapping02() throws IOException,
//			ServletException {
//		checkResourceNameSpaceMapping("", "/", "/", "/fudd/bugs", "/fudd/bugs");
//		checkResourceNameSpaceMapping("war", "/", "/", "/fudd/bugs", "/fudd/bugs");
//	}
//
//	@Test
//	public void checkResourceNameSpaceMapping03() throws IOException,
//			ServletException {
//		checkResourceNameSpaceMapping("", "/", "/tmp", "/fudd/bugs",
//				"/tmp/fudd/bugs");
//		checkResourceNameSpaceMapping("war", "/", "/tmp", "/fudd/bugs",
//				"/tmp/fudd/bugs");
//	}
//
//	@Test
//	public void checkResourceNameSpaceMapping03a() throws IOException,
//			ServletException {
//		checkResourceNameSpaceMapping("", "/", "default", "/fudd/bugs",
//				"/fudd/bugs");
//		checkResourceNameSpaceMapping("war", "/", "default", "/fudd/bugs",
//				"/fudd/bugs");
//	}
//
//	@Test
//	public void checkResourceNameSpaceMapping04() throws IOException,
//			ServletException {
//		checkResourceNameSpaceMapping("", "/fudd", "", "/fudd/bugs", "/bugs");
//		checkResourceNameSpaceMapping("war", "/fudd", "", "/fudd/bugs", "/bugs");
//		checkResourceNameSpaceMapping("fudd", "/fudd", "", "/fudd/bugs", "/bugs");
//	}
//
//	@Test
//	public void checkResourceNameSpaceMapping04a() throws IOException,
//			ServletException {
//		checkResourceNameSpaceMapping("", "/fudd", "default", "/fudd/bugs", "/bugs");
//		checkResourceNameSpaceMapping("war", "/fudd", "default", "/fudd/bugs", "/bugs");
//		checkResourceNameSpaceMapping("fudd", "/fudd", "default", "/fudd/bugs", "/bugs");
//	}
//
//	@Test
//	public void checkResourceNameSpaceMapping05() throws IOException,
//			ServletException {
//		checkResourceNameSpaceMapping("", "/fudd", "/", "/fudd/bugs", "/bugs");
//		checkResourceNameSpaceMapping("war", "/fudd", "/", "/fudd/bugs", "/bugs");
//		checkResourceNameSpaceMapping("fudd", "/fudd", "/", "/fudd/bugs", "/bugs");
//	}
//
//	@Test
//	public void checkResourceNameSpaceMapping06() throws IOException,
//			ServletException {
//		checkResourceNameSpaceMapping("", "/fudd", "/tmp", "/fudd/bugs",
//				"/tmp/bugs");
//		checkResourceNameSpaceMapping("war", "/fudd", "/tmp", "/fudd/bugs",
//				"/tmp/bugs");
//		checkResourceNameSpaceMapping("fudd", "/fudd", "/tmp", "/fudd/bugs",
//				"/tmp/bugs");
//	}
//
//	@Test
//	public void checkResourceNameSpaceMapping07() throws IOException,
//			ServletException {
//		checkResourceNameSpaceMapping("", "/fudd", "tmp", "/fudd/bugs/x.gif",
//				"tmp/bugs/x.gif");
//		checkResourceNameSpaceMapping("war", "/fudd", "tmp", "/fudd/bugs/x.gif",
//				"tmp/bugs/x.gif");
//		checkResourceNameSpaceMapping("fudd", "/fudd", "tmp", "/fudd/bugs/x.gif",
//				"tmp/bugs/x.gif");
//	}
//
//	@Test
//	public void checkResourceNameSpaceMapping08() throws IOException,
//			ServletException {
//		checkResourceNameSpaceMapping("", "/fudd/bugs/x.gif", "tmp/y.gif",
//				"/fudd/bugs/x.gif", "tmp/y.gif");
//		checkResourceNameSpaceMapping("war", "/fudd/bugs/x.gif", "tmp/y.gif",
//				"/fudd/bugs/x.gif", "tmp/y.gif");
//		checkResourceNameSpaceMapping("fudd", "/fudd/bugs/x.gif", "tmp/y.gif",
//				"/fudd/bugs/x.gif", "tmp/y.gif");
//	}

}
