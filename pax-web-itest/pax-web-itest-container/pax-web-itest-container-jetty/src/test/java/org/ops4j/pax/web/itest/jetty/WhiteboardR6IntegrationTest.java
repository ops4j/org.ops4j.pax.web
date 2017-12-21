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

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.common.AbstractWhiteboardR6IntegrationTest;

@RunWith(PaxExam.class)
public class WhiteboardR6IntegrationTest extends AbstractWhiteboardR6IntegrationTest {

    private static final Map<Integer, String> ERROR_MESSAGES = Stream.of(
            new SimpleEntry<>(404, "Not Found"),
            new SimpleEntry<>(442, "442"),
            new SimpleEntry<>(502, "Bad Gateway"),
            new SimpleEntry<>(500, "java.io.IOException: somethingwronghashappened"))
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}

	@Override
	protected String getErrorMessage(int statusCode) {
        return ERROR_MESSAGES.get(statusCode);
    }
}
