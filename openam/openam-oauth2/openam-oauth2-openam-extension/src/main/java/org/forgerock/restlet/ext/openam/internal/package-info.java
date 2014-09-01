/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [Forgerock Inc]"
 */

/**
 * Something
 * <p/>
 * Example Config:
 *
 * <pre>
 * &lt;!-- Servlet to Restlet adapter declaration (Mandatory) --&gt;
 *  &lt;servlet&gt;
 *      &lt;servlet-name&gt;RestletAdapter&lt;/servlet-name&gt;
 *      &lt;servlet-class&gt;org.restlet.ext.servlet.ServerServlet&lt;/servlet-class&gt;
 *      &lt;!-- Your application class name (Optional - For mode 3) --&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;org.restlet.application&lt;/param-name&gt;
 *          &lt;param-value&gt;org.forgerock.restlet.ext.openam.DemoApplication&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *      &lt;!-- List of supported client protocols --&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;org.restlet.clients&lt;/param-name&gt;
 *          &lt;param-value&gt;HTTP CLAP FILE RIAP&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *  &lt;/servlet&gt;</p>
 *  &lt;!-- servlet declaration --&gt;
 *  &lt;servlet-mapping&gt;
 *      &lt;servlet-name&gt;RestletAdapter&lt;/servlet-name&gt;
 *      &lt;url-pattern&gt;/oauth2&lt;/url-pattern&gt;
 *  &lt;/servlet-mapping&gt;
 * </pre>
 * <pre>
 *
 * </pre>
 */
package org.forgerock.restlet.ext.openam.internal;
