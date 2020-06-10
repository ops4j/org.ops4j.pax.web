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
package org.ops4j.pax.web.service.undertow.internal.configuration;

import io.undertow.predicate.PathPrefixPredicate;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.handlers.IPAddressAccessControlHandler;
import io.undertow.server.handlers.builder.HandlerParser;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class PredicateParserTest {

	@Test
	public void parseExpression() {
		HandlerWrapper wrapper = HandlerParser.parse("ip-access-control(default-allow=false, acl={'127.0.0.1 allow', '127.0.0.2 allow'})", this.getClass().getClassLoader());
		assertNotNull(wrapper);
		assertThat(wrapper.wrap(null).getClass(), equalTo(IPAddressAccessControlHandler.class));
	}

	@Test
	public void parsePredicate() {
		Predicate predicate = Predicates.parse("path-prefix('/cxf')", this.getClass().getClassLoader());
		assertNotNull(predicate);
		assertThat(predicate.getClass(), equalTo(PathPrefixPredicate.class));
	}

}
