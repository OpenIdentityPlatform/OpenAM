/**
 * Copyright 2005-2024 Qlik
 *
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: Apache 2.0 or or EPL 1.0 (the "Licenses"). You can
 * select the license that you prefer but you may not use this file except in
 * compliance with one of these Licenses.
 *
 * You can obtain a copy of the Apache 2.0 license at
 * http://www.opensource.org/licenses/apache-2.0
 *
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0
 *
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 *
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * https://restlet.talend.com/
 *
 * Restlet is a registered trademark of QlikTech International AB.
 */

package org.forgerock.openam.rest.jakarta.servlet;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.List;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.restlet.Application;
import org.restlet.Client;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Server;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.engine.Engine;
import org.restlet.engine.adapter.HttpServerHelper;
import org.restlet.engine.adapter.ServerCall;
import org.restlet.engine.component.ComponentContext;
import org.forgerock.openam.rest.jakarta.servlet.internal.ServletCall;
import org.forgerock.openam.rest.jakarta.servlet.internal.ServletWarClient;
import org.restlet.representation.Representation;
import org.restlet.routing.Route;
import org.restlet.routing.TemplateRoute;
import org.restlet.routing.VirtualHost;

/**
 * Servlet acting like an HTTP server connector. This Servlet can deploy
 * multiple Restlet applications or components. This allows you to reuse an
 * existing standalone Restlet Component, potentially containing several
 * applications, and declaring client connectors, for example for the CLAP, FILE
 * or HTTP protocols.<br>
 * <br>
 * There are three separate ways to configure the deployment using this Servlet.
 * Please note that you can also combine the two first of them whereas the last
 * one is a full alternative. They are described below by order of priority:
 * <table>
 * <tr>
 * <th>Mode</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td><b>1</b></td>
 * <td>A "/WEB-INF/restlet.xml" file exists and contains a valid XML
 * configuration as described in the documentation of the {@link Component}
 * class. It is used to instantiate and attach the described component,
 * contained applications and connectors. Please note that you can combine the
 * usage of such configuration file and method 2.</td>
 * </tr>
 * <tr>
 * <td><b>2</b></td>
 * <td>The "/WEB-INF/web.xml" file contains a parameter named
 * "org.restlet.component". Its value must be the path of a class that inherits
 * from {@link Component}. It is used to instantiate and attach the described
 * component, contained applications and connectors. Please note that you can
 * combine the definition of your own custom Component subclass and method 1.</td>
 * </tr>
 * <tr>
 * <td><b>3</b></td>
 * <td>The "/WEB-INF/web.xml" file contains a parameter named
 * "org.restlet.application". Its value must be the path of a class that
 * inherits from {@link Application}. It is used to instantiate the application
 * and to attach it to an implicit Restlet Component.</td>
 * </tr>
 * </table>
 * <br>
 * In deployment mode 3, you can also add an optional "org.restlet.clients"
 * context parameter that contains a space separated list of client protocols
 * supported by the underlying component. For each one, a new client connector
 * is added to the implicit {@link Component} instance.<br>
 *
 * Here is an example configuration to attach two separate applications:
 *
 * <pre>
 * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
 * &lt;web-app xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot;
 *         xmlns=&quot;http://java.sun.com/xml/ns/javaee&quot; xmlns:web=&quot;http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd&quot;
 *         xsi:schemaLocation=&quot;http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd&quot;
 *         id=&quot;WebApp_ID&quot; version=&quot;2.5&quot;&gt;
 *
 *         &lt;display-name&gt;Restlet adapters&lt;/display-name&gt;
 *
 *         &lt;servlet&gt;
 *                 &lt;servlet-name&gt;Restlet1&lt;/servlet-name&gt;
 *                 &lt;servlet-class&gt;org.restlet.ext.servlet.ServerServlet&lt;/servlet-class&gt;
 *                 &lt;init-param&gt;
 *                         &lt;param-name&gt;org.restlet.application&lt;/param-name&gt;
 *                         &lt;param-value&gt;test.MyApplication1&lt;/param-value&gt;
 *                 &lt;/init-param&gt;
 *         &lt;/servlet&gt;
 *
 *         &lt;servlet-mapping&gt;
 *                 &lt;servlet-name&gt;Restlet1&lt;/servlet-name&gt;
 *                 &lt;url-pattern&gt;/1/*&lt;/url-pattern&gt;
 *         &lt;/servlet-mapping&gt;
 *
 *         &lt;servlet&gt;
 *                 &lt;servlet-name&gt;Restlet2&lt;/servlet-name&gt;
 *                 &lt;servlet-class&gt;org.restlet.ext.servlet.ServerServlet&lt;/servlet-class&gt;
 *                 &lt;init-param&gt;
 *                         &lt;param-name&gt;org.restlet.application&lt;/param-name&gt;
 *                         &lt;param-value&gt;test.MyApplication2&lt;/param-value&gt;
 *                 &lt;/init-param&gt;
 *         &lt;/servlet&gt;
 *
 *         &lt;servlet-mapping&gt;
 *                 &lt;servlet-name&gt;Restlet2&lt;/servlet-name&gt;
 *                 &lt;url-pattern&gt;/2/*&lt;/url-pattern&gt;
 *         &lt;/servlet-mapping&gt;
 *
 * &lt;/web-app&gt;
 * </pre>
 *
 * Now, here is a more detailed template configuration showing you more
 * configuration options:
 *
 * <pre>
 * &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
 * &lt;web-app xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot;
 *         xmlns=&quot;http://java.sun.com/xml/ns/javaee&quot; xmlns:web=&quot;http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd&quot;
 *         xsi:schemaLocation=&quot;http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd&quot;
 *         id=&quot;WebApp_ID&quot; version=&quot;2.5&quot;&gt;
 *
 *         &lt;display-name&gt;Restlet adapters&lt;/display-name&gt;
 *
 *         &lt;!-- Servlet to Restlet adapter declaration (Mandatory) --&gt;
 *         &lt;servlet&gt;
 *                 &lt;servlet-name&gt;RestletAdapter&lt;/servlet-name&gt;
 *                 &lt;servlet-class&gt;org.restlet.ext.servlet.ServerServlet&lt;/servlet-class&gt;
 *
 *                 &lt;!-- Your component class name (Optional - For mode 2) --&gt;
 *                 &lt;init-param&gt;
 *                         &lt;param-name&gt;org.restlet.component&lt;/param-name&gt;
 *                         &lt;param-value&gt;test.MyComponent&lt;/param-value&gt;
 *                 &lt;/init-param&gt;
 *
 *                 &lt;!-- Your application class name (Optional - For mode 3) --&gt;
 *                 &lt;init-param&gt;
 *                         &lt;param-name&gt;org.restlet.application&lt;/param-name&gt;
 *                         &lt;param-value&gt;test.MyApplication&lt;/param-value&gt;
 *                 &lt;/init-param&gt;
 *
 *                 &lt;!-- List of supported client protocols (Optional - Only in mode 3) --&gt;
 *                 &lt;init-param&gt;
 *                         &lt;param-name&gt;org.restlet.clients&lt;/param-name&gt;
 *                         &lt;param-value&gt;HTTP HTTPS FILE&lt;/param-value&gt;
 *                 &lt;/init-param&gt;
 *
 *                 &lt;!-- Add the Servlet context path to routes (Optional) --&gt;
 *                 &lt;init-param&gt;
 *                         &lt;param-name&gt;org.restlet.autoWire&lt;/param-name&gt;
 *                         &lt;param-value&gt;true&lt;/param-value&gt;
 *                 &lt;/init-param&gt;
 *         &lt;/servlet&gt;
 *
 *         &lt;!-- Mapping catching all requests on a given path (Mandatory) --&gt;
 *         &lt;servlet-mapping&gt;
 *                 &lt;servlet-name&gt;RestletAdapter&lt;/servlet-name&gt;
 *                 &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 *         &lt;/servlet-mapping&gt;
 *
 * &lt;/web-app&gt;
 * </pre>
 *
 * Note that the enumeration of "initParameters" of your Servlet will be copied
 * to the "context.parameters" property of your Restlet Application. This way,
 * you can pass additional initialization parameters to your application, and
 * maybe share them with other Servlets.<br>
 * <br>
 * An additional boolean parameter called "org.restlet.autoWire" allows you to
 * control the way your customized Component fits in the context of the wrapping
 * Servlet. The root cause is that both your Servlet Container and your Restlet
 * Component handle part of the URI routing, respectively to the right Servlet
 * and to the right virtual host and Restlets (most of the time Application
 * instances).<br>
 * <br>
 * When a request reaches the Servlet container, it is first routed according to
 * its web.xml configuration (i.e. declared virtual hosts and webapp context
 * path which is generally the name of the webapp WAR file). Once the incoming
 * request reaches the ServerServlet and the wrapped Restlet Component, its URI
 * is, for the second time, entirely subject to a separate routing chain. It
 * begins with the virtual hosts, then continues to the URI pattern used when
 * attaching Restlets to the host. The important conclusion is that both routing
 * configurations must be consistent in order to work fine.<br>
 * <br>
 * In deployment mode 3, the context path of the servlet is automatically added.
 * That's what we call the auto-wire feature. This is the default case, and is
 * equivalent to setting the value "true" for the "org.restlet.autoWire"
 * parameter as described above. In modes 1 or 2, if you want to manually
 * control the URI wiring, you can disable the auto-wiring by setting the
 * property to "false".<br>
 * <br>
 * Also, a WAR client connector is automatically attached to the parent Restlet
 * component. It lets you access to resources inside your WAR using the uniform
 * interface. Here is an example of WAR URI that can be resolved by this client:
 * "war:///WEB-INF/web.xml". In order to use it, just call the
 * {@link Context#getClientDispatcher()} in your application.<br>
 * <br>
 * Also, the ServletContext is copied into an
 * "org.restlet.ext.servlet.ServletContext" attribute of the Restlet application
 * in case you need access to it.<br>
 * <br>
 * Finally, an "org.restlet.ext.servlet.offsetPath" attribute, containing the
 * computed offset path used to attach applications when (and only when) the
 * auto-wiring feature is set, is added to the component's context.
 *
 * @see <a href="http://www.oracle.com/technetwork/java/javaee/">J2EE home
 *      page</a>
 * @author Jerome Louvel
 */
