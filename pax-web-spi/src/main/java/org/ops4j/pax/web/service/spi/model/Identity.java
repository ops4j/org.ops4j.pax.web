/*
 * Copyright 2008 Alin Dreghiciu.
 *
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
 */
package org.ops4j.pax.web.service.spi.model;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Auto generated id.
 * 
 * @author Alin Dreghiciu
 * @since 0.3.0, January 12, 2008
 */
public class Identity {

	private static final Lock LOCK = new ReentrantLock();
	private static Integer next = 0;
	private final String id;

	public Identity() {
		LOCK.lock();
		try {
			next++;
			id = this.getClass().getName() + "-" + next;
		} finally {
			LOCK.unlock();
		}
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("id=").append(getId()).append("}")
				.toString();
	}

}
