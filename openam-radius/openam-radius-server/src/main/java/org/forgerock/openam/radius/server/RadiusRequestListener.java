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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.server;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.guava.common.eventbus.EventBus;
import org.forgerock.openam.radius.server.config.ClientConfig;
import org.forgerock.openam.radius.server.config.RadiusServerConstants;
import org.forgerock.openam.radius.server.config.RadiusServiceConfig;
import org.forgerock.openam.radius.server.events.PacketDroppedSilentlyEvent;
import org.forgerock.openam.radius.server.events.PacketReceivedEvent;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Listens for incoming radius requests, validates they are for defined clients, drops packets that aren't, and queues
 * for handling those that are. If the listener is being shutdown then it accepts packets and drops them to drain any
 * buffered incoming requests while packets in process of being handled are polished off and can send their responses
 * through the backing channel. Then it closes the channel and exits.
 * <p/>
 */
public class RadiusRequestListener implements Runnable {

    private static final Debug LOG = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    /**
     * The configuration values for the Radius service pulled from OpenAM admin console constructs.
     */
    private volatile RadiusServiceConfig config;

    /**
     * The thread pool for handling requests.
     */
    // private final ThreadPoolExecutor pool;

    /**
     * Indicates if the listener was successfully started meaning it was able to bind to a listening data port and set
     * up its handling thread pool.
     */
    private volatile boolean startedSuccessfully = false;
    /**
     * Indicates to the daemon for this class embedded in its run method that the JVM or web app is shutting down and
     * thus the daemon should exit.
     */
    private volatile boolean terminated = false;
    /**
     * The datagram channel of this listener.
     */
    private DatagramChannel channel = null;

    /**
     * The thread instance that is running this listener's Runnable.
     */
    private volatile Thread listenerThread = null;

    /**
     * Service factory from which we may obtain an executor service that is automatically wired up to shutdown when the
     * shutdown listener event triggers.
     */
    private final ExecutorService executorService;

    /**
     * Issue events so that we can do things like audit events and JMXMonitoring etc.
     */
    private EventBus eventBus;

    /**
     * A factory that a <code>RadiusRequestHandler</code> may use to create <code>AccessRequestHandler</code> instances.
     */
    private AccessRequestHandlerFactory accessRequestHandlerFactory;

