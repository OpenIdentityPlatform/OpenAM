/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AccessAccept.java,v 1.2 2008/06/25 05:42:00 qcheng Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package com.sun.identity.authentication.modules.radius.client;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.authentication.modules.radius.RADIUSServer;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.shared.debug.Debug;

import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.radius.common.AccessChallenge;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketFactory;
import org.forgerock.openam.radius.common.RequestAuthenticator;
import org.forgerock.openam.radius.common.StateAttribute;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.forgerock.openam.radius.common.UserPasswordAttribute;
import org.forgerock.openam.radius.common.packet.NASIPAddressAttribute;
import org.forgerock.openam.radius.common.packet.NASPortAttribute;

/**
 * This class implements RFC2865 - Remote Authentication Dial In User Service (RADIUS), June 2000.
 */
public class RadiusConn {
    /**
     * The default timeout.
     */
    public static final int DEFAULT_TIMEOUT = 5;
    /**
     * The client secret.
     */
    private String secret = null;
    /**
     * The data gram socket.
     */
    private DatagramSocket socket = null;
    /**
     * The source of id generation for access request packets.
     */
    private short id = (short) currentTimeMillis();
    /**
     * Source of random bytes.
     */
    private SecureRandom random = null;
    /**
     * The set of primary servers to connect to.
     */
    private Set<RADIUSServer> primaries;
    /**
     * The secondary set of servers to connect to if the primaries are offline.
     */
    private Set<RADIUSServer> secondaries;
    /**
     * The health check interval.
     */
    private int healthCheckInterval = 5;
    /**
     * The status of the various servers.
     */
    private static final Map<RADIUSServer, Boolean> SERVER_STATUS = new HashMap<RADIUSServer, Boolean>();
    /**
     * The Debug logger instance for this class.
     */
    private static Debug debug = Debug.getInstance("amAuthRadius");
    /**
     * The runnable task monitor that verifies connectivity to servers.
     */
    private static RADIUSMonitor serverMonitor;
    /**
     * An object used as an access lock.
     */
    private static final Object SERVER_MONITOR_LOCK = new Object();

    /**
     * Construct a connection object with a set of primary and seconary servers.
     *
     * @param primaries           the primary servers to connect to.
     * @param secondaries         the secondary servers to connect to.
     * @param secret              the secret shared between this client and the remote servers.
     * @param timeout             the timeout value.
     * @param healthCheckInterval the health check interval.
     * @throws SocketException thrown if unable to create a DatagramSocket.
     */
    public RadiusConn(Set<RADIUSServer> primaries, Set<RADIUSServer> secondaries,
                      String secret, int timeout, int healthCheckInterval) throws SocketException {
        this(primaries, secondaries, secret, timeout, null, healthCheckInterval);
    }

    /**
     * Constructs a connection object with only a set of primary servers.
     *
     * @param primaries           the primary servers to connect to.
     * @param secret              the secret shared between this client and the remote servers.
     * @param seed                the seed value to be used to create a {@link java.security.SecureRandom} instance.
     * @param healthCheckInterval the health check interval.
     * @throws SocketException thrown if unable to create a DatagramSocket.
     */
    public RadiusConn(Set<RADIUSServer> primaries, String secret, byte[] seed,
                      int healthCheckInterval) throws SocketException {
        this(primaries, primaries, secret, DEFAULT_TIMEOUT, seed, healthCheckInterval);
    }

    /**
     * Construct a connection object primary and secondary servers and seed for generating a {@link
     * java.security.SecureRandom}.
     *
     * @param primaries           the primary servers to connect to.
     * @param secondaries         the secondary servers to connect to.
     * @param secret              the secret shared between this client and the remote servers.
     * @param timeout             the timeout value.
     * @param seed                the seed value to be used to create a {@link java.security.SecureRandom} instance.
     * @param healthCheckInterval the health check interval.
     * @throws SocketException if a socket exception occurs.
     */
    public RadiusConn(Set<RADIUSServer> primaries, Set<RADIUSServer> secondaries,
                      String secret, int timeout, byte[] seed, int healthCheckInterval)
            throws SocketException {
        this.secret = secret;
        this.primaries = primaries;
        this.secondaries = secondaries;
        this.healthCheckInterval = healthCheckInterval;
        if (debug.messageEnabled()) {
            debug.message("Primary RADIUS servers: " + primaries);
            debug.message("Secondary RADIUS servers: " + secondaries);
        }
        socket = new DatagramSocket();
        socket.setSoTimeout(timeout * 1000);
        if (seed == null) {
            random = new SecureRandom();
        } else {
            random = new SecureRandom(seed);
        }
    }

