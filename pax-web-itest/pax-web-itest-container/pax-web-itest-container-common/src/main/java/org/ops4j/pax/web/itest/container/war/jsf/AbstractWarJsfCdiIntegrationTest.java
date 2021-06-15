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
 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ops4j.pax.web.itest.container.war.jsf;

import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;

public abstract class AbstractWarJsfCdiIntegrationTest extends AbstractContainerTestBase {

	protected abstract String containerSpecificCdiBundle();

	// only Jetty implementation for now, I need to prepare Pax CDI 1.2.x to integrate with new Pax Web...

}
