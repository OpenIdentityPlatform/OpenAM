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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.entitlement.conditions.environment;

public final class ConditionConstants {

    private ConditionConstants() {
    }

    /**
     * Key that is used to identify the advice messages from <code>AuthSchemeCondition</code>.
     */
    public static final String AUTH_SCHEME_CONDITION_ADVICE = "AuthSchemeConditionAdvice";

    /**
     * Key that is used to identify the advice messages from <code>AuthenticateToServiceCondition</code>.
     */
    public static final String AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE = "AuthenticateToServiceConditionAdvice";

    /**
     * Key that is used to identify the advice messages from <code>AuthLevelCondition</code>.
     */
    public static final String AUTH_LEVEL_CONDITION_ADVICE = "AuthLevelConditionAdvice";

    /**
     * Key that is used to identify the advice messages from <code>AuthenticateToRealmCondition</code>.
     */
    public static final String AUTHENTICATE_TO_REALM_CONDITION_ADVICE = "AuthenticateToRealmConditionAdvice";

    /**
     * Key that is used to define the name of authentication scheme of the request. It's passed down as part of the
     * <code>environment</code> Map to <code>evaluate</code> of an <code>EntitlementCondition</code> for condition
     * evaluation. If the <code>environment</code> parameter is null or does not define value for
     * <code>REQUEST_AUTH_SCHEMES</code>, the value for <code>REQUEST_AUTH_SCHEMES</code> is obtained from the single
     * sign on token of the user.
     */
    public static final String REQUEST_AUTH_SCHEMES = "requestAuthSchemes";

    /**
     * Key that is used to identify the names of authentication chains in the request. It's passed down as part of the
     * <code>environment</code> Map to <code>evaluate</code> of an <code>EntitlementCondition</code> for condition
     * evaluation. If the <code>environment</code> parameter is null or does not define value for
     * <code>REQUEST_AUTHENTICATED_TO_SERVICES</code>, the value for <code>REQUEST_AUTHENTICATED_TO_SERVICES</code>
     * is obtained from the single sign on token of the user.
     */
    public static final String REQUEST_AUTHENTICATED_TO_SERVICES = "requestAuthenticatedToServices";

    /**
     * Key that is used to define the minimum authentication level in an <code>AuthLevelCondition</code> or the maximum
     * authentication level in a <code>LEAuthLevelCondition</code> of a policy being evaluated. In case of
     * <code>AuthLevelCondition</code> policy would apply if the request authentication level is at least the level
     * defined in condition while in case of <code>LEAuthLevelCondition</code> policy would apply if the request
     * authentication level is less than or equal to the level defined in the condition.
     */
    public static final String AUTH_LEVEL = "AuthLevel";

    /**
     * Key that is used to identify the names of authenticated realms in the request. It's passed down as part of the
     * <code>environment</code> Map to <code>evaluate</code> of an <code>EntitlementCondition</code> for condition
     * evaluation. If the <code>environment</code> parameter is null or does not define value for
     * <code>REQUEST_AUTHENTICATED_TO_REALMS</code>, the value for <code>REQUEST_AUTHENTICATED_TO_REALMS</code> is
     * obtained from the single sign on token of the user.
     */
    public static final String REQUEST_AUTHENTICATED_TO_REALMS = "requestAuthenticatedToRealms";

    /**
     * Key that is used to define the authentication level of the request. It's passed down as part of the
     * <code>environment</code> Map to <code>evaluate</code> of an <code>EntitlementCondition</code> for condition
     * evaluation. If the <code>env</code> parameter is null or does not define value for
     * <code>REQUEST_AUTH_LEVEL</code>, the value for <code>REQUEST_AUTH_LEVEL</code> is obtained from the single
     * sign on token of the user.
     */
    public static final String REQUEST_AUTH_LEVEL = "requestAuthLevel";

    /**
     * Key that is used to define request IP address that is passed down as part of the <code>environment</code> Map
     * to <code>evaluate</code> of an <code>EntitlementCondition</code> for condition evaluation. Value for the key
     * should be a <code>String</code> representation of IP of the client,
     * <p/>
     * For IP version 4:
     * The form is  n.n.n.n where n is a value between 0 and 255 inclusive.
     * <p/>
     * For IP version 6:
     * The form is x:x:x:x:x:x:x:x where x is the hexadecimal values of the eight 16-bit pieces of the address.
     */
    public static final String REQUEST_IP = "requestIp";