    /**
     * Disconnects the underlying datagram socket.
     *
     * @throws IOException if an io exception occurs.
     */
    public void disconnect() throws IOException {
        socket.disconnect();
    }

    /**
     * Authenticates the username and password against the remote servers.
     *
     * @param name     the username.
     * @param password the password.
     * @throws IOException              if there is a problem.
     * @throws NoSuchAlgorithmException if there is a problem.
     * @throws RejectException          if there is a problem.
     * @throws ChallengeException       if there is a problem.
     */
    public void authenticate(String name, String password)
            throws IOException, NoSuchAlgorithmException,
            RejectException, ChallengeException {
        AccessRequest req = createAccessRequest();
        req.addAttribute(new UserNameAttribute(name));
        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
                secret, password));
        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
        req.addAttribute(new NASPortAttribute(socket.getLocalPort()));
        sendPacket(req);
    }

    /**
     * Sends an access-request to the server in response to a challenge request.
     *
     * @param name     the username.
     * @param password the password.
     * @param ce       the challenge exception providing access to the original challenge response.
     * @throws IOException              if there is a problem.
     * @throws NoSuchAlgorithmException if there is a problem.
     * @throws RejectException          if there is a problem.
     * @throws ChallengeException       if there is a problem.
     */
    public void replyChallenge(String name, String password,
                               ChallengeException ce) throws IOException, NoSuchAlgorithmException,
            RejectException, ChallengeException {
        StateAttribute state = (StateAttribute)
                ce.getAttributeSet().getAttributeByType(AttributeType.STATE);
        if (state == null) {
            throw new IOException("State not found in challenge");
        }
        AccessRequest req = createAccessRequest();
        req.addAttribute(state); // needed in challenge
        if (name != null) {
            req.addAttribute(new UserNameAttribute(name));
        }
        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
                secret, password));
        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
        req.addAttribute(new NASPortAttribute(socket.getLocalPort()));

        sendPacket(req);
    }

    /**
     * Finds an available server and then sends a packet to that servers.
     *
     * @param packet the packet.
     * @throws IOException        if there is a problem.
     * @throws RejectException    if there is a problem.
     * @throws ChallengeException if there is a problem.
     */
    private void sendPacket(Packet packet) throws IOException,
            RejectException, ChallengeException {
        Packet res = null;
        RADIUSServer server = null;
        while (res == null) {
            server = getOnlineServer();
            if (debug.messageEnabled()) {
                debug.message("Using " + server + " for contact RADIUS");
            }
            try {
                send(packet, server);
                res = receive();
                if (res instanceof AccessReject) {
                    throw new RejectException((AccessReject) res);
                } else if (res instanceof AccessChallenge) {
                    throw new ChallengeException((AccessChallenge) res);
                }
            } catch (IOException ioe) {
                if (ioe instanceof ConnectException || ioe instanceof SocketTimeoutException) {
                    if (debug.messageEnabled()) {
                        debug.message("Moving server to offline state - " + server);
                    }
                    synchronized (SERVER_STATUS) {
                        SERVER_STATUS.put(server, Boolean.FALSE);
                    }
                    synchronized (SERVER_MONITOR_LOCK) {
                        if (serverMonitor == null || serverMonitor.scheduledExecutionTime() == -1) {
                            serverMonitor = new RADIUSMonitor();
                            SystemTimer.getTimer().schedule(serverMonitor,
                                    new Date(((currentTimeMillis()) / 1000) * 1000));
                        }
                    }
                } else {
                    throw ioe;
                }
            }
        }
    }

    /**
     * Returns a server that has successfully been contacted.
     *
     * @return the server to connect to.
     */
    private RADIUSServer getOnlineServer() {
        synchronized (SERVER_STATUS) {
            for (RADIUSServer server : primaries) {
                Boolean state = SERVER_STATUS.get(server);
                if (state == null) {
                    SERVER_STATUS.put(server, Boolean.TRUE);
                    return server;
                } else if (state) {
                    return server;
                }
            }
            for (RADIUSServer server : secondaries) {
                Boolean state = SERVER_STATUS.get(server);
                if (state == null) {
                    SERVER_STATUS.put(server, Boolean.TRUE);
                    return server;
                } else if (state) {
                    return server;
                }
            }
        }
        return null;
    }

    /**
     * Generates a new access request packet identifier.
     *
     * @return the new identifier.
     */
    private short getIdentifier() {
        return id++;
    }

    /**
     * Sends the packet.
     *
     * @param packet the packet.
     * @param server the target server.
     * @throws IOException
     */
    private void send(Packet packet, RADIUSServer server)
            throws IOException {
        if (server == null) {
            throw new IOException("No RADIUS server is online.");
        }
        byte[] buffer = new byte[4096];
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
        dp.setPort(server.getPort());
        dp.setAddress(InetAddress.getByName(server.getHost()));
        byte[] data = packet.getOctets();
        dp.setLength(data.length);
        dp.setData(data);
        socket.send(dp);
        if (debug.messageEnabled()) {
            debug.message("Sent " + packet);
        }
    }

    /**
     * Blocking call that waits until a response packet is received.
     *
     * @return the received packet.
     * @throws IOException
     */
    private Packet receive()
            throws IOException {
        byte[] buffer = new byte[4096];
        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
        socket.receive(dp);
        byte[] data = dp.getData();
        Packet p = PacketFactory.toPacket(data);
        if (debug.messageEnabled()) {
            debug.message("Received " + p + " size=" + p.getAttributeSet().size());
        }
        return p;
    }

    /**
     * Generates an access request packet.
     *
     * @return the access request packet.
     * @throws NoSuchAlgorithmException
     */
    private AccessRequest createAccessRequest() throws
            NoSuchAlgorithmException {
        RequestAuthenticator ra = new RequestAuthenticator(random, secret);
        AccessRequest req = new AccessRequest(getIdentifier(), ra);
        return req;
    }

    /**
     * A monitor that periodically checks for currently available remote servers to connect to.
     */
    private class RADIUSMonitor extends GeneralTaskRunnable {
        /**
         * Indicates if monitoring is cancelled.
         */
        private boolean cancelled = false;

        /**
         * Not implemented in this monitor. Always returns false.
         *
         * @param key unused parameter.
         * @return always returns false.
         */
        public boolean addElement(Object key) {
            return false;
        }

        /**
         * Not implemented in this monitor. Always returns false.
         *
         * @param key unused parameter.
         * @return always returns false.
         */
        public boolean removeElement(Object key) {
            return false;
        }

        /**
         * Always returns true.
         *
         * @return always returns true.
         */
        public boolean isEmpty() {
            return true;
        }

        /**
         * Returns the run period of this TaskRunnable.
         *
         * @return A long value to indicate the run period.
         */
        public long getRunPeriod() {
            return cancelled ? 0 : healthCheckInterval * 60000;
        }

        /**
         * Scans through the set of servers testing connectivity and marking the servers accordingly.
         */
        public void run() {
            Map<RADIUSServer, Boolean> tmp;
            if (debug.messageEnabled()) {
                debug.message("Checking server statuses");
            }
            synchronized (SERVER_STATUS) {
                tmp = new LinkedHashMap<RADIUSServer, Boolean>(SERVER_STATUS);
            }
            DatagramSocket testSocket = null;
            int offline = 0;
            for (Map.Entry<RADIUSServer, Boolean> entry : tmp.entrySet()) {
                RADIUSServer server = entry.getKey();
                if (!entry.getValue()) {
                    offline++;
                    try {
                        testSocket = new DatagramSocket();
                        testSocket.setSoTimeout(socket.getSoTimeout());
                        AccessRequest req = createAccessRequest();
                        req.addAttribute(new UserNameAttribute("nonexistent"));
                        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
                                secret, "invalidpass"));
                        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
                        req.addAttribute(new NASPortAttribute(socket.getLocalPort()));
                        byte[] buffer = new byte[4096];
                        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                        dp.setPort(server.getPort());
                        dp.setAddress(InetAddress.getByName(server.getHost()));
                        byte[] data = req.getOctets();
                        dp.setLength(data.length);
                        dp.setData(data);
                        testSocket.send(dp);
                        byte[] buffer2 = new byte[4096];
                        dp = new DatagramPacket(buffer2, buffer2.length);
                        testSocket.receive(dp);
                        dp.getData();
                        if (debug.messageEnabled()) {
                            debug.message("Moving server to online state - " + server);
                        }
                        synchronized (SERVER_STATUS) {
                            SERVER_STATUS.put(server, Boolean.TRUE);
                        }
                        offline--;
                    } catch (Exception ex) {
                        if (debug.messageEnabled()) {
                            debug.message("Exception occured while checking RADIUS server status: " + ex.getMessage());
                        }
                        //server is still unavailable
                    } finally {
                        if (testSocket != null) {
                            testSocket.disconnect();
                        }
                    }
                }
            }
            if (offline == 0) {
                cancelled = true;
            }
        }
    }
}
