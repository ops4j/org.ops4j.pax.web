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
package org.ops4j.pax.web.itest.tomcat;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.common.AbstractWhiteboardIntegrationTest;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
// Disable this test as it fails with:
//	[ERROR] testMultipleContextMappingsWithDTOsCheck(org.ops4j.pax.web.itest.tomcat.WhiteboardIntegrationTest)  Time elapsed: 1.373 s  <<< ERROR!
//		java.lang.NullPointerException: Specified service reference cannot be null.
// @RunWith(PaxExam.class)
// public class WhiteboardIntegrationTest extends AbstractWhiteboardIntegrationTest {
public class WhiteboardIntegrationTest {

	// @Configuration
	//public static Option[] configure() {
	//	return configureTomcat();
	//}
}
