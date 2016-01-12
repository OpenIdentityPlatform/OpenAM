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
 * Copyright 2013-2016 ForgeRock AS.
 */
package org.forgerock.openam.ldap;

import static org.forgerock.opendj.ldap.LDAPConnectionFactory.*;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import javax.naming.InvalidNameException;

import org.forgerock.i18n.LocalizedIllegalArgumentException;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.Connections;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.LoadBalancerEventListener;
import org.forgerock.opendj.ldap.RDN;
import org.forgerock.opendj.ldap.SSLContextBuilder;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.util.Options;
import org.forgerock.util.Reject;
import org.forgerock.util.time.Duration;

/**
 * Utility methods to help interaction with the OpenDJ LDAP SDK.
 * There are two main ways currently to create connection pools/factories:
 * <ul>
 *  <li>Providing a set of servers in the format specified in {@link
 * #prioritizeServers(java.util.Set, java.lang.String, java.lang.String)}, which will be prioritized based on the
 * current server's server ID/site ID.</li>
 *  <li>Providing a set of LDAPURLs, which are already considered as "prioritized".</li>
 * </ul>
 * In case the configuration provides the possibility to assign LDAP servers to OpenAM servers/sites, then either you
 * can prioritize manually (if the logic differs from this implementation) and create the corresponding {@link LDAPURL}
 * objects, or you can pass in the list to the newPrioritized* methods.
 *
 * @supported.all.api
 */
public final class LDAPUtils {

    private static final char[] ESCAPED_CHAR = {',', '+', '"', '\\', '<', '>', ';'};
    private static final String LDAP_SCOPE_BASE = "SCOPE_BASE";
    private static final String LDAP_SCOPE_ONE = "SCOPE_ONE";
    private static final String LDAP_SCOPE_SUB = "SCOPE_SUB";
    private static final Map<String, SearchScope> SCOPES;
    private static final Debug DEBUG = Debug.getInstance("LDAPUtils");
    private static final int DEFAULT_HEARTBEAT_TIMEOUT = 3;

    static {
        Map<String, SearchScope> mappings = new HashMap<String, SearchScope>(3);
        mappings.put(LDAP_SCOPE_BASE, SearchScope.BASE_OBJECT);
        mappings.put(LDAP_SCOPE_ONE, SearchScope.SINGLE_LEVEL);
        mappings.put(LDAP_SCOPE_SUB, SearchScope.WHOLE_SUBTREE);
        SCOPES = Collections.unmodifiableMap(mappings);
    }

    private LDAPUtils() {
    }

    /**
     * Based on the incoming parameters prioritizes the LDAP server list, then creates a connection pool that is
     * capable to failover to the servers defined in case there is an error.
     *
     * @param servers The set of servers in the format defined in {@link
     * #prioritizeServers(java.util.Set, java.lang.String, java.lang.String)}.
     * @param hostServerId The server ID for this OpenAM server.
     * @param hostSiteId The site ID for this OpenAM server.
     * @param username The directory user's DN. May be null if this is an anonymous connection.
     * @param password The directory user's password.
     * @param maxSize The max size of the created pool.
     * @param heartBeatInterval The interval for sending out heartbeat requests.
     * @param heartBeatTimeUnit The timeunit for the heartbeat interval.
     * @param ldapOptions Additional LDAP settings used to create the pool.
     * @return A failover loadbalanced authenticated/anonymous connection pool, which may also send heartbeat requests.
     */
    public static ConnectionFactory newPrioritizedFailoverConnectionPool(Set<String> servers,
            String hostServerId,
            String hostSiteId,
            String username,
            char[] password,
            int maxSize,
            int heartBeatInterval,
            String heartBeatTimeUnit,
            Options ldapOptions) {
        return newFailoverConnectionPool(prioritizeServers(servers, hostServerId, hostSiteId),
                username, password, maxSize, heartBeatInterval, heartBeatTimeUnit, ldapOptions);
    }

