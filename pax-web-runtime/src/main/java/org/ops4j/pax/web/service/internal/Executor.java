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
package org.ops4j.pax.web.service.internal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Executor
{

    private final Object lock = new Object();
    private final LinkedList jobs = new LinkedList();
    private Worker worker;

    public static class Future
    {
        boolean done;
        Runnable runnable;
        final Object lock = new Object();

        Future(Runnable runnable)
        {
            this.runnable = runnable;
        }

        void run()
        {
            try
            {
                runnable.run();
            }
            finally
            {
                synchronized (lock)
                {
                    done = true;
                    lock.notifyAll();
                }
            }
        }

        public void await() throws InterruptedException
        {
            synchronized (lock)
            {
                if (!done)
                {
                    lock.wait();
                }
            }
        }
    }

    public Future submit(Runnable runnable)
    {
        synchronized (lock)
        {
            Future future = new Future(runnable);
            jobs.add(future);
            if (worker == null)
            {
                worker = new Worker();
                worker.start();
            }
            return future;
        }
    }

    public void shutdown() throws InterruptedException
    {
        synchronized (lock)
        {
            lock.notifyAll();
            while (!jobs.isEmpty())
            {
                lock.wait();
            }
        }
    }

    private class Worker extends Thread
    {
        private Worker()
        {
            super("Pax Web Runtime worker");
        }

        public void run()
        {
            for (;;)
            {
                Future job;
                synchronized (lock)
                {
                    if (!jobs.isEmpty())
                    {
                        job = (Future) jobs.getFirst();
                    }
                    else
                    {
                        worker = null;
                        break;
                    }
                }
                try
                {
                    job.run();
                }
                catch (Throwable t)
                {
                    t.printStackTrace();
                }
                synchronized (lock)
                {
                    jobs.removeFirst();
                    lock.notifyAll();
                }
            }
        }
    }
}

