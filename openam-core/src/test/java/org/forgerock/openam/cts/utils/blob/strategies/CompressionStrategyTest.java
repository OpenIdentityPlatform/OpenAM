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
 * Copyright 2013-2016 ForgeRock AS.
 */
package org.forgerock.openam.cts.utils.blob.strategies;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.AtomicHistogram;
import org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.openam.utils.Time.*;

public class CompressionStrategyTest {
    private CompressionStrategy compression;
    private byte[] data;

    private static final String JSON_SAMPLE = "{\"clientDomain\":\"dc=openam,dc=openidentityplatform,dc=org\",\"" +
            "clientID\":\"id=amadmin,ou=user,dc=openam,dc=openidentityplatform,dc=org\",\"cookieMode\":null,\"" +
            "cookieStr\":null,\"creationTime\":1375353841,\"isISStored\":true,\"latestAccessTime\":" +
            "1375353841,\"maxCachingTime\":3,\"maxIdleTime\":30,\"maxSessionTime\":120,\"" +
            "reschedulePossible\":false,\"restrictedTokensByRestriction\":{},\"restrictedTokensBySid\":" +
            "{},\"sessionEventURLs\":{\"http://rwapshott.forgerock.com:8080/openam/notificationservice" +
            "\":[{\"comingFromAuth\":false,\"cookieMode\":null,\"encryptedString\":\"" +
            "AQIC5wM2LY4SfcxjU9TuISV5pcZVBhh8fA2kRtHPX065uzE.*AAJTSQACMDIAAlNLABM4NjE3NjM5MTc2NTIyMzc" +
            "3Mzg1AAJTMQACMDE.*\",\"extensionPart\":null,\"extensions\":{},\"isParsed\":false,\"session" +
            "Domain\":\"\",\"sessionServer\":\"\",\"sessionServerID\":\"\",\"sessionServerPort\":\"\",\"s" +
            "essionServerProtocol\":\"\",\"sessionServerURI\":\"\",\"tail\":null}]},\"sessionHandle\":\"" +
            "shandle:AQIC5wM2LY4Sfcx3QShvJQovWxXLo4HeN8INGNzJ0ObVPs0.*AAJTSQACMDIAAlMxAAIwMQACU0sAEzg2MT" +
            "c2MzkxNzY1MjIzNzczODU.*\",\"sessionID\":{\"comingFromAuth\":false,\"cookieMode\":null,\"enc" +
            "ryptedString\":\"AQIC5wM2LY4SfcxjU9TuISV5pcZVBhh8fA2kRtHPX065uzE.*AAJTSQACMDIAAlNLABM4NjE3Nj" +
            "M5MTc2NTIyMzc3Mzg1AAJTMQACMDE.*\",\"extensionPart\":\"AAJTSQACMDIAAlNLABM4NjE3NjM5MTc2NTIyMz" +
            "c3Mzg1AAJTMQACMDE=\",\"extensions\":{\"SI\":\"02\",\"S1\":\"01\",\"SK\":\"86176391765223773" +
            "85\"},\"isParsed\":true,\"sessionDomain\":\"dc=openam,dc=openidentityplatform,dc=org\",\"sessionServer" +
            "\":\"rwapshott.forgerock.com\",\"sessionServerID\":\"02\",\"sessionServerPort\":\"8080\",\"s" +
            "essionServerProtocol\":\"http\",\"sessionServerURI\":\"/openam\",\"tail\":\"\"},\"sessionPro" +
            "perties\":{\"CharSet\":\"UTF-8\",\"UserId\":\"amadmin\",\"FullLoginURL\":\"/openam/UI/Logi" +
            "n\",\"successURL\":\"/openam/console\",\"cookieSupport\":\"true\",\"AuthLevel\":\"0\",\"Sessi" +
            "onHandle\":\"shandle:AQIC5wM2LY4Sfcyn6TUnRk0cPYbywbMa5eHp3KodJFMuh08.*AAJTSQACMDIAAlMxAAIwMQA" +
            "CU0sAFC0yOTc3NjI0NjQ4NDYyODA4NDk0*\",\"UserToken\":\"amadmin\",\"loginURL\":\"/openam/UI/Logi" +
            "n\",\"Principals\":\"amadmin\",\"Service\":\"ldapService\",\"sun.am.UniversalIdentifier\":\"i" +
            "d=amadmin,ou=user,dc=openam,dc=openidentityplatform,dc=org\",\"amlbcookie\":\"01\",\"Organization\":\"dc" +
            "=openam,dc=openidentityplatform,dc=org\",\"Locale\":\"en_US\",\"HostName\":\"172.16.100.130\",\"AuthType" +
            "\":\"DataStore\",\"Host\":\"172.16.100.130\",\"UserProfile\":\"Required\",\"clientType\":\"ge" +
            "nericHTML\",\"AMCtxId\":\"70cb377418240f5601\",\"authInstant\":\"2013-08-01T10:44:01Z\",\"Pri" +
            "ncipal\":\"id=amadmin,ou=user,dc=openam,dc=openidentityplatform,dc=org\"},\"sessionState\":1,\"sessionTyp" +
            "e\":0,\"timedOutAt\":0,\"uuid\":\"id=amadmin,ou=user,dc=openam,dc=openidentityplatform,dc=org\",\"version" +
            "\":0,\"willExpireFlag\":true}";