    /**
     * Creates a new connection pool that is capable to failover to the servers defined in case there is an error.
     *
     * @param servers The set of LDAP URLs that will be used to set up the connection factory.
     * @param username The directory user's DN. May be null if this is an anonymous connection.
     * @param password The directory user's password.
     * @param maxSize The max size of the created pool.
     * @param heartBeatInterval The interval for sending out heartbeat requests.
     * @param heartBeatTimeUnit The timeunit for the heartbeat interval.
     * @param ldapOptions Additional LDAP settings used to create the pool
     * @return A failover loadbalanced authenticated/anonymous connection pool, which may also send heartbeat requests.
     */
    public static ConnectionFactory newFailoverConnectionPool(Set<LDAPURL> servers,
            String username,
            char[] password,
            int maxSize,
            int heartBeatInterval,
            String heartBeatTimeUnit,
            Options ldapOptions) {
        List<ConnectionFactory> factories = new ArrayList<ConnectionFactory>(servers.size());
        for (LDAPURL ldapurl : servers) {
            ConnectionFactory cf = Connections.newFixedConnectionPool(
                    newConnectionFactory(ldapurl, username, password, heartBeatInterval, heartBeatTimeUnit,
                    ldapOptions), maxSize);
            factories.add(cf);
        }

        return loadBalanceFactories(factories);
    }

    /**
     * Based on the incoming parameters prioritizes the LDAP server list, then creates a connection factory that is
     * capable to failover to the servers defined in case there is an error.
     *
     * @param servers The set of servers in the format defined in {@link
     * #prioritizeServers(java.util.Set, java.lang.String, java.lang.String)}.
     * @param hostServerId The server ID for this OpenAM server.
     * @param hostSiteId The site ID for this OpenAM server.
     * @param username The directory user's DN. May be null if this is an anonymous connection.
     * @param password The directory user's password.
     * @param heartBeatInterval The interval for sending out heartbeat requests.
     * @param heartBeatTimeUnit The timeunit for the heartbeat interval.
     * @param options Additional LDAP settings used to create the connection factory.
     * @return A failover loadbalanced authenticated/anonymous connection factory, which may also send heartbeat
     * requests.
     */
    public static ConnectionFactory newPrioritizedFailoverConnectionFactory(Set<String> servers,
            String hostServerId,
            String hostSiteId,
            String username,
            char[] password,
            int heartBeatInterval,
            String heartBeatTimeUnit,
            Options options) {
        return newFailoverConnectionFactory(prioritizeServers(servers, hostServerId, hostSiteId),
                username, password, heartBeatInterval, heartBeatTimeUnit, options);
    }

    /**
     * Creates a new connection factory that is capable to failover to the servers defined in case there is an error.
     *
     * @param servers The set of LDAP URLs that will be used to set up the connection factory.
     * @param username The directory user's DN. May be null if this is an anonymous connection.
     * @param password The directory user's password.
     * @param heartBeatInterval The interval for sending out heartbeat requests.
     * @param heartBeatTimeUnit The timeunit for the heartbeat interval.
     * @param ldapOptions Additional LDAP settings used to create the connection factory.
     * @return A failover loadbalanced authenticated/anonymous connection factory, which may also send heartbeat
     * requests.
     */
    public static ConnectionFactory newFailoverConnectionFactory(Set<LDAPURL> servers,
            String username,
            char[] password,
            int heartBeatInterval,
            String heartBeatTimeUnit,
            Options ldapOptions) {
        List<ConnectionFactory> factories = new ArrayList<ConnectionFactory>(servers.size());
        for (LDAPURL ldapurl : servers) {
            factories.add(newConnectionFactory(ldapurl, username, password, heartBeatInterval, heartBeatTimeUnit,
                    ldapOptions));
        }
        return loadBalanceFactories(factories);
    }

