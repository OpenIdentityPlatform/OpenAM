/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: PolicyProperties.java,v 1.11 2009/11/04 21:06:41 veiming Exp $
 *
 */



package com.sun.identity.policy.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.services.naming.URLNotFoundException;

import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.interfaces.ResourceName;
import com.sun.identity.policy.plugins.PrefixResourceName;

/**
 * This class initializes configuration parameters for policy 
 * client API using properties retrieved from <code>SystemProperties</code>
 */
class PolicyProperties {

    private final static String SERVER_LOG 
	    = "com.sun.identity.agents.server.log.file.name";

    private final static String DEFAULT_SERVER_LOG = "amRemotePolicyLog";

    private final static String LOGGING_LEVEL 
            = "com.sun.identity.agents.logging.level";

    private final static String CACHE_TTL  
	    = "com.sun.identity.agents.polling.interval"; //minutes

    private final static int CACHE_TTL_DEFAULT = 3; //minutes

    private final static String NOTIFICATION_ENABLED 
            = "com.sun.identity.agents.notification.enabled";

    private final static String BOOLEAN_ACTION_VALUES 
            = "com.sun.identity.policy.client.booleanActionValues";

    private final static String RESOURCE_COMPARATORS 
            = "com.sun.identity.policy.client.resourceComparators";

    private final static String HEADER_ATTRIBUTES 
	    = "com.sun.identity.agents.header.attributes";

    //self | subtree | strict-subtree
    private final static String CACHE_MODE 
            = "com.sun.identity.policy.client.cacheMode"; 

    //use pre2.2 boolean values
    private final static String USE_PRE22_BOOLEAN_VALUES
                    = "com.sun.identity.policy.client.usePre22BooleanValues";

    //use pre2.2 boolean values default to true
    private final static String USE_PRE22_BOOLEAN_VALUES_DEFAULT = "true";

    //pre2.2 default trueValue 
    private final static String PRE22_TRUE_VALUE_DEFAULT = "allow";

    //pre2.2 trueValue property
    private final static String PRE22_FALSE_VALUE = "deny";

    private final static String NOTIFICATION_ENABLED_DEFAULT= "false";

    public static final String RESULTS_CACHE_SESSION_CAP 
            = "com.sun.identity.policy.client.resultsCacheSessionCap";

    public static final String  USE_REST_PROTOCOL
            = "com.sun.identity.policy.client.useRESTProtocol";

    public static final String  USE_REST_PROTOCOL_DEFAULT
            = "false";

    public static int DEFAULT_RESULTS_CACHE_RESOURCE_CAP = 20;

    public static int resultsCacheResourceCap = DEFAULT_RESULTS_CACHE_RESOURCE_CAP;

    public static int DEFAULT_RESULTS_CACHE_SESSION_CAP = 10000;

    public static int resultsCacheSessionCap = DEFAULT_RESULTS_CACHE_SESSION_CAP;

    public static final String RESULTS_CACHE_RESOURCE_CAP 
            = "com.sun.identity.policy.client.resultsCacheResourceCap";

    private final static String REST_NOTIFICATION_URL
            = "com.sun.identity.client.rest.notification.url";

    private final static String COLON = ":";
    private final static String PIPE = "|";
    final static String ALLOW = "ALLOW";
    final static String DENY = "DENY";
    final static String BOTH = "BOTH";
    final static String DEFAULT_LOGGING_LEVEL = BOTH;
    final static String DECISION = "DECISION";
    final static String NONE = "NONE";
    final static String SUBTREE = "subtree";
    final static String SELF = "self";

    private final static String CACHE_MODE_DEFAULT  = SUBTREE;

    private Set responseAttributeNames = Collections.EMPTY_SET;
    private String logName;
    private String notificationURL;
    private int cacheTtl; //milliseconds
    private String cacheMode;
    private int cleanupInterval; //milliseconds
    private int urlReadTimeout; //milliseconds
    private boolean notificationEnabledFlag = false;
    private Map resourceComparators = new HashMap(10);
    private Map booleanActionValues = new HashMap(10);
    private boolean usePre22BooleanValues = true;
    private String pre22TrueValue = PRE22_TRUE_VALUE_DEFAULT;
    private String pre22FalseValue = PRE22_FALSE_VALUE;
    private ResourceName prefixResourceName = new PrefixResourceName();
    private boolean useRESTProtocolFlag = false;
    private String restNotificationURL;

