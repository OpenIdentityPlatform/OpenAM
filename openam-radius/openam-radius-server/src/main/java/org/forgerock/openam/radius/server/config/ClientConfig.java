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

import java.util.Properties;

/**
 * Holds information for a RADIUS client that is allowed to connect to this RADIUS server to perform authentication.
 */
public class ClientConfig {
    /**
     * The name of the client used solely for associating configuration and log messages with a given NAS server.
     */
    private String name;

    /**
     * The IP address from which incoming packets must be to be associated with this client.
     */
    private String ipaddr;

    /**
     * The shared secret used by both client and server for encryption and decryption and signing of the packets.
     */
    private String secret;

    /**
     * The declared classname for the client. This is what is declared for the client and does not indicated whether or
     * not the class is found.
     */
    private String accessRequestHandlerClassname;

    /**
     * The class declared for this client to handle requests and that implements the AccessRequestHandler interface. May
     * be null if the class declared for the client was not found by the classloader.
     */
    private Class accessRequestHandlerClass;

    /**
     * Indicates if the classname specified for the client was load-able and implemented the proper interface.
     */
    private boolean classIsValid;

    /**
     * The set of declared properties to be passed to the declared handler class immediately after instantiation and
     * before handling.
     */
    private Properties handlerConfig;

    /**
     * Indicates if packet contents for this client should be dumped to log for troubleshooting.
     */
    private boolean logPackets = false;

    /**
     * Get the name of the client. The name is used solely for associating configuration and log messages with a given
     * NAS server.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the name of the client. The name is used solely for associating configuration and log messages with a given
     * NAS server.
     *
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the IP address from which incoming packets must be to be associated with this client.
     *
     * @return the ipaddr
     */
    public String getIpaddr() {
        return this.ipaddr;
    }

    /**
     * Set the IP address from which incoming packets must be to be associated with this client.
     *
     * @param ipaddr
     *            the ipaddr to set
     */
    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }

    /**
     * Get the shared secret used by both client and server for encryption and decryption and signing of the packets.
     *
     * @return the secret
     */
    public String getSecret() {
        return this.secret;
    }

    /**
     * Set the shared secret used by both client and server for encryption and decryption and signing of the packets.
     *
     * @param secret
     *            the secret to set
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Get the declared classname for the client. This is what is declared for the client and does not indicated whether
     * or not the class is found.
     *
     * @return the classname
     */
    public String getAccessRequestHandlerClassname() {
        return this.accessRequestHandlerClassname;
    }

    /**
     * Set the declared classname for the client. This is what is declared for the client and does not indicated whether
     * or not the class is found.
     *
     * @param classname
     *            the classname to set
     */
    public void setAccessRequestHandlerClassname(String classname) {
        this.accessRequestHandlerClassname = classname;
    }

    /**
     * Get the class declared for this client to handle requests and that implements the AccessRequestHandler interface.
     * May be null if the class declared for the client was not found by the classloader.
     *
     * @return the clazz
     */
    public Class getAccessRequestHandlerClass() {
        return this.accessRequestHandlerClass;
    }

    /**
     * Set the class declared for this client to handle requests and that implements the AccessRequestHandler interface.
     * May be null if the class declared for the client was not found by the classloader.
     *
     * @param clazz
     *            the clazz to set
     */
    public void setAccessRequestHandler(Class clazz) {
        this.accessRequestHandlerClass = clazz;
    }

    /**
     * Indicates if the classname specified for the client was load-able and implemented the proper interface.
     *
     * @return the classIsValid
     */
    public boolean isClassIsValid() {
        return this.classIsValid;
    }

    /**
     * Sets a value to indicate if the classname specified for the client was load-able and implemented the proper
     * interface.
     *
     * @param classIsValid
     *            the classIsValid to set
     */
    public void setClassIsValid(boolean classIsValid) {
        this.classIsValid = classIsValid;
    }

    /**
     * Get the set of declared properties to be passed to the declared handler class immediately after instantiation and
     * before handling.
     *
     * @return the handlerConfig
     */
    public Properties getHandlerConfig() {
        return this.handlerConfig;
    }

    /**
     * Set the set of declared properties to be passed to the declared handler class immediately after instantiation and
     * before handling.
     *
     * @param handlerConfig
     *            the handlerConfig to set
     */
    public void setHandlerConfig(Properties handlerConfig) {
        this.handlerConfig = handlerConfig;
    }

    /**
     * Indicates if packet contents for this client should be dumped to log for troubleshooting.
     *
     * @return true if the contents should be dumped to log, false if not.
     */
    public boolean isLogPackets() {
        return this.logPackets;
    }

    /**
     * Set a value to indicate if a packet's contents for this client should be dumped to log for troubleshooting.
     *
     * @param logPackets
     *            the logPackets to set
     */
    public void setLogPackets(boolean logPackets) {
        this.logPackets = logPackets;
    }
}