    /**
     * Creates a new connection factory based on the provided parameters.
     *
     * @param ldapurl The address of the LDAP server.
     * @param username The directory user's DN. May be null if this is an anonymous connection.
     * @param password The directory user's password.
     * @param heartBeatInterval The interval for sending out heartbeat requests.
     * @param heartBeatTimeUnit The timeunit for the heartbeat interval.
     * @param ldapOptions Additional LDAP settings used to create the connection factory.
     * @return An authenticated/anonymous connection factory, which may also send heartbeat requests.
     */
    private static ConnectionFactory newConnectionFactory(LDAPURL ldapurl,
            String username,
            char[] password,
            int heartBeatInterval,
            String heartBeatTimeUnit,
            Options ldapOptions) {
        Boolean ssl = ldapurl.isSSL();
        int heartBeatTimeout =
                SystemPropertiesManager.getAsInt(Constants.LDAP_HEARTBEAT_TIMEOUT, DEFAULT_HEARTBEAT_TIMEOUT);

        if (ssl != null && ssl.booleanValue()) {
            try {
                //Creating a defensive copy of ldapOptions to handle the case when a mixture of SSL/non-SSL connections
                //needs to be established.
                ldapOptions = Options.copyOf(ldapOptions).set(LDAPConnectionFactory.SSL_CONTEXT,
                        new SSLContextBuilder().getSSLContext());
            } catch (GeneralSecurityException gse) {
                DEBUG.error("An error occurred while creating SSLContext", gse);
            }
        }

        // Enable heartbeat
        if (heartBeatInterval > 0 && heartBeatTimeUnit != null) {
            TimeUnit unit = TimeUnit.valueOf(heartBeatTimeUnit.toUpperCase());
            ldapOptions = ldapOptions
                    .set(HEARTBEAT_ENABLED, true)
                    .set(HEARTBEAT_INTERVAL, new Duration(unit.toSeconds(heartBeatInterval), TimeUnit.SECONDS))
                    .set(HEARTBEAT_TIMEOUT, new Duration(unit.toSeconds(heartBeatTimeout), TimeUnit.SECONDS));
        }

        // Enable Authenticated connection
        if (username != null) {
            ldapOptions = ldapOptions.set(AUTHN_BIND_REQUEST, LDAPRequests.newSimpleBindRequest(username, password));
        }

        return new LDAPConnectionFactory(ldapurl.getHost(), ldapurl.getPort(), ldapOptions);
    }

    private static ConnectionFactory loadBalanceFactories(List<ConnectionFactory> factories) {
        return Connections.newFailoverLoadBalancer(factories, Options.defaultOptions());
    }

    /**
     * Prioritizes the incoming LDAP servers based on their assigned servers/sites.
     * The format of the server list can be either one of the followings:
     * <ul>
     *  <li><code>host:port</code> - The LDAP server has no preferred
     * server/site</li>
     *  <li><code>host:port|serverid</code> - The LDAP server should be mainly
     * used by an OpenAM instance with the same serverid</li>
     *  <li><code>host:port|serverid|siteid</code> - The LDAP server should be
     * mainly used by an OpenAM instance with the same serverid or with the same
     * siteid</li>
     * </ul>
     * The resulting priority list will have the following order:
     * <ul>
     *  <li>servers that are linked with this server</li>
     *  <li>servers that are linked with the current site</li>
     *  <li>any other server that did not match in the same order as they were defined</li>
     * </ul>
     *
     * @param servers The Set of servers that needs to be prioritized in the previously described format.
     * @param hostServerId This server's ID.
     * @param hostSiteId This server's site ID.
     * @return The prioritized Set of LDAP URLs that can be used to create connection factories.
     */
    public static Set<LDAPURL> prioritizeServers(Set<String> servers, String hostServerId, String hostSiteId) {
        Set<LDAPURL> ldapServers = new LinkedHashSet<LDAPURL>(servers.size());
        Set<LDAPURL> serverDefined = new LinkedHashSet<LDAPURL>(servers.size());
        Set<LDAPURL> siteDefined = new LinkedHashSet<LDAPURL>(servers.size());
        Set<LDAPURL> nonMatchingServers = new LinkedHashSet<LDAPURL>(servers.size());
        for (String server : servers) {
            StringTokenizer tokenizer = new StringTokenizer(server, "|");
            String ldapUrl = tokenizer.nextToken();
            String assignedServerId = "";
            String assignedSiteId = "";

            if (tokenizer.hasMoreTokens()) {
                assignedServerId = tokenizer.nextToken();
            }
            if (tokenizer.hasMoreTokens()) {
                assignedSiteId = tokenizer.nextToken();
            }
            if (!assignedServerId.isEmpty() && assignedServerId.equals(hostServerId)) {
                serverDefined.add(LDAPURL.valueOf(ldapUrl));
            } else if (!assignedSiteId.isEmpty() && assignedSiteId.equals(hostSiteId)) {
                siteDefined.add(LDAPURL.valueOf(ldapUrl));
            } else {
                nonMatchingServers.add(LDAPURL.valueOf(ldapUrl));
            }
        }
        //Let's add them in the order of priority to the ldapServers set, this way the most appropriate servers should
        //be at the beginning of the list and towards the end of the list are the possibly most remote servers.
        ldapServers.addAll(serverDefined);
        ldapServers.addAll(siteDefined);
        ldapServers.addAll(nonMatchingServers);
        return ldapServers;
    }

