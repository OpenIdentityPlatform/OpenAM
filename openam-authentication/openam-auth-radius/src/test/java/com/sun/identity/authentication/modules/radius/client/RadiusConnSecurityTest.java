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
package com.sun.identity.authentication.modules.radius.client;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.Packet;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.authentication.modules.radius.RADIUSServer;

/**
 * Regression tests for GHSA-386j-6m86-78f9.
 *
 * <p>Each test wires a {@link RadiusConn} client against a UDP responder that runs in a
 * background thread and crafts a particular response shape. The original (vulnerable) code
 * accepts every non-Reject/non-Challenge response without verifying the RFC 2865 Response
 * Authenticator, so the {@code attackerForgedAccessAccept} test below would have produced a
 * successful {@link AccessAccept}. With the fix in place that test - and every other malformed
 * response variant - must fail with an {@link IOException}, while a properly authenticated
 * response is still accepted.
 */
public class RadiusConnSecurityTest {

    private static final String SHARED_SECRET = "real-radius-shared-secret-NOT-KNOWN-TO-ATTACKER";
    private static final String USERNAME = "victim";
    private static final String PASSWORD = "wrong-password-attacker-tries";

    private DatagramSocket serverSocket;
    private Thread serverThread;
    private volatile boolean serverRunning;
    private final AtomicReference<Throwable> serverError = new AtomicReference<>();

    @BeforeMethod
    public void startServerSocket() throws IOException {
        // RadiusConn keeps the server-availability map and the health-check timer in static
        // fields that outlive any single connection. Left untouched they leak across test
        // methods: e.g. failoverToSecondary() permanently records its dead primary as OFFLINE
        // and schedules a background RADIUSMonitor that keeps probing. A later test can then
        // see getOnlineServer() return null ("No RADIUS server is online.") when an ephemeral
        // port number is reused, or have its manually-driven monitor.run() race the background
        // one. Reset the shared statics so every method starts from a clean slate.
        resetRadiusConnStatics();

        serverSocket = new DatagramSocket(0, InetAddress.getByName("127.0.0.1"));
        serverSocket.setSoTimeout(5000);
        serverRunning = true;
        serverError.set(null);
    }

