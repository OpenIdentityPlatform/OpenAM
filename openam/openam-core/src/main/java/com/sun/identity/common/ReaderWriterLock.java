/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: ReaderWriterLock.java,v 1.2 2008/06/25 05:42:26 qcheng Exp $
 *
 */

package com.sun.identity.common;

import java.util.LinkedList;

/**
 * This reader/writer lock prevents reads from occurring while writes are in
 * progress, and it also prevents multiple writes from happening simultaneously.
 * Multiple read operations can run in parallel, however. Reads take priority
 * over writes, so any read operations that are pending while a write is in
 * progress will execute before any subsequent writes execute. Writes are
 * guaranteed to execute in the order in which they were requested -- the oldest
 * request is processed first.
 * 
 * You should use the lock as follows: public class Data_structure_or_resource {
 * ReaderWriterLock lock = new ReaderWriterLock(); public void access() { try{
 * lock.readRequest(); // do the read/access operation here. }finally {
 * lock.readDone(); } }
 * 
 * public void modify( ) { try { lock.writeRequest(); // do the write/modify
 * operation here. } finally { lock.writeDone(); } }
 */

public class ReaderWriterLock {
    private int currentReaders;

    private int queuedReaders;

    private int currentWriters;

    /**
     * Keep a linked list of writers waiting for access so that I can release
     * them in the order that the requests were received. The size of this list
     * is the "waiting writers" count. Note that the monitor of the
     * ReaderWriterLock object itself is used to lock out readers while writes
     * are in progress, thus there's no need for a separate "reader_lock."
     */
    private final LinkedList writerLocks = new LinkedList();