    /**
     * Difference of system clock on the client machine compared to 
     * policy server machine. Valid life of policy decisions are extended 
     * by this skew on the client side.
     * Value for this is set by reading property 
     * com.sun.identity.policy.client.clockSkew
     * from AMConfig.properties in classpath.
     * If the value is not defined in AMConfig.properties, 
     * this would default to 0.
     */
    private static long clientClockSkew = 0;

    /**
     * Previous notification status
     */
    private static boolean previousNotificationEnabledFlag = false;

     /**
     * Previous notification URL
     */
    private static String previousNotificationURL = null;


    private static String logActions = NONE;
    private static Debug debug = PolicyEvaluator.debug;

    public static final String CLIENT_CLOCK_SKEW 
            = "com.sun.identity.policy.client.clockSkew";

    /**
     * Creates an instance of <code>PolicyProperties</code>
     */

    PolicyProperties() throws PolicyException {

        // ignore the case of property name while looking up for value
        boolean ignoreCase = true;

        //initialize logName 
	logName = getSystemProperty(SERVER_LOG, ignoreCase);
	if ((logName == null) || (logName.length() == 0)) {
            logName = DEFAULT_SERVER_LOG;
            if (debug.messageEnabled()) {
                debug.message("PolicyProperties:property " + SERVER_LOG
                        + " is not specified, use default value " 
                        + DEFAULT_SERVER_LOG);
            }
	} else {
            if (debug.messageEnabled()) {
                debug.message("PolicyProperties:logName=" 
                        + logName);
            }
        }

        //initialize cacheTtl and cleanupInterval
	String interval = getSystemProperty(CACHE_TTL, ignoreCase);
	if ((interval == null) || (interval.length() == 0)) {
	    throw new PolicyException(ResBundleUtils.rbName,
		"invalid_cache_ttl", null, null);
	}
	try {
	    cacheTtl = Integer.parseInt(interval);

            //convert from minutes to milliseconds 
            cacheTtl = cacheTtl * 60 * 1000; 
            cleanupInterval = cacheTtl;

	} catch (NumberFormatException nfe) {
            throw new PolicyException(ResBundleUtils.rbName,
                    "invalid_cache_ttl", null, nfe);
	}
        if (cacheTtl <= 0) {
            if (debug.warningEnabled()) {
                debug.warning("PolicyProperties():configured cacheTtl"
                    + cacheTtl + " seconds too small");
                debug.warning("PolicyProperties():setting cacheTtl as"
                    + CACHE_TTL_DEFAULT + " minutes");
            }
	    cacheTtl = CACHE_TTL_DEFAULT * 60 * 1000; //convert to milliseconds
            cleanupInterval = cacheTtl;
        } else {
            if (debug.messageEnabled()) {
                debug.message("PolicyProperties:cacheTtl=" 
                        + (cacheTtl/60/1000) + "minutes" );
            }
        }

        previousNotificationEnabledFlag = notificationEnabledFlag;
        previousNotificationURL = notificationURL;

        //initialize notification status
	String isEnabled = getSystemProperty(NOTIFICATION_ENABLED, ignoreCase);
	if ((isEnabled == null) || (isEnabled.length() == 0)) {
            if (debug.warningEnabled()) {
                debug.warning("PolicyProperties:invalid value for poperty:"
                    + NOTIFICATION_ENABLED + ":defaulting to:"
                    + NOTIFICATION_ENABLED_DEFAULT);
            }
	}
        notificationEnabledFlag = Boolean.valueOf(isEnabled).booleanValue();
        if (debug.messageEnabled()) {
            debug.message("PolicyProperties:notificationEnabledFlag=" 
                    + notificationEnabledFlag);
        }
	if(notificationEnabledFlag == true) {
            //initialize notification url
            try {
                notificationURL = WebtopNaming.getNotificationURL().toString();
            } catch(URLNotFoundException e) {
                if (debug.messageEnabled()) {
                    debug.message("PolicyProperties:notificationURL not found",
                            e);
                }
            }
	    if ((notificationURL == null) || (notificationURL.length() == 0)) {
		throw new PolicyException(ResBundleUtils.rbName,
                "invalid.notificationurl", null, null);
	    } else {
                if (debug.messageEnabled()) {
                    debug.message("PolicyProperties:notificationURL=" 
                            + notificationURL);
                }
            }
	}

        //initialize cahceMode:subtree | self| strict-subtree
        cacheMode = getSystemProperty(CACHE_MODE, ignoreCase);
        if ((cacheMode == null) || !((cacheMode.equals(SUBTREE)
                    || cacheMode.equals(SELF)))) {
            if (debug.warningEnabled()) {
                debug.warning("PolicyProperties.init():" + CACHE_MODE
                        + ":not defined, or invalid, defaulting to:"
                        + CACHE_MODE_DEFAULT);
            }
            cacheMode = CACHE_MODE_DEFAULT;
        } else {
            if (debug.messageEnabled()) {
                debug.message("PolicyProperties.init():" 
                        + "cacheMode=" + cacheMode);
            }
        }

        //initialize logging status
        String status = getSystemProperty(LOGGING_LEVEL, ignoreCase);
        if ((status == null) || (status.length() == 0)) {
            status = DEFAULT_LOGGING_LEVEL;
            if (debug.messageEnabled()) {
                debug.message("PolicyProperties:property " + LOGGING_LEVEL
                        + " is not specified, use default value " 
                        + DEFAULT_LOGGING_LEVEL);
            }
        }
        if (status != null) {
            if (status.equalsIgnoreCase(ALLOW)) {
                logActions = ALLOW;
            } else if (status.equalsIgnoreCase(DENY)) {
                logActions = DENY;
            } else if (status.equalsIgnoreCase(BOTH)) {
                logActions = BOTH;
            } else if (status.equalsIgnoreCase(DECISION)) {
                logActions = DECISION;
            } else {
                logActions = NONE;
            }
            if (debug.messageEnabled()) {
                debug.message("PolicyProperties():property:"
                        + logActions + "=logActions");
            }
	}

        //intialize boolean action values
        String booleanActionValuesString  = getSystemProperty(
                BOOLEAN_ACTION_VALUES, ignoreCase);
        if (booleanActionValuesString != null) {
            StringTokenizer st1 = new StringTokenizer(
                    booleanActionValuesString, COLON);
            while (st1.hasMoreTokens()) {
                String str = st1.nextToken();
                StringTokenizer st2 = new StringTokenizer(str, PIPE); 
                int tokenCount = st2.countTokens();
                if ( tokenCount != 4) { //todo: throw exception
                    debug.error("PolicyProperties():"
                            + "booleanActionValues not well formed:" 
                            + booleanActionValuesString);
                    Object[] args = {str};
                    throw new PolicyException(ResBundleUtils.rbName,
                        "invalid_boolean_action_values", args, null);
                } else {
                    //set boolean action values
                    String serviceName = st2.nextToken();
                    String actionName = st2.nextToken();
                    String trueValue = st2.nextToken();
                    String falseValue = st2.nextToken();
                    setBooleanActionValues(serviceName, actionName, 
                            trueValue, falseValue);
                }
            }
        } else {
            if (debug.warningEnabled()) {
                debug.warning("PolicyProperties():property:"
                        + BOOLEAN_ACTION_VALUES + ":not defined");
            }
        }

        //intialize pre2.2 booleanValues
	String usePre22BooleanValuesString 
                = getSystemProperty(USE_PRE22_BOOLEAN_VALUES, 
                ignoreCase,
                USE_PRE22_BOOLEAN_VALUES_DEFAULT);
        usePre22BooleanValues 
                = Boolean.valueOf(usePre22BooleanValuesString).booleanValue();
        if (debug.messageEnabled()) {
            debug.message("PolicyProperries:usePre22BooleanValues="
                    + usePre22BooleanValues);
        }
        if (usePre22BooleanValues) {
            pre22TrueValue 
                    = getSystemProperty(
                    "com.sun.identity.agents.true.value",
                    ignoreCase,
                    PRE22_TRUE_VALUE_DEFAULT);

            if (debug.messageEnabled()) {
                debug.message("PolicyProperries:pre22TrueValue="
                        + pre22TrueValue);

                //pre22FalseValue is initialised to PRE22_FALSE_VALUE at decln
                debug.message("PolicyProperries:pre22FalseValue="
                        + pre22FalseValue);
            }
        }

        //initialize resourceComparators
        String resourceComparatorsString  = getSystemProperty(
                RESOURCE_COMPARATORS, ignoreCase);
        if (resourceComparatorsString != null) {
            StringTokenizer st1 
                    = new StringTokenizer(resourceComparatorsString,
                    COLON);
            while (st1.hasMoreTokens()) {
                String str = st1.nextToken();
                setResourceComparator(str);
            }
        } else {
            if (debug.warningEnabled()) {
                debug.warning("PolicyProperties():property:"
                        + RESOURCE_COMPARATORS + ":not defined");
            }
        }

        //initialize responseAttribtue names
	String attrs = getSystemProperty(HEADER_ATTRIBUTES, ignoreCase);
	if ((attrs != null) && (attrs.length() > 0)) {
	    StringTokenizer st = new StringTokenizer(attrs, PIPE);
	    responseAttributeNames = new HashSet(st.countTokens());
	    while (st.hasMoreTokens()) {
		responseAttributeNames.add(st.nextToken());
	    }
	}

        //initialize clientClockSkew
        String clientClockSkewString 
                = getSystemProperty(CLIENT_CLOCK_SKEW, ignoreCase);
        if (clientClockSkewString == null) {
            if (debug.messageEnabled()) {
                debug.message("PolicyProperties.getClientClockSkew():"
                        + CLIENT_CLOCK_SKEW + " Property not defined "
                        + ": defaulting to 0");
            }
        } else {
            try {

                //convert from seconds to milliseconds
                clientClockSkew = Long.valueOf(clientClockSkewString).longValue()*1000;
                if (debug.messageEnabled()) {
                    debug.message(
                            "PolicyProperties.constructor():"
                            + CLIENT_CLOCK_SKEW + " = "
                            + clientClockSkewString);
                }
            } catch (NumberFormatException nfe) {
                if (debug.messageEnabled()) {
                    debug.message(
                            "PolicyProperties.constructor():"
                            + CLIENT_CLOCK_SKEW + " not a long number"
                            + ": defaulting to 0", nfe);
                }
            }
        }
        
        //initialize resultsCacheSessionCap
        String resultsCacheSessionCapString 
                = getSystemProperty(RESULTS_CACHE_SESSION_CAP, ignoreCase);
        if (resultsCacheSessionCapString == null) {
            if (debug.messageEnabled()) {
                debug.message("PolicyProperties.constructor():"
                        + RESULTS_CACHE_SESSION_CAP
                        + " Property not defined "
                        + ": defaulting to " + DEFAULT_RESULTS_CACHE_SESSION_CAP);
            }
            resultsCacheSessionCap = DEFAULT_RESULTS_CACHE_SESSION_CAP;
        } else {
            try {
                resultsCacheSessionCap 
                        = Integer.valueOf(resultsCacheSessionCapString).intValue();
                if (debug.messageEnabled()) {
                    debug.message(
                            "PolicyProperties.constructor():"
                            + RESULTS_CACHE_SESSION_CAP + " = "
                            + resultsCacheSessionCap);
                }
            } catch (NumberFormatException nfe) {
                if (debug.messageEnabled()) {
                    debug.message(
                            "PolicyProperties.constructor():"
                            + RESULTS_CACHE_SESSION_CAP + " not a number"
                            + ": defaulting to " 
                            + DEFAULT_RESULTS_CACHE_SESSION_CAP);
                }
                resultsCacheSessionCap = DEFAULT_RESULTS_CACHE_SESSION_CAP;
            }
        }
        
        //initialize resultsCacheResourceCap
        String resultsCacheResourceCapString 
                = getSystemProperty(RESULTS_CACHE_RESOURCE_CAP, ignoreCase);
        if (resultsCacheResourceCapString == null) {
            if (debug.messageEnabled()) {
                debug.message("PolicyProperties.constructor():"
                        + RESULTS_CACHE_RESOURCE_CAP
                        + " Property not defined "
                        + ": defaulting to " + DEFAULT_RESULTS_CACHE_RESOURCE_CAP);
            }
            resultsCacheResourceCap = DEFAULT_RESULTS_CACHE_RESOURCE_CAP;
        } else {
            try {
                resultsCacheResourceCap 
                        = Integer.valueOf(resultsCacheResourceCapString).intValue();
                if (debug.messageEnabled()) {
                    debug.message(
                            "PolicyProperties.constructor():"
                            + RESULTS_CACHE_RESOURCE_CAP + " = "
                            + resultsCacheResourceCap);
                }
            } catch (NumberFormatException nfe) {
                if (debug.messageEnabled()) {
                    debug.message(
                            "PolicyProperties.constructor():"
                            + RESULTS_CACHE_RESOURCE_CAP + " not a number"
                            + ": defaulting to " 
                            + DEFAULT_RESULTS_CACHE_RESOURCE_CAP);
                }
                resultsCacheResourceCap = DEFAULT_RESULTS_CACHE_RESOURCE_CAP;
            }
        }
        
        //initialize useRESTProtocolFlag property
        String useRESTProtocolString = getSystemProperty(USE_REST_PROTOCOL, ignoreCase);
        if ((useRESTProtocolString == null) || (useRESTProtocolString.length() == 0)) {
                if (debug.warningEnabled()) {
                    debug.warning("PolicyProperties:invalid value for poperty:"
                        + USE_REST_PROTOCOL + ":defaulting to:"
                        + USE_REST_PROTOCOL_DEFAULT);
                }
                useRESTProtocolString = USE_REST_PROTOCOL_DEFAULT;
        }
        useRESTProtocolFlag = Boolean.valueOf(useRESTProtocolString).booleanValue();
        if (debug.messageEnabled()) {
            debug.message("PolicyProperties:useRESTProtocolFlag=" 
                    + useRESTProtocolFlag);
        }

        //initialize restNotificationURL
        restNotificationURL = getSystemProperty(REST_NOTIFICATION_URL, ignoreCase);
        if ((restNotificationURL == null) || (restNotificationURL.length() == 0)) {
            if (notificationEnabledFlag && useRESTProtocolFlag) {
                if (debug.warningEnabled()) {
                    debug.warning("PolicyProperties:empty REST notification URL, "
                        + "disabling notification");
                }
                notificationEnabledFlag = false;
            } else {
                if (debug.messageEnabled()) {
                    debug.message("PolicyProperties: restNotificationURL:"
                        + restNotificationURL);
                }
            }
        }

        if (debug.messageEnabled()) {
            debug.message("PolicyProperties():constructed");
        }
    }

