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
package org.forgerock.openam.radius.server.config;

/**
 * Constants used by the RADIUS server implementation. Created by markboyd on 11/12/14.
 */
public final class RadiusServerConstants {

    /**
     * Private constructor ensures this class is never instantiated.
     */
    private RadiusServerConstants() {
    }

    /**
     * The name of the logger used by the radius server classes.
     */
    public static final String RADIUS_SERVER_LOGGER = "amRadiusServer";

    /**
     * The default port for RADIUS authentication as specified in rfc 2865.
     */
    public static final int RADIUS_AUTHN_PORT = 1812;

    /**
     * The unique name of the RADIUS service registered in the admin console with our amRadiusServer.xml file exposing
     * configuration pieces in openAM's admin console.
     */
    public static final String RADIUS_SERVICE_NAME = "RadiusServerService";

    /**
     * The classpath based path to the file that defines the configuration pieces in openAM's admin console for the
     * RADIUS server functionality.
     */
    public static final String RADIUS_SERVICE_CFG_FILE = RADIUS_SERVICE_NAME + ".xml";

    /**
     * Name of the global radius service attribute whose value indicates if RADIUS server features should be turned on
     * allowing RADIUS clients to use openAM as a RADIUS server for authentication. See the service descriptor
     * amRadiusServer.xml file.
     */
    public static final String GBL_ATT_LISTENER_ENABLED = "radiusListenerEnabled";

    /**
     * Name of the global radius service attribute whose value indicates the port on which to listen for RADIUS
     * access-request requests. See the service descriptor amRadiusServer.xml file.
     */
    public static final String GBL_ATT_LISTENER_PORT = "radiusServerPort";

    /**
     * Name of the global radius service attribute whose value indicates the core pool size for the executor thread pool
     * that processes incoming RADIUS requests. See the service descriptor amRadiusServer.xml file and
     * ThreadPoolExecutor for details.
     */
    public static final String GBL_ATT_THREADS_CORE_SIZE = "radiusThreadPoolCoreSize";

    /**
     * Name of the global radius service attribute whose value indicates the max pool size for the executor thread pool
     * that processes incoming RADIUS requests. See the service descriptor amRadiusServer.xml file and
     * ThreadPoolExecutor for details.
     */
    public static final String GBL_ATT_THREADS_MAX_SIZE = "radiusThreadPoolMaxSize";

    /**
     * Name of the global radius service attribute whose value indicates the number of seconds idle threads should be
     * kept in the pool when the number of threads are greater than the core pool size. See the service descriptor
     * amRadiusServer.xml file.
     */
    public static final String GBL_ATT_THREADS_KEEPALIVE_SECONDS = "radiusThreadPoolKeepaliveSeconds";

    /**
     * Name of the global radius service attribute whose value indicates the number of items that can be queued up for
     * the thread pool before additional items will be dropped and not processed. See the service descriptor
     * amRadiusServer.xml file.
     */
    public static final String GBL_ATT_QUEUE_SIZE = "radiusThreadPoolQueueSize";

    /**
     * In a configured radius client this is the name of the attribute whose value indicates IP source address from
     * which a UDP packet must be received to be accepted and processed by the server. If an incoming packet has a
     * source IP address that does not match any client then the packet is dropped silently according to spec. This
     * value matches the corresponding field name declared in the amRadiusServer.xml file.
     */
    public static final String CLIENT_ATT_IP_ADDR = "clientIpAddress";

    /**
     * In a configured radius client this is the name of the attribute whose value holds the secret shared between
     * server and client and used to encrypt and decrypt the packet authenticators providing signer authenticity and
     * message integrity. This value matches the corresponding field name declared in the amRadiusServer.xml file.
     */
    public static final String CLIENT_ATT_SECRET = "clientSecret";

    /**
     * In a configured radius client this is the name of the attribute whose value indicates if request and response
     * packets for the client should be dumped to log with all content visible save for the USER_PASSWORD field which is
     * obfiscated with asterisks in place of characters.
     */
    public static final String CLIENT_ATT_LOG_PACKETS = "clientPacketsLogged";

    /**
     * In a configured radius client this is the fully qualified name of a class that implements the
     * AccessRequestHandler interface and will handle requests from that client.
     */
    public static final String CLIENT_ATT_CLASSNAME = "handlerClass";

    /**
     * In a configured radius client this is the name of a list of configuration properties specific to the handler
     * class.
     */
    public static final String CLIENT_ATT_PROPERTIES = "handlerConfig";

    /**
     * Empty String array pattern for use in toArray().
     */
    public static final String[] EMPTY_STRING_ARY = new String[0];

    /**
     * The name of the thread that is responsible for starting up the RADIUS service as openAM startup and making
     * adjustments when RADIUS configuration changes occur in the openAM admin console.
     */
    public static final String COORDINATION_THREAD_NAME = "RADIUS-{0}";

    /**
     * The name of the thread that binds to the port, listens for requests, and launches request handlers. The port on
     * which we listen is included in the name when starting the listener.
     */
    public static final String LISTENER_THREAD_NAME = "RADIUS-{0,number,#####}-Listener";

    /**
     * The name of the thread that handles requests.
     */
    public static final String REQUEST_HANDLER_THREAD_NAME = "RADIUS-Request-Handler-{0,number,#####}";

    /**
     * The maximum radius packet size as per section 3 of rfc 2865.
     */
    public static final int MAX_PACKET_SIZE = 4096;

    /**
     * The time period in seconds allowed for the RADIUS request handling pool to finish process current requests before
     * another warning message will be issued to log indicating that the pool is taking longer than allowed.
     */
    public static final long THREAD_POOL_SHUTDOWN_WAIT_SECONDS = 5;
}