    /**
     * Converts string representation of scope (as defined in the configuration) to the corresponding
     * {@link SearchScope} object.
     *
     * @param scope the string representation of the scope.
     * @param defaultScope in case the coversion fail this default scope should be returned.
     * @return the corresponding {@link SearchScope} object.
     */
    public static SearchScope getSearchScope(String scope, SearchScope defaultScope) {
        SearchScope searchScope = SCOPES.get(scope);
        return searchScope == null ? defaultScope : searchScope;
    }

    /**
     * Parses the incoming filter, and in case of failure falls back to the default filter.
     *
     * @param filter The filter that needs to be parsed.
     * @param defaultFilter If the parsing fails, this will be returned.
     * @return The parsed Filter object, or the default Filter, if the parse failed.
     */
    public static Filter parseFilter(String filter, Filter defaultFilter) {
        try {
            return filter == null ? defaultFilter : Filter.valueOf(filter);
        } catch (LocalizedIllegalArgumentException liae) {
            DEBUG.error("Unable to construct Filter from " + filter + " -> " + liae.getMessage()
                    + "\nFalling back to " + defaultFilter.toString());
        }
        return defaultFilter;
    }

    /**
     * Returns the RDN without the attribute name from the passed in {@link DN} object, for example:
     * <code>uid=demo,ou=people,dc=example,dc=com</code> will return <code>demo</code>.
     *
     * @param dn The DN that we need the name of.
     * @return The RDN of the DN without the attribute name.
     */
    public static String getName(DN dn) {
        return dn.rdn().getFirstAVA().getAttributeValue().toString();
    }

    /**
     * Converts the Attribute to an attribute name, 2-dimensional byte array map and adds it to the map passed in.
     * The first dimension of the byte array separates the different values, the second dimension holds the actual
     * value.
     *
     * @param attribute The attribute that needs to be converted.
     * @param map The map where the converted attribute is added to.
     */
    public static void addAttributeToMapAsByteArray(Attribute attribute, Map<String, byte[][]> map) {
        byte[][] values = new byte[attribute.size()][];
        int counter = 0;
        for (ByteString byteString : attribute) {
            byte[] bytes = byteString.toByteArray();
            values[counter++] = bytes;
        }
        map.put(attribute.getAttributeDescriptionAsString(), values);
    }

    /**
     * Converts the Attribute to an attribute name, set of String values map and adds it to the map passed in.
     *
     * @param attribute The attribute that needs to be converted.
     * @param map The map where the converted attribute is added to.
     */
    public static void addAttributeToMapAsString(Attribute attribute, Map<String, Set<String>> map) {
        map.put(attribute.getAttributeDescriptionAsString(), getAttributeValuesAsStringSet(attribute));
    }

    /**
     * Converts all the attribute values to a String Set.
     *
     * @param attribute the attribute to be converted.
     * @return A Set of String representations of the Attribute values.
     */
    public static Set<String> getAttributeValuesAsStringSet(Attribute attribute) {
        Set<String> values = new HashSet<String>(attribute.size());
        for (ByteString byteString : attribute) {
            values.add(byteString.toString());
        }
        return values;
    }

    /**
     * Converts the incoming set of URLs to {@link LDAPURL} instances and returns them as a set. The iteration order
     * of the originally passed in Set is retained.
     *
     * @param servers The LDAP server URLs that needs to be converted to {@link LDAPURL} instances.
     * @return A set of LDAPURLs corresponding to the passed in URLs.
     */
    public static Set<LDAPURL> convertToLDAPURLs(Set<String> servers) {
        if (servers == null) {
            return new LinkedHashSet<LDAPURL>(0);
        } else {
            Set<LDAPURL> ret = new LinkedHashSet<LDAPURL>(servers.size());
            for (String server : servers) {
                ret.add(LDAPURL.valueOf(server));
            }
            return ret;
        }
    }

