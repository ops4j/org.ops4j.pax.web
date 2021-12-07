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
package org.ops4j.pax.web.service.undertow.configuration.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import org.ops4j.pax.web.service.undertow.internal.configuration.ParserUtils;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

public class IoSubsystem {

	private final List<Worker> workers = new ArrayList<>();

	private final List<BufferPool> bufferPools = new ArrayList<>();

	public List<Worker> getWorkers() {
		return workers;
	}

	public List<BufferPool> getBufferPools() {
		return bufferPools;
	}

	@Override
	public String toString() {
		return "{\n\t\tworkers: " + workers +
				"\n\t\tbuffer pools: " + bufferPools +
				"\n\t}";
	}

	public static class Worker {
		private static final QName ATT_NAME = new QName("name");
		private static final QName ATT_IO_THREADS = new QName("io-threads");
		private static final QName ATT_TASK_KEEP_ALIVE = new QName("task-keepalive");
		private static final QName ATT_TASK_CORE_THREADS = new QName("task-core-threads");
		private static final QName ATT_TASK_MAX_THREADS = new QName("task-max-threads");
		private static final QName ATT_STACK_SIZE = new QName("stack-size");

		private static final int DEFAULT_IO_THREADS = Math.max(Runtime.getRuntime().availableProcessors(), 2);
		private static final int DEFAULT_TASK_CORE_THREADS = Math.max(Runtime.getRuntime().availableProcessors(), 2) * 8;
		private static final int DEFAULT_TASK_MAX_THREADS = Math.max(Runtime.getRuntime().availableProcessors(), 2) * 8;

		private String name;
		private int ioThreads = DEFAULT_IO_THREADS;
		private int taskKeepalive = 60000;
		private int taskCoreThreads = DEFAULT_TASK_CORE_THREADS;
		private int taskMaxThreads = DEFAULT_TASK_MAX_THREADS;
		private long stackSize = 0;
		//<xs:element name="outbound-bind-address" type="outboundBindAddressType"/> 0:N

		public static Worker create(Map<QName, String> attributes, Locator locator) throws SAXParseException {
			Worker worker = new Worker();
			worker.name = attributes.get(ATT_NAME);
			worker.ioThreads = ParserUtils.toInteger(attributes.get(ATT_IO_THREADS), locator, DEFAULT_IO_THREADS);
			worker.taskKeepalive = ParserUtils.toInteger(attributes.get(ATT_TASK_KEEP_ALIVE), locator, 60000);
			worker.taskCoreThreads = ParserUtils.toInteger(attributes.get(ATT_TASK_CORE_THREADS), locator, DEFAULT_TASK_CORE_THREADS);
			worker.taskMaxThreads = ParserUtils.toInteger(attributes.get(ATT_TASK_MAX_THREADS), locator, DEFAULT_TASK_MAX_THREADS);
			worker.stackSize = ParserUtils.toLong(attributes.get(ATT_STACK_SIZE), locator, 0L);

			return worker;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getIoThreads() {
			return ioThreads;
		}

		public void setIoThreads(int ioThreads) {
			this.ioThreads = ioThreads;
		}

		public int getTaskKeepalive() {
			return taskKeepalive;
		}

		public void setTaskKeepalive(int taskKeepalive) {
			this.taskKeepalive = taskKeepalive;
		}

		public int getTaskCoreThreads() {
			return taskCoreThreads;
		}

		public void setTaskCoreThreads(int taskCoreThreads) {
			this.taskCoreThreads = taskCoreThreads;
		}

		public int getTaskMaxThreads() {
			return taskMaxThreads;
		}

		public void setTaskMaxThreads(int taskMaxThreads) {
			this.taskMaxThreads = taskMaxThreads;
		}

		public long getStackSize() {
			return stackSize;
		}

		public void setStackSize(long stackSize) {
			this.stackSize = stackSize;
		}

		@Override
		public String toString() {
			return "{ name: " + name +
					", io threads: " + ioThreads +
					", task max threads: " + taskMaxThreads +
					", task core threads: " + taskCoreThreads +
					", task keep alive: " + taskKeepalive +
					", stack size: " + stackSize +
					" }";
		}
	}

	public static class BufferPool {
		private static final QName ATT_NAME = new QName("name");
		private static final QName ATT_BUFFER_SIZE = new QName("buffer-size");
		private static final QName ATT_DIRECT_BUFFERS = new QName("direct-buffers");
		//<xs:attribute name="buffers-per-slice" use="optional" type="xs:int">

		private String name;
		private Integer bufferSize;
		private boolean directBuffers = true;

		public static BufferPool create(Map<QName, String> attributes, Locator locator) throws SAXParseException {
			BufferPool pool = new BufferPool();
			pool.name = attributes.get(ATT_NAME);
			pool.bufferSize = ParserUtils.toInteger(attributes.get(ATT_BUFFER_SIZE), locator, null);
			pool.directBuffers = ParserUtils.toBoolean(attributes.get(ATT_DIRECT_BUFFERS), locator, true);

			return pool;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getBufferSize() {
			return bufferSize;
		}

		public void setBufferSize(Integer bufferSize) {
			this.bufferSize = bufferSize;
		}

		public boolean getDirectBuffers() {
			return directBuffers;
		}

		public void setDirectBuffers(boolean directBuffers) {
			this.directBuffers = directBuffers;
		}

		@Override
		public String toString() {
			return "{ name: " + name +
					", buffer size: " + bufferSize +
					", use direct buffers: " + directBuffers +
					" }";
		}
	}

}
