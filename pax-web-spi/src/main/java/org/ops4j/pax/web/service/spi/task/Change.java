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

import java.util.Collections;
import java.util.List;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

/**
 * Single operation to be performed within a {@link Batch}. For now it has two purposes:<ul>
 *     <li>Global model alteration</li>
 *     <li>Interaction with {@link org.ops4j.pax.web.service.spi.ServerController} to turn model changes
 *     into actual server runtime (re)configuration operations.</li>
 * </ul>
 */
public abstract class Change {

	private final OpCode kind;
	private Change batchCompletedAction;

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

	/**
	 * <p>Get a list of associated context models.</p>
	 *
	 * <p>Usually the list comes from associated model being ADDed and usually doesn't make sense when a change
	 * concerns more models. Special scenario is to get a list of <em>new</em> context models associated with existing
	 * element model after it has been removed (from previous list of associated contexts) in the same {@link Batch}.</p>
	 *
	 * @return
	 */
	public List<OsgiContextModel> getContextModels() {
		return Collections.emptyList();
	}

	/**
	 * A {@link Change} may be reversed, which is handy when rolling back existing {@link Batch}. A single
	 * change may be a no-op during uninstallation (like for example welcome files), but also may consist of
	 * more uninstallation changes - like {@link OsgiContextModelChange} which has to follow unregistrations of
	 * dynamic servlets/filters/listeners.
	 * @return
	 */
	public void uninstall(List<Change> operations) {
	}

	/**
	 * If during action/change handling, the visitor (action invoker) decides there's another action to perform
	 * but in separate thread (next "tick" of single event thread), this is the way to register such action.
	 * @param action
	 */
	public void registerBatchCompletedAction(Change action) {
		this.batchCompletedAction = action;
	}

	public Change getBatchCompletedAction() {
		return batchCompletedAction;
	}

}
