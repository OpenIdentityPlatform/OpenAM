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
 * Portions Copyrighted 2026 3A Systems LLC.
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
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.AccessChallenge;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.Attribute;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.MessageAuthenticatorAttribute;
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
     * When {@code true}, the client enforces the strict RFC 3579 / Cisco BlastRADIUS-mitigation
     * profile and rejects any Access-Accept/Reject/Challenge that does not carry a verifiable
     * Message-Authenticator (attribute 80). When {@code false} (the default) the client is in
     * "lax" mode: Message-Authenticator is still sent on outgoing requests and verified when
     * present on responses, but legacy servers that never emit MA on responses remain
     * interoperable. The flag is exposed so deployments that control both sides of the link can
     * lock down to the fully-protected flow.
     */
    private final boolean requireMessageAuthenticator;
    /**
     * Socket read timeout (in milliseconds) captured at construction time.
     *
     * <p>The {@link RADIUSMonitor} health-check task uses this value when it constructs its
     * per-iteration probe socket. It cannot read the timeout from {@link #socket} at run time
     * because (a) {@code RADIUSMonitor} is a non-static inner class with an implicit reference to
     * the {@code RadiusConn} that scheduled it, (b) {@code serverMonitor} is a static field that
     * outlives any individual {@code RadiusConn}, and (c) {@link #disconnect()} closes
     * {@code socket} at the end of every authentication. Calling
     * {@link DatagramSocket#getSoTimeout()} on a closed socket throws {@link SocketException},
     * which would silently abort every health-check iteration and leave servers stuck OFFLINE
     * forever.
     */
    private final int socketTimeoutMillis;
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

    // ---- Outstanding request state used to validate the matching response (RFC 2865/3579). ----
    /** Identifier of the most recently sent Access-Request awaiting a response. */
    private short currentIdentifier;
    /** 16-octet Request Authenticator of the outstanding request. */
    private byte[] currentRequestAuth;
    /** Address of the server the outstanding request was sent to. */
    private InetAddress currentServerAddress;
    /** Port of the server the outstanding request was sent to. */
    private int currentServerPort;

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
        this(primaries, secondaries, secret, timeout, null, healthCheckInterval, false);
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
        this(primaries, primaries, secret, DEFAULT_TIMEOUT, seed, healthCheckInterval, false);
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
        this(primaries, secondaries, secret, timeout, seed, healthCheckInterval, false);
    }

    /**
     * Full-featured constructor that allows the caller to opt-in to the strict RFC 3579 /
     * BlastRADIUS-mitigation profile.
     *
     * @param primaries                    the primary servers to connect to.
     * @param secondaries                  the secondary servers to connect to.
     * @param secret                       the secret shared between this client and the remote servers.
     * @param timeout                      the timeout value, in seconds.
     * @param seed                         optional {@link SecureRandom} seed; {@code null} for default entropy.
     * @param healthCheckInterval          the health check interval, in minutes.
     * @param requireMessageAuthenticator  when {@code true}, every Access-Accept/Reject/Challenge
     *                                     received from the server MUST carry a verifiable
     *                                     Message-Authenticator (RFC 3579 attribute 80). This is
     *                                     the fully protected flow recommended by Cisco's
     *                                     BlastRADIUS guidance. When {@code false}, the client
     *                                     verifies MA only when the server happens to include it,
     *                                     remaining interoperable with legacy servers.
     * @throws SocketException if a socket exception occurs.
     */
    public RadiusConn(Set<RADIUSServer> primaries, Set<RADIUSServer> secondaries,
                      String secret, int timeout, byte[] seed, int healthCheckInterval,
                      boolean requireMessageAuthenticator)
            throws SocketException {
        this.secret = secret;
        this.primaries = primaries;
        this.secondaries = secondaries;
        this.healthCheckInterval = healthCheckInterval;
        this.requireMessageAuthenticator = requireMessageAuthenticator;
        if (debug.messageEnabled()) {
            debug.message("Primary RADIUS servers: " + primaries);
            debug.message("Secondary RADIUS servers: " + secondaries);
            debug.message("Require Message-Authenticator on responses: " + requireMessageAuthenticator);
        }
        socket = new DatagramSocket();
        this.socketTimeoutMillis = timeout * 1000;
        socket.setSoTimeout(this.socketTimeoutMillis);
        if (seed == null) {
            random = new SecureRandom();
        } else {
            random = new SecureRandom(seed);
        }
    }

    /**
     * Closes the underlying datagram socket, releasing the associated file descriptor and
     * ephemeral UDP port back to the operating system.
     *
     * <p>Historically this method only invoked {@link DatagramSocket#disconnect()}, which merely
     * removes the peer association but keeps the socket (and therefore its file descriptor and
     * ephemeral port) allocated until the JVM finaliser eventually reclaims it. Because
     * {@code RADIUS.shutdown()} discards its {@code RadiusConn} reference right after calling
     * this method, that behaviour leaked one FD / one ephemeral port per authentication attempt
     * under load. The method is now an idempotent close, which is what every caller in the
     * tree actually wants.
     *
     * @throws IOException if an io exception occurs.
     */
    public void disconnect() throws IOException {
        if (!socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Authenticates the username and password against the remote servers.
     *
     * @param name     the username.
     * @param password the password.
     * @return the response packet.
     * @throws IOException              if there is a problem.
     * @throws NoSuchAlgorithmException if there is a problem.
     * @throws RejectException          if there is a problem.
     * @throws ChallengeException       if there is a problem.
     */
    public Packet authenticate(String name, String password)
            throws IOException, NoSuchAlgorithmException,
            RejectException, ChallengeException {
        AccessRequest req = createAccessRequest();
        req.addAttribute(new UserNameAttribute(name));
        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
                secret, password));
        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
        req.addAttribute(new NASPortAttribute(socket.getLocalPort()));
        return sendPacket(req);
    }

    /**
     * Sends an access-request to the server in response to a challenge request.
     *
     * @param name     the username.
     * @param password the password.
     * @param ce       the challenge exception providing access to the original challenge response.
     * @return the response packet.
     * @throws IOException              if there is a problem.
     * @throws NoSuchAlgorithmException if there is a problem.
     * @throws RejectException          if there is a problem.
     * @throws ChallengeException       if there is a problem.
     */
    public Packet replyChallenge(String name, String password,
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

        return sendPacket(req);
    }

    /**
     * Finds an available server and then sends a packet to that servers.
     *
     * @param packet the packet.
     * @return the response packet.
     * @throws IOException        if there is a problem.
     * @throws RejectException    if there is a problem.
     * @throws ChallengeException if there is a problem.
     */
    private Packet sendPacket(Packet packet) throws IOException,
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
                } else if (!(res instanceof AccessAccept)) {
                    // Anything other than Accept/Reject/Challenge MUST NOT be treated as a successful
                    // authentication. Previously the code fell through to STATE_SUCCEED for any
                    // unknown packet type, which contributed to GHSA-386j-6m86-78f9.
                    throw new IOException("Unexpected RADIUS response type: "
                            + (res == null ? "null" : res.getClass().getSimpleName()));
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
        return res;
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

        final InetAddress addr = InetAddress.getByName(server.getHost());
        final int port = server.getPort();

        // Connect the datagram socket to the chosen server so the kernel drops datagrams that do
        // not originate from that (address, port) pair. This prevents off-path attackers from
        // racing forged Access-Accept packets to the client's ephemeral source port
        // (GHSA-386j-6m86-78f9).
        if (socket.isConnected()) {
            socket.disconnect();
        }
        socket.connect(addr, port);

        // Remember the outstanding request so the response can be matched and verified.
        currentIdentifier = packet.getIdentifier();
        currentRequestAuth = packet.getAuthenticator().getOctets().clone();
        currentServerAddress = addr;
        currentServerPort = port;

        final byte[] data = signOutgoingRequest(packet, secret);
        final DatagramPacket dp = new DatagramPacket(data, data.length, addr, port);
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

        final Packet p = verifyAndParse(buffer, dp.getLength(), dp.getAddress(), dp.getPort(),
                currentServerAddress, currentServerPort, currentIdentifier, currentRequestAuth, secret,
                requireMessageAuthenticator);
        if (debug.messageEnabled()) {
            debug.message("Received " + p + " size=" + p.getAttributeSet().size());
        }
        return p;
    }

    /**
     * Embeds a fresh RFC 2869/3579 Message-Authenticator into the supplied packet and returns the
     * signed wire-format bytes ready for transmission. Any Message-Authenticator left over from a
     * previous send attempt (e.g. the failover retry case on the same {@link Packet} instance) is
     * evicted first to satisfy RFC 3579 §3.2 (exactly one MA per packet).
     *
     * <p>Extracted as a static helper so the health-check probe in {@link RADIUSMonitor} can reuse
     * the exact same signing logic as the regular authentication path. Diverging the two paths is
     * how the original CVE GHSA-386j-6m86-78f9 happened in the first place.
     */
    static byte[] signOutgoingRequest(Packet packet, String secret) throws IOException {
        packet.getAttributeSet().removeAttributesByType(AttributeType.MESSAGE_AUTHENTICATOR);
        packet.addAttribute(new MessageAuthenticatorAttribute());
        final byte[] data = packet.getOctets();
        final int maOffset = findMessageAuthenticatorOffset(data);
        if (maOffset < 0) {
            throw new IOException("Failed to embed Message-Authenticator into Access-Request");
        }
        try {
            final Mac hmac = Mac.getInstance("HmacMD5");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacMD5"));
            final byte[] mac = hmac.doFinal(data);
            System.arraycopy(mac, 0, data, maOffset + 2, 16);
        } catch (GeneralSecurityException gse) {
            throw new IOException("HMAC-MD5 unavailable for RADIUS Message-Authenticator", gse);
        }
        return data;
    }

    /**
     * Performs every check required by RFC 2865/3579 against an incoming UDP datagram and returns
     * the parsed {@link Packet} on success, or throws {@link IOException}.
     *
     * <p>Checks: source address/port match (defence-in-depth on top of socket.connect()), datagram
     * length sanity, identifier match against the outstanding request, RFC 2865 §3 Response
     * Authenticator equality (constant-time), and RFC 3579 Message-Authenticator HMAC equality
     * when the attribute is present.
     *
     * <p>Both the regular authentication path ({@link #receive()}) and the health-check probe in
     * {@link RADIUSMonitor} call this method, so any future tightening of response validation
     * benefits both call sites automatically.
     */
    static Packet verifyAndParse(byte[] buffer, int rxLen, InetAddress sourceAddr, int sourcePort,
                                 InetAddress expectedAddr, int expectedPort, short expectedIdentifier,
                                 byte[] requestAuth, String secret,
                                 boolean requireMessageAuthenticator) throws IOException {
        // Defence-in-depth: even though the socket is connect()-ed, double-check the source.
        if (expectedAddr != null && (!expectedAddr.equals(sourceAddr) || sourcePort != expectedPort)) {
            throw new IOException("RADIUS response received from unexpected source "
                    + sourceAddr + ":" + sourcePort);
        }
        if (rxLen < 20) {
            throw new IOException("RADIUS response shorter than 20 octets");
        }
        final int packetLen = ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
        if (packetLen < 20 || packetLen > rxLen) {
            throw new IOException("RADIUS response length field invalid: " + packetLen);
        }
        // Match identifier of the outstanding request - prevents acceptance of unsolicited or
        // delayed packets generated for a different transaction.
        final int rxId = buffer[1] & 0xFF;
        if (rxId != (expectedIdentifier & 0xFF)) {
            throw new IOException("RADIUS response identifier mismatch: got " + rxId
                    + ", expected " + (expectedIdentifier & 0xFF));
        }
        // Verify Response Authenticator per RFC 2865 §3:
        //   ResponseAuth = MD5(Code || ID || Length || RequestAuth || Attributes || Secret)
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(buffer, 0, 4);
            md5.update(requestAuth);
            if (packetLen > 20) {
                md5.update(buffer, 20, packetLen - 20);
            }
            md5.update(secret.getBytes(StandardCharsets.UTF_8));
            final byte[] expected = md5.digest();
            if (!constantTimeEquals(expected, 0, buffer, 4, 16)) {
                throw new IOException("RADIUS Response Authenticator verification failed");
            }
        } catch (NoSuchAlgorithmException nsae) {
            throw new IOException("MD5 unavailable for RADIUS Response Authenticator", nsae);
        }
        // Build the trimmed octet array used both by the parser and Message-Authenticator HMAC.
        final byte[] data = new byte[packetLen];
        System.arraycopy(buffer, 0, data, 0, packetLen);
        final Packet p = PacketFactory.toPacket(data);
        if (p == null) {
            throw new IOException("Unparseable or unsupported RADIUS response");
        }
        // If the server included a Message-Authenticator (RFC 3579), validate it. In strict mode
        // (Cisco BlastRADIUS-mitigation "fully protected flow") the attribute is mandatory.
        final Attribute maAttr = p.getAttributeSet().getAttributeByType(AttributeType.MESSAGE_AUTHENTICATOR);
        if (maAttr instanceof MessageAuthenticatorAttribute) {
            verifyResponseMessageAuthenticator(data, (MessageAuthenticatorAttribute) maAttr,
                    requestAuth, secret);
        } else if (requireMessageAuthenticator) {
            throw new IOException("RADIUS response is missing the required Message-Authenticator "
                    + "(strict RFC 3579 mode is enabled)");
        }
        return p;
    }

    /**
     * Locate the Message-Authenticator attribute within the assembled packet octets.
     *
     * @param data the on-the-wire bytes of the packet.
     * @return the offset within {@code data} of the attribute's type octet, or -1 if not present.
     */
    private static int findMessageAuthenticatorOffset(byte[] data) {
        final int len = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        int off = 20;
        while (off + 2 <= len) {
            final int aType = data[off] & 0xFF;
            final int aLen = data[off + 1] & 0xFF;
            if (aLen < 2 || off + aLen > len) {
                return -1;
            }
            if (aType == AttributeType.MESSAGE_AUTHENTICATOR.getTypeCode() && aLen == 18) {
                return off;
            }
            off += aLen;
        }
        return -1;
    }

    /**
     * Verifies the RFC 3579 Message-Authenticator attribute carried in a response packet.
     * For responses the response authenticator field is replaced with the request authenticator
     * and the Message-Authenticator value field is treated as 16 zero octets during HMAC-MD5
     * computation.
     */
    private static void verifyResponseMessageAuthenticator(byte[] data, MessageAuthenticatorAttribute ma,
                                                           byte[] requestAuth, String secret)
            throws IOException {
        final int maOffset = findMessageAuthenticatorOffset(data);
        if (maOffset < 0) {
            throw new IOException("Message-Authenticator not found in response");
        }
        final byte[] received = ma.getHmac();
        final byte[] copy = data.clone();
        System.arraycopy(requestAuth, 0, copy, 4, 16);
        for (int i = 0; i < 16; i++) {
            copy[maOffset + 2 + i] = 0;
        }
        try {
            final Mac hmac = Mac.getInstance("HmacMD5");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacMD5"));
            final byte[] expected = hmac.doFinal(copy);
            if (!constantTimeEquals(expected, 0, received, 0, 16)) {
                throw new IOException("RADIUS Message-Authenticator verification failed");
            }
        } catch (GeneralSecurityException gse) {
            throw new IOException("HMAC-MD5 unavailable for RADIUS Message-Authenticator", gse);
        }
    }

    /** Constant-time array comparison to avoid leaking timing information about the secret. */
    private static boolean constantTimeEquals(byte[] a, int aOff, byte[] b, int bOff, int len) {
        int diff = 0;
        for (int i = 0; i < len; i++) {
            diff |= (a[aOff + i] ^ b[bOff + i]);
        }
        return diff == 0;
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
         *
         * <p>The probe goes through the same hardening as the regular authentication path
         * (see {@link RadiusConn#signOutgoingRequest(Packet, String)} and
         * {@link RadiusConn#verifyAndParse}): the probe socket is connect()-ed to the candidate
         * server so the kernel discards datagrams from other sources, the outgoing Access-Request
         * carries a Message-Authenticator HMAC, and the response is rejected unless it has the
         * expected source, identifier and a verifiable RFC 2865 Response Authenticator (plus
         * Message-Authenticator if the server included one). This prevents an attacker from
         * promoting a dead RADIUS server back to "online" by spraying forged datagrams at the
         * health-check ephemeral port - i.e. the same class of bug that GHSA-386j-6m86-78f9 fixed
         * on the main path.
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
                        final InetAddress addr = InetAddress.getByName(server.getHost());
                        final int port = server.getPort();
                        testSocket = new DatagramSocket();
                        // Read the captured timeout, NOT socket.getSoTimeout(): the enclosing
                        // RadiusConn's socket may have already been close()d by RADIUS.shutdown()
                        // after a previous authentication, in which case getSoTimeout() would
                        // throw SocketException and the broad catch below would silently bury it.
                        testSocket.setSoTimeout(socketTimeoutMillis);
                        testSocket.connect(addr, port);

                        AccessRequest req = createAccessRequest();
                        req.addAttribute(new UserNameAttribute("nonexistent"));
                        req.addAttribute(new UserPasswordAttribute(req.getAuthenticator(),
                                secret, "invalidpass"));
                        req.addAttribute(new NASIPAddressAttribute(InetAddress.getLocalHost()));
                        req.addAttribute(new NASPortAttribute(testSocket.getLocalPort()));

                        final short probeId = req.getIdentifier();
                        final byte[] probeRequestAuth = req.getAuthenticator().getOctets().clone();
                        final byte[] data = signOutgoingRequest(req, secret);
                        testSocket.send(new DatagramPacket(data, data.length, addr, port));

                        final byte[] buffer = new byte[4096];
                        final DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                        testSocket.receive(dp);

                        // Any well-formed, authenticated response (including Access-Reject,
                        // which is the expected reply to "nonexistent"/"invalidpass") proves the
                        // server is alive AND that it shares the configured secret. An IOException
                        // here means the server is still considered offline.
                        verifyAndParse(buffer, dp.getLength(), dp.getAddress(), dp.getPort(),
                                addr, port, probeId, probeRequestAuth, secret,
                                requireMessageAuthenticator);

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
                        // Each iteration allocates a fresh DatagramSocket; we must release its
                        // file descriptor here, otherwise the periodic health check leaks one FD
                        // (and one ephemeral UDP port) per offline server per tick. The previous
                        // implementation called disconnect(), which only removes peer
                        // association and does NOT free the underlying socket.
                        if (testSocket != null && !testSocket.isClosed()) {
                            testSocket.close();
                        }
                        testSocket = null;
                    }
                }
            }
            if (offline == 0) {
                cancelled = true;
            }
        }
    }
}