    /**
     * Request the read lock. Block until a read operation can be performed
     * safely. This call must be followed by a call to readDone() when the read
     * operation completes.
     */
    public synchronized void readRequest() {
        if (currentWriters == 0 && writerLocks.size() == 0) {
            ++currentReaders;
        } else {
            ++queuedReaders;
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * This version of read() requests read access and returns true if you get
     * it. If it returns false, you may not safely read from the guarded
     * resource. If it returns true, you should do the read, then call readDone
     * in the normal way. Here's an example: public void read() { if(
     * lock.readRequestImmediate() ) { try { // do the read operation here }
     * finally { lock.readDone(); } }else { // couldn't read safely. }
     */

    public synchronized boolean readRequestImmediate() {
        if (currentWriters == 0 && writerLocks.size() == 0) {
            ++currentReaders;
            return true;
        }
        return false;
    }

    /**
     * Release the lock. You must call this method when you're done with the
     * read operation.
     */
    public synchronized void readDone() {
        if (--currentReaders == 0) {
            notify_writers();
        }
    }

    /**
     * Request the write lock. Block until a write operation can be performed
     * safely. Write requests are guaranteed to be executed in the order
     * received. Pending read requests take precedence over all write requests.
     * This call must be followed by a call to writeDone() when the write
     * operation completes.
     */
    public void writeRequest() {
        // This method can't be synchronized or there'd be a nested-monitor
        // lockout problem: We have to acquire the lock for "this" in
        // order to modify the fields, but that lock must be released
        // before we start waiting for a safe time to do the writing.
        // If writeRequest() were synchronized, we'd be holding
        // the monitor on the ReaderWriterLock object while we were
        // waiting. Since the only way to be released from the wait is
        // for someone to call either readDone()
        // or writeDone() (both of which are synchronized),
        // there would be no way for the wait to terminate.

        Object lock = new Object();
        synchronized (lock) {
            synchronized (this) {
                boolean goAheadWithWrite = writerLocks.size() == 0
                        && currentReaders == 0 && currentWriters == 0;
                if (goAheadWithWrite) {
                    ++currentWriters;
                    return; // the "return" jumps over the "wait" call
                }
                writerLocks.addLast(lock);
            }
            try {
                lock.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * This version of the write request returns false immediately (without
     * blocking) if any read or write operations are in progress and a write
     * isn't safe; otherwise, it returns true and acquires the resource. Use it
     * like this: public void write() { 
     * if( lock.writeRequestImmediate() ) { try { 
     * //
     * do the write operation here } finally { lock.writeDone(); } }else { //
     * couldn't write safely. } }
     */
    synchronized public boolean writeRequestImmediate() {
        if (writerLocks.size() == 0 && currentReaders == 0
                && currentWriters == 0) {
            ++currentWriters;
            return true;
        }
        return false;
    }

    /**
     * Release the lock. You must call this method when you're done with the
     * read operation.
     */
    public synchronized void writeDone() {
        // The logic here is more complicated than it appears.
        // If readers have priority, you'll notify them. As they
        // finish up, they'll call readDone(), one at
        // a time. When they're all done, readDone() will
        // notify the next writer. If no readers are waiting, then
        // just notify the writer directly.

        --currentWriters;
        if (queuedReaders > 0) // priority to waiting readers
            notify_readers();
        else
            notify_writers();
    }

    /**
     * Notify all the threads that have been waiting to read.
     */

    private void notify_readers() {
        // must be accessed from a synchronized method
        currentReaders += queuedReaders;
        queuedReaders = 0;
        notifyAll();
    }

    /**
     * Notify the writing thread that has been waiting the longest.
     */
    private void notify_writers() {
        // must be accessed from a synchronized method
        if (writerLocks.size() > 0) {
            Object oldest = writerLocks.removeFirst();
            ++currentWriters;
            synchronized (oldest) {
                oldest.notify();
            }
        }
    }

    /**
     * The Test class is a unit test for the other code in the current file. Run
     * the test with: java com.holub.asynch.ReaderWriterLock\$Test
     * 
     * (the backslash isn't required with windows boxes), and don't include this
     * class file in your final distribution. The output could vary in trivial
     * ways, depending on system timing. The read/write order should be exactly
     * the same as in the following sample:
     * 
     * Starting w/0 w/0 writing Starting r/1 Starting w/1 Starting w/2 Starting
     * r/2 Starting r/3 w/0 done Stopping w/0 r/1 reading r/2 reading r/3
     * reading r/1 done Stopping r/1 r/2 done r/3 done Stopping r/2 Stopping r/3
     * w/1 writing w/1 done Stopping w/1 w/2 writing w/2 done Stopping w/2
     */

    public static class Test {
        Resource resource = new Resource();

        /**
         * The Resource class simulates a simple locked resource. The read
         * operation simply pauses for .1 seconds. The write operation (which is
         * typically higher overhead) pauses for .5 seconds. Note that the use
         * of try...finally is not critical in the current test, but it's good
         * style to always release the lock in a finally block in real code.
         */
        static class Resource {
            ReaderWriterLock lock = new ReaderWriterLock();

            public void read(String reader) {
                try {
                    lock.readRequest();
                    System.out.println("\t\t" + reader + " reading");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    System.out.println("\t\t" + reader + " done");
                } finally {
                    lock.readDone();
                }
            }

            public void write(String writer) {
                try {
                    lock.writeRequest();
                    System.out.println("\t\t" + writer + " writing");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    System.out.println("\t\t" + writer + " done");
                } finally {
                    lock.writeDone();
                }
            }

            public boolean read_if_possible() {
                if (lock.readRequestImmediate()) {
                    // in the real world, you'd actually do the read here
                    lock.readDone();
                    return true;
                }
                return false;
            }

            public boolean write_if_possible() {
                if (lock.writeRequestImmediate()) {
                    // in the real world, you'd actually do the write here
                    lock.writeDone();
                    return true;
                }
                return false;
            }
        }

        /**
         * A simple reader thread. Just reads from the resource, passing it a
         * unique string id.
         */
        class Reader extends Thread {
            private String name;

            Reader(String name) {
                this.name = name;
            }

            public void run() {
                System.out.println("Starting " + name);
                resource.read(name);
                System.out.println("Stopping " + name);
            }
        }

        /**
         * A simple writer thread. Just writes to the resource, passing it a
         * unique string id.
         */
        class Writer extends Thread {
            private String name;

            Writer(String name) {
                this.name = name;
            }

            public void run() {
                System.out.println("Starting " + name);
                resource.write(name);
                System.out.println("Stopping " + name);
            }
        }

        /**
         * Test by creating several readers and writers. The initial write
         * operation (w/0) should complete before the first read (r/1) runs.
         * Since readers have priority, r/2 and r/3 should run before w/1; and
         * r/1, r/2 and r3 should all run in parallel. When all three reads
         * complete, w1 and w2 should execute sequentially in that order.
         */

        public Test() {
            if (!resource.read_if_possible()) {
                System.out.println("Immediate read request didn't work");
            }
            if (!resource.write_if_possible()) {
                System.out.println("Immediate write request didn't work");
            }

            new Writer("w/0").start();
            new Reader("r/1").start();
            new Writer("w/1").start();
            new Writer("w/2").start();
            new Reader("r/2").start();
            new Reader("r/3").start();
        }

        static public void main(String[] args) {
            new Test();
        }
    }
}