    /**
     * When provided a DN, returns the value part of the first RDN.
     * @param dn A DN.
     * @return The value part of the first RDN.
     * @throws IllegalArgumentException When the DN's RDN is multivalued, or when the DN is not a valid name.
     */
    public static String rdnValueFromDn(String dn) {
        return rdnValueFromDn(DN.valueOf(dn));
    }

    /**
     * When provided a DN, returns the value part of the first RDN.
     * @param dn A DN.
     * @return The value part of the first RDN.
     * @throws IllegalArgumentException When the DN's RDN is multivalued.
     */
    public static String rdnValueFromDn(DN dn) {
        if (dn.size() > 0) {
            return rdnValue(dn.rdn());
        }
        return "";
    }

    /**
     * When provided an RDN, returns the value part.
     * @param rdn An RDN.
     * @return The value part.
     * @throws IllegalArgumentException When the RDN is multivalued.
     */
    public static String rdnValue(RDN rdn) {
        Reject.ifTrue(rdn.isMultiValued(), "Multivalued RDNs not supported");
        return rdn.getFirstAVA().getAttributeValue().toString();
    }

    /**
     * When provided a DN, returns the attribute type name of the first RDN.
     * @param dn A DN.
     * @return The attribute type name of the first RDN.
     * @throws IllegalArgumentException When the DN's RDN is multivalued.
     */
    public static String rdnTypeFromDn(String dn) {
        return rdnTypeFromDn(DN.valueOf(dn));
    }

    /**
     * When provided a DN, returns the attribute type name of the first RDN.
     * @param dn A DN.
     * @return The attribute type name of the first RDN.
     * @throws IllegalArgumentException When the DN's RDN is multivalued.
     */
    public static String rdnTypeFromDn(DN dn) {
        if (dn.size() > 0) {
            return rdnType(dn.rdn());
        }
        return "";
    }

    /**
     * When provided an RDN, returns the attribute type name.
     * @param rdn An RDN.
     * @return The attribute type name.
     * @throws IllegalArgumentException When the RDN is multivalued.
     */
    public static String rdnType(RDN rdn) {
        Reject.ifTrue(rdn.size() != 1, "Multivalued RDNs not supported");
        return rdn.getFirstAVA().getAttributeType().getNameOrOID();
    }

    /**
     * Returns a set of all the non-root DNs from the collection that are not equal to the {@code compare} parameter.
     * @param compare The DN to compare against.
     * @param dns THe DNs to compare.
     * @return A {@code Set} of non identical DNs.
     * @throws InvalidNameException If an error occurs.
     */
    public static Set<String> collectNonIdenticalValues(DN compare, Set<String> dns) throws InvalidNameException {
        Set<String> results = new HashSet<>();
        for (String dnString : dns) {
            DN dn = DN.valueOf(dnString);
            if (dn.size() > 0 && compare.compareTo(dn) != 0) {
                results.add(rdnValueFromDn(dn));
            }
        }
        return results;
    }

    /**
     * Gets the DB name.
     *
     * @param suffix The suffix.
     * @param ld The connection.
     * @return The name of the DB.
     */
    public static String getDBName(String suffix, Connection ld) {
        String filter = "cn=" + suffix;

        try {
            ConnectionEntryReader results = ld.search(LDAPRequests.newSearchRequest("cn=mapping tree,cn=config",
                    SearchScope.WHOLE_SUBTREE, filter));
            while (results.hasNext()) {
                Attribute dbName = results.readEntry().getAttribute("nsslapd-backend");
                if (dbName != null) {
                    return dbName.firstValueAsString();
                }
            }
        } catch (LdapException e) {
            // If not S1DS, then cn=mapping tree wouldn't exist.
            // Hence return userRoot as DBNAME.
        } catch (SearchResultReferenceIOException e) {
            DEBUG.error("LDAPUtils.getDBName: Did not expect to get a reference", e);
        }
        return "userRoot";
    }

    /**
     * Tests whether the supplied string is a DN, and is not the root DN.
     * @param candidateDN The possible DN.
     * @return {@code true} if the string is a DN.
     */
    public static boolean isDN(String candidateDN) {
        return newDN(candidateDN).size() > 0;
    }