    /**
     * Returns name of the log used by policy client API
     * @return name of the log used by policy client API
     */
    String getLogName() throws PolicyException {
        return logName;
    }

    /**
     * Returns control value for actions to be logged. 
     * Supported values are <code>ALLOW</code>, <code>DENY</code>, 
     * <code>NONE</code>, <code>BOTH</code>, <code>DECISION</code>
     * @return control value used by policy client API for logging
     */
    String getLogActions() throws PolicyException {
        return logActions;
    }
 
    /**
     * Returns local cache time limit.
     * Policy decisions would be discarded after this time limit.
     * @return cache time limit in milliseconds
     */
    int getCacheTtl() throws PolicyException {
        return cacheTtl; //milliseconds
    }

    /**
     * Returns cache mode used by client policy API.
     * Supported values are <code>self</code>, <code>subtree</code>,
     * <code>strict-subtree</code>
     * @return cache mode used by client policy API
     */
    String getCacheMode() throws PolicyException {
        return cacheMode;
    }

    /**
     * Returns clean up interval used by policy client API.
     * @return cache clean up interval used by policy client API in milliseconds
     * @deprecated
     */
    int getCleanupInterval() throws PolicyException {
        return cleanupInterval; //milliseconds
    }

    /**
     * Returns the client clock skew 
     * @return skew the time skew in milliseconds, serverTime - clientTime
     * @see #CLIENT_CLOCK_SKEW
     */
    long getClientClockSkew() {
        return clientClockSkew;
    }