public class ServerServlet extends HttpServlet {
    /**
     * Name of the attribute key containing a reference to the current
     * application.
     */
    private static final String APPLICATION_KEY = "org.restlet.application";

    /**
     * The Servlet context initialization parameter's name containing a boolean
     * value. "true" indicates that all applications will be attached to the
     * Component's virtual hosts with the Servlet Context path value.
     */
    private static final String AUTO_WIRE_KEY = "org.restlet.autoWire";

    /** The default value for the AUTO_WIRE_KEY parameter. */
    private static final String AUTO_WIRE_KEY_DEFAULT = "true";

    /**
     * Name of the attribute key containing a list of supported client
     * protocols.
     */
    private static final String CLIENTS_KEY = "org.restlet.clients";

    /**
     * Name of the attribute key containing a reference to the current
     * component.
     */
    private static final String COMPONENT_KEY = "org.restlet.component";

    /**
     * The Servlet context initialization parameter's name containing the name
     * of the Servlet context attribute that should be used to store the Restlet
     * Application instance.
     */
    private static final String NAME_APPLICATION_ATTRIBUTE = "org.restlet.attribute.application";

    /** The default value for the NAME_APPLICATION_ATTRIBUTE parameter. */
    private static final String NAME_APPLICATION_ATTRIBUTE_DEFAULT = "org.restlet.ext.servlet.ServerServlet.application";

