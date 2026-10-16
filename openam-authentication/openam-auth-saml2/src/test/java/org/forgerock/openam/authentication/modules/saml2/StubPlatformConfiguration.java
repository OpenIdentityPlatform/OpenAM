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

package org.forgerock.openam.authentication.modules.saml2;

import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.shared.Constants;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The platform configuration a multi server site would have, so that the load balancer cookie bounce
 * SAML2Proxy sits behind can be driven from a unit test.
 * <p>
 * <code>FSUtils.needSetLBCookieAndRedirect</code> starts by asking
 * <code>SystemConfigurationUtil</code> for the platform server list; without a configuration plugin
 * that lookup fails, an empty server list reads as a single server deployment, and the bounce
 * declines before anything testable happens. One server other than the one under test is all it
 * takes to get past that.
 * <p>
 * Registered through <code>com.sun.identity.plugin.configuration.class</code> in this module's test
 * <code>FederationConfig.properties</code> rather than from a test method:
 * <code>ConfigurationManager</code> reads that property once into a static field when the class is
 * first loaded, and surefire runs the whole module in one JVM.
 * <p>
 * openam-federation-library carries its own copy for its own tests - it publishes no test jar, so
 * there is nothing to share.
 *
 * @see SAML2ProxyTest
 */
public class StubPlatformConfiguration implements ConfigurationInstance {

    /** The other server in the site, which is what makes the load balancer cookie matter. */
    public static final String REMOTE_SERVER = "http://remote.example.com:8080/openam";

    private String componentName;

    @Override
    public void init(String componentName, Object session) {
        this.componentName = componentName;
    }

    /**
     * The platform server list, and only for the PLATFORM component. Registering this plugin makes
     * it the answer for every getConfigurationInstance call in the module, SAML2 included, and
     * handing those a map holding the server list would look to them like configuration they can
     * read rather than like the absent configuration they get without the plugin.
     */
    @Override
    public Map getConfiguration(String realm, String configName) {
        if (!"PLATFORM".equals(componentName)) {
            return Collections.emptyMap();
        }
        Map<String, Set<String>> configuration = new HashMap<>();
        configuration.put(Constants.PLATFORM_LIST, Collections.singleton(REMOTE_SERVER + "|02"));
        configuration.put(Constants.SITE_LIST, Collections.<String>emptySet());
        return configuration;
    }

    @Override
    public void setConfiguration(String realm, String configName, Map avPairs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createConfiguration(String realm, String configName, Map avPairs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteConfiguration(String realm, String configName, Set attributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set getAllConfigurationNames(String realm) {
        return Collections.emptySet();
    }

    @Override
    public String addListener(ConfigurationListener listener) {
        return StubPlatformConfiguration.class.getName();
    }

    @Override
    public void removeListener(String listenerID) {
    }
}