    /**
     * Checks if policy client should use REST protocol to talk to sever
     * to get results. At present, REST protocol would be used only if
     * results are requested for self mode
     * @return <code>true</code> if client should use REST protocol to
     * talk to server
     */
    boolean useRESTProtocol() {
        return useRESTProtocolFlag;
    }

    /**
     * Returns REST notificaton URL
     * @return REST notification URL
     */
    String getRESTNotificationURL() {
        return restNotificationURL;
    }

    /**
     * Checks if policy client is enabled to get notifications from policy
     * service
     * @return <code>true</code> if client is enabled to get notifications from
     * policy service
     */
    boolean notificationEnabled() {
        return notificationEnabledFlag;
    }

    /**
     * Checks if policy client was enabled to get notifications previously
     * @return <code>true</code> if client wass enabled to get notifications from
     * policy service
     */
    static boolean previouslyNotificationEnabled() {
        return previousNotificationEnabledFlag;
    }

    /**
     * Returns notification URL on the client side that would listen for 
     * notifications from policy service
     *
     * @return notification URL on the client side that would listen for
     * notifications from policy service
     */
    String getNotificationURL() {
        return notificationURL;
    }

    /**
     * Returns previous notification URL used on the client side to get 
     * notifications from policy service
     *
     * @return previous notification URL on the client side that was listening
     * for notifications from policy service
     */
    static String getPreviousNotificationURL() {
        return previousNotificationURL;
    }