    /**
     * The Servlet context initialization parameter's name containing the name
     * of the Servlet context attribute that should be used to store the Restlet
     * Component instance.
     */
    private static final String NAME_COMPONENT_ATTRIBUTE = "org.restlet.attribute.component";

    /** The default value for the NAME_COMPONENT_ATTRIBUTE parameter. */
    private static final String NAME_COMPONENT_ATTRIBUTE_DEFAULT = "org.restlet.ext.servlet.ServerServlet.component";

    /**
     * Name of the attribute containing the computed offset path used to attach
     * applications when (and only when) the auto-wiring feature is set, is
     * added to the component's context.
     */
    private static final String NAME_OFFSET_PATH_ATTRIBUTE = "org.restlet.ext.servlet.offsetPath";

    /**
     * The Servlet context initialization parameter's name containing the name
     * of the Servlet context attribute that should be used to store the HTTP
     * server connector instance.
     */
    private static final String NAME_SERVER_ATTRIBUTE = "org.restlet.attribute.server";

    /** The default value for the NAME_SERVER_ATTRIBUTE parameter. */
    private static final String NAME_SERVER_ATTRIBUTE_DEFAULT = "org.restlet.ext.servlet.ServerServlet.server";

    /** Serial version identifier. */
    private static final long serialVersionUID = 1L;

    /** The associated Restlet application. */
    private volatile transient Application application;

    /** The associated Restlet component. */
    private volatile transient Component component;

    /** The associated HTTP server helper. */
    private volatile transient HttpServerHelper helper;

    /**
     * Constructor.
     */
    public ServerServlet() {
        this.application = null;
        this.component = null;
        this.helper = null;
    }

