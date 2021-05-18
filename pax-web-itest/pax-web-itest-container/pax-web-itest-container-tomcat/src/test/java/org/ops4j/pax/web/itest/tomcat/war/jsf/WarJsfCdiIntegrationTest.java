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

import static org.ops4j.pax.exam.OptionUtils.combine;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.common.AbstractWarJsfCdiIntegrationTest;

@RunWith(PaxExam.class)
@Ignore("pax-cdi doesn't have tomcat specific bundle")
public class WarJsfCdiIntegrationTest extends AbstractWarJsfCdiIntegrationTest {

    @Configuration
    public Option[] config() {
        return combine(configureTomcat(), configureJsfAndCdi());
    }

    @Override
    protected String cdiWebBundleArtifact() {
        return null;
    }

    @Override
    protected String containerIdentification() {
        return null;
    }

}