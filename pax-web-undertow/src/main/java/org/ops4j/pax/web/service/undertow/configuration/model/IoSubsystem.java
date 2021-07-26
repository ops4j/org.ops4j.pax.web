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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import static org.ops4j.pax.web.service.undertow.configuration.model.ObjectFactory.NS_IO;

@XmlType(name = "io-subsystemType", namespace = NS_IO, propOrder = {
		"workers",
		"bufferPools"
})
public class IoSubsystem {

	@XmlElement(name = "worker")
	private final List<Worker> workers = new ArrayList<>();

	@XmlElement(name = "buffer-pool")
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

	@XmlType(name = "workerType", namespace = NS_IO)
	public static class Worker {
		//<xs:element name="outbound-bind-address" type="outboundBindAddressType"/> 0:N
		@XmlAttribute
		private String name;
		@XmlAttribute(name = "io-threads")
		private int ioThreads = Math.max(Runtime.getRuntime().availableProcessors(), 2);
		@XmlAttribute(name = "task-keepalive")
		private int taskKeepalive = 60000;
		@XmlAttribute(name = "task-core-threads")
		private int taskCoreThreads = Math.max(Runtime.getRuntime().availableProcessors(), 2) * 8;
		@XmlAttribute(name = "task-max-threads")
		private int taskMaxThreads = Math.max(Runtime.getRuntime().availableProcessors(), 2) * 8;
		@XmlAttribute(name = "stack-size")
		private long stackSize = 0;

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

	@XmlType(name = "bufferPoolType", namespace = NS_IO)
	public static class BufferPool {
		@XmlAttribute
		private String name;
		@XmlAttribute(name = "buffer-size")
		private Integer bufferSize;
		//<xs:attribute name="buffers-per-slice" use="optional" type="xs:int">
		@XmlAttribute(name = "direct-buffers")
		private boolean directBuffers = true;

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