    /**
     * Construct listener, opens the DatagramChannel to receive requests, sets up the thread pool, and launches the
     * listener's thread which will capture the requests, drop unauthorized clients, and spool to the thread pool.
     *
     * @param config the configuration loaded from our admin console pages
     * @param executorService the thread pool executor to process radius requests.
     * @param eventBus may used to notify interested parties when events occur during the processing of radius events.
     * @param accessRequestHandlerFactory used to obtain access request handler classes for specific clients, as defined
     *            in the configuration.
     * @throws RadiusLifecycleException when the config is insufficient or invalid.
     */
    public RadiusRequestListener(final RadiusServiceConfig config,
            final ExecutorService executorService,
            final EventBus eventBus,
            final AccessRequestHandlerFactory accessRequestHandlerFactory)
            throws RadiusLifecycleException {
        LOG.warning("RADIUS service enabled. Starting Listener.");
        this.config = config;
        this.executorService = executorService;
        this.eventBus = eventBus;
        this.accessRequestHandlerFactory = accessRequestHandlerFactory;

        // lets get our inbound channel opened and bound
        try {
            this.channel = DatagramChannel.open();
            // ensure that we can re-open port immediately after shutdown when changing handlerConfig
            this.channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        } catch (final IOException e) {
            this.startedSuccessfully = false;
            throw new RadiusLifecycleException("RADIUS listener unable to open datagram channel.", e);
        }

        try {
            final int radiusPort = config.getPort();
            LOG.message("Starting RADIUS listener on port " + Integer.toString(radiusPort));
            this.channel.socket().bind(new InetSocketAddress(radiusPort));
        } catch (final SocketException e) {
            this.startedSuccessfully = false;
            throw new RadiusLifecycleException("RADIUS listener unable to bind to port " + config.getPort(), e);
        }

        // verify necessary resources are available that will prevent any handling if not found. Should never happen
        // but allows us to avoid propagating these exceptions in RadiusRequestContext.injectResponseAuthenticator()
        // (which uses these objects) back up to this level to terminate the listener. Alternatively, we could pass
        // this listener into the RadiusRequestHandler so that it can catch that exception and call the
        // listener's terminate method.
        try {
            MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RadiusLifecycleException("RADIUS listener unable to start due to missing required MD5 "
                    + "MessageDigest type.", e);
        }
        if (Charset.isSupported("UTF-8")) {
            try {
                Charset.forName("UTF-8");
            } catch (UnsupportedCharsetException e) {
                throw new RadiusLifecycleException("RADIUS listener unable to start due to missing required UTF-8 "
                        + "Charset.", e);
            }
        }

        // final DroppedRequestHandler dropsHandler = new DroppedRequestHandler();
        // final ThreadFactory fact = new RadiusThreadFactory();
        // final ExecutorServiceFactory esf = new ExecutorServiceFactory(ShutdownManager.getInstance());

        // pool = new ThreadPoolExecutor(poolCfg.getCoreThreads(), poolCfg.getMaxThreads(),
        // poolCfg.getKeepAliveSeconds(),
        // TimeUnit.SECONDS, queue, fact, dropsHandler);

        // now spin up our listener thread to feed the pool
        listenerThread = new Thread(this);
        listenerThread.setName(MessageFormat.format(RadiusServerConstants.LISTENER_THREAD_NAME, config.getPort()));
        listenerThread.setDaemon(true);
        listenerThread.start();
        this.startedSuccessfully = true;
    }

    /**
     * Indicates if the constructor successfully started up the listener.
     *
     * @return returns true if the listener successfully bound to a UDP port and started a thread pool or false
     *         otherwise.
     */
    public boolean isStartedSuccessfully() {
        return this.startedSuccessfully;
    }

    /**
     * Updates the configuration seen by this listener but should only be called when changes between the new
     * handlerConfig and the only are limited to changes in the set of defined clients. Any other change requires that
     * the listener be shutdown and possibly restarted.
     *
     * @param config
     *            the configuration loaded from the admin console pages.
     */
    public void updateConfig(RadiusServiceConfig config) {
        this.config = config;
    }

