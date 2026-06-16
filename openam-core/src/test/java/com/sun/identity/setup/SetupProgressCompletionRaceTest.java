/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems LLC.
 */
package com.sun.identity.setup;

import static org.testng.Assert.assertTrue;

import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.config.SetupWriter;

/**
 * Regression test for the wizard setup-progress completion race.
 *
 * <p>The configuration wizard streams install progress to the browser through a long-lived progress
 * iframe ({@code SetSetupProgress.doGet}): it registers a {@link SetupWriter} via
 * {@link SetupProgress#setWriter} and blocks in {@link SetupWriter#realFlush()} until the install
 * calls {@link SetupProgress#closeOutputStream()}. On a fast install (e.g. against an external
 * directory) the install can finish - and close the stream - <em>before</em> the iframe attaches its
 * writer. Pre-fix, that late {@code realFlush()} then waited forever for a {@code close()} that had
 * already happened, the iframe never completed and the wizard's "proceed to console" link never
 * appeared (a ~20 minute setup hang reproduced across JDK 11/17/26 on CI).
 *
 * <p>These tests exercise the race deterministically at the unit level - no servlet container - by
 * driving {@code closeOutputStream()} and {@code setWriter()} in the problematic order.
 */
public class SetupProgressCompletionRaceTest {

    @BeforeMethod
    public void reset() {
        SetupProgress.setWriter(null);
        SetupProgress.resetCompletion();
    }

    @AfterMethod
    public void cleanup() {
        SetupProgress.setWriter(null);
        SetupProgress.resetCompletion();
    }

    /**
     * The bug: configuration completes (closeOutputStream) BEFORE the progress iframe attaches its
     * writer. {@code realFlush()} must still return promptly instead of blocking forever.
     */
    @Test(timeOut = 30000)
    public void completionBeforeWriterAttach_realFlushDoesNotHang() throws Exception {
        // Install finished and closed the progress stream before the iframe registered its writer.
        SetupProgress.closeOutputStream();

        final SetupWriter writer = new SetupWriter(new StringWriter());
        SetupProgress.setWriter(writer);

        assertTrue(realFlushReturnsWithin(writer, 10, TimeUnit.SECONDS),
                "SetupWriter.realFlush() must return promptly when configuration completed before "
                        + "the progress stream attached; otherwise the wizard progress iframe hangs.");
    }

    /**
     * Normal ordering guard: the iframe attaches first (realFlush blocks), then configuration
     * completes - realFlush must unblock and return as it always did.
     */
    @Test(timeOut = 30000)
    public void writerAttachThenCompletion_realFlushReturns() throws Exception {
        final SetupWriter writer = new SetupWriter(new StringWriter());
        SetupProgress.setWriter(writer);

        final CountDownLatch returned = new CountDownLatch(1);
        final Thread t = new Thread(() -> {
            writer.realFlush();
            returned.countDown();
        }, "realFlush-normal");
        t.setDaemon(true);
        t.start();

        // Give realFlush a moment to block in wait(), then signal completion.
        Thread.sleep(250);
        SetupProgress.closeOutputStream();

        assertTrue(returned.await(10, TimeUnit.SECONDS),
                "SetupWriter.realFlush() must return once closeOutputStream() is called.");
    }

    private static boolean realFlushReturnsWithin(SetupWriter writer, long timeout, TimeUnit unit)
            throws InterruptedException {
        final CountDownLatch returned = new CountDownLatch(1);
        final Thread t = new Thread(() -> {
            writer.realFlush();
            returned.countDown();
        }, "realFlush-race");
        t.setDaemon(true);
        t.start();
        return returned.await(timeout, unit);
    }
}
