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
 * Single operation to be performed within a {@link Batch}. For now it has two purposes:<ul>
 *     <li>Global model alteration</li>
 *     <li>Interaction with {@link org.ops4j.pax.web.service.spi.ServerController} to turn model changes
 *     into actual server runtime (re)configuration operations.</li>
 * </ul>
 */
public abstract class Change {

	private final OpCode kind;

	public Change(OpCode kind) {
		this.kind = kind;
	}

	public OpCode getKind() {
		return kind;
	}

	/**
	 * Perform an operation in acceptor-visitor pattern.
	 * @param visitor
	 */
	public abstract void accept(BatchVisitor visitor);

}