    /**
     * Blocking call that terminates the thread pool, tells the listener to drop any new requests, waits until the
     * thread pool is empty, and then interrupts the listener thread in case it is blocked waiting for new requests. We
     * must wait for the pool to empty before interrupting the listener thread since that closes the channel if the
     * thread is blocked on waiting for a new request and a closed channel then throws exceptions when any request
     * handlers in-progress attempt to send their responses to their clients.
     */
    public void terminate() {
        // tell listener to stop accepting requests if any come in while pool is shutting down
        this.terminated = true;

        // tell the pool to perform an orderly shutdown; stop accepting more work, but empty the work queue.
        executorService.shutdown();

        // now wait until the pool is finished
        boolean finished = false;
        boolean interrupted = false;

        // So we need to make a decision on how long to wait for the executor service to shutdown. 5 secs seems
        // reasonable.
        while (!finished) {
            try {
                LOG.warning("Waiting for RADIUS thread pool's request handler(s) to finish processing.");
                finished = executorService.awaitTermination(RadiusServerConstants.THREAD_POOL_SHUTDOWN_WAIT_SECONDS,
                        TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                LOG.warning("InterruptedException caught while waiting for executorService to terminate.");
                interrupted = true;
            }
        }
        // now that all in-process requests are finished with the channel we can interrupt the listener if it is still
        // around (like when it was waiting for more requests prior to termination and received none and needs to be
        // kicked out of receiving mode
        final Thread t = listenerThread;
        if (t != null) {
            t.interrupt();

            while (listenerThread != null) {
                LOG.warning("Waiting for RADIUS Listener to exit.");
                try {
                    Thread.sleep(200);
                } catch (final InterruptedException e) {
                    // ignore and wait for our listener thread to exit
                }
            }
        }
    }

    /**
     * Where the work gets done. :-) Blocks until packets are received, validates the source IP against configured
     * clients and drops packets accordingly, then spools valid ones to the thread pool for handling and goes back to
     * listening.
     */
    @Override
    public void run() {
        // Flag to hold interrupted state for returning after cleanup.
        boolean interrupted = false;

        dumpBannerToLog();

        while (!terminated && !interrupted) {
            try {
                // assure big-endian (network) byte order for our buffer
                final ByteBuffer bfr = ByteBuffer.allocate(RadiusServerConstants.MAX_PACKET_SIZE);
                bfr.order(ByteOrder.BIG_ENDIAN);
                InetSocketAddress iAddr = null;

                // see if we have a datagram packet waiting for us
                try {
                    iAddr = (InetSocketAddress) channel.receive(bfr);
                    if (iAddr == null) {
                        // no datagram was available, it happens, just go back to listening
                        LOG.message("DatagramChannel receive returned null. No datagram available.");
                        continue;
                    } else {
                        eventBus.post(new PacketReceivedEvent());
                    }
                } catch (final ClosedByInterruptException c) {
                    interrupted = true;
                    continue;
                } catch (final IOException e) {
                    LOG.warning("Exception Receiving RADIUS packet. Ignoring.", e);
                    continue;
                } catch (final SecurityException e) {
                    LOG.error("a security manager has been installed and it does not permit datagrams to be "
                            + " accepted from the datagram's sender. Ignoring", e);
                    continue;
                }
                // see if it is for a registered client
                final String ipAddr = iAddr.getAddress().toString();
                final ClientConfig clientConfig = config.findClient(ipAddr);

                if (clientConfig == null) {
                    LOG.warning("No Defined RADIUS Client matches IP address " + ipAddr + ". Dropping request.");
                    eventBus.post(new PacketDroppedSilentlyEvent());
                    continue;
                }
                if (!clientConfig.isClassIsValid()) {
                    LOG.warning("Declared Handler Class for Client '" + clientConfig.getName()
                            + "' is not valid. See earlier loading exception. Dropping request.");
                    eventBus.post(new PacketDroppedSilentlyEvent());
                    continue;
                }

                // prepare buffer for draining and queue up a handler
                bfr.flip();
                final RadiusRequestContext reqCtx = new RadiusRequestContext(clientConfig, channel, iAddr);

                final RadiusRequestHandler requestHandler = new RadiusRequestHandler(accessRequestHandlerFactory,
                        reqCtx, bfr, eventBus);

                executorService.execute(requestHandler);
            } catch (final Exception t) {
                LOG.error("Error receiving request.", t);
            }
        } // End of while loop

        // re-assert interrupted state if it occurred
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        try {
            // be sure that channel is closed
            channel.close();
        } catch (final Exception e) {
            LOG.error("Failed to close the Listener's UDP channel", e);
        }
        LOG.message("RADIUS Listener Exited.");
        this.listenerThread = null;
    }

    private void dumpBannerToLog() {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        pw.println("RADIUS Listener is Active.");
        pw.println("Port              : " + config.getPort());
        pw.println("Threads Core      : " + config.getThreadPoolConfig().getCoreThreads());
        pw.println("Threads Max       : " + config.getThreadPoolConfig().getMaxThreads());
        pw.println("Thread Keep-alive : " + config.getThreadPoolConfig().getKeepAliveSeconds() + " sec");
        pw.println("Request Queue     : " + config.getThreadPoolConfig().getQueueSize());
        pw.flush();

        LOG.message(sw.toString());
    }
}