    /**
     * Returns <code>String</code> that is used as boolean true value 
     * for a policy controlled action
     *
     * @param serviceName name of policy controlled service
     * @param actionName name of policy controlled action
     * 
     * @return <code>String</code> that is used as boolean true value 
     * for a policy controlled action
     */
    String getTrueValue(String serviceName, String actionName) {
        String trueValue = null;
        Map serviceEntry = (Map)booleanActionValues.get(serviceName);
        if ( serviceEntry != null) {
            String[] actionEntry = (String[])serviceEntry.get(actionName);
            if (actionEntry != null) {
                trueValue = actionEntry[0];
            }
        } else {
            if (usePre22BooleanValues) {
                trueValue = pre22TrueValue;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("PolicyProperties.getTrueValue():"
                + "servcieName=" + serviceName + ",actionName=" + actionName 
                + " " + ":returning:" + trueValue );
        }
        return trueValue;
    }

    /**
     * Returns <code>String</code> that is used as boolean false value 
     * for a policy controlled action
     *
     * @param serviceName name of policy controlled service
     * @param actionName name of policy controlled action
     * 
     * @return <code>String</code> that is used as boolean false value 
     * for a policy controlled action
     */
    String getFalseValue(String serviceName, String actionName) {
        String falseValue = null;
        Map serviceEntry = (Map)booleanActionValues.get(serviceName);
        if ( serviceEntry != null) {
            String[] actionEntry = (String[])serviceEntry.get(actionName);
            if (actionEntry != null) {
                falseValue = actionEntry[1];
            }
        } else {
            if (usePre22BooleanValues) {
                falseValue = pre22FalseValue;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("PolicyProperties.getFalseValue():"
                + "servcieName=" + serviceName + ",actionName=" + actionName 
                + ":returning:" + falseValue );
        }
        return falseValue;
    }

    /**
     * Returns a <code>Map</code> of <code>ResourceName</code> used by policy
     * client API. The key of the <code>Map</code> is service name and the 
     * value is <code>ResourceName</code>
     *
     * @return a <code>Map</code> of <code>ResourceName</code> used by policy
     * client API 
     */
    private Map getResourceComparators() {
        return resourceComparators;
    }

    /**
     * Returns the <code>ResourceName</code> used by a policy controlled service
     *
     * @param serviceName name of the service for which to find 
     * <code>ResourceName</code>
     *
     * @return the <code>ResourceName</code> used by the policy controlled 
     * service
     */
    ResourceName getResourceComparator(String serviceName) {
        ResourceName resourceComparator 
                = (ResourceName)resourceComparators.get(serviceName);
        if (resourceComparator == null) {
            if (debug.warningEnabled()) {
                debug.warning("PolicyProperties.getResourceComparator():"
                        + "ResourceName not configured for service:"
                        + serviceName + ":defaulting to PrefixResourceName");
            }
            resourceComparator = prefixResourceName;
        }
        return resourceComparator;
    }

    /**
     * Returns the names of the response attributes that would be requested from
     * policy service. 
     *
     * @return the names of the response attributes that would be requested from
     * policy service. 
     *
     * @deprecated
     */
    Set getResponseAttributeNames() {
        return responseAttributeNames;
    }

    /**
     * Sets<code>String</code> values to be used as boolean true value 
     * and boolean false value for a policy controlled action
     *
     * @param serviceName name of policy controlled service
     * @param actionName name of policy controlled action
     * @param trueValue <code>String</code> to be uses as true value
     * @param falseValue <code>String</code> to be uses as false value
     * 
     */
    private void setBooleanActionValues(String serviceName, String actionName,
            String trueValue, String falseValue) {
        if (debug.messageEnabled()) {
            debug.message("PolicyProperties.setBooleanActionValues():"
                + "servcieName=" + serviceName + ",actionName=" + actionName
                + ",trueValue=" + trueValue 
                + ",falseValue=" + falseValue );
        }
        Map serviceEntry = (Map)booleanActionValues.get(serviceName);
        if ( serviceEntry == null) {
            serviceEntry = new HashMap(4);
            booleanActionValues.put(serviceName, serviceEntry);
        }
        String[] actionEntry = (String[])serviceEntry.get(actionName);
        if (actionEntry == null) {
            actionEntry = new String[2];
            serviceEntry.put(actionName, actionEntry);
        }
        actionEntry[0] = trueValue;
        actionEntry[1] = falseValue;
    }

    /**
     * Sets the <code>ResourceName</code> to be used by policy client API
     *
     * @param str <code>ResourceName</code> to be used by different services
     * with control parameters formatted in a proprietary <code>String</code>
     * format
     *
     */
    void setResourceComparator(String str) throws PolicyException {
        if (debug.messageEnabled()) {
            debug.message("PolicyProperties.setResourceComparator():"
                    + "entering with str value=" + str);
        }
        ResourceName resourceComparator = null;
        String[] tokens = new String[5];
        String serviceName = null;
        String className = null;
        String delimiter = null;
        String wildCard = null;
        String oneLevelWildCard = null;
        String caseSensitive = null;
        int count = 0;
        Map configMap = new HashMap(4);
        StringTokenizer st = new StringTokenizer(str, PIPE);
        while (st.hasMoreTokens()) {
            tokens[count++] = st.nextToken();
            if (count > 4) { // accept only first five tokens
                break;
            }
        }
        for (int i = 0; i < count; i++) {
            int equal = tokens[i].indexOf("=");
            String name = tokens[i].substring(0, equal);	
            String value = tokens[i].substring(equal + 1);	
            if (name == null) {
                debug.error("PolicyProperties.setResourceComparator():"
                        + "Resource comapartaor: name is null");
                continue;
            }
            if (value == null) {
                debug.error("PolicyProperties.setResourceComparator():"
                        + "Resource comapartaor: value is null");
                continue;
            }
            if (debug.messageEnabled()) {
                debug.message("PolicyProperties.setResourceComparator():"
                        + "Attr Name= " + name + ":Attr Value=" + value);
            }
            if (name.equalsIgnoreCase(PolicyConfig.RESOURCE_COMPARATOR_TYPE)) {
                serviceName = value;
            } else if (name.equalsIgnoreCase(
                    PolicyConfig.RESOURCE_COMPARATOR_CLASS)) {
                    configMap.put(PolicyConfig.RESOURCE_COMPARATOR_CLASS,
                            className);
                className = value;
            } else if (name.equalsIgnoreCase(
                    PolicyConfig.RESOURCE_COMPARATOR_DELIMITER)) {
                delimiter = value;
                configMap.put(PolicyConfig.RESOURCE_COMPARATOR_DELIMITER, 
                        delimiter);
            } else if (name.equalsIgnoreCase(
                    PolicyConfig.RESOURCE_COMPARATOR_WILDCARD)) {
                wildCard = value;
                configMap.put(PolicyConfig.RESOURCE_COMPARATOR_WILDCARD, 
                        wildCard);
            } else if (name.equalsIgnoreCase(
                    PolicyConfig.RESOURCE_COMPARATOR_ONE_LEVEL_WILDCARD)) {
                oneLevelWildCard = value;
                configMap.put(
                        PolicyConfig.RESOURCE_COMPARATOR_ONE_LEVEL_WILDCARD, 
                        oneLevelWildCard);
            } else if (name.equalsIgnoreCase(
                    PolicyConfig.RESOURCE_COMPARATOR_CASE_SENSITIVE)) {
                caseSensitive = value;
                configMap.put(PolicyConfig.RESOURCE_COMPARATOR_CASE_SENSITIVE, 
                        caseSensitive);
            }
        }
        if (serviceName == null) { 
            debug.error("PolicyProperties().setResourceComparator():"
                    + "ResourceComparator definition"
                    + " not well formed" + str);
            Object[] args = {str};
            throw new PolicyException(ResBundleUtils.rbName,
                "invalid_resource_comparator", args, null);
        } else {
            try {
                if (className != null) {
                    Class resourceClass = Class.forName(className);
                    resourceComparator 
                            = (ResourceName) resourceClass.newInstance();
                    resourceComparator.initialize(configMap);
                } 
            } catch (ClassNotFoundException e) {
                debug.error("PolicyProperties.setResourceComparator():"
                        + "Illegal exception ", e);
            } catch (IllegalAccessException e) {
                debug.error("PolicyProperties.setResourceComparator():"
                        + "Illegal exception ", e);
            } catch (InstantiationException e) {
                debug.error("PolicyProperties.setResourceComparator():"
                        + "InstantiationException " + " exception ", e);
            } finally {
                if (resourceComparator == null) {
                    debug.error("PolicyProperties.setResourceCompartor():"
                            + "invalid configuration:" + str 
                            + ":defaulting to PrefixResourceName");
                    resourceComparator = new PrefixResourceName();
                }
            }
            resourceComparators.put(serviceName, resourceComparator);
        }
    }

    int getResultsCacheSessionCap() {
        return resultsCacheSessionCap;
    }

    int getResultsCacheResourceCap() {
        return resultsCacheResourceCap;
    }

    /**
     * Gets system property
     * @param name name of the property
     * @return value of the system property
     */
    private String getSystemProperty(String name) {
        return getSystemProperty(name, false, null);
    }

    /**
     * Gets system property
     * @param name name of the property
     * @param ignoreCase flag to indicate whether case of the name shoule be
     * ignored
     * properties
     * @return value of the system property
     */
    private String getSystemProperty(String name, boolean ignoreCase) {
        return getSystemProperty(name, ignoreCase, null);
    }

    /**
     * Gets system property
     * @param name name of the property
     * @param ignoreCase flag to indicate whether case of the name shoule be
     * ignored
     * @param defaultValue default value if the value is not defined in system
     * properties
     * @return value of the system property
     */
    private String getSystemProperty(String name, boolean ignoreCase, 
            String defaultValue) {
        if (name == null) {
            return null;
        }
        String value = SystemProperties.get(name);
        if ((value == null) && (ignoreCase)) {
            value = SystemProperties.get(name.toLowerCase());
        }
        return (value != null) ? value.trim() : defaultValue;
    }

}
