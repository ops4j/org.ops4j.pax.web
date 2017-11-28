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
package org.ops4j.pax.web.itest.jetty;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.itest.base.WaitCondition;
import org.ops4j.pax.web.itest.common.AbstractHttpServiceIntegrationTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class HttpServiceIntegrationTest extends AbstractHttpServiceIntegrationTest {
    private static final Logger LOG = LoggerFactory.getLogger(HttpServiceIntegrationTest.class);

	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}

	@Override
    protected boolean isInitEager() {
        return true;
    }

    @Test
    // Logging depends on the container implementation and configuration
    public void testNCSALogger() throws Exception {
        testServletPath();

        SimpleDateFormat formater = new SimpleDateFormat("yyyy_MM_dd");
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
