/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.itest.jetty.httpservice;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.httpservice.AbstractHttpServiceBundleIntegrationTest;
import org.ops4j.pax.web.itest.utils.WaitCondition;
import org.ops4j.pax.web.service.PaxWebConfig;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public class HttpServiceBundleIntegrationTest extends AbstractHttpServiceBundleIntegrationTest {

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebJetty());
		return combine(
				serverOptions,
				systemProperty(PaxWebConfig.PID_CFG_LOG_NCSA_ENABLED).value("true"),
				systemProperty(PaxWebConfig.PID_CFG_LOG_NCSA_LOGFILE_DATE_FORMAT).value("yyyy_MM_dd"),
				systemProperty(PaxWebConfig.PID_CFG_LOG_NCSA_LOGDIR).value("target/logs")
		);
	}

	@Test
	// Logging depends on the container implementation and configuration
	public void testNCSALogger() throws Exception {
		testServletPath();

		DateFormat formater = new SimpleDateFormat("yyyy_MM_dd");
		String date = formater.format(new Date());

		final File logFile = new File("target/logs/" + date + ".request.log");

		LOG.info("Log-File: {}", logFile.getAbsoluteFile());

		assertNotNull(logFile);

		new WaitCondition("logfile") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return logFile.exists();
			}
		}.waitForCondition();

		boolean exists = logFile.getAbsoluteFile().exists();

		assertTrue(exists);

		FileInputStream fstream = new FileInputStream(logFile.getAbsoluteFile());
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine = br.readLine();
		assertNotNull(strLine);
		in.close();
		fstream.close();
	}

}
