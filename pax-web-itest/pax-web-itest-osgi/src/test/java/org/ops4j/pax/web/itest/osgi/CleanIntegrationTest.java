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

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * Clean test without any pax-web bundles. Just simplest showcase of what's needed to run
 * manually controlled pax-exam test.
 */
@RunWith(PaxExam.class)
public class CleanIntegrationTest extends AbstractControlledTestBase {

	public static final Logger LOG = LoggerFactory.getLogger(CleanIntegrationTest.class);

	@Configuration
	public Option[] configure() {
		return combine(baseConfigure(), paxWebCore());
	}

	@Test
	public void justRun() {
		Set<Bundle> bundles = new TreeSet<>((b1, b2) -> (int) (b1.getBundleId() - b2.getBundleId()));
		bundles.addAll(Arrays.asList(context.getBundles()));
		for (Bundle b : bundles) {
			String info = String.format("#%02d: %s/%s (%s)",
					b.getBundleId(), b.getSymbolicName(), b.getVersion(), b.getLocation());
			LOG.info(info);
		}

		Bundle api = bundle("org.ops4j.pax.web.pax-web-api");
		Bundle spi = bundle("org.ops4j.pax.web.pax-web-spi");
		assertThat(api.getState(), equalTo(Bundle.ACTIVE));
		assertThat(spi.getState(), equalTo(Bundle.ACTIVE));
	}

}