    /**
     * <p>Key that is used in <code>AuthenticateToServiceCondition</code> to specify the authentication chain for which
     * the user should authenticate for the policy to apply.</p>
     *
     * <p>The value should be a <code>Set</code> with only one element.</p>
     *
     * <p>The should be a <code>String</code>, the realm name.</p>
     */
    public static final String AUTHENTICATE_TO_SERVICE = "AuthenticateToService";

    /**
     * <p>Key used in <code>AuthenticateToRealmCondition</code> to specify the realm for which the user should
     * authenticate for the policy to apply.</p>
     *
     * <p>The value should be  a <code>Set</code> with only one element.</p>
     *
     * <p>The should be a  <code>String</code>, the realm name.</p>
     */
    public static final String AUTHENTICATE_TO_REALM = "AuthenticateToRealm";

    /**
     * <p>Key that is used in a <code>AMIdentityMembershipCondition</code> to specify the uuid(s) of
     * <code>AMIdentity</code> objects to which the policy would apply. These uuid(s) are specified in the condition
     * at policy definition time.</p>
     *
     * <p>The value should be a <code>Set</code></p>
     * <p>Each element of the <code>Set</code> should be a String, the uuid of the invocator.</p>
     */
    public static final String AM_IDENTITY_NAME = "amIdentityName";

    /**
     * <p>Key that is used in <code>SessionCondition</code> to define the maximum  session time in minutes for which a
     * policy applies.</p>
     *
     * <p>The value corresponding to the key has to be a  <code>Set</code> that has just one element which is a string
     * and parse-able as an {@code Integer}.
     */
    public static final String MAX_SESSION_TIME = "MaxSessionTime";

    /**
     * <p>Key in <code>SessionCondition</code> that is used to define the option to terminate the session if the session
     * exceeds the maximum session time.</p>
     *
     * <p>The value corresponding to the key has to be a {@code Set} that has just one element which is a string. The
     * option is on if the string value is equal to {@code true}.
     */
    public static final String TERMINATE_SESSION = "TerminateSession";

    /**
     * Key that is passed in the <code>env</code> parameter while invoking {@code getConditionDecision} method of an
     * {@code AMIdentityMembershipCondition}. The value specifies the uuid(s) for which the policy would apply. The
     * value should be a {@code Set}. Each element of the {@code Set} should be a String, the uuid of the
     * {@code AMIdentity} object.
     */
    public static final String INVOCATOR_PRINCIPAL_UUID = "invocatorPrincipalUuid";

    /**
     * <p>Key that is used in {@code SimpleTimeCondition} to define the beginning of time range during which a policy
     * applies.</p>
     *
     * <p>The value corresponding to the key has to be a {@code Set} that has just one element which is a {@code String}
     * that conforms to the pattern described here. If a value is defined for {@code START_TIME}, a value should also be
     * defined for {@code END_TIME}.</p>
     *
     * <p>
     * The patterns is:
     * <pre>
     *    HH:mm
     * </pre>
     *</p>
     *
     * <p>
     * Some sample values are
     * <pre>
     *     08:25
     *     18:45
     * </pre>
     * </p>
     *
     * @see #END_TIME
     */
    public static final String START_TIME = "startTime";

    /**
     * <p>Key that is used in a {@code SimpleTimeCondition} to define the end of time range during which a policy
     * applies.</p>
     *
     * <p>The value corresponding to the key has to be a {@code Set} that has just one element which is a {@code String}
     * that conforms to the pattern described here. If a value is defined for {@code END_TIME}, a value should also be
     * defined for {@code START_TIME}.</p>
     *
     * <p>
     * The patterns is:
     * <pre>
     *    HH:mm
     * </pre>
     *</p>
     *
     * <p>
     * Some sample values are
     * <pre>
     *     08:25
     *     18:45
     * </pre>
     * </p>
     *
     * @see #START_TIME
     */
    public static final String END_TIME = "endTime";

    /**
     * <p>Key that is used in a {@code SimpleTimeCondition} to define the start of day of week range for which a policy
     * applies.</p>
     *
     * <p>The value corresponding to the key has to be a {@code Set} that has just one element which is a {@code String}
     * that is one of the values {@code Sun, Mon, Tue, Wed, Thu, Fri, Sat}.</p>
     *
     * <p>If a value is defined for {@code START_DAY},
     * a value should also be defined for {@code END_DAY}.</p>
     *
     * <p>
     * Some sample values are
     * <pre>
     *     Sun
     *     Mon
     * </pre>
     * </p>
     *
     * @see #END_DAY
     */
    public static final String START_DAY = "startDay";