    /**
     * Creates the single Application used by this Servlet.
     *
     * @param parentContext
     *            The parent component context.
     *
     * @return The newly created Application or null if unable to create
     */
    protected Application createApplication(Context parentContext) {
        Application application = null;

        // Try to instantiate a new target application
        // First, find the application class name
        String applicationClassName = getInitParameter(APPLICATION_KEY, null);

        // Load the application class using the given class name
        if (applicationClassName != null) {
            try {
                Class<?> targetClass = loadClass(applicationClassName);

                try {
                    // Instantiate an application with the default constructor
                    // then invoke the setContext method.
                    application = (Application) targetClass.getConstructor()
                            .newInstance();

                    // Set the context based on the Servlet's context
                    application.setContext(parentContext.createChildContext());
                } catch (NoSuchMethodException e) {
                    log("[Restlet] ServerServlet couldn't invoke the constructor of the target class. Please check this class has a constructor without parameter. The constructor with a parameter of type Context will be used instead.");
                    // The constructor with the Context parameter does not
                    // exist. Create a new instance of the application class by
                    // invoking the constructor with the Context parameter.
                    application = (Application) targetClass.getConstructor(
                            Context.class).newInstance(
                            parentContext.createChildContext());
                }
            } catch (ClassNotFoundException e) {
                log("[Restlet] ServerServlet couldn't find the target class. Please check that your classpath includes "
                        + applicationClassName, e);

            } catch (InstantiationException e) {
                log("[Restlet] ServerServlet couldn't instantiate the target class. Please check this class has an empty constructor "
                        + applicationClassName, e);
            } catch (IllegalAccessException e) {
                log("[Restlet] ServerServlet couldn't instantiate the target class. Please check that you have to proper access rights to "
                        + applicationClassName, e);
            } catch (NoSuchMethodException e) {
                log("[Restlet] ServerServlet couldn't invoke the constructor of the target class. Please check this class has a constructor with a single parameter of Context "
                        + applicationClassName, e);
            } catch (InvocationTargetException e) {
                log("[Restlet] ServerServlet couldn't instantiate the target class. An exception was thrown while creating "
                        + applicationClassName, e);
            }
        }

        return application;
    }

    /**
     * Creates a new Servlet call wrapping a Servlet request/response couple and
     * a Server connector.
     *
     * @param server
     *            The Server connector.
     * @param request
     *            The Servlet request.
     * @param response
     *            The Servlet response.
     * @return The new ServletCall instance.
     */
    protected ServerCall createCall(Server server, HttpServletRequest request,
                                    HttpServletResponse response) {
        return new ServletCall(server, request, response);
    }

    /**
     * Creates the single Component used by this Servlet.
     *
     * @return The newly created Component or null if unable to create.
     */
    @SuppressWarnings("deprecation")
    protected Component createComponent() {
        // Detect customized Component
        String componentClassName = getInitParameter(COMPONENT_KEY, null);
        Class<?> targetClass = null;
        Component component = null;

        if (componentClassName != null) {
            try {
                targetClass = loadClass(componentClassName);
            } catch (ClassNotFoundException e) {
                log("[Restlet] ServerServlet couldn't find the target component class. Please check that your classpath includes "
                        + componentClassName, e);
            }
        }
        log("[Restlet] ServerServlet: component class is " + componentClassName);

        // Detect the configuration of Component using restlet.xml file.
        Client warClient = createWarClient(new Context(), getServletConfig());
        Response response = warClient.handle(new Request(Method.GET,
                "war:///WEB-INF/restlet.xml"));

        boolean xmlConfiguration = response.getStatus().isSuccess()
                && response.isEntityAvailable();

        if (targetClass != null) {
            try {
                if (xmlConfiguration) {
                    @SuppressWarnings("unchecked")
                    Constructor<? extends Component> ctor = ((Class<? extends Component>) targetClass)
                            .getConstructor(Representation.class);

                    log("[Restlet] ServerServlet: configuring custom component from war:///WEB-INF/restlet.xml");
                    component = (Component) ctor.newInstance(response
                            .getEntity());
                } else {
                    @SuppressWarnings("unchecked")
                    Constructor<? extends Component> ctor = ((Class<? extends Component>) targetClass)
                            .getConstructor();

                    log("[Restlet] ServerServlet: instantiating custom component");
                    component = (Component) ctor.newInstance();
                }
            } catch (IllegalAccessException e) {
                log("[Restlet] ServerServlet couldn't instantiate the target class. Please check that you have proper access rights to "
                        + componentClassName, e);
            } catch (InvocationTargetException e) {
                log("[Restlet] ServerServlet encountered an exception instantiating the target class "
                        + componentClassName, e.getTargetException());
            } catch (InstantiationException e) {
                log(String.format(
                        "[Restlet] ServerServlet couldn't instantiate the target class. Please check that %s has %s.",
                        componentClassName,
                        ((xmlConfiguration) ? "a constructor taking a Representation as single parameter"
                                : "an empty constructor")), e);
            } catch (NoSuchMethodException e) {
                log(String.format(
                        "[Restlet] ServerServlet couldn't instantiate the target class. Please check that %s has %s.",
                        componentClassName,
                        ((xmlConfiguration) ? "a constructor taking Representation as single parameter"
                                : "an empty constructor")), e);
            }
        } else if (xmlConfiguration) {
            log("[Restlet] ServerServlet: configuring component from war:///WEB-INF/restlet.xml");
            component = new Component(response.getEntity());
        }

        // Create the default Component
        if (component == null) {
            component = new Component();

            // The status service is disabled by default.
            component.getStatusService().setEnabled(false);

            // Define the list of supported client protocols.
            final String clientProtocolsString = getInitParameter(CLIENTS_KEY,
                    null);
            if (clientProtocolsString != null) {
                final String[] clientProtocols = clientProtocolsString
                        .split(" ");
                Client client;

                for (final String clientProtocol : clientProtocols) {
                    client = new Client(clientProtocol);

                    if (client.isAvailable()) {
                        component.getClients().add(client);
                    } else {
                        log("[Restlet] Couldn't find a client connector for protocol "
                                + clientProtocol);
                    }
                }
            }
        }

        return component;
    }

