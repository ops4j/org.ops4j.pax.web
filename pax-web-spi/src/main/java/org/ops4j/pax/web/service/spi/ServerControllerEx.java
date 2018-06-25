/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.spi;

import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;

/**
 * <p>This interface complements {@link ServerController} but allows us to not change this interface
 * between 7.1.x and 7.2.0 versions of pax-web.</p>
 * <p><strong>Please move this method to {@link ServerController} at major version change and remove
 * this interface hoping no one will implement it externally.</strong></p>
 */
public interface ServerControllerEx {

	void removeSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel);

}
