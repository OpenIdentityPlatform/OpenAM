/**
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
 * $Id: RadiusConn.java,v 1.2 2008/06/25 05:42:02 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.authentication.modules.radius.client;

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

/**
 * This class implements RFC2865 - Remote Authentication Dial In
 * User Service (RADIUS), June 2000.
 */
public class RadiusConn {
    public static final int MAX_RETRIES = 10;
    public static final int RETRIES = 5;
    public static int OFFICAL_PORT = 1812;
    public static int DEFAULT_PORT = 1645;
    public static int DEFAULT_TIMEOUT = 5;
    public static String OPTION_DEBUG = "OPTION_DEBUG";
    private String secret = null;
    private DatagramSocket socket = null;
    private short _id = (short)System.currentTimeMillis();
    private SecureRandom _rand = null;
    private Set<RADIUSServer> primaries;
    private Set<RADIUSServer> secondaries;
    private int healthCheckInterval = 5;
    private static final Map<RADIUSServer, Boolean> serverStatus = new HashMap<RADIUSServer, Boolean>();
    private static Debug debug = Debug.getInstance("amAuthRadius");
    private static RADIUSMonitor serverMonitor;
    private static final Object SERVER_MONITOR_LOCK = new Object();
    
    public RadiusConn(Set<RADIUSServer> primaries, Set<RADIUSServer> secondaries,
            String secret, int timeout, int healthCheckInterval) throws SocketException {
        this(primaries, secondaries, secret, timeout, null, healthCheckInterval);
    }

    public RadiusConn(Set<RADIUSServer> primaries, String secret, byte seed[],
            int healthCheckinterval) throws SocketException {
        this(primaries, primaries, secret, DEFAULT_TIMEOUT, seed, healthCheckinterval);
    }

    public RadiusConn(Set<RADIUSServer> primaries, Set<RADIUSServer> secondaries,
            String secret, int timeout, byte seed[], int healthCheckInterval)
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
            _rand = new SecureRandom();
        } else {
            _rand = new SecureRandom(seed);
        }
    }
    
    
    public void disconnect() throws IOException {
        socket.disconnect();
    }
    
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
    
    public void replyChallenge(String password, ChallengeException ce)
            throws IOException, NoSuchAlgorithmException,
            RejectException, ChallengeException {
        replyChallenge(null, password, ce);
    }
    
    public void replyChallenge(String name, String password,
            ChallengeException ce) throws IOException, NoSuchAlgorithmException,
            RejectException, ChallengeException {
        StateAttribute state = (StateAttribute)
        ce.getAttributeSet().getAttributeByType(Attribute.STATE);
        if (state == null)
            throw new IOException("State not found in challenge");
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
    
    public void replyChallenge(String name, String password, String state)
            throws IOException, NoSuchAlgorithmException,
            RejectException, ChallengeException {
        if (state == null) {
            throw new IOException("State not found in challenge");
        }
        AccessRequest req = createAccessRequest();
        req.addAttribute(new StateAttribute(state)); // needed in challenge
        req.addAttribute(new UserNameAttribute(name));
        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
                secret, password));
        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
        req.addAttribute(new NASPortAttribute(socket.getLocalPort()));

        sendPacket(req);
    }

    private void sendPacket(NASPacket packet) throws IOException,
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
                    synchronized (serverStatus) {
                        serverStatus.put(server, Boolean.FALSE);
                    }
                    synchronized (SERVER_MONITOR_LOCK) {
                        if (serverMonitor == null || serverMonitor.scheduledExecutionTime() == -1) {
                            serverMonitor = new RADIUSMonitor();
                            SystemTimer.getTimer().schedule(serverMonitor,
                                    new Date(((System.currentTimeMillis()) / 1000) * 1000));
                        }
                    }
                } else {
                    throw ioe;
                }
            }
        }
    }

    private RADIUSServer getOnlineServer() {
        synchronized (serverStatus) {
            for (RADIUSServer server : primaries) {
                Boolean state = serverStatus.get(server);
                if (state == null) {
                    serverStatus.put(server, Boolean.TRUE);
                    return server;
                } else if (state) {
                    return server;
                }
            }
            for (RADIUSServer server : secondaries) {
                Boolean state = serverStatus.get(server);
                if (state == null) {
                    serverStatus.put(server, Boolean.TRUE);
                    return server;
                } else if (state) {
                    return server;
                }
            }
        }
        return null;
    }

    private short getIdentifier() {
        return _id++;
    }
    
    private void send(NASPacket packet, RADIUSServer server)
            throws IOException {
        if (server == null) {
            throw new IOException("No RADIUS server is online.");
        }
        DatagramPacket dp = new DatagramPacket(new byte[4096], 4096);
        dp.setPort(server.getPort());
        dp.setAddress(InetAddress.getByName(server.getHost()));
        byte data[] = packet.getData();
        dp.setLength(data.length);
        dp.setData(data);
        socket.send(dp);
        if (debug.messageEnabled()) {
            debug.message("Sent " + packet);
        }
    }
    
    private ServerPacket receive()
    throws IOException {
        DatagramPacket dp = new DatagramPacket(new byte[4096], 4096);
        socket.receive(dp);
        byte data[] = dp.getData();
        ServerPacket p = PacketFactory.createServerPacket(data);
        if (debug.messageEnabled()) {
            debug.message("Received " + p + " size=" + p.getAttributeSet().size());
        }
        return p;
    }
    
    private AccessRequest createAccessRequest() throws 
        NoSuchAlgorithmException {
        RequestAuthenticator ra = new RequestAuthenticator(_rand, secret);
        AccessRequest req = new AccessRequest(getIdentifier(), ra);
        return req;
    }

    private class RADIUSMonitor extends GeneralTaskRunnable {

        private boolean cancelled = false;

        public boolean addElement(Object key) {
            return false;
        }

        public boolean removeElement(Object key) {
            return false;
        }

        public boolean isEmpty() {
            return true;
        }

        public long getRunPeriod() {
            return cancelled ? 0 : healthCheckInterval * 60000;
        }

        public void run() {
            Map<RADIUSServer, Boolean> tmp;
            if (debug.messageEnabled()) {
                debug.message("Checking server statuses");
            }
            synchronized (serverStatus) {
                 tmp = new LinkedHashMap<RADIUSServer, Boolean>(serverStatus);
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
                        DatagramPacket dp = new DatagramPacket(new byte[4096], 4096);
                        dp.setPort(server.getPort());
                        dp.setAddress(InetAddress.getByName(server.getHost()));
                        byte data[] = req.getData();
                        dp.setLength(data.length);
                        dp.setData(data);
                        testSocket.send(dp);
                        dp = new DatagramPacket(new byte[4096], 4096);
                        testSocket.receive(dp);
                        data = dp.getData();
                        if (debug.messageEnabled()) {
                            debug.message("Moving server to online state - " + server);
                        }
                        synchronized (serverStatus) {
                            serverStatus.put(server, Boolean.TRUE);
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