    /**
     * Creates the associated HTTP server handling calls.
     *
     * @param request
     *            The HTTP Servlet request.
     * @return The new HTTP server handling calls.
     */
    protected HttpServerHelper createServer(HttpServletRequest request) {
        HttpServerHelper result = null;
        Component component = getComponent();

        if (component != null) {
            // First, let's create a pseudo server
            Server server = new Server(component.getContext()
                    .createChildContext(), (List<Protocol>) null,
                    this.getLocalAddr(request), this.getLocalPort(request),
                    component);
            result = new HttpServerHelper(server);

            // Change the default adapter
            Context serverContext = server.getContext();
            serverContext.getParameters().add("adapter",
                    "org.restlet.ext.servlet.internal.ServletServerAdapter");

            // Attach the hosted application(s) to the right path
            String uriPattern = this.getContextPath(request)
                    + request.getServletPath();

            if (isDefaultComponent()) {
                if (getApplication() != null) {
                    log("[Restlet] Attaching application: " + getApplication()
                            + " to URI: " + uriPattern);
                    component.getDefaultHost().attach(uriPattern,
                            getApplication());
                }
            } else {
                // According to the mode, configure correctly the component.
                String autoWire = getInitParameter(AUTO_WIRE_KEY,
                        AUTO_WIRE_KEY_DEFAULT);
                if (AUTO_WIRE_KEY_DEFAULT.equalsIgnoreCase(autoWire)) {
                    // Translate all defined routes as much as possible
                    // with the context path only or the full servlet path.

                    // 1- get the offset
                    boolean addContextPath = false;
                    boolean addFullServletPath = false;

                    if (component.getDefaultHost().getRoutes().isEmpty()) {
                        // Case where the default host has a default route (with
                        // an empty pattern).
                        addFullServletPath = component.getDefaultHost()
                                .getDefaultRoute() != null;
                    } else {
                        for (Route route : component.getDefaultHost()
                                .getRoutes()) {
                            if (route instanceof TemplateRoute) {
                                TemplateRoute templateRoute = (TemplateRoute) route;

                                if (templateRoute.getTemplate().getPattern() == null) {
                                    addFullServletPath = true;
                                    continue;
                                }

                                if (!templateRoute.getTemplate().getPattern()
                                        .startsWith(uriPattern)) {
                                    if (!templateRoute
                                            .getTemplate()
                                            .getPattern()
                                            .startsWith(
                                                    request.getServletPath())) {
                                        addFullServletPath = true;
                                    } else {
                                        addContextPath = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (!addContextPath) {
                        for (VirtualHost virtualHost : component.getHosts()) {
                            if (virtualHost.getRoutes().isEmpty()) {
                                // Case where the default host has a default
                                // route (with an empty pattern).
                                addFullServletPath = virtualHost
                                        .getDefaultRoute() != null;
                            } else {
                                for (Route route : virtualHost.getRoutes()) {
                                    if (route instanceof TemplateRoute) {
                                        TemplateRoute templateRoute = (TemplateRoute) route;

                                        if (templateRoute.getTemplate()
                                                .getPattern() == null) {
                                            addFullServletPath = true;
                                            continue;
                                        }

                                        if (!templateRoute.getTemplate()
                                                .getPattern()
                                                .startsWith(uriPattern)) {
                                            if (!templateRoute
                                                    .getTemplate()
                                                    .getPattern()
                                                    .startsWith(
                                                            request.getServletPath())) {
                                                addFullServletPath = true;
                                            } else {
                                                addContextPath = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            if (addContextPath) {
                                break;
                            }
                        }
                    }

                    // 2- Translate all routes.
                    if (addContextPath || addFullServletPath) {
                        String offsetPath = null;

                        if (addContextPath) {
                            offsetPath = this.getContextPath(request);
                        } else {
                            offsetPath = uriPattern;
                        }

                        if (offsetPath != null) {
                            getComponent()
                                    .getContext()
                                    .getAttributes()
                                    .put(NAME_OFFSET_PATH_ATTRIBUTE, offsetPath);
                        }

                        // Shift the default route (if any) of the default host
                        Route defaultRoute = component.getDefaultHost()
                                .getDefaultRoute();

                        if (defaultRoute != null) {
                            if (defaultRoute instanceof TemplateRoute) {
                                TemplateRoute defaultTemplateRoute = (TemplateRoute) defaultRoute;
                                defaultTemplateRoute.getTemplate().setPattern(
                                        offsetPath
                                                + defaultTemplateRoute
                                                .getTemplate()
                                                .getPattern());
                                log("[Restlet] Attaching restlet: "
                                        + defaultRoute.getNext()
                                        + " to URI: "
                                        + offsetPath
                                        + defaultTemplateRoute.getTemplate()
                                        .getPattern());
                            } else {
                                log("[Restlet] Attaching restlet: "
                                        + defaultRoute.getNext());
                            }
                        }

                        // Shift the routes of the default host
                        for (Route route : component.getDefaultHost()
                                .getRoutes()) {
                            if (route instanceof TemplateRoute) {
                                TemplateRoute templateRoute = (TemplateRoute) route;

                                log("[Restlet] Attaching restlet: "
                                        + route.getNext()
                                        + " to URI: "
                                        + offsetPath
                                        + templateRoute.getTemplate()
                                        .getPattern());
                                templateRoute.getTemplate().setPattern(
                                        offsetPath
                                                + templateRoute.getTemplate()
                                                .getPattern());
                            } else {
                                log("[Restlet] Attaching restlet: "
                                        + defaultRoute.getNext());
                            }
                        }

                        for (VirtualHost virtualHost : component.getHosts()) {
                            // Shift the default route (if any) of the virtual
                            // host
                            defaultRoute = virtualHost.getDefaultRoute();
                            if (defaultRoute != null) {
                                if (defaultRoute instanceof TemplateRoute) {
                                    TemplateRoute defaultTemplateRoute = (TemplateRoute) defaultRoute;
                                    defaultTemplateRoute
                                            .getTemplate()
                                            .setPattern(
                                                    offsetPath
                                                            + defaultTemplateRoute
                                                            .getTemplate()
                                                            .getPattern());
                                    log("[Restlet] Attaching restlet: "
                                            + defaultRoute.getNext()
                                            + " to URI: "
                                            + offsetPath
                                            + defaultTemplateRoute
                                            .getTemplate().getPattern());
                                } else {
                                    log("[Restlet] Attaching restlet: "
                                            + defaultRoute.getNext());
                                }
                            }

                            // Shift the routes of the virtual host
                            for (Route route : virtualHost.getRoutes()) {
                                if (route instanceof TemplateRoute) {
                                    TemplateRoute templateRoute = (TemplateRoute) route;

                                    log("[Restlet] Attaching restlet: "
                                            + route.getNext()
                                            + " to URI: "
                                            + offsetPath
                                            + templateRoute.getTemplate()
                                            .getPattern());
                                    templateRoute.getTemplate().setPattern(
                                            offsetPath
                                                    + templateRoute
                                                    .getTemplate()
                                                    .getPattern());
                                } else {
                                    log("[Restlet] Attaching restlet: "
                                            + route.getNext());
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Creates a new client for the WAR protocol.
     *
     * @param context
     *            The parent context.
     * @param config
     *            The Servlet config.
     * @return The new WAR client instance.
     */
    protected Client createWarClient(Context context, ServletConfig config) {
        return new ServletWarClient(context, config.getServletContext());
    }

    @Override
    public void destroy() {
        if ((getComponent() != null) && (getComponent().isStarted())) {
            try {
                getComponent().stop();
            } catch (Exception e) {
                log("Error during the stopping of the Restlet component", e);
            }
        }

        super.destroy();
    }

    /**
     * Returns the application. It creates a new one if none exists.
     *
     * @return The application.
     */
    public Application getApplication() {
        // Lazy initialization with double-check.
        Application result = this.application;

        if (result == null) {
            synchronized (this) {
                result = this.application;
                if (result == null) {
                    // In case a component is explicitly defined, it cannot be
                    // completed.
                    if (isDefaultComponent()) {
                        // Find the attribute name to use to store the
                        // application
                        String applicationAttributeName = getInitParameter(
                                NAME_APPLICATION_ATTRIBUTE,
                                NAME_APPLICATION_ATTRIBUTE_DEFAULT + "."
                                        + getServletName());

                        // Look up the attribute for a target
                        result = (Application) getServletContext()
                                .getAttribute(applicationAttributeName);

                        if (result == null) {
                            result = createApplication(getComponent()
                                    .getContext());
                            init(result);
                            getServletContext().setAttribute(
                                    applicationAttributeName, result);
                        }

                        this.application = result;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns the component. It creates a new one if none exists.
     *
     * @return The component.
     */
    public Component getComponent() {
        // Lazy initialization with double-check.
        Component result = this.component;

        if (result == null) {
            synchronized (this) {
                result = this.component;
                if (result == null) {
                    // Find the attribute name to use to store the component
                    final String componentAttributeName = getInitParameter(
                            NAME_COMPONENT_ATTRIBUTE,
                            NAME_COMPONENT_ATTRIBUTE_DEFAULT + "."
                                    + getServletName());

                    // Look up the attribute for a target
                    result = (Component) getServletContext().getAttribute(
                            componentAttributeName);

                    if (result == null) {
                        result = createComponent();
                        init(result);
                        getServletContext().setAttribute(
                                componentAttributeName, result);
                    }
                }

                this.component = result;
            }
        }

        return result;
    }

    /**
     * Intercepter method need for subclasses such as XdbServerServlet.
     *
     * @param request
     *            The Servlet request.
     * @return The portion of the request URI that indicates the context of the
     *         request.
     */
    protected String getContextPath(HttpServletRequest request) {
        return request.getContextPath();
    }

    /**
     * Returns the value of a given initialization parameter, first from the
     * Servlet configuration, then from the Web Application context.
     *
     * @param name
     *            The parameter name.
     * @param defaultValue
     *            The default to use in case the parameter is not found.
     * @return The value of the parameter or null.
     */
    public String getInitParameter(String name, String defaultValue) {
        String result = getServletConfig().getInitParameter(name);

        if (result == null) {
            result = getServletConfig().getServletContext().getInitParameter(
                    name);
        }

        if (result == null) {
            result = defaultValue;
        }

        return result;
    }

    /**
     * Intercepter method need for subclasses such as XdbServerServlet.
     *
     * @param request
     *            The Servlet request.
     * @return The Internet Protocol (IP) address of the interface on which the
     *         request was received.
     */
    protected String getLocalAddr(HttpServletRequest request) {
        return request.getLocalAddr();
    }

    /**
     * Intercepter method need for subclasses such as XdbServerServlet.
     *
     * @param request
     *            The Servlet request.
     * @return The Internet Protocol (IP) port number of the interface on which
     *         the request was received
     */
    protected int getLocalPort(HttpServletRequest request) {
        return request.getLocalPort();
    }

    /**
     * Returns the associated HTTP server handling calls. It creates a new one
     * if none exists.
     *
     * @param request
     *            The HTTP Servlet request.
     * @return The HTTP server handling calls.
     */
    public HttpServerHelper getServer(HttpServletRequest request) {
        // Lazy initialization with double-check.
        HttpServerHelper result = this.helper;

        if (result == null) {
            synchronized (this) {
                result = this.helper;
                if (result == null) {
                    // Find the attribute name to use to store the server
                    // reference
                    final String serverAttributeName = getInitParameter(
                            NAME_SERVER_ATTRIBUTE,
                            NAME_SERVER_ATTRIBUTE_DEFAULT + "."
                                    + getServletName());

                    // Look up the attribute for a target
                    result = (HttpServerHelper) getServletContext()
                            .getAttribute(serverAttributeName);

                    if (result == null) {
                        result = createServer(request);
                        getServletContext().setAttribute(serverAttributeName,
                                result);
                    }

                    this.helper = result;
                }
            }
        }

        return result;
    }

    @Override
    public void init() throws ServletException {
        if ((getComponent() != null) && (getComponent().isStopped())) {
            try {
                getComponent().start();
            } catch (Exception e) {
                log("Error during the starting of the Restlet Application", e);
            }
        }
    }

    /**
     * Initialize a application. Copies Servlet parameters into the component's
     * context. Copies the ServletContext into an
     * "org.restlet.ext.servlet.ServletContext" attribute.
     *
     * @param application
     *            The application to configure.
     */
    protected void init(Application application) {
        if (application != null) {
            Context applicationContext = application.getContext();

            if (applicationContext != null) {
                // Copies the ServletContext into an attribute
                applicationContext.getAttributes().put(
                        "org.restlet.ext.servlet.ServletContext",
                        getServletContext());

                // Copy all the servlet parameters into the context
                String initParam;

                // Copy all the Servlet component initialization parameters
                jakarta.servlet.ServletConfig servletConfig = getServletConfig();
                for (Enumeration<String> enum1 = servletConfig
                        .getInitParameterNames(); enum1.hasMoreElements();) {
                    initParam = enum1.nextElement();
                    applicationContext.getParameters().add(initParam,
                            servletConfig.getInitParameter(initParam));
                }

                // Copy all the Servlet application initialization parameters
                for (Enumeration<String> enum1 = getServletContext()
                        .getInitParameterNames(); enum1.hasMoreElements();) {
                    initParam = enum1.nextElement();
                    applicationContext.getParameters().add(initParam,
                            getServletContext().getInitParameter(initParam));
                }
            }
        }
    }

    /**
     * Initialize a component. Adds a default WAR client and copies Servlet
     * parameters into the component's context. Copies the ServletContext into
     * an "org.restlet.ext.servlet.ServletContext" attribute.
     *
     * @param component
     *            The component to configure.
     */
    protected void init(Component component) {
        if (component != null) {
            // Complete the configuration of the Component
            // Add the WAR client
            component.getClients()
                    .add(createWarClient(component.getContext(),
                            getServletConfig()));

            // Copy all the servlet parameters into the context
            ComponentContext componentContext = (ComponentContext) component
                    .getContext();

            // Copies the ServletContext into an attribute
            componentContext.getAttributes().put(
                    "org.restlet.ext.servlet.ServletContext",
                    getServletContext());

            // Copy all the Servlet container initialization parameters
            String initParam;
            jakarta.servlet.ServletConfig servletConfig = getServletConfig();

            for (Enumeration<String> enum1 = servletConfig
                    .getInitParameterNames(); enum1.hasMoreElements();) {
                initParam = enum1.nextElement();
                componentContext.getParameters().add(initParam,
                        servletConfig.getInitParameter(initParam));
            }

            // Copy all the Servlet application initialization parameters
            for (Enumeration<String> enum1 = getServletContext()
                    .getInitParameterNames(); enum1.hasMoreElements();) {
                initParam = enum1.nextElement();
                componentContext.getParameters().add(initParam,
                        getServletContext().getInitParameter(initParam));
            }

            // Copy all Servlet's context attributes
            String attributeName;
            for (Enumeration<String> namesEnum = getServletContext()
                    .getAttributeNames(); namesEnum.hasMoreElements();) {
                attributeName = namesEnum.nextElement();
                componentContext.getAttributes().put(attributeName,
                        getServletContext().getAttribute(attributeName));
            }
        }
    }

    /**
     * Indicates if the Component hosted by this Servlet is the default one or
     * one provided by the user.
     *
     * @return True if the Component is the default one, false otherwise.
     */
    private boolean isDefaultComponent() {
        // The Component is provided via an XML configuration file.
        Client client = createWarClient(new Context(), getServletConfig());
        Response response = client.handle(new Request(Method.GET,
                "war:///WEB-INF/restlet.xml"));
        if (response.getStatus().isSuccess() && response.isEntityAvailable()) {
            return false;
        }

        // The Component is provided via a context parameter in the "web.xml"
        // file.
        String componentAttributeName = getInitParameter(COMPONENT_KEY, null);
        if (componentAttributeName != null) {
            return false;
        }

        return true;
    }

    /**
     * Returns a class for a given qualified class name.
     *
     * @param className
     *            The class name to lookup.
     * @return The class object.
     * @throws ClassNotFoundException
     */
    protected Class<?> loadClass(String className)
            throws ClassNotFoundException {
        return Engine.loadClass(className);
    }

    /**
     * Services a HTTP Servlet request as an uniform call.
     *
     * @param request
     *            The HTTP Servlet request.
     * @param response
     *            The HTTP Servlet response.
     */
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpServerHelper helper = getServer(request);

        if (helper != null) {
            helper.handle(createCall(helper.getHelped(), request, response));
        } else {
            log("[Restlet] Unable to get the Restlet HTTP server connector. Status code 500 returned.");
            response.sendError(500);
        }
    }
}
