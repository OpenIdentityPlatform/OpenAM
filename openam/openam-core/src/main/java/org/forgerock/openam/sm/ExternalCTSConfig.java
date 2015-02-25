
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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.sm;

import com.iplanet.am.util.AMPasswordUtil;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.service.SessionConstants;
import com.iplanet.services.naming.ServerEntryNotFoundException;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.utils.ModifiedProperty;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Models the external configuration of the Core Token Service.
 *
 * This model is used by the {@link ConnectionConfigFactory} to establish a connection to the
 * persistence layer.
 *
 * @see ConnectionConfigFactory
 */
@Singleton
public class ExternalCTSConfig implements ConnectionConfig {

    /**
     * Invalid number indicates a non numeric was entered into the configuration for a
     * numeric field.
     */
    public static final int INVALID = -1;

    private final Debug debug;

    private ModifiedProperty<String> hosts = new ModifiedProperty<String>();
    private ModifiedProperty<String> username = new ModifiedProperty<String>();
    private ModifiedProperty<String> password = new ModifiedProperty<String>();
    private ModifiedProperty<String> maxConnections = new ModifiedProperty<String>();
    private ModifiedProperty<Boolean> sslMode = new ModifiedProperty<Boolean>();
    private ModifiedProperty<Integer> heartbeat = new ModifiedProperty<Integer>();

    @Inject
    public ExternalCTSConfig(@Named(SessionConstants.SESSION_DEBUG) Debug debug) {
        this.debug = debug;
    }

    /**
     * The hosts to connect to.
     * @return A set of connection details with serverId/siteId preferences.
     */
    public Set<LDAPURL> getLDAPURLs() {
        String serverId = null;
        String siteId = "";
        try {
            serverId = WebtopNaming.getAMServerID();
            siteId = WebtopNaming.getSiteID(serverId);
        } catch (ServerEntryNotFoundException senfe) {
            if (debug.warningEnabled()) {
                debug.warning("ServerEntryNotFoundException, serverId=" + serverId
                        + ", siteId=" + siteId);
            }
        }

        String hosts = this.hosts.get();
        Set<String> urls = new LinkedHashSet<String>();
        urls.addAll(Arrays.asList(hosts.split(",")));

        boolean isSSL = isSSLMode();

        Set<LDAPURL> ldapurls = new LinkedHashSet<LDAPURL>();
        for (LDAPURL url : LDAPUtils.prioritizeServers(urls, serverId, siteId)) {
            ldapurls.add(LDAPURL.valueOf(url.getHost(), url.getPort(), isSSL));
        }

        if (debug.messageEnabled()) {
            debug.message("Priotized server list [" + hosts + "] using server ID [" + serverId +
                    "] and site ID [" + siteId + "]");
        }

        return ldapurls;
    }

    /**
     * @return The External Token Store username. Maybe null.
     */
    public String getBindDN() {
        return username.get();
    }

    /**
     * @return The External Token Store password. Maybe null.
     */
    public char[] getBindPassword() {
        if (password.get() == null) {
            return null;
        }
        return password.get().toCharArray();
    }

    /**
     * The maximum number of connections permitted to this server.
     *
     * @return A positive number if valid, or -1.
     */
    public int getMaxConnections() {
        return parseNumber(maxConnections.get());
    }

    /**
     * @return The External Token Store heartbeat value. Value 0 or negetive indicates no heartbeat used.
     */
    public int getLdapHeartbeat(){
        return heartbeat.get();
    }

    /**
     * @return True indicates the External Token Store should use SSL for its connection.
     */
    public boolean isSSLMode() {
        return sslMode.get();
    }

    /**
     * @return True if the configuration is now in a changed state.
     */
    public boolean hasChanged() {
        return hosts.hasChanged() ||
               username.hasChanged() ||
               password.hasChanged() ||
               maxConnections.hasChanged() ||
               sslMode.hasChanged() ||
               heartbeat.hasChanged();
    }

    /**
     * Causes this instance to refresh its configuration using System Properties.
     */
    public void update() {
        hosts.set(SystemProperties.get(CoreTokenConstants.CTS_STORE_HOSTNAME));
        username.set(SystemProperties.get(CoreTokenConstants.CTS_STORE_USERNAME));
        password.set(AMPasswordUtil.decrypt(SystemProperties.get(CoreTokenConstants.CTS_STORE_PASSWORD)));
        maxConnections.set(SystemProperties.get(CoreTokenConstants.CTS_STORE_MAX_CONNECTIONS));
        sslMode.set(SystemProperties.getAsBoolean(CoreTokenConstants.CTS_STORE_SSL_ENABLED, false));

        String heartbeatStr = SystemProperties.get(Constants.LDAP_HEARTBEAT);
        if (StringUtils.isNotEmpty(heartbeatStr)) {
            try {
                heartbeat.set(Integer.parseInt(heartbeatStr));
            } catch (NumberFormatException e) {
                heartbeat.set(new Integer(-1));
            }
        } else {
            heartbeat.set(new Integer(-1));
        }
    }

    /**
     * Convenience parsing method which handles the unassigned condition.
     *
     * @param number String representation of the number.
     *
     * @return A number if parsed successfully. -1 if there was a parsing issue and null if there was not
     * number provided.
     */
    private Integer parseNumber(String number) {
        if (StringUtils.isEmpty(number)) {
            return null;
        }
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return INVALID;
        }
    }

    @Override
    public String toString() {
        return "ExternalTokenConfig{" +
                "hosts=" + hosts +
                ", username=" + username +
                ", password=" + password +
                ", maxConnections=" + maxConnections +
                ", sslMode=" + sslMode +
                ", heartbeat=" + heartbeat +
                '}';
    }
}
