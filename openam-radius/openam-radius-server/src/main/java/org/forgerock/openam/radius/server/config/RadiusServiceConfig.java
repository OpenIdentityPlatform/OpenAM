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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the suite of configuration from the admin console for rapid determination of whether packets should be dropped
 * or accepted and processed.
 */
public class RadiusServiceConfig {
    /**
     * Configuration properties of the thread pool for handling requests.
     */
    private final ThreadPoolConfig threadPoolConfig;

    /**
     * The set of configured clients.
     */
    private final Map<String, ClientConfig> clients = new HashMap<String, ClientConfig>();

    /**
     * Whether the port should be opened and we should be listening for incoming UDP packet requests. By default we set
     * it to false when instantiated and then set the value to reflect what is
     */
    private final boolean isEnabled;

    /**
     * The port address on which we should be listening when enabled.
     */
    private final int port;

    /**
     * Instance created from loading handlerConfig from openAM's admin console constructs.
     *
     * @param isEnabled
     *            - The RADIUS Server will only open a port and listen for requests when enabled.
     * @param port
     *            - The UDP port on which each OpenAM server will listen for RADIUS Access-Request packets
     * @param poolCfg
     *            - the configuration for the thread pool to be used by the RADIUS server service.
     * @param clientConfigs
     *            - a number of <code>ClientConfig</code> objects, one for each client that will be connecting to the
     *            radius server.
     */
    public RadiusServiceConfig(boolean isEnabled, int port, ThreadPoolConfig poolCfg, ClientConfig... clientConfigs) {
        this.isEnabled = isEnabled;
        this.port = port;
        this.threadPoolConfig = poolCfg;

        for (final ClientConfig c : clientConfigs) {
            this.clients.put(c.getIpaddr(), c);
        }
    }

    /**
     * Get the thread pool configuration values.
     *
     * @return the configuration for the thread pool.
     */
    public ThreadPoolConfig getThreadPoolConfig() {
        return this.threadPoolConfig;
    }

    /**
     * Returns the defined client for the given IP address or null if not client for that IP address is defined.
     *
     * @param ipAddress
     *            the ipAddress of the client to be found.
     * @return the <code>ClientConfig</code> that has the ipAddress passed to <code>ipAddress</code> or null if no
     *         client has that ipAddress.
     */
    public ClientConfig findClient(String ipAddress) {
        return clients.get(ipAddress);
    }

    /**
     * Returns true if the RADIUS service should have an open UDP Datagram Channel listening for incoming packets.
     * Returns false if the RADIUS service should NOT be listening for and accepting packets.
     *
     * @return true if the RADIUS server is enabled, false if not.
     */
    public boolean isEnabled() {
        return this.isEnabled;
    }

    /**
     * The port to which the RADIUS service should have a bound Datagram Channel listening for incoming packets if
     * isEnabled() returns true.
     *
     * @return the port that the RADIUS server service should listen for Datagrams on.
     */
    public int getPort() {
        return this.port;
    }

    @Override
    public String toString() {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        final ThreadPoolConfig pc = getThreadPoolConfig();
        pw.print("[" + this.getClass().getSimpleName() + " " + (isEnabled() ? "YES" : "NO") + " " + getPort() + " P( "
                + pc.getCoreThreads() + ", " + pc.getMaxThreads() + ", " + pc.getKeepAliveSeconds() + ", "
                + pc.getQueueSize() + ")");
        for (final Map.Entry<String, ClientConfig> ent : clients.entrySet()) {
            final ClientConfig c = ent.getValue();
            pw.print(", C( "
                    + c.getIpaddr()
                    + "="
                    + c.getName()
                    + ", "
                    + c.getSecret()
                    + ", "
                    + c.isLogPackets()
                    + ", "
                    + (c.isClassIsValid() ? c.getAccessRequestHandlerClass().getName() : "not-found: "
                            + c.getAccessRequestHandlerClassname()) + ", " + c.getHandlerConfig() + ")");
        }
        pw.println("]");
        pw.flush();
        return sw.toString();

    }
}