    @AfterMethod(alwaysRun = true)
    public void stopServer() {
        serverRunning = false;
        if (serverSocket != null) {
            serverSocket.close();
        }
        if (serverThread != null) {
            try {
                serverThread.join(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        // Tear down any state/timer this test scheduled so it cannot bleed into the next one.
        resetRadiusConnStatics();
    }

    /**
     * Cancel any scheduled health-check monitor and clear the static server-status map on
     * {@link RadiusConn}, isolating each test method from the shared singleton state.
     */
    private static void resetRadiusConnStatics() {
        try {
            final java.lang.reflect.Field monitorField = RadiusConn.class.getDeclaredField("serverMonitor");
            monitorField.setAccessible(true);
            final Object monitor = monitorField.get(null);
            if (monitor != null) {
                // RADIUSMonitor extends GeneralTaskRunnable, whose cancel() unschedules it from
                // the shared SystemTimer so no background thread keeps probing.
                monitor.getClass().getMethod("cancel").invoke(monitor);
                monitorField.set(null, null);
            }

            final java.lang.reflect.Field statusField = RadiusConn.class.getDeclaredField("SERVER_STATUS");
            statusField.setAccessible(true);
            @SuppressWarnings("unchecked")
            final java.util.Map<RADIUSServer, Boolean> serverStatus =
                    (java.util.Map<RADIUSServer, Boolean>) statusField.get(null);
            synchronized (serverStatus) {
                serverStatus.clear();
            }
        } catch (ReflectiveOperationException roe) {
            throw new IllegalStateException("Unable to reset RadiusConn static state for test isolation", roe);
        }
    }

    private RadiusConn newClient() throws IOException {
        return newClient(false);
    }

    private RadiusConn newClient(boolean strict) throws IOException {
        final Set<RADIUSServer> servers = new HashSet<>();
        servers.add(new RADIUSServer("127.0.0.1", serverSocket.getLocalPort()));
        // 10-second read timeout (defence-in-depth; the real CI de-flake is the responder no
        // longer dying on its own read timeout - see startResponder). Every test using this client
        // receives a response and returns as soon as it arrives, so a generous timeout never slows
        // the happy path; it only adds margin against scheduling jitter on a loaded CI runner.
        return new RadiusConn(servers, Collections.<RADIUSServer>emptySet(), SHARED_SECRET, 10, null, 60, strict);
    }

    /** Start a background responder that crafts a reply per the supplied lambda. */
    private void startResponder(Responder responder) {
        serverThread = new Thread(() -> {
            final byte[] buf = new byte[4096];
            try {
                while (serverRunning) {
                    final DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    try {
                        serverSocket.receive(dp);
                    } catch (java.net.SocketTimeoutException ste) {
                        // The server socket carries a 5s SO_TIMEOUT (see @BeforeMethod). A read
                        // timeout only means no request has arrived *yet* - it must NOT kill the
                        // responder. The client can legitimately be slow to send its first packet
                        // (cold-JVM class loading, or InetAddress.getLocalHost() blocking on reverse
                        // DNS on a CI host), and if the responder died here it would never answer the
                        // request that eventually arrives, leaving the client to time out and report
                        // "No RADIUS server is online." Keep waiting instead.
                        continue;
                    } catch (IOException e) {
                        if (!serverRunning) {
                            return;
                        }
                        throw e;
                    }
                    final byte[] req = new byte[dp.getLength()];
                    System.arraycopy(dp.getData(), 0, req, 0, dp.getLength());
                    final byte[] resp = responder.buildResponse(req);
                    if (resp != null) {
                        serverSocket.send(new DatagramPacket(resp, resp.length, dp.getAddress(), dp.getPort()));
                    }
                }
            } catch (Throwable t) {
                serverError.set(t);
            }
        }, "radius-test-responder");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private interface Responder {
        byte[] buildResponse(byte[] request) throws Exception;
    }

    // ----------------------------------------------------------------------------------------
    // 1. The exact PoC from the advisory: forged Access-Accept with all-zero authenticator.
    //    Pre-fix: client returned an AccessAccept and OpenAM minted a session.
    //    Post-fix: client must throw IOException because Response Authenticator does not verify.
    // ----------------------------------------------------------------------------------------
    @Test
    public void attackerForgedAccessAccept_withZeroAuthenticator_isRejected() throws Exception {
        startResponder(req -> {
            // Code=2 (Access-Accept), echoed id, Length=20, 16-byte zero authenticator.
            final byte[] resp = new byte[20];
            resp[0] = 2;
            resp[1] = req[1];
            resp[2] = 0;
            resp[3] = 20;
            return resp;
        });

        final RadiusConn client = newClient();
        try {
            client.authenticate(USERNAME, PASSWORD);
            fail("Forged Access-Accept must not be accepted (GHSA-386j-6m86-78f9)");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("authenticator")
                            || expected.getMessage().toLowerCase().contains("unparseable"),
                    "Unexpected IOException message: " + expected.getMessage());
        } finally {
            client.disconnect();
        }
    }

    // ----------------------------------------------------------------------------------------
    // 2. Properly signed Access-Accept (legitimate server) is still accepted by the client.
    // ----------------------------------------------------------------------------------------
    @Test
    public void legitimateAccessAccept_withValidResponseAuthenticator_isAccepted() throws Exception {
        startResponder(req -> buildLegitimateAccept(req, SHARED_SECRET));

        final RadiusConn client = newClient();
        try {
            final Packet res = client.authenticate(USERNAME, PASSWORD);
            assertTrue(res instanceof AccessAccept,
                    "Expected AccessAccept, got " + res.getClass().getSimpleName());
            assertEquals(res.getIdentifier() & 0xFF, extractId(lastRequest), "id mismatch");
        } finally {
            client.disconnect();
        }
    }

    // ----------------------------------------------------------------------------------------
    // 3. Identifier mismatch must be rejected even if the Response Authenticator computes against
    //    the wrong id (i.e. an off-by-one delayed reply for a previous transaction).
    // ----------------------------------------------------------------------------------------
    @Test
    public void responseWithMismatchedIdentifier_isRejected() throws Exception {
        startResponder(req -> {
            final byte[] tampered = req.clone();
            tampered[1] = (byte) ((req[1] & 0xFF) ^ 0x01); // flip id
            return buildLegitimateAccept(tampered, SHARED_SECRET);
        });

        final RadiusConn client = newClient();
        try {
            client.authenticate(USERNAME, PASSWORD);
            fail("Response with mismatched identifier must be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("identifier"),
                    "Unexpected IOException message: " + expected.getMessage());
        } finally {
            client.disconnect();
        }
    }

    // ----------------------------------------------------------------------------------------
    // 4. Wrong shared-secret on the responder => Response Authenticator cannot be verified.
    //    This is the "BlastRADIUS-class" attacker that knows the protocol but not the secret.
    // ----------------------------------------------------------------------------------------
    @Test
    public void responseSignedWithWrongSecret_isRejected() throws Exception {
        startResponder(req -> buildLegitimateAccept(req, "attacker-guessed-secret"));

        final RadiusConn client = newClient();
        try {
            client.authenticate(USERNAME, PASSWORD);
            fail("Response signed with wrong shared secret must be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("authenticator"),
                    "Unexpected IOException message: " + expected.getMessage());
        } finally {
            client.disconnect();
        }
    }

    // ----------------------------------------------------------------------------------------
    // 5. Unexpected packet code (e.g. 99) - even with a "matching" Response Authenticator -
    //    must not be promoted to a successful authentication.
    // ----------------------------------------------------------------------------------------
    @Test
    public void unexpectedPacketType_isRejected() throws Exception {
        startResponder(req -> buildResponseWithCode(req, (byte) 99, SHARED_SECRET));

        final RadiusConn client = newClient();
        try {
            client.authenticate(USERNAME, PASSWORD);
            fail("Unknown packet code must not be treated as success");
        } catch (IOException expected) {
            // Accept either "unparseable" (PacketFactory rejects unknown code) or
            // "unexpected RADIUS response type" thrown by sendPacket().
            final String msg = expected.getMessage().toLowerCase();
            assertTrue(msg.contains("unexpected") || msg.contains("unparseable")
                            || msg.contains("authenticator"),
                    "Unexpected IOException message: " + expected.getMessage());
        } finally {
            client.disconnect();
        }
    }

    // ----------------------------------------------------------------------------------------
    // 6. Failover regression: when the primary RADIUS server times out and sendPacket() retries
    //    against a secondary, the same Packet instance must NOT accumulate multiple
    //    Message-Authenticator attributes. RFC 3579 §3.2 mandates exactly one MA per packet.
    //    Pre-regression-fix: every retry appended a fresh MA, producing a malformed packet.
    // ----------------------------------------------------------------------------------------
    @Test
    public void failoverToSecondary_emitsExactlyOneMessageAuthenticator() throws Exception {
        // Primary: bind a socket and never reply, so the client times out and fails over.
        final DatagramSocket primarySocket = new DatagramSocket(0, InetAddress.getByName("127.0.0.1"));
        // Secondary: reuse the inherited serverSocket from @BeforeMethod as the legit responder.
        final AtomicReference<byte[]> secondaryRequest = new AtomicReference<>();
        startResponder(req -> {
            secondaryRequest.set(req.clone());
            return buildLegitimateAccept(req, SHARED_SECRET);
        });

        final Set<RADIUSServer> primaries = new HashSet<>();
        primaries.add(new RADIUSServer("127.0.0.1", primarySocket.getLocalPort()));
        final Set<RADIUSServer> secondaries = new HashSet<>();
        secondaries.add(new RADIUSServer("127.0.0.1", serverSocket.getLocalPort()));

        final RadiusConn client = new RadiusConn(primaries, secondaries, SHARED_SECRET, 2, null, 60, false);
        try {
            final Packet res = client.authenticate(USERNAME, PASSWORD);
            assertTrue(res instanceof AccessAccept,
                    "Expected AccessAccept after failover, got " + res.getClass().getSimpleName());

            final byte[] req = secondaryRequest.get();
            assertTrue(req != null, "Secondary server did not receive the retried request");
            final int maCount = countMessageAuthenticators(req);
            assertEquals(maCount, 1,
                    "RFC 3579 §3.2 requires exactly one Message-Authenticator per packet, "
                            + "got " + maCount + " on failover retry");
        } finally {
            client.disconnect();
            primarySocket.close();
        }
    }

    /** Walk the RADIUS attribute list and count occurrences of type 80 (Message-Authenticator). */
    private static int countMessageAuthenticators(byte[] packet) {
        if (packet.length < 20) {
            return 0;
        }
        final int len = ((packet[2] & 0xFF) << 8) | (packet[3] & 0xFF);
        final int end = Math.min(len, packet.length);
        int off = 20;
        int count = 0;
        while (off + 2 <= end) {
            final int type = packet[off] & 0xFF;
            final int alen = packet[off + 1] & 0xFF;
            if (alen < 2 || off + alen > end) {
                break;
            }
            if (type == 80) {
                count++;
            }
            off += alen;
        }
        return count;
    }

    // ----------------------------------------------------------------------------------------
    // 7. Socket leak regression: RadiusConn.disconnect() (called from RADIUS.shutdown()) used to
    //    call DatagramSocket.disconnect(), which only removes peer association and keeps the
    //    file descriptor + ephemeral port allocated. The fix changes it to close() so callers
    //    that discard the connection (RADIUS module's shutdown() does exactly that) release the
    //    resource immediately instead of waiting for the GC finaliser.
    // ----------------------------------------------------------------------------------------
    @Test
    public void disconnectMustReleaseSocketResources() throws Exception {
        startResponder(req -> buildLegitimateAccept(req, SHARED_SECRET));

        final RadiusConn client = newClient();
        try {
            client.authenticate(USERNAME, PASSWORD);
        } finally {
            client.disconnect();
        }

        // Reach into the private 'socket' field to assert it was actually closed - this is the
        // only externally observable evidence that the FD/port was released.
        final java.lang.reflect.Field socketField = RadiusConn.class.getDeclaredField("socket");
        socketField.setAccessible(true);
        final DatagramSocket inner = (DatagramSocket) socketField.get(client);
        assertTrue(inner.isClosed(),
                "RadiusConn.disconnect() must close() the underlying DatagramSocket, "
                        + "otherwise the file descriptor and ephemeral UDP port leak until GC.");

        // disconnect() must be idempotent (RADIUS.shutdown() swallows IOException, but the fix
        // additionally guards against double-close to keep behaviour predictable).
        client.disconnect();
    }

    // ----------------------------------------------------------------------------------------
    // 8. Health-check probe hardening (RADIUSMonitor). The probe used to send an Access-Request
    //    without a Message-Authenticator, receive one datagram and unconditionally mark the
    //    target server "online" without verifying source, identifier or Response Authenticator.
    //    The refactor routes the probe through the same static helpers as the regular path:
    //    signOutgoingRequest() and verifyAndParse(). These two tests directly exercise the
    //    helpers used by RADIUSMonitor, providing focused regression coverage without having to
    //    wait for the timer-driven monitor to fire.
    //
    //    8a) A legitimately signed Access-Reject (the expected reply to "nonexistent" /
    //        "invalidpass") MUST be accepted, proving the server is alive AND shares the secret.
    // ----------------------------------------------------------------------------------------
    @Test
    public void healthCheckProbe_acceptsAuthenticatedAccessReject() throws Exception {
        // Build a synthetic Access-Request the way RADIUSMonitor does, sign it, then construct a
        // response with code 3 (Access-Reject) signed with the same secret and feed it through
        // the validation helper.
        final org.forgerock.openam.radius.common.AccessRequest req =
                new org.forgerock.openam.radius.common.AccessRequest(
                        (short) 42,
                        new org.forgerock.openam.radius.common.RequestAuthenticator(
                                new java.security.SecureRandom(new byte[]{1, 2, 3, 4}), SHARED_SECRET));
        final byte[] requestAuth = req.getAuthenticator().getOctets().clone();
        final byte[] signed = RadiusConn.signOutgoingRequest(req, SHARED_SECRET);

        // Confirm the helper produced exactly one Message-Authenticator (defence against the
        // RFC 3579 §3.2 multi-MA regression).
        assertEquals(countMessageAuthenticators(signed), 1,
                "signOutgoingRequest must emit exactly one Message-Authenticator");

        // Forge a legitimate Access-Reject response signed with the real secret.
        final byte[] response = buildResponseWithCodeAndId(signed, (byte) 3, SHARED_SECRET);

        final Packet parsed = RadiusConn.verifyAndParse(response, response.length,
                InetAddress.getByName("127.0.0.1"), 1812,
                InetAddress.getByName("127.0.0.1"), 1812,
                (short) 42, requestAuth, SHARED_SECRET, false);
        assertTrue(parsed instanceof org.forgerock.openam.radius.common.AccessReject,
                "Expected AccessReject from health-check helper, got " + parsed.getClass().getSimpleName());
    }

    // ----------------------------------------------------------------------------------------
    // 8b) A forged Access-Accept with an all-zero Response Authenticator (the advisory PoC)
    //     MUST NOT mark the probe target as online. Pre-fix: RADIUSMonitor swallowed the source
    //     and the authenticator entirely. Post-fix: verifyAndParse() throws IOException, which
    //     RADIUSMonitor treats as "still offline".
    // ----------------------------------------------------------------------------------------
    @Test
    public void healthCheckProbe_rejectsForgedAccessAccept() throws Exception {
        final byte[] requestAuth = new byte[16];
        new java.security.SecureRandom().nextBytes(requestAuth);

        final byte[] forged = new byte[20];
        forged[0] = 2;                 // Access-Accept
        forged[1] = (byte) 99;         // identifier
        forged[2] = 0;
        forged[3] = 20;
        // 16-byte authenticator left as zeros, the advisory PoC payload.

        try {
            RadiusConn.verifyAndParse(forged, forged.length,
                    InetAddress.getByName("127.0.0.1"), 1812,
                    InetAddress.getByName("127.0.0.1"), 1812,
                    (short) 99, requestAuth, SHARED_SECRET, false);
            fail("Health-check probe must not accept a forged response with zero authenticator");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("authenticator"),
                    "Unexpected IOException message: " + expected.getMessage());
        }

        // Source-mismatch must also be rejected (defence-in-depth on top of socket.connect()).
        final byte[] valid = buildResponseWithCodeAndId(forgeSignedHeader((short) 7, requestAuth),
                (byte) 2, SHARED_SECRET);
        try {
            RadiusConn.verifyAndParse(valid, valid.length,
                    InetAddress.getByName("127.0.0.2"), 1812,             // wrong source
                    InetAddress.getByName("127.0.0.1"), 1812,             // expected
                    (short) 7, requestAuth, SHARED_SECRET, false);
            fail("Response from unexpected source must be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("unexpected source"),
                    "Unexpected IOException message: " + expected.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------
    // 8c) RADIUSMonitor must continue to function after RadiusConn.disconnect() has closed the
    //     enclosing socket. Pre-fix: monitor.run() called socket.getSoTimeout() on the closed
    //     socket, which throws SocketException; the broad catch silently swallowed it and every
    //     server marked offline stayed offline forever (no recovery). Post-fix: the timeout is
    //     captured at construction time, so the monitor can still build probe sockets and
    //     promote a healthy server back to ONLINE.
    // ----------------------------------------------------------------------------------------
    @Test
    public void healthCheckSurvivesClientSocketClose() throws Exception {
        // Real responder: replies with a properly signed Access-Reject (typical probe answer).
        startResponder(req -> buildResponseWithCodeAndId(req, (byte) 3, SHARED_SECRET));

        final RadiusConn client = newClient();

        // Mark the configured server as OFFLINE so the monitor will try to revive it. The map
        // is a private static field on RadiusConn.
        final java.lang.reflect.Field statusField = RadiusConn.class.getDeclaredField("SERVER_STATUS");
        statusField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final java.util.Map<RADIUSServer, Boolean> serverStatus =
                (java.util.Map<RADIUSServer, Boolean>) statusField.get(null);
        final RADIUSServer target = new RADIUSServer("127.0.0.1", serverSocket.getLocalPort());
        synchronized (serverStatus) {
            serverStatus.put(target, Boolean.FALSE);
        }

        // Simulate the production lifecycle: RADIUS.shutdown() invokes disconnect() right after
        // every authentication. After this call, the enclosing client.socket is closed.
        client.disconnect();

        // Instantiate the private inner RADIUSMonitor against the (now disconnected) client.
        final Class<?> monitorClass = Class.forName(
                "com.sun.identity.authentication.modules.radius.client.RadiusConn$RADIUSMonitor");
        final java.lang.reflect.Constructor<?> ctor = monitorClass.getDeclaredConstructor(RadiusConn.class);
        ctor.setAccessible(true);
        final Object monitor = ctor.newInstance(client);

        try {
            // Pre-fix this call would internally throw SocketException("Socket is closed") from
            // socket.getSoTimeout() and silently bury it; the server would remain OFFLINE.
            monitorClass.getMethod("run").invoke(monitor);

            final Boolean state;
            synchronized (serverStatus) {
                state = serverStatus.get(target);
            }
            assertTrue(Boolean.TRUE.equals(state),
                    "Monitor must promote a live server back to ONLINE even after the enclosing "
                            + "RadiusConn's socket has been closed by disconnect(). Actual state: " + state);
        } finally {
            synchronized (serverStatus) {
                serverStatus.remove(target);
            }
        }
    }

    /**
     * Build a minimal Access-Request-like header (Code|ID|Length|RequestAuth, no attributes) so
     * buildResponseWithCodeAndId() can derive a matching Response Authenticator.
     */
    private static byte[] forgeSignedHeader(short id, byte[] requestAuth) {
        final byte[] header = new byte[20];
        header[0] = 1;                 // Access-Request
        header[1] = (byte) (id & 0xFF);
        header[2] = 0;
        header[3] = 20;
        System.arraycopy(requestAuth, 0, header, 4, 16);
        return header;
    }

    /** Same as buildResponseWithCode but preserves the supplied request identifier. */
    private static byte[] buildResponseWithCodeAndId(byte[] request, byte code, String secret) throws Exception {
        final byte[] reqAuth = new byte[16];
        System.arraycopy(request, 4, reqAuth, 0, 16);
        final byte[] resp = new byte[20];
        resp[0] = code;
        resp[1] = request[1];
        resp[2] = 0;
        resp[3] = 20;
        final MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(resp, 0, 4);
        md5.update(reqAuth);
        md5.update(secret.getBytes(StandardCharsets.UTF_8));
        System.arraycopy(md5.digest(), 0, resp, 4, 16);
        return resp;
    }

    // ========================================================================================

    /** Stash of the last request the responder saw, used by the legitimate-accept test. */
    private volatile byte[] lastRequest;

    private byte[] buildLegitimateAccept(byte[] request, String secret) throws Exception {
        lastRequest = request.clone();
        return buildResponseWithCode(request, (byte) 2, secret);
    }

    /**
     * Assemble a RADIUS response of the given code containing only the header (no attributes)
     * and a Response Authenticator computed against the supplied shared secret.
     */
    private byte[] buildResponseWithCode(byte[] request, byte code, String secret) throws Exception {
        final byte[] reqAuth = new byte[16];
        System.arraycopy(request, 4, reqAuth, 0, 16);

        final byte[] resp = new byte[20];
        resp[0] = code;
        resp[1] = request[1]; // echo id
        resp[2] = 0;
        resp[3] = 20;
        // ResponseAuthenticator = MD5(Code || ID || Length || RequestAuth || Attributes || Secret)
        final MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(resp, 0, 4);
        md5.update(reqAuth);
        md5.update(secret.getBytes(StandardCharsets.UTF_8));
        final byte[] respAuth = md5.digest();
        System.arraycopy(respAuth, 0, resp, 4, 16);
        return resp;
    }

    private static int extractId(byte[] request) {
        return request[1] & 0xFF;
    }

    /** Demonstrates that HMAC-MD5 can also be used to forge a Message-Authenticator. */
    @SuppressWarnings("unused")
    private static byte[] hmacMd5(byte[] key, byte[] data) throws Exception {
        final Mac mac = Mac.getInstance("HmacMD5");
        mac.init(new SecretKeySpec(key, "HmacMD5"));
        return mac.doFinal(data);
    }

    // ----------------------------------------------------------------------------------------
    // 9. Strict RFC 3579 / Cisco BlastRADIUS "fully protected flow" profile
    //    (requireMessageAuthenticator = true).
    //
    // 9a) Legacy server that does not echo Message-Authenticator on Access-Accept must be
    //     REJECTED when the client is configured with strict mode.
    // ----------------------------------------------------------------------------------------
    @Test
    public void strictMode_rejectsResponseWithoutMessageAuthenticator() throws Exception {
        startResponder(req -> buildLegitimateAccept(req, SHARED_SECRET));

        final RadiusConn client = newClient(/* strict = */ true);
        try {
            client.authenticate(USERNAME, PASSWORD);
            fail("Strict mode must reject responses missing Message-Authenticator");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("message-authenticator"),
                    "Unexpected IOException message: " + expected.getMessage());
        } finally {
            client.disconnect();
        }
    }

    // ----------------------------------------------------------------------------------------
    // 9b) RFC-3579-compliant server that includes a valid Message-Authenticator on Access-Accept
    //     must be ACCEPTED in strict mode.
    // ----------------------------------------------------------------------------------------
    @Test
    public void strictMode_acceptsResponseWithValidMessageAuthenticator() throws Exception {
        startResponder(req -> buildAcceptWithMessageAuthenticator(req, SHARED_SECRET));

        final RadiusConn client = newClient(/* strict = */ true);
        try {
            final Packet res = client.authenticate(USERNAME, PASSWORD);
            assertTrue(res instanceof AccessAccept,
                    "Expected AccessAccept in strict mode with valid MA, got "
                            + res.getClass().getSimpleName());
        } finally {
            client.disconnect();
        }
    }

    // ----------------------------------------------------------------------------------------
    // 9c) Same legacy server (no MA) MUST still be accepted in default/lax mode - backwards
    //     compatibility regression guard for the new opt-in flag.
    // ----------------------------------------------------------------------------------------
    @Test
    public void laxMode_acceptsLegacyResponseWithoutMessageAuthenticator() throws Exception {
        startResponder(req -> buildLegitimateAccept(req, SHARED_SECRET));

        final RadiusConn client = newClient(/* strict = */ false);
        try {
            final Packet res = client.authenticate(USERNAME, PASSWORD);
            assertTrue(res instanceof AccessAccept,
                    "Lax mode must still accept legacy responses (no MA). Got "
                            + res.getClass().getSimpleName());
        } finally {
            client.disconnect();
        }
    }

    /**
     * Build an RFC-3579-style Access-Accept that contains a single Message-Authenticator
     * attribute. The Response Authenticator is computed after the HMAC is placed so the whole
     * thing verifies end-to-end against the supplied shared secret.
     */
    private byte[] buildAcceptWithMessageAuthenticator(byte[] request, String secret) throws Exception {
        final byte[] reqAuth = new byte[16];
        System.arraycopy(request, 4, reqAuth, 0, 16);

        // Layout: [Code|ID|Len|RespAuth(16) | MA-type(80)|MA-len(18)|MA-value(16 zeros)]
        final int len = 20 + 18;
        final byte[] resp = new byte[len];
        resp[0] = 2;                          // Access-Accept
        resp[1] = request[1];                 // echo id
        resp[2] = (byte) ((len >> 8) & 0xFF);
        resp[3] = (byte) (len & 0xFF);
        resp[20] = 80;                        // Message-Authenticator type
        resp[21] = 18;                        // attribute length

        // Compute the MA per RFC 3579: HMAC-MD5(secret, Code|ID|Len|RequestAuth|Attributes
        // with MA-value zeroed). Because for responses the on-the-wire RespAuth is *not* yet
        // known when MA is computed, RFC 3579 specifies using the RequestAuthenticator in its
        // place during HMAC calculation.
        final byte[] hmacInput = resp.clone();
        System.arraycopy(reqAuth, 0, hmacInput, 4, 16);
        // MA value field is already zero.
        final Mac mac = Mac.getInstance("HmacMD5");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacMD5"));
        final byte[] hmac = mac.doFinal(hmacInput);
        System.arraycopy(hmac, 0, resp, 22, 16);

        // Finally compute the standard RFC 2865 ResponseAuthenticator over the whole packet
        // (with MA already in place).
        final MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(resp, 0, 4);
        md5.update(reqAuth);
        md5.update(resp, 20, len - 20);
        md5.update(secret.getBytes(StandardCharsets.UTF_8));
        System.arraycopy(md5.digest(), 0, resp, 4, 16);

        return resp;
    }
}

