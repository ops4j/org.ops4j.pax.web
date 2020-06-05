/*
 * Copyright 2019 OPS4J.
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
package org.ops4j.pax.web.itest.osgi;

import java.io.IOException;
import java.util.Arrays;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * This test check whether pax-web-spi contains proper private packages and whether the imports are sufficient
 * to perform various tasks related to class/annotation discovery.
 */
@RunWith(PaxExam.class)
public class PaxWebRuntimeNoMetaTypeIntegrationTest extends AbstractControlledBase2 {

	public static Logger LOG = LoggerFactory.getLogger(PaxWebRuntimeNoMetaTypeIntegrationTest.class);

	@Inject
	private MetaTypeService metaTypeService;

	@Configuration
	public Option[] configure() {
		return combine(baseConfigure(), combine(paxWebCore(), paxWebRuntime(), metaTypeService()));
	}

	@Test
	public void checkPaxWebRuntimeMetaTypeWithMetaTypeService() throws IOException {
		Bundle runtime = bundle("org.ops4j.pax.web.pax-web-runtime");
		MetaTypeInformation info = metaTypeService.getMetaTypeInformation(runtime);
		ObjectClassDefinition ocd = info.getObjectClassDefinition(PaxWebConstants.PID, null);
		AttributeDefinition[] attributes = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);

		assertTrue(attributes.length > 0);

		AttributeDefinition ad = Arrays.stream(attributes)
				.filter(a -> a.getID().equals(PaxWebConfig.PID_CFG_TEMP_DIR)).findFirst().orElse(null);
		assertNotNull(ad);

		assertThat(ad.getCardinality(), equalTo(0));
		assertThat("Property should not be resolved", ad.getDefaultValue()[0], equalTo("${java.io.tmpdir}"));
	}

}
