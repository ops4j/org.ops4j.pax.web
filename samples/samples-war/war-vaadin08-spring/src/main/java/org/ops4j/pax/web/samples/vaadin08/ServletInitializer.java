/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.samples.vaadin08;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.websocket.WebSocketAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

// without the exclude, I was getting:
// org.springframework.beans.factory.BeanCreationException: Error creating bean with name \
//     'requestMappingHandlerMapping' defined in class path resource \
//     [org/springframework/boot/autoconfigure/web/WebMvcAutoConfiguration$EnableWebMvcConfiguration.class]: \
//     Invocation of init method failed; nested exception is java.lang.ArrayStoreException: \
//     sun.reflect.annotation.TypeNotPresentExceptionProxy
// that's probably fixed, but not with Spring Boot 1.5 / Framework 4.3
@SpringBootApplication(exclude = WebSocketAutoConfiguration.class)
public class ServletInitializer extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		// because of how org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider.findCandidateComponents()
		// works in OSGi environment (it doesn't), we have to specify all the beans explicitly
		return application.sources(ServletInitializer.class, Greeter.class, MyUI.class,
				NullEmbeddedServletContainerFactory.class, NullWebSocketContainerCustomizer.class);
	}

}
