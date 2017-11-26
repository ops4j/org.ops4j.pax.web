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
package org.ops4j.pax.web.itest.undertow;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.itest.base.WaitCondition;
import org.ops4j.pax.web.itest.common.AbstractHttpServiceIntegrationTest;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class HttpServiceIntegrationTest extends AbstractHttpServiceIntegrationTest {

    @Configuration
    public static Option[] configure() {
        return configureUndertow();
    }

    @Test
    @Ignore("FIXME: The old test worked")
    public void testRegisterMultipleServletsSameContext() throws Exception {
        super.testRegisterMultipleServletsSameContext();
    }

    @Test
    public void testNCSALogger() throws Exception {
        testServletPath();

        final File logFile = new File("target/logs/request.log");

        if (!logFile.exists()) {
            logFile.getParentFile().mkdirs();
        }

        logger.info("Log-File: {}", logFile.getAbsoluteFile());

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

    @Override
    protected boolean isInitEager() {
        return false;
    }
}