    /**
     * <p>Key that is used in a {@code SimpleTimeCondition} to define the end of day of week range for which a policy
     * applies.</p>
     *
     * <p>Its defined in a {@code SimpleTimeCondition} associated with the policy. The value corresponding to the key
     * has to be a {@code Set} that has just one element which is a {@code String} that is one of the values
     * {@code Sun, Mon, Tue, Wed, Thu, Fri, Sat}.</p>
     *
     * <p>If a value is defined for {@code END_DAY}, a value should also be defined for {@code START_DAY}.</p>
     *
     * <p>
     * Some sample values are
     * <pre>
     *     Sun
     *     Mon
     * </pre>
     * </p>
     *
     * @see #START_DAY
     */
    public static final String END_DAY = "endDay";

    /**
     * <p>Key that is used in a {@code SimpleTimeCondition} to define the start of date range for which a policy
     * applies.</p>
     *
     * <p>The value corresponding to the key has to be a {@code Set} that has just one element which is a {@code String}
     * that corresponds to the pattern described below.</p>
     *
     * <p>If a value is defined for {@code START_DATE}, a value should also be defined for {@code END_DATE}.</p>
     *
     * <p>
     * The pattern is
     * <pre>
     *     yyyy:MM:dd
     * Some sample values are
     *     2001:02:26
     *     2002:12:31
     * </pre>
     * </p>
     *
     * @see #END_DATE
     */
    public static final String START_DATE = "startDate";

    /**
     * <p>Key that is used in a {@code SimpleTimeCondition} to define the end of date range for which a policy applies.
     * </p>
     *
     * <p>The value corresponding to the key has to be a {@code Set} that has just one element which is a {@code String}
     * that corresponds to the pattern described below.</p>
     *
     * <p>If a value is defined for {@code END_DATE}, a value should also be defined for {@code START_DATE}.</p>
     *
     * <p>
     * The pattern is
     * <pre>
     *     yyyy:MM:dd
     * Some sample values are
     *     2001:02:26
     *     2002:12:31
     * </pre>
     * </p>
     *
     * @see #START_DATE
     */
    public static final String END_DATE = "endDate";

    /**
     * Key that is passed in the <code>env</code> parameter while invoking {@code getConditionDecision} method of a
     * {@code SessionPropertyCondition} to indicate if a case insensitive match needs to done of the property value
     * against same name property in the user's single sign on token.
     */
    public static final String VALUE_CASE_INSENSITIVE = "valueCaseInsensitive";

    /**
     * <p>Key that is used to define the authentication scheme in an {@code AuthSchemeCondition} of a policy.</p>
     *
     * <p>Policy would apply if the authentication scheme of the request is same as defined in the condition. The value
     * should be a {@code Set} with only one element. The element should be a {@code String}, the authentication scheme
     * name.
     */
    public static final String AUTH_SCHEME = "authScheme";

    /**
     * Key that is used to specify application name for the resources protected by the policy.
     */
    public static final String APPLICATION_NAME = "applicationName";

    /**
     * Key that is used to specify the application idle time out.
     */
    public static final String APPLICATION_IDLE_TIMEOUT = "applicationIdleTimeout";

    /**
     * <p>Key that is used to define request DNS name that is passed in the {@code env} parameter while invoking
     * {@code getConditionDecision} method of an {@code IPCondition}.</p>
     *
     * <p>Value for the key should be a set of strings representing the DNS names of the client, in the form
     * {@code ccc.ccc.ccc} for IP version 4.</p>
     *
     * <p>For IP version 6, the form would be {@code x:x:x:x:x:x:x:x}.</p>
     *
     * <p>If the {@code env} parameter is null or does not define value for {@code REQUEST_DNS_NAME}, the value for
     * {@code REQUEST_DNS_NAME} is obtained from the single sign on token of the user.
     */
    public static final String REQUEST_DNS_NAME = "requestDnsName";