    @BeforeMethod
    public void setUp() throws Exception {
        data = JSON_SAMPLE.getBytes();
        compression = new CompressionStrategy();
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldRejectIfNullBlobOnPerform() throws TokenStrategyFailedException {
        compression.perform(null);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldRejectIfNullBlobOnReverse() throws TokenStrategyFailedException {
        compression.reverse(null);
    }

    @Test
    public void shouldCompressContents() throws TokenStrategyFailedException {
        assertThat(compression.perform(data).length).isLessThan(data.length);
    }

    @Test
    public void shouldDecompressCompressedContents() throws TokenStrategyFailedException {
        assertThat(compression.reverse(compression.perform(data))).isEqualTo(data);
    }

    @DataProvider
    public Object[][] numThreads() {
        return new Object[][]{
                { 1 },
                { 2 },
                { 5 },
                { 10 },
                { 25 },
                { 50 },
                { 100 }
        };
    }

    /**
     * Tests performance of CompressionStrategy as a factor of the number of threads.
     * Disabled by default to avoid slowing down the build.
     *
     * @param numThreads the number of threads to concurrently hammer the CompressionStrategy.
     */
    @Test(dataProvider = "numThreads", enabled = false)
    public void testThroughPut(int numThreads) throws Exception {
        final int TOTAL_ROUNDS = 100000;
        final int roundsPerThread = TOTAL_ROUNDS / numThreads;
        // Given
        final Set<Throwable> errors = Collections.newSetFromMap(new ConcurrentHashMap<Throwable, Boolean>());
        final MonitoredCompressionStrategy strategy = new MonitoredCompressionStrategy();
        final Executor executor = Executors.newFixedThreadPool(numThreads);

        final byte[] dataToCompress = JSON_SAMPLE.getBytes(Charset.forName("UTF-8"));
        final CyclicBarrier barrier = new CyclicBarrier(numThreads + 1);

        // When
        for (int i = 0; i < numThreads; ++i) {
            executor.execute(new CompressionTask(barrier, dataToCompress, strategy, errors, roundsPerThread));
        }
        // Wait for start
        barrier.await();
        // Warmup
        barrier.await();
        strategy.reset();
        barrier.await();
        // Actual test
        barrier.await();

        // Then
        assertThat(errors).isEmpty();
        strategy.printStats(System.out);

        // See http://hdrhistogram.github.io/HdrHistogram/plotFiles.html
//        final PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream
//                (String.format(Locale.US, "/Users/neilmadden/Desktop/CompressionStrategy-%03d.hgrm", numThreads))));
//        strategy.printStats(out);
//        out.flush();
//        out.close();
    }

    /**
     * Benchmarking task that compresses and uncompresses some data in a tight loop.
     */
    private static class CompressionTask implements Runnable {
        private static final int WARMUP_ROUNDS = 1000;
        private final CyclicBarrier barrier;
        private final byte[] dataToCompress;
        private final CompressionStrategy strategy;
        private final Collection<Throwable> errors;
        private final int rounds;

        CompressionTask(final CyclicBarrier barrier, final byte[] dataToCompress, final CompressionStrategy strategy,
                final Collection<Throwable> errors, final int rounds) {
            this.barrier = barrier;
            this.dataToCompress = dataToCompress;
            this.strategy = strategy;
            this.errors = errors;
            this.rounds = rounds;
        }

        @Override
        public void run() {
            try {
                // Wait for start
                barrier.await();

                for (int i = 0; i < WARMUP_ROUNDS; ++i) {
                    final byte[] compressed = strategy.perform(dataToCompress);
                    assertThat(compressed.length).isLessThan(dataToCompress.length);
                    final byte[] decompressed = strategy.reverse(compressed);
                    assertThat(decompressed).isEqualTo(dataToCompress);
                }

                barrier.await();
                // Wait for stats to be reset
                barrier.await();

                // Try to thwart any attempt by the JIT to eliminate redundant code:
                boolean b = true;
                for (int i = 0; i < rounds; ++i) {
                    final byte[] compressed = strategy.perform(dataToCompress);
                    final byte[] decompressed = strategy.reverse(compressed);
                    b &= decompressed.length == dataToCompress.length;
                }

                barrier.await();

                assertThat(b).isTrue();

            } catch (Exception ex) {
                errors.add(ex);
            }
        }
    }

    private static final class MonitoredCompressionStrategy extends CompressionStrategy {
        // Histograms for storing millisecond precision timings: max=10000 (10 seconds), 5 significant digits
        // Should be slightly less than 2MB total storage per Histogram
        private final AtomicHistogram performSamples = new AtomicHistogram(10000, 5);
        private final AtomicHistogram reverseSamples = new AtomicHistogram(10000, 5);

        @Override
        public byte[] perform(final byte[] data) throws TokenStrategyFailedException {
            // Cannot use System.nanoTime() as it gives invalid results if thread gets scheduled to
            // a different CPU during the test (highly likely in the large thread count tests).
            final long start = currentTimeMillis();
            try {
                return super.perform(data);
            } finally {
                final long end = currentTimeMillis();
                performSamples.recordValue(end - start);
            }
        }

        @Override
        public byte[] reverse(final byte[] data) throws TokenStrategyFailedException {
            final long start = currentTimeMillis();
            try {
                return super.reverse(data);
            } finally {
                final long end = currentTimeMillis();
                reverseSamples.recordValue(end - start);
            }
        }

        void reset() {
            performSamples.reset();
            reverseSamples.reset();
        }

        void printStats(PrintStream out, AbstractHistogram histogram) {
            histogram.outputPercentileDistribution(out, 1.0);
        }

        void printStats(PrintStream out) throws InterruptedException {
            //out.println("Perform:");
            printStats(out, performSamples);
            //out.println("Reverse:");
            //printStats(out, reverseSamples);
            //out.flush();
            Thread.sleep(50l);
        }
    }
}
