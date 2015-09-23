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

/**
 * Holds directly or via sub-packages mechanism for enabling openAM to be a radius server.
 *
 * The RADIUS server is created as a part of OpenAM using the Java service loader mechanism.
 * in /src/main/resources/META-INF/services there exists a file com.sun.identity.setup.SetupListener
 * that specifies implementations of SetupListener that will be loaded by AMSetupServlet.
 *
 * The RADIUS server uses that mechanism to have <code>AMSetupServlet</code> load the
 * {@link org.forgerock.openam.radius.server.RadiusServiceManager} and call addListener on it. When that happens
 * the <code>RadiusServerManager</code> will use the <code>RadiusServerGuiceModule</code> to create a Guice injector
 * and get from it a singleton instance of RadiusServiceStarter, which loads config, creates the RadiusRequestListener
 * and monitors for further config changes.
 *
 * Shutdown of the RADIUS server can happen for a number of reasons;
 * a) The container is stopping the OpenAM servlet.
 * b) A catastrophic error occurred while processing a radius request.
 *
 * In case a, when the OpenAM servlet is shutting down, the <code>RadiusServerManager</code> will be notified as
 * when it is created it registers for shutdown notification with the ShutdownListener. This event starts an orderly
 * shutdown of the RADIUS server.
 *
 */
package org.forgerock.openam.radius.server;

