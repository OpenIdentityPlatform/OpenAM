/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.forgerock.openam.server.engine.tomcat;

import org.apache.catalina.*;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.AbstractHttp11JsseProtocol;
import org.apache.tomcat.util.IntrospectionUtils;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.util.HashMap;
import java.util.Map;

/**
 * This is an improvement to Embedded Tomcat.
 * The objective is to provide a cleaner API for Developers embedding TC.
 * <p/>
 * Tomcat is a trademark of the Apache Software Foundation (http://apache.org)
 */
@SuppressWarnings("unused")
public class BetterTomcat {
    private Tomcat tomcat = new Tomcat();
    private static Map<String, Connector> connectors = new HashMap<String, Connector>();
    private boolean unpackWars = true;

    public BetterTomcat() {
        // Override the default Tomcat connector. Otherwise a Connector on 8080 will be started
        tomcat.setConnector(new Connector(Protocol.HTTP_11.getProtocolName()));
//        setDefaultHost("defaultHost");
//        setBaseDir(".");
    }

    public BetterTomcat(int port) {
        this();
        addConnector(Protocol.HTTP_11, null, port);
    }

    /**
     * Indicates whether WAR files should be unpacked or not
     *
     * @param unpackWars true - unpackWars
     */
    public void setUnpackWars(boolean unpackWars) {
        this.unpackWars = unpackWars;
    }

    /**
     * Start the server.
     *
     * @throws LifecycleException If an irrecoverable error occurs while starting
     */
    public void start() throws LifecycleException {
        tomcat.start();
    }

    /**
     * Start the server.
     *
     * @throws LifecycleException If an irrecoverable error occurs while starting
     */
    public void startAndWait() throws LifecycleException {
        tomcat.start();
        tomcat.getServer().await();
    }

    /**
     * Stop the server.
     *
     * @throws LifecycleException If an irrecoverable error occurs while stopping
     */
    public void stop() throws LifecycleException {
        tomcat.stop();
    }

    public Host getHost() {
        Host host = tomcat.getHost();
        ((StandardHost) host).setUnpackWARs(unpackWars);
        return host;
    }

    /**
     * Add a webapp using normal WEB-INF/web.xml if found.
     *
     * @param contextPath    The context of the webapp, e.g., /foo
     * @param webappFilePath The file path of the webapp. Can be the path to a WAR file or a
     *                       directory,
     *                       e.g.,
     *                       1. /home/azeez/bettertomcat/foo.war
     *                       2. /home/azeez/bettertomcat/foo
     * @return new Context   The Context of the deployed webapp
     * @throws BetterTomcatException If webapp deployment fails
     */
    public Context addWebapp(String contextPath,
                             String webappFilePath) throws BetterTomcatException {

        Context context;
        try {
            context = tomcat.addWebapp(contextPath, webappFilePath);
            if (context.getState().equals(LifecycleState.STOPPED)) {
                throw new BetterTomcatException("Webapp " + context + " failed to deploy");
            }
            if (!unpackWars) {
                context.addParameter("antiJARLocking", "false");
                context.addParameter("antiResourceLocking", "false");
            }
            return context;
        } catch (ServletException e) {
            throw new BetterTomcatException("Webapp failed to deploy", e);
        }
    }

    /**
     * Add a webapp to a particular Host
     *
     * @param host           The Host to which this webapp is added to
     * @param contextPath    The context of the webapp, e.g., /foo
     * @param webappFilePath The file path of the webapp. Can be the path to a WAR file or a
     *                       directory,
     *                       e.g.,
     *                       1. /home/azeez/bettertomcat/foo.war
     *                       2. /home/azeez/bettertomcat/foo
     * @return new Context   The Context of the deployed webapp
     */
    public Context addWebapp(Host host, String contextPath, String webappFilePath) {
        return tomcat.addWebapp(host, contextPath, webappFilePath);
    }

    /**
     * Add a context - programmatic mode, no web.xml used.
     * <p/>
     * API calls equivalent with web.xml:
     * <p/>
     * context-param
     * ctx.addParameter("name", "value");
     * <p/>
     * <p/>
     * error-page
     * ErrorPage ep = new ErrorPage();
     * ep.setErrorCode(500);
     * ep.setLocation("/error.html");
     * ctx.addErrorPage(ep);
     * <p/>
     * ctx.addMimeMapping("ext", "type");
     * <p/>
     * Note: If you reload the Context, all your configuration will be lost. If
     * you need reload support, consider using a LifecycleListener to provide
     * your configuration.
     * <p/>
     *
     * @param contextPath The context of the webapp. "" for root context.
     * @param baseDir     base dir for the context, for static files. Must exist,
     *                    relative to the server home
     * @return new Context   The Context of the deployed webapp
     */
    public Context addContext(String contextPath, String baseDir) {
        return tomcat.addContext(contextPath, baseDir);
    }

    public Context addContext(Host host, String contextPath, String dir) {
        return tomcat.addContext(host, contextPath, dir);
    }