    /**
     * <p>Key that is used in {@code IPCondition} to define the  IP address values for which a policy applies. The
     * value corresponding to the key has to be a {@code Set} where each element is a {@code String} that conforms to
     * the pattern described here.</p>
     *
     * <p>The pattern has a mandatory start IP address, possibly followed by "-" and end IP address if
     * specifying an IP address range rather than a single address. Ranges are inclusive of their bounds.</p>
     *
     * <p>For IPv4, the patterns is: n.n.n.n[-n.n.n.n]
     * where n would take any integer value between 0 and 255 inclusive.</p>
     *
     * <p>Some sample values are
     * <ul>
     *     <li>122.100.85.45-125.110.90.66</li>
     *     <li>145.64.55.35-215.110.173.145</li>
     *     <li>15.64.55.35</li>
     * </ul></p>
     *
     * <p>The patterns for IP Version 6 is:
     *    x:x:x:x:x:x:x:x[-x:x:x:x:x:x:x:x]
     * where x are the hexadecimal values of the eight 16-bit pieces of the address.</p>
     *
     * <p>Some sample values are:
     *      FEDC:BA98:7654:3210:FEDC:BA98:7654:3210
     *      1080:0:0:0:8:800:200C:417A</p>
     *
     * @see <a href="http://tools.ietf.org/html/rfc3513#section-2.2">RFC 3513 - Section 2.2</a>
     */
    public static final String IP_RANGE = "ipRange";

    /**
     * <p>Key used in {@code IPCondition} to define the  start of IP address range for which a policy applies.</p>
     *
     * <p>The value corresponding to the key  has to be a <code>Set</code> that has just one element which is a
     * {@code String} that conforms to the pattern described here. If a value is defined for {@code START_IP}, a value
     * should also be defined for {@code END_IP}.</p>
     *
     * <p>The patterns for IP Version 4 is :
     *    n.n.n.n
     * where n would take any integer value between 0 and 255 inclusive.</p>
     *
     * <p>Some sample values are:
     *     122.100.85.45
     *     145.64.55.35
     *     15.64.55.35</p>
     *
     * <p>The patterns for IP Version 6 is:
     *    x:x:x:x:x:x:x:x
     * where x are the hexadecimal values of the eight 16-bit pieces of the address.</p>
     *
     * <p>Some sample values are:
     *      FEDC:BA98:7654:3210:FEDC:BA98:7654:3210
     *      1080:0:0:0:8:800:200C:417A</p>
     *
     * @see <a href="http://tools.ietf.org/html/rfc3513#section-2.2">RFC 3513 - Section 2.2</a>
     */
    public static final String START_IP = "startIp";

    /**
     * <p>Key that is used in {@code IPCondition} to define the end of IP address range for which a policy applies.</p>
     *
     * <p>The value corresponding to the key has to be a {@code Set} that has just one element which is a {@code String}
     * that conforms to the pattern described here. If a value is defined for {@code END_IP}, a value should also be
     * defined for {@code START_IP}.</p>
     *
     * <p>The patterns is :
     *    n.n.n.n
     * where n would take any integer value between 0 and 255 inclusive.</p>
     *
     * <p>Some sample values are
     *     122.100.85.45
     *     145.64.55.35
     *     15.64.55.35</p>
     *
     * <p>The patterns for IP Version 6 is:
     *    x:x:x:x:x:x:x:x
     * where x are the hexadecimal values of the eight 16-bit pieces of the address.</p>
     *
     * <p>Some sample values are:
     *      FEDC:BA98:7654:3210:FEDC:BA98:7654:3210
     *      1080:0:0:0:8:800:200C:417A</p>
     *
     * @see <a href="http://tools.ietf.org/html/rfc3513#section-2.2">RFC 3513 - Section 2.2</a>
     */
    public static final String END_IP = "endIp";

    /**
     * <p>Key that is used in an {@code IPCondition} to define the  DNS name values for which a policy applies. The
     * value corresponding to the key has to be a {@code Set} where each element is a {@code String} that conforms to
     * the patterns described here.</p>
     *
     * <p>The patterns is :
     * <pre>
     * ccc.ccc.ccc.ccc
     * *.ccc.ccc.ccc</pre>
     * where c is any valid character for DNS domain/host name.
     * There could be any number of <code>.ccc</code> components.
     * Some sample values are:
     * <pre>
     * www.sun.com
     * finace.yahoo.com
     * *.yahoo.com
     * </pre>
     * </p>
     */
    public static final String DNS_NAME = "dnsName";

    /**
     * <p>Key that is used in a {@code LDAPFilterCondition} to define the ldap filter that should  be satisfied by the
     * ldap entry of the user for the condition to be satisfied.</p>
     *
     * <p>The value should be a {@code Set} with only one element. The element should be a {@code String}.</p>
     */
    public static final String LDAP_FILTER = "ldapFilter";
}
