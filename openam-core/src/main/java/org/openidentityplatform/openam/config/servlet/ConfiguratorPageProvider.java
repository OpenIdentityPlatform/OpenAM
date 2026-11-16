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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems LLC.
 */
package org.openidentityplatform.openam.config.servlet;

/**
 * Lets a {@link SetupPage} that lives in a module downstream of {@code openam-core} (one that
 * depends on {@code openam-core}, so cannot be depended on back) register itself into
 * {@link ConfiguratorServlet}'s migrated-page registry without {@code openam-core} needing a
 * compile-time reference to it.
 *
 * <p>Discovered via {@link java.util.ServiceLoader}, the same {@code META-INF/services}
 * self-registration idiom already used in this codebase by
 * {@link com.sun.identity.setup.SetupListener}: implement this interface in the downstream
 * module and list the implementation's fully-qualified name in a
 * {@code META-INF/services/org.openidentityplatform.openam.config.servlet.ConfiguratorPageProvider}
 * resource file. {@code openam-core}'s own isolated test runs simply see zero providers - a
 * graceful no-op, not a failure.
 */
public interface ConfiguratorPageProvider {

    /** The {@code *.htm} servlet path this provider's page handles, e.g. {@code "/config/upgrade/upgrade.htm"}. */
    String getPath();

    Class<? extends SetupPage> getPageClass();
}