    /**
     * Equivalent with
     * <servlet><servlet-name><servlet-class>.
     * <p/>
     * In general it is better/faster to use the method that takes a
     * Servlet as param - this one can be used if the servlet is not
     * commonly used, and want to avoid loading all deps.
     * ( for example: jsp servlet )
     * <p/>
     * You can customize the returned servlet, ex:
     * <p/>
     * wrapper.addInitParameter("name", "value");
     *
     * @param contextPath  Context to add Servlet to
     * @param servletName  Servlet name (used in mappings)
     * @param servletClass The class to be used for the Servlet
     * @return The wrapper for the servlet
     */
    public Wrapper addServlet(String contextPath,
                              String servletName,
                              String servletClass) {
        return tomcat.addServlet(contextPath, servletName, servletClass);
    }

    /**
     * Add an existing Servlet to the context with no class.forName or
     * initialisation.
     *
     * @param contextPath Context to add Servlet to
     * @param servletName Servlet name (used in mappings)
     * @param servlet     The Servlet to add
     * @return The wrapper for the servlet
     */
    public Wrapper addServlet(String contextPath,
                              String servletName,
                              Servlet servlet) {
        return tomcat.addServlet(contextPath, servletName, servlet);
    }

    /**
     * Enables JNDI naming which is disabled by default. Server must implement
     * {@link org.apache.catalina.Lifecycle} in order for the
     * {@link org.apache.catalina.core.NamingContextListener} to be used.
     */
    public void enableNaming() {
        tomcat.enableNaming();
    }

    public Connector addConnector(int port) {
        return addConnector(Protocol.HTTP_11, null, port);
    }

    /**
     * Get a Tomcat Connector. If a connector with the Tomcat connector does not exist, create a new
     * one.
     *
     * @param protocol The protocol of the connector.
     * @param address  The IP address of the network interface to which this connector should bind
     *                 to. Specify this as null if the connector should bind to all network interfaces.
     * @param port     The port on which this connector has to be run
     * @return The Tomcat connector
     */
    public Connector addConnector(Protocol protocol, String address, int port) {
        Connector connector = connectors.get(protocol + "-" + address + "-" + port);
        if (connector == null) {
            connector = new Connector(protocol.getProtocolClass());
            if (address != null) {
                IntrospectionUtils.setProperty(connector, "address", address);
            }
            connector.setPort(port);
            connector.setEnableLookups(true);
            connector.setProperty("bindOnInit", "false");
            if (protocol.equals(Protocol.HTTPS_11) || protocol.equals(Protocol.HTTPS_11_NIO)) {
                connector.setSecure(true);
                connector.setAttribute("SSLEnabled", "true");
                connector.setScheme("https");
            }
            tomcat.getService().addConnector(connector);
        }
        return connector;
    }

    public void setClientAuth(Connector connector, String clientAuth) {
        ((AbstractHttp11JsseProtocol) connector.getProtocolHandler()).setClientAuth(clientAuth);
    }

    public Connector getConnector(Protocol protocol, String address, int port) {
        return addConnector(protocol, address, port);
    }

    public void setBaseDir(String baseDir) {
        tomcat.setBaseDir(baseDir);
    }

    public void setDefaultHost(String defaultHostName) {
        tomcat.setHostname(defaultHostName);
        tomcat.getEngine().setDefaultHost(defaultHostName);
    }

    public void setDefaultRealm(Realm realm) {
        tomcat.setDefaultRealm(realm);
    }

    /**
     * The valid protocol types
     */
    @SuppressWarnings("unused")
    public static enum Protocol {
        HTTP_11("HTTP/1.1", "HTTP/1.1"),
        HTTPS_11("HTTPS/1.1", "HTTP/1.1"),
        HTTP_11_NIO("HTTP/1.1/NIO", "org.apache.coyote.http11.Http11NioProtocol"),
        HTTPS_11_NIO("HTTPS/1.1/NIO", "org.apache.coyote.http11.Http11NioProtocol"),
        HTTP_11_APR("HTTP/1.1/APR", "org.apache.coyote.http11.Http11AprProtocol"),
        HTTPS_11_APR("HTTPS/1.1/APR", "org.apache.coyote.http11.Http11AprProtocol"),
        MEMORY("memory", "org.apache.coyote.memory.MemoryProtocolHandler"),
        AJP("ajp", "org.apache.coyote.ajp.AjpProtocol");

        private String protocolName;
        private String protocolClass;

        Protocol(String protocolName, String protocolClass) {
            this.protocolName = protocolName;
            this.protocolClass = protocolClass;
        }

        public String getProtocolName() {
            return protocolName;
        }

        public String getProtocolClass() {
            return protocolClass;
        }
    }

    /**
     * Returns the wrapped Tomcat instance. This should be used only when advanced functionality
     * is required
     *
     * @return The Tomcat instance which can be used by advanced users who wish to gain more control
     */
    public Tomcat getTomcat() {
        return tomcat;
    }
}

