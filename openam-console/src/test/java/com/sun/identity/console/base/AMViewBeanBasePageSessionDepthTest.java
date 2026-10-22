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
 * Copyright 2026 3A Systems, LLC.
 */
package com.sun.identity.console.base;

import static org.assertj.core.api.Assertions.assertThat;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.shared.Constants;
import org.forgerock.openam.utils.IOUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Pins the admin-console page-session collection depth. The console page session nests
 * page-session map -&gt; attribute-value map -&gt; value set, one level past the default
 * {@code maxcollectiondepth} of {@link IOUtils#deserialise}; if {@link AMViewBeanBase} stopped overriding
 * {@code getPageSessionCollectionDepth()}, every console page would silently lose its page session.
 */
public class AMViewBeanBasePageSessionDepthTest {

    @BeforeClass
    public void provideServerAddress() {
        // AMViewBeanBase's static initialiser copies the server address into system properties and fails
        // without one; outside a deployment nothing supplies it, so give it a placeholder.
        SystemProperties.initializeProperties(Constants.AM_SERVER_PROTOCOL, "http");
        SystemProperties.initializeProperties(Constants.AM_SERVER_HOST, "localhost");
        SystemProperties.initializeProperties(Constants.AM_SERVER_PORT, "8080");
    }

    @Test
    public void shouldDeserialisePageSessionWithConsoleCollectionDepth() {
        assertThat(new MinimalConsoleViewBean().pageSessionCollectionDepth())
                .isEqualTo(IOUtils.CONSOLE_STATE_MAX_COLLECTION_DEPTH);
    }

    /**
     * The smallest concrete console view bean. It must not override {@code getPageSessionCollectionDepth()}
     * itself, so the value observed is the one every real console page inherits from {@link AMViewBeanBase}.
     */
    private static final class MinimalConsoleViewBean extends AMViewBeanBase {

        MinimalConsoleViewBean() {
            super("MinimalConsoleViewBean");
        }

        @Override
        protected AMModel getModelInternal() {
            return null;
        }

        // A call on this, from a subclass, compiles whether or not AMViewBeanBase declares the override.
        long pageSessionCollectionDepth() {
            return getPageSessionCollectionDepth();
        }
    }
}
