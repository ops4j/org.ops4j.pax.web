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
package org.ops4j.pax.web.service.spi.task;

import java.util.List;

/**
 * A change related to transactional {@link Batch}, so we know that some operations related to given context path
 * are <em>transactional</em>, which means that associated context may be started at the end.
 */
public class TransactionStateChange extends Change {

	private final String contextPath;

	public TransactionStateChange(OpCode kind, String contextPath) {
		super(kind);
		this.contextPath = contextPath;
	}

	@Override
	public void accept(BatchVisitor visitor) {
		visitor.visitTransactionStateChange(this);
	}

	@Override
	public void uninstall(List<Change> operations) {
		if (getKind() == OpCode.ASSOCIATE) {
			operations.add(new TransactionStateChange(OpCode.DISASSOCIATE, contextPath));
		} else if (getKind() == OpCode.DISASSOCIATE) {
			operations.add(new TransactionStateChange(OpCode.ASSOCIATE, contextPath));
		}
	}

	public String getContextPath() {
		return contextPath;
	}

	@Override
	public String toString() {
		return getKind() + ": " + contextPath;
	}

}
