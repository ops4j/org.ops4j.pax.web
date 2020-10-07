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
package org.ops4j.pax.web.service.spi.task;

/**
 * Kind of operation to perform on model
 */
public enum OpCode {

	/** Operation fully defined by its class */
	NONE,

	/** Add new model */
	ADD,

	/** Modify properties of existing model */
	MODIFY,

	/** Delete existing model permanently */
	DELETE,

	/** Mark existing model as <em>enabled</em> */
	ENABLE,

	/**
	 * Mark existing model as <em>disabled</em>. It is still known, but won't be registered. Such disabled
	 * element may wait for the moment where other model (with higher ranking) was disabled/removed
	 */
	DISABLE,

	/**
	 * Special association operation between {@link org.ops4j.pax.web.service.WebContainerContext} and
	 * {@link org.ops4j.pax.web.service.spi.model.OsgiContextModel}
	 */
	ASSOCIATE,

	/**
	 * Special deassociation operation between {@link org.ops4j.pax.web.service.WebContainerContext} and
	 * {@link org.ops4j.pax.web.service.spi.model.OsgiContextModel}
	 */
	DISASSOCIATE

}