    /**
     * Escapes characters that should be escaped.
     *
     * @param str The string to escape.
     * @return The escaped string.
     */
    public static String escapeValue(String str) {
        StringBuilder retbuf = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char currentChar = str.charAt(i);
            if (shouldEscapeCharacter(currentChar)) {
                retbuf.append('\\');
            }
            retbuf.append(currentChar);
        }
        return retbuf.toString();
    }

    private static boolean shouldEscapeCharacter(char c) {
        for (char escaped : ESCAPED_CHAR) {
            if (c == escaped) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normalizes the DN.
     *
     * @param dn The DN to normalize.
     * @return The normalized DN.
     */
    public static String normalizeDN(String dn) {
        return newDN(dn).toString().toLowerCase();
    }

    /**
     * Creates a DN from the specified DN string.
     *
     * @param orgName The DN string.
     * @return A DN.
     */
    public static DN newDN(String orgName) {
        if (orgName == null || orgName.startsWith("/") || !orgName.contains("=")) {
            return DN.rootDN();
        } else {
            return DN.valueOf(orgName);
        }
    }

    /**
     * Converts a DN String to a RFC format and lowers case.
     *
     * @param dn
     *            the DN String to be formated
     * @return a lowercase RFC fromat DN String
     */
    public static String formatToRFC(String dn) {
        return DN.valueOf(dn).toString().toLowerCase();
    }

    /**
     * Determines if the DN's are equal.
     *
     * @param dn1 The first DN.
     * @param dn2 The second DN.
     * @return {@code true} if the DN's are equal.
     */
    public static boolean dnEquals(String dn1, String dn2) {
        DN dnObj1 = DN.valueOf(dn1);
        DN dnObj2 = DN.valueOf(dn2);
        return dnObj1.equals(dnObj2);
    }

    private static class LoggingLBEventListener implements LoadBalancerEventListener {

        public void handleConnectionFactoryOffline(ConnectionFactory factory, LdapException error) {
            DEBUG.error("Connection factory became offline: " + factory, error);
        }

        public void handleConnectionFactoryOnline(ConnectionFactory factory) {
            DEBUG.error("Connection factory became online: " + factory);
        }
    }

    /**
     * Creates a ConnectionFactory from the host string and associated details. The host string can be any of the
     * following:
     * <ul>
     *     <li>A plain hostname/IP address</li>
     *     <li>A hostname and port, in the format <code>[host]:[port]</code></li>
     *     <li>A space-separated list of hostnames in priority order, e.g. <code>host1 host2 host3</code></li>
     *     <li>
     *         A space-separated list of hostnames with port numbers in priority order, e.g.
     *         <code>host1:389 host2:50389</code>
     *     </li>
     * </ul>
     * If a list of hosts is given, a load balanced {@code ConnectionFactory} is returned. All factories are
     * pre-authenticated using the supplied credentials.
     * @param host The host/host-port string.
     * @param defaultPort The port number to use for hosts that do not specify a port in the string.
     * @param authDN The DN to bind with.
     * @param authPasswd The password to bind with.
     * @param options Any additional options.
     * @return A connection factory.
     */
    public static ConnectionFactory createFailoverConnectionFactory(String host, int defaultPort,
            String authDN, String authPasswd, Options options) {
        StringTokenizer st = new StringTokenizer(host);
        String[] hostList = new String[st.countTokens()];
        int[] portList = new int[st.countTokens()];
        int hostCount = 0;
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            int colon = s.indexOf(':');
            if (colon > 0) {
                hostList[hostCount] = s.substring(0, colon);
                portList[hostCount] = Integer.parseInt(s.substring(colon + 1));
            } else {
                hostList[hostCount] = s;
                portList[hostCount] = defaultPort;
            }
            hostCount++;
        }

        if (hostCount > 1) {
            List<ConnectionFactory> factories = new ArrayList<>();
            for (int i = 0; i < hostCount; i++) {
                factories.add(createSingleHostConnectionFactory(hostList[i], portList[i], authDN, authPasswd, options));
            }
            return Connections.newFailoverLoadBalancer(factories, options);
        } else {
            return createSingleHostConnectionFactory(hostList[0], portList[0], authDN, authPasswd, options);
        }
    }

    private static ConnectionFactory createSingleHostConnectionFactory(String host, int port,
            String authDN, String authPasswd, Options options) {
        options = options.set(AUTHN_BIND_REQUEST, LDAPRequests.newSimpleBindRequest(authDN, authPasswd.toCharArray()));
        return new LDAPConnectionFactory(host, port, options);
    }
}
