/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

/*
 * Portions Copyrighted 2013-2014 Nomura Research Institute, Ltd
 */

package org.forgerock.openam.authentication.modules.adaptive;

import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6AddressRange;
import com.googlecode.ipv6.IPv6Network;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.encode.Hash;
import org.forgerock.openam.utils.ClientUtils;
import org.forgerock.openam.utils.IPRange;
import org.forgerock.openam.utils.ValidateIPaddress;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.Principal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class Adaptive extends AMLoginModule implements AMPostAuthProcessInterface {

    private static final String ADAPTIVE = "amAuthAdaptive";
    private static final String AUTHLEVEL = "openam-auth-adaptive-auth-level";
    private static final String ADAPTIVETHRESHOLD = "openam-auth-adaptive-auth-threshold";
    private static final String AUTH_FAILURE_CHECK = "openam-auth-adaptive-failure-check";
    private static final String AUTH_FAILURE_SCORE = "openam-auth-adaptive-failure-score";
    private static final String AUTH_FAILURE_INVERT = "openam-auth-adaptive-failure-invert";
    private static final String IP_RANGE_CHECK = "openam-auth-adaptive-ip-range-check";
    private static final String IP_RANGE_RANGE = "openam-auth-adaptive-ip-range-range";
    private static final String IP_RANGE_SCORE = "openam-auth-adaptive-ip-range-score";
    private static final String IP_RANGE_INVERT = "openam-auth-adaptive-ip-range-invert";
    private static final String IP_HISTORY_CHECK = "openam-auth-adaptive-ip-history-check";
    private static final String IP_HISTORY_COUNT = "openam-auth-ip-adaptive-history-count";
    private static final String IP_HISTORY_ATTRIBUTE = "openam-auth-adaptive-ip-history-attribute";
    private static final String IP_HISTORY_SAVE = "openam-auth-adaptive-ip-history-save";
    private static final String IP_HISTORY_SCORE = "openam-auth-adaptive-ip-history-score";
    private static final String IP_HISTORY_INVERT = "openam-auth-adaptive-ip-history-invert";
    private static final String KNOWN_COOKIE_CHECK = "openam-auth-adaptive-known-cookie-check";
    private static final String KNOWN_COOKIE_NAME = "openam-auth-adaptive-known-cookie-name";
    private static final String KNOWN_COOKIE_VALUE = "openam-auth-adaptive-known-cookie-value";
    private static final String KNOWN_COOKIE_SAVE = "openam-auth-adaptive-known-cookie-save";
    private static final String KNOWN_COOKIE_SCORE = "openam-auth-adaptive-known-cookie-score";
    private static final String KNOWN_COOKIE_INVERT = "openam-auth-adaptive-known-cookie-invert";
    private static final String DEVICE_COOKIE_CHECK = "openam-auth-adaptive-device-cookie-check";
    private static final String DEVICE_COOKIE_NAME = "openam-auth-adaptive-device-cookie-name";
    private static final String DEVICE_COOKIE_SAVE = "openam-auth-adaptive-device-cookie-save";
    private static final String DEVICE_COOKIE_SCORE = "openam-auth-adaptive-device-cookie-score";
    private static final String DEVICE_COOKIE_INVERT = "openam-auth-adaptive-device-cookie-invert";
    private static final String TIME_OF_DAY_CHECK = "openam-auth-time-of-day-check";
    private static final String TIME_OF_DAY_RANGE = "openam-auth-time-of-day-range";
    private static final String TIME_OF_DAY_INVERT = "openam-auth-time-of-day-invert";
    private static final String TIME_SINCE_LAST_LOGIN_CHECK = "openam-auth-adaptive-time-since-last-login-check";
    private static final String TIME_SINCE_LAST_LOGIN_ATTRIBUTE = "openam-auth-adaptive-time-since-last-login-cookie-name";
    private static final String TIME_SINCE_LAST_LOGIN_VALUE = "openam-auth-adaptive-time-since-last-login-value";
    private static final String TIME_SINCE_LAST_LOGIN_SAVE = "openam-auth-adaptive-time-since-last-login-save";
    private static final String TIME_SINCE_LAST_LOGIN_SCORE = "openam-auth-adaptive-time-since-last-login-score";
    private static final String TIME_SINCE_LAST_LOGIN_INVERT = "openam-auth-adaptive-time-since-last-login-invert";
    private static final String RISK_ATTRIBUTE_CHECK = "openam-auth-adaptive-risk-attribute-check";
    private static final String RISK_ATTRIBUTE_NAME = "openam-auth-adaptive-risk-attribute-name";
    private static final String RISK_ATTRIBUTE_VALUE = "openam-auth-adaptive-risk-attribute-value";
    private static final String RISK_ATTRIBUTE_SCORE = "openam-auth-adaptive-risk-attribute-score";
    private static final String RISK_ATTRIBUTE_INVERT = "openam-auth-adaptive-risk-attribute-invert";
    private static final String GEO_LOCATION_CHECK = "openam-auth-adaptive-geo-location-check";
    private static final String GEO_LOCATION_DATABASE = "openam-auth-adaptive-geo-location-database";
    private static final String GEO_LOCATION_VALUES = "openam-auth-adaptive-geo-location-values";
    private static final String GEO_LOCATION_SCORE = "openam-auth-adaptive-geo-location-score";
    private static final String GEO_LOCATION_INVERT = "openam-auth-adaptive-geo-location-invert";
    private static final String REQ_HEADER_CHECK = "openam-auth-adaptive-req-header-check";
    private static final String REQ_HEADER_NAME = "openam-auth-adaptive-req-header-name";
    private static final String REQ_HEADER_VALUE = "openam-auth-adaptive-req-header-value";
    private static final String REQ_HEADER_SCORE = "openam-auth-adaptive-req-header-score";
    private static final String REQ_HEADER_INVERT = "openam-auth-adaptive-req-header-invert";
    private static Debug debug = Debug.getInstance(ADAPTIVE);
    private static DatabaseReader lookupService = null;
    private String userUUID = null;
    private String userName = null;
    private AMIdentity amAuthIdentity = null;
    private Map postAuthNMap = null;
    private Principal userPrincipal = null;
    private String clientIP = null;
    private int adaptiveThreshold = 1;
    private boolean authFailureCheck = false;
    private int authFailureScore = 1;
    private boolean authFailureInvert = false;
    private boolean IPRangeCheck = false;
    private Set<String> IPRangeRange = null;
    private int IPRangeScore = 1;
    private boolean IPRangeInvert = false;
    private boolean IPHistoryCheck = false;
    private int IPHistoryCount = 0;
    private String IPHistoryAttribute = null;
    private boolean IPHistorySave = false;
    private int IPHistoryScore = 1;
    private boolean IPHistoryInvert = false;
    private boolean knownCookieCheck = false;
    private String knownCookieName = null;
    private String knownCookieValue = null;
    private boolean knownCookieSave = false;
    private int knownCookieScore = 1;
    private boolean knownCookieInvert = false;
    private boolean deviceCookieCheck = false;
    private String deviceCookieName = null;
    private boolean deviceCookieSave = false;
    private int deviceCookieScore = 1;
    private boolean deviceCookieInvert = false;
    private boolean timeOfDayCheck = false;
    private String timeOfDayRange = null;
    private boolean timeOfDayInvert = false;
    private boolean timeSinceLastLoginCheck = false;
    private String timeSinceLastLoginAttribute = null;
    private int timeSinceLastLoginValue = 0;
    private boolean timeSinceLastLoginSave = false;
    private int timeSinceLastLoginScore = 1;
    private boolean timeSinceLastLoginInvert = false;
    private boolean riskAttributeCheck = false;
    private String riskAttributeName = null;
    private String riskAttributeValue = null;
    private int riskAttributeScore = 1;
    private boolean riskAttributeInvert = false;
    private boolean geoLocationCheck = false;
    private String geoLocationDatabase = null;
    private String geoLocationValues = null;
    private int geoLocationScore = 1;
    private boolean geoLocationInvert = false;
    private boolean reqHeaderCheck = false;
    private String reqHeaderName = null;
    private String reqHeaderValue = null;
    private int reqHeaderScore = 1;
    private boolean reqHeaderInvert = false;
    private final static String IP_V4 = "IPv4";
    private final static String IP_V6 = "IPv6";
    private static final String IP_Version = "IPVersion";
    private static final String IP_START = "IPStart";
    private static final String IP_END = "IPEnd";
    private static final String IP_TYPE = "Type";

    private static final String UNKNOWN_COUNTRY_CODE = "--";

    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        postAuthNMap = new HashMap<String, String>();
        String authLevel = CollectionHelper.getMapAttr(options, AUTHLEVEL);

        if (authLevel != null) {
            try {
                setAuthLevel(Integer.parseInt(authLevel));
            } catch (Exception e) {
                debug.error("Unable to set auth level " + authLevel, e);
            }
        }
        Locale locale = getLoginLocale();
        initParams(options);

        if (debug.messageEnabled()) {
            debug.message("Adaptive resbundle locale=" + locale);
        }

        try {
            userName = (String) sharedState.get(getUserKey());
        } catch (Exception e) {
            debug.error("Adaptive.init() : " + "Unable to set userName : ", e);
        }
    }

    @Override
    public int process(Callback[] callbacks, int state)
            throws AuthLoginException {
        int currentScore = 0;

        debug.message("Adaptive: process called with state = " + state);

        if (state != ISAuthConstants.LOGIN_START) {
            throw new AuthLoginException("Authentication failed: Internal Error - NOT LOGIN_START");
        }

        if (userName == null || userName.length() == 0) {
            // session upgrade case. Need to find the user ID from the old
            // session
            try {
                SSOTokenManager mgr = SSOTokenManager.getInstance();
                InternalSession isess = getLoginState(ADAPTIVE).getOldSession();
                if (isess == null) {
                    throw new AuthLoginException(ADAPTIVE, "noInternalSession",
                            null);
                }
                SSOToken token = mgr.createSSOToken(isess.getID().toString());
                userUUID = token.getPrincipal().getName();
                userName = token.getProperty("UserToken");
                if (debug.messageEnabled()) {
                    debug.message("Adaptive.process() : " + "UserName in SSOToken : " + userName);
                }

                if (userName == null || userName.length() == 0) {
                    throw new AuthLoginException("amAuth", "noUserName", null);
                }
            } catch (SSOException e) {
                debug.message(ADAPTIVE + ": amAuthIdentity NULL ");
                throw new AuthLoginException(ADAPTIVE, "noIdentity", null);
            }
        }
        debug.message(ADAPTIVE + ": Login Attempt User = " + userName);

        amAuthIdentity = getIdentity(userName);
        clientIP = ClientUtils.getClientIPAddress(getHttpServletRequest());

        if (amAuthIdentity == null) {
            debug.message(ADAPTIVE + ": amAuthIdentity NULL : " + userName);
            throw new AuthLoginException(ADAPTIVE, "noIdentity", null);
        }

        try {
            if (IPRangeCheck) {
                currentScore += checkIPRange();
            }
            if (IPHistoryCheck) {
                currentScore += checkIPHistory();
            }
            if (knownCookieCheck) {
                currentScore += checkKnownCookie();
            }
            if (timeOfDayCheck) {
                currentScore += checkTimeDay();
            }
            if (timeSinceLastLoginCheck) {
                currentScore += checkLastLogin();
            }
            if (riskAttributeCheck) {
                currentScore += checkRiskAttribute();
            }
            if (authFailureCheck) {
                currentScore += checkAuthFailure();
            }
            if (deviceCookieCheck) {
                currentScore += checkRegisteredClient();
            }
            if (geoLocationCheck) {
                currentScore += checkGeoLocation();
            }
            if (reqHeaderCheck) {
                currentScore += checkRequestHeader();
            }
        } catch (Exception ex) {
            currentScore = Integer.MAX_VALUE;
            debug.error(ADAPTIVE + ".process() : Unknown exception occured while"
                    + " executing checks, module will fail", ex);
        }

        setPostAuthNParams();

        if (currentScore < adaptiveThreshold) {
            debug.message(ADAPTIVE + ": Returning Success : " + userName);
            return ISAuthConstants.LOGIN_SUCCEED;
        } else {
            debug.message(ADAPTIVE + ": Returning FAIL : " + userName);
            throw new AuthLoginException(ADAPTIVE + " - Risk determined.");
        }
    }

    /**
     * Checks what type of version of IP range is
     * @param range can be a range, CIDR, or a single IP address
     * @return map containing details about range. IPType, IPVersion,
     * start IP and End IP is applicable
     */
    private Map<String, String> checkIPVersion(String range) {
        Map<String, String> details = new HashMap<String, String>(3);
        StringTokenizer st;
        String ipStart, ipEnd;
        if (range.contains("-")) {
            debug.message("IPRange found - ");
            st = new StringTokenizer(range, "-");
            if(st.countTokens() != 2){
                throw new IllegalArgumentException(range + " is not a valid range");
            }
            ipStart = st.nextToken();
            ipEnd = st.nextToken();
            if(ValidateIPaddress.isIPv4(ipStart) && ValidateIPaddress.isIPv4(ipEnd)){
                details.put(IP_Version, IP_V4);
                details.put(IP_TYPE, "Range");
            } else if (ValidateIPaddress.isIPv6(ipStart) && ValidateIPaddress.isIPv6(ipEnd)){
                details.put(IP_Version, IP_V6);
                details.put(IP_TYPE, "Range");
                details.put(IP_START, ipStart);
                details.put(IP_END, ipEnd);
            } else {
                throw new IllegalArgumentException(range + " is not a valid range");
            }
        } else if (range.contains("/")) {
            debug.message("IPRange found / ");
            String cidr;
            st = new StringTokenizer(range, "/");
            if(st.countTokens() != 2){
                throw new IllegalArgumentException("Invalid CIDR found.");
            }
            ipStart = st.nextToken();
            cidr = st.nextToken();
            if(ValidateIPaddress.isIPv4(ipStart) &&
                    (Integer.parseInt(cidr) >= 0) && (Integer.parseInt(cidr) <= 32)){
                details.put(IP_Version, IP_V4);
                details.put(IP_TYPE, "CIDR");
            } else if (ValidateIPaddress.isIPv6(ipStart) &&
                    (Integer.parseInt(cidr) >= 0) && (Integer.parseInt(cidr) <= 128)) {
                details.put(IP_Version, IP_V6);
                details.put(IP_TYPE, "CIDR");
            } else {
                throw new IllegalArgumentException(ipStart + " is not a valid format for CIDR");
            }
        } else {
            debug.message("IPRange found single IP ");
            // check single ip range
            if(ValidateIPaddress.isIPv4(range)){
                details.put(IP_Version, IP_V4);
                details.put(IP_TYPE, "Single");
            } else if(ValidateIPaddress.isIPv6(range)){
                details.put(IP_Version, IP_V6);
                details.put(IP_TYPE, "Single");
            } else {
                throw new IllegalArgumentException(range + " is not a valid IP");
            }
        }
        return details;
    }

    @Override
    public Principal getPrincipal() {
        if (userUUID != null) {
            userPrincipal = new org.forgerock.openam.authentication.modules.adaptive.AdaptivePrincipal(userUUID);
            return userPrincipal;
        }
        if (userName != null) {
            userPrincipal = new org.forgerock.openam.authentication.modules.adaptive.AdaptivePrincipal(userName);
            return userPrincipal;
        } else {
            return null;
        }
    }

    /**
     * Check to see if there have been any auth failures since last successful login.
     * This relies on the Auth Failure framework with Account Lockout enabled.
     *
     * Post_Auth_Class:  Should failure count be reset if successful?
     *
     * Returns authFailScore if AuthFailure
     *
     * @return score achieved with this test
     */
    protected int checkAuthFailure() {
        int retVal = 0;

        try {
            // Check if zero (or -1 if Account lockout is not enabled)
            if (0 >= getFailCount(amAuthIdentity)) {
                 retVal = authFailureScore;
            }
        } catch (AuthenticationException e) {
            debug.error(ADAPTIVE
                    + ".checkAuthFailure() : Failed to get fail count", e);
        }

        if (!authFailureInvert) {
            retVal = authFailureScore - retVal;
        }

        return retVal;
    }

    /**
     * Check to see if the IP address is within the ranges specified
     *
     * Range can be in the form of:
     * x.x.x.x/YY
     * or
     * x.x.x.x-y.y.y.y.
     * or
     * x:x:x:x:x:x:x:x/YY
     * or
     * x:x:x:x:x:x:x:x-y:y:y:y:y:y:y:y
     *
     * There can be multiple ranges passed in
     *
     * @return score achieved with this test
     */
    protected int checkIPRange() {
        int retVal = 0;
        String ipVersion;
        String ipType;
        Map<String, String> holdDetails;
        for (String nextIP : IPRangeRange) {

            try {
                holdDetails = checkIPVersion(nextIP);
            } catch (Exception e) {
                debug.message("IP type could not be validated." + nextIP);
                continue;
            }

            ipVersion = holdDetails.get(IP_Version);
            ipType = holdDetails.get(IP_TYPE);

            if(ipVersion.equalsIgnoreCase(IP_V6) && ValidateIPaddress.isIPv6(clientIP)){
                if (debug.messageEnabled()) {
                    debug.message(ADAPTIVE + ".checkIPRange: " + clientIP + " --> " + nextIP);
                    debug.message("IP version is: " + IP_V6);
                    debug.message("Client IP is: " + IPv6Address.fromString(clientIP));
                }
                if(ipType.equalsIgnoreCase("Range")){
                    // Do range IPv6
                    String first = holdDetails.get(IP_START);
                    String last = holdDetails.get(IP_END);
                    IPv6AddressRange iPv6AddressRange = IPv6AddressRange.fromFirstAndLast(
                            IPv6Address.fromString(first), IPv6Address.fromString(last));
                    if(iPv6AddressRange.contains(IPv6Address.fromString(clientIP))) {
                        retVal = IPRangeScore;
                    }
                } else if(ipType.equalsIgnoreCase("CIDR")){
                    // Subnet mask ip
                    IPv6Network iPv6Network = IPv6Network.fromString(nextIP);
                    if(iPv6Network.contains(IPv6Address.fromString(clientIP))){
                        retVal = IPRangeScore;
                    }
                } else {
                    // treat as single ip address
                    IPv6Address iPv6AddressNextIP = IPv6Address.fromString(nextIP);
                    if(iPv6AddressNextIP.compareTo(IPv6Address.fromString(clientIP)) == 0){
                        retVal = IPRangeScore;
                    }
                }
            } else if(ipVersion.equalsIgnoreCase(IP_V4) && ValidateIPaddress.isIPv4(clientIP)){ // treat as IPv4
                if (debug.messageEnabled()) {
                    debug.message(ADAPTIVE + ".checkIPRange: " + clientIP + " --> " + nextIP);
                    debug.message("IP version is: " + IP_V4);
                    debug.message("Client IP is: " + clientIP);
                }
                IPRange theRange = new IPRange(nextIP);
                if (theRange.inRange(clientIP)) {
                    retVal = IPRangeScore;
                }
            }
        }

        if (!IPRangeInvert) {
            retVal = IPRangeScore - retVal;
        }
        debug.message(ADAPTIVE + ".checkIPRange: returns " + retVal);
        return retVal;
    }

    /**
     * Check to see if the IP address being used is in the clients history
     * IPHistory is stored in a single attribute,  separated by "|"
     * If the client IP is new, (Not seen before) then add it to front of list,  and drop
     * from end of list.
     *
     * The PostAuthN Method will update the profile if needed.
     *
     * @return score achieved with this test
     */
    protected int checkIPHistory() {
        int retVal = 0;
        String ipHistoryValues = null;
        String newHistory = clientIP;
        int historyCount = 0;

        if (IPHistoryAttribute != null) {
            ipHistoryValues = getIdentityAttributeString(IPHistoryAttribute);

            debug.message(ADAPTIVE + ".checkIPHistory: Client IP = " + clientIP + " History IP = " + ipHistoryValues);

            if (ipHistoryValues != null) {
                StringTokenizer st = new StringTokenizer(ipHistoryValues, "|");
                while (st.hasMoreTokens()) {
                    String theIP = st.nextToken();
                    historyCount += 1;
                    if (historyCount < IPHistoryCount) {
                        newHistory += "|" + theIP;
                    }

                    if (clientIP.equals(theIP)) {
                        retVal = IPHistoryScore;
                    }
                }
            }
        }

        /*
         * retVal is 0, if there was no match with history
         */
        if (IPHistorySave && retVal == 0) {
            postAuthNMap.put("IPSAVE", newHistory);
            postAuthNMap.put("IPAttr", IPHistoryAttribute);
        }

        if (!IPHistoryInvert) {
            retVal = IPHistoryScore - retVal;
        }

        debug.message(ADAPTIVE + ".checkIPHistory: returns " + retVal);

        return retVal;
    }

    private String getCountryCode(DatabaseReader db, String ipAddress) throws IOException, GeoIp2Exception {
        try {
            return db.country(InetAddress.getByName(ipAddress)).getCountry().getIsoCode();
        } catch (UnknownHostException e) {
            return UNKNOWN_COUNTRY_CODE;
        }
    }

    protected int checkGeoLocation() throws IOException, GeoIp2Exception {
        int retVal = 0;
        String countryCode;

        debug.message("GeoLocation database location = " + geoLocationDatabase);

        DatabaseReader db = getLookupService(geoLocationDatabase);

        if (db == null) {
            debug.message("GeoLocation database lookup returns null");
        } else {
            countryCode = getCountryCode(db, clientIP);
            debug.message(ADAPTIVE + ".checkGeoLocation: " + clientIP + " returns " + countryCode);

            if (geoLocationValues != null) {
                StringTokenizer st = new StringTokenizer(geoLocationValues, "|");
                while (st.hasMoreTokens()) {

                    if (countryCode.equalsIgnoreCase(st.nextToken())) {
                        debug.message("Found Country Code : " + countryCode);
                        retVal = geoLocationScore;
                        break;
                    }
                }
            }
        }

        if (!geoLocationInvert) {
            retVal = geoLocationScore - retVal;
        }
        debug.message(ADAPTIVE + ".checkGeoLocation: returns " + retVal);
        return retVal;
    }

    /**
     * Check to see if the client has a cookie with optional value
     *
     * @return score achieved with this test
     */
    protected int checkKnownCookie() {
        int retVal = 0;
        debug.message(ADAPTIVE + ".checkKnownCookie: ");

        HttpServletRequest req = getHttpServletRequest();
        if (req != null) {
            Cookie cookie = CookieUtils.getCookieFromReq(req, knownCookieName);
            if (cookie != null) {
                if (knownCookieValue.equalsIgnoreCase(CookieUtils.getCookieValue(cookie))) {
                    retVal = knownCookieScore;
                }
            }
        }

        if (knownCookieValue == null) {
            knownCookieValue = "1";
        }
        if (knownCookieSave) {
            postAuthNMap.put("COOKIENAME", knownCookieName);
            postAuthNMap.put("COOKIEVALUE", knownCookieValue);
        }

        if (!knownCookieInvert) {
            retVal = knownCookieScore - retVal;
        }
        debug.message(ADAPTIVE + ".checkKnownCookie: returns " + retVal);
        return retVal;
    }

    /**
     * Check to see if the client has a cookie with optional value
     *
     * @return score achieved with this test
     */
    protected int checkRequestHeader() {
        int retVal = 0;
        debug.message(ADAPTIVE + ".checkRequestHeader: ");

        HttpServletRequest req = getHttpServletRequest();
        if (req != null) {
            Enumeration<String> eHdrs = req.getHeaderNames();
            while (eHdrs.hasMoreElements()) {
                String header = eHdrs.nextElement();
                if (reqHeaderName.equalsIgnoreCase(header)) {
                    debug.message(ADAPTIVE + ".checkRequestHeader: Found header: " + header);
                    if (reqHeaderValue != null) {
                        Enumeration<String> eVals = req.getHeaders(header);
                        while (eVals.hasMoreElements()) {
                            String val = eVals.nextElement();
                            if (reqHeaderValue.equalsIgnoreCase(val)) {
                                debug.message(ADAPTIVE + ".checkRequestHeader: Found header Value: " + val);
                                retVal = reqHeaderScore;
                            }
                        }
                    } else {
                        retVal = reqHeaderScore;
                    }
                    break;
                }
            }
        }

        if (!reqHeaderInvert) {
            retVal = reqHeaderScore - retVal;
        }
        debug.message(ADAPTIVE + ".checkRequestHeader: returns " + retVal);
        return retVal;
    }

    /**
     * Check to see if the client has a cookie with optional value
     *
     * @return score achieved with this test
     */
    protected int checkRegisteredClient() {
        int retVal = 0;
        String deviceHash = null;

        debug.message(ADAPTIVE + ".checkRegisteredClient: ");
        HttpServletRequest req = getHttpServletRequest();

        if (req != null) {
            StringBuilder sb = new StringBuilder(150);
            sb.append(req.getHeader("User-Agent"));
            sb.append("|").append(req.getHeader("accept"));
            sb.append("|").append(req.getHeader("accept-language"));
            sb.append("|").append(req.getHeader("accept-encoding"));
            sb.append("|").append(req.getHeader("accept-charset"));
            sb.append("|").append(userName);

            deviceHash = AccessController.doPrivileged(new EncodeAction(Hash.hash(sb.toString())));

            Cookie cookie = CookieUtils.getCookieFromReq(req, deviceCookieName);
            if (cookie != null ) {
                if (debug.messageEnabled()) {
                    debug.message(ADAPTIVE + ".checkRegisteredClient: Found Cookie :" + deviceCookieName);
                }
                if (deviceHash.equalsIgnoreCase(CookieUtils.getCookieValue(cookie))) {
                    retVal = deviceCookieScore;
                }
            }
        }

        if (deviceCookieSave) {
            postAuthNMap.put("DEVICENAME", deviceCookieName);
            postAuthNMap.put("DEVICEVALUE", deviceHash);
        }

        if (!deviceCookieInvert) {
            retVal = deviceCookieScore - retVal;
        }
        debug.message(ADAPTIVE + ".checkRegisteredClient: returns " + retVal);
        return retVal;
    }

    /**
     * Check to see if current time is within range
     *
     * @return score achieved with this test
     */
    protected int checkTimeDay() {
        //TODO
        return 0;
    }

    /**
     * Check to see if the last login is within the allowed range
     * Last login is stored in a cookie in encrypted format
     *
     * @return score achieved with this test
     */
    protected int checkLastLogin() {
        DateFormat formatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        Date now = new Date();
        Date loginTime = null;
        String lastLoginEnc = null;
        String lastLogin = null;
        String savedUserName = null;
        int retVal = 0;

        if (timeSinceLastLoginAttribute != null) {
            HttpServletRequest req = getHttpServletRequest();

            if (req != null) {
                Cookie cookie = CookieUtils.getCookieFromReq(req, timeSinceLastLoginAttribute);
                if (cookie != null) {
                    if (debug.messageEnabled()) {
                        debug.message(ADAPTIVE + ".checkLastLogin: Found Cookie :" + timeSinceLastLoginAttribute);
                    }
                    lastLoginEnc = CookieUtils.getCookieValue(cookie);
                    lastLogin = AccessController.doPrivileged(new DecodeAction(lastLoginEnc));
                }
                if (lastLogin != null) {
                    String[] tokens = lastLogin.split("\\|");
                    if (tokens.length == 3) {
                        lastLogin = tokens[1];
                        savedUserName = tokens[2];
                    }

                    if (!userName.equalsIgnoreCase(savedUserName)) {
                        lastLogin = null;
                    }

                    if (lastLogin != null) {
                        try {
                            loginTime = formatter.parse(lastLogin); // "2002.01.29.08.36.33");
                        } catch (ParseException pe) {
                        }
                        if ((now.getTime() - loginTime.getTime()) < timeSinceLastLoginValue * 1000 * 60 * 60 * 24L) {
                            retVal = timeSinceLastLoginScore;
                        }
                    }
                }
            }
            if (timeSinceLastLoginSave) {
                postAuthNMap.put("LOGINNAME", timeSinceLastLoginAttribute);
                lastLogin = formatter.format(now);
                lastLogin = Math.random() + "|" + lastLogin + "|" + userName;
                lastLoginEnc = AccessController.doPrivileged(new EncodeAction(lastLogin));
                postAuthNMap.put("LOGINVALUE", lastLoginEnc);
            }
        }

        if (!timeSinceLastLoginInvert) {
            retVal = timeSinceLastLoginScore - retVal;
        }

        debug.message(ADAPTIVE + ".checkLastLogin: returns " + retVal);
        return retVal;
    }

    /**
     * Check to see if the user profile has a risk attribute with value
     *
     * @return score achieved with this test
     */
    protected int checkRiskAttribute() {
        int retVal = 0;
        debug.message(ADAPTIVE + ".checkRiskAttribute: ");

        if (riskAttributeName != null && riskAttributeValue != null) {
            Set<String> riskAttributeValues = null;

            riskAttributeValues = getIdentityAttributeSet(riskAttributeName);

            if (riskAttributeValues != null) {
                for (String riskAttr : riskAttributeValues) {
                    if (riskAttributeValue.equalsIgnoreCase(riskAttr)) {
                        debug.message(ADAPTIVE + ".checkRiskAttribute: Found Match");
                        retVal = riskAttributeScore;
                        break;
                    }
                }
            }
        }

        if (!riskAttributeInvert) {
            retVal = riskAttributeScore - retVal;
        }
        debug.message(ADAPTIVE + ".checkRiskAttribute: returns " + retVal);

        return retVal;
    }

    private Set<String> getIdentityAttributeSet(String attr) {
        Set<String> retVal = null;

        try {
            retVal = amAuthIdentity.getAttribute(attr);
        } catch (Exception e) {
            debug.message(ADAPTIVE + ".getIdentityAttributeSet Attribute: " + attr, e);
        }
        return retVal;
    }

    private String getIdentityAttributeString(String attr) {
        Set<String> theSet = null;
        String retVal = null;

        try {
            theSet = amAuthIdentity.getAttribute(attr);
            if (theSet.size() > 0) {
                retVal = theSet.iterator().next();
            }
        } catch (Exception e) {
            debug.message(ADAPTIVE + ".getIdentityAttributeString Attribute: " + attr, e);
        }
        return retVal;

    }

    private AMIdentity getIdentity(String uName) {
        AMIdentity theID = null;
        AMIdentityRepository amIdRepo = getAMIdentityRepository(getRequestOrg());

        IdSearchControl idsc = new IdSearchControl();
        idsc.setRecursive(true);
        idsc.setAllReturnAttributes(true);
        // search for the identity
        Set<AMIdentity> results = Collections.EMPTY_SET;
        try {
            idsc.setMaxResults(0);
            IdSearchResults searchResults =
                    amIdRepo.searchIdentities(IdType.USER, uName, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }

            if (results.isEmpty()) {
                throw new IdRepoException(ADAPTIVE + ".getIdentity : User "
                        + uName + " is not found");
            } else if (results.size() > 1) {
                throw new IdRepoException(ADAPTIVE
                        + ".getIdentity : More than one user found for the userName "
                        + userName);
            }

            theID = results.iterator().next();
        } catch (IdRepoException e) {
            debug.error(ADAPTIVE + ".getIdentity : " + "error searching Identities with username : " + userName, e);
        } catch (SSOException e) {
            debug.error(ADAPTIVE + ".getIdentity : " + "AuthAdaptive module exception : ", e);
        }
        return theID;
    }

    public void onLoginSuccess(Map requestParamsMap, HttpServletRequest request,
                               HttpServletResponse response, SSOToken token)
            throws AuthenticationException {
        Map<String, String> m = new HashMap<String, String>();
        Map<String, Set> attrMap = new HashMap<String, Set>();

        debug.message(ADAPTIVE + " executing PostProcessClass");
        try {
            String s = token.getProperty("ADAPTIVE");
            if (s != null && !s.isEmpty()) {
                stringToMap(s, m);
                token.setProperty("ADAPTIVE", "");
            }
            if (m.containsKey("IPSAVE")) {
                String value = m.get("IPSAVE");
                String name = m.get("IPAttr");
                Set<String> vals = new HashSet<String>();
                vals.add(value);

                attrMap.put(name, vals);
            }

            // Now we save the attribs,  since we can do it in one shot
            if (!attrMap.isEmpty()) {
                try {
                    AMIdentity id = IdUtils.getIdentity(
                            AccessController.doPrivileged(AdminTokenAction.getInstance()),
                            token.getProperty(Constants.UNIVERSAL_IDENTIFIER));
                    id.setAttributes(attrMap);
                    id.store();
                } catch (Exception e) {
                    debug.error(ADAPTIVE + ".getIdentity : " + "Unable to save Attribute : ", e);
                }

            }
            int autoLoginExpire = (60 * 60 * 24) * 365; // 1 year, configurable?
            //Cookie domain?
            if (m.containsKey("LOGINNAME")) {
                String value = m.get("LOGINVALUE");
                String name = m.get("LOGINNAME");

                addCookieToResponse(response, name, value, autoLoginExpire);
            }
            if (m.containsKey("COOKIENAME")) {
                String name = m.get("COOKIENAME");
                String value = m.get("COOKIEVALUE");

                addCookieToResponse(response, name, value, autoLoginExpire);
            }
            if (m.containsKey("DEVICENAME")) {
                String name = m.get("DEVICENAME");
                String value = m.get("DEVICEVALUE");

                addCookieToResponse(response, name, value, autoLoginExpire);
            }
        } catch (Exception e) {
            debug.message(ADAPTIVE + " Unable to Retrieve PostAuthN Params", e);
        }
    }

    private void addCookieToResponse(HttpServletResponse response, String name, String value, int expire) {
        Set<String> domains = AuthUtils.getCookieDomains();
        for (String domain : domains) {
            CookieUtils.addCookieToResponse(response, CookieUtils.newCookie(name, value, expire, "/", domain));
        }
    }

    public void onLoginFailure(Map requestParamsMap, HttpServletRequest request,
                               HttpServletResponse response) throws AuthenticationException {
    }

    public void onLogout(HttpServletRequest request,
                         HttpServletResponse response, SSOToken token)
            throws AuthenticationException {
    }

    /**
     * This builds a map, of the data needed by the PostAuth Class, serializes it,  and then adds it to the Session
     */
    protected void setPostAuthNParams() {
        try {
            String s = mapToString(postAuthNMap);
            if (!s.isEmpty()) {
                setUserSessionProperty("ADAPTIVE", s);
            }

        } catch (Exception e) {
            debug.message(ADAPTIVE + " Unable to Set PostAuthN Params" + e);
        }
    }

    private void initParams(Map options) {
        try {
            adaptiveThreshold = getOptionAsInteger(options, ADAPTIVETHRESHOLD);

            authFailureCheck = getOptionAsBoolean(options, AUTH_FAILURE_CHECK);
            authFailureScore = getOptionAsInteger(options, AUTH_FAILURE_SCORE);
            authFailureInvert = getOptionAsBoolean(options, AUTH_FAILURE_INVERT);

            IPRangeCheck = getOptionAsBoolean(options, IP_RANGE_CHECK);
            IPRangeRange = (Set<String>) options.get(IP_RANGE_RANGE);
            IPRangeScore = getOptionAsInteger(options, IP_RANGE_SCORE);
            IPRangeInvert = getOptionAsBoolean(options, IP_RANGE_INVERT);

            IPHistoryCheck = getOptionAsBoolean(options, IP_HISTORY_CHECK);
            IPHistoryCount = getOptionAsInteger(options, IP_HISTORY_COUNT);
            IPHistoryAttribute = CollectionHelper.getMapAttr(options, IP_HISTORY_ATTRIBUTE);
            IPHistorySave = getOptionAsBoolean(options, IP_HISTORY_SAVE);
            IPHistoryScore = getOptionAsInteger(options, IP_HISTORY_SCORE);
            IPHistoryInvert = getOptionAsBoolean(options, IP_HISTORY_INVERT);

            knownCookieCheck = getOptionAsBoolean(options, KNOWN_COOKIE_CHECK);
            knownCookieName = CollectionHelper.getMapAttr(options, KNOWN_COOKIE_NAME);
            knownCookieValue = CollectionHelper.getMapAttr(options, KNOWN_COOKIE_VALUE);
            knownCookieSave = getOptionAsBoolean(options, KNOWN_COOKIE_SAVE);
            knownCookieScore = getOptionAsInteger(options, KNOWN_COOKIE_SCORE);
            knownCookieInvert = getOptionAsBoolean(options, KNOWN_COOKIE_INVERT);

            deviceCookieCheck = getOptionAsBoolean(options, DEVICE_COOKIE_CHECK);
            deviceCookieName = CollectionHelper.getMapAttr(options, DEVICE_COOKIE_NAME);
            deviceCookieSave = getOptionAsBoolean(options, DEVICE_COOKIE_SAVE);
            deviceCookieScore = getOptionAsInteger(options, DEVICE_COOKIE_SCORE);
            deviceCookieInvert = getOptionAsBoolean(options, DEVICE_COOKIE_INVERT);

            //TODO add these to service XML
            timeOfDayCheck = getOptionAsBoolean(options, TIME_OF_DAY_CHECK);
            timeOfDayRange = CollectionHelper.getMapAttr(options, TIME_OF_DAY_RANGE);
            timeOfDayInvert = getOptionAsBoolean(options, TIME_OF_DAY_INVERT);

            timeSinceLastLoginCheck = getOptionAsBoolean(options, TIME_SINCE_LAST_LOGIN_CHECK);
            timeSinceLastLoginAttribute = CollectionHelper.getMapAttr(options, TIME_SINCE_LAST_LOGIN_ATTRIBUTE);
            timeSinceLastLoginValue = getOptionAsInteger(options, TIME_SINCE_LAST_LOGIN_VALUE);
            timeSinceLastLoginSave = getOptionAsBoolean(options, TIME_SINCE_LAST_LOGIN_SAVE);
            timeSinceLastLoginScore = getOptionAsInteger(options, TIME_SINCE_LAST_LOGIN_SCORE);
            timeSinceLastLoginInvert = getOptionAsBoolean(options, TIME_SINCE_LAST_LOGIN_INVERT);

            riskAttributeCheck = getOptionAsBoolean(options, RISK_ATTRIBUTE_CHECK);
            riskAttributeName = CollectionHelper.getMapAttr(options, RISK_ATTRIBUTE_NAME);
            riskAttributeValue = CollectionHelper.getMapAttr(options, RISK_ATTRIBUTE_VALUE);
            riskAttributeScore = getOptionAsInteger(options, RISK_ATTRIBUTE_SCORE);
            riskAttributeInvert = getOptionAsBoolean(options, RISK_ATTRIBUTE_INVERT);

            geoLocationCheck = getOptionAsBoolean(options, GEO_LOCATION_CHECK);
            geoLocationDatabase = CollectionHelper.getMapAttr(options, GEO_LOCATION_DATABASE);
            geoLocationValues = CollectionHelper.getMapAttr(options, GEO_LOCATION_VALUES);
            geoLocationScore = getOptionAsInteger(options, GEO_LOCATION_SCORE);
            geoLocationInvert = getOptionAsBoolean(options, GEO_LOCATION_INVERT);

            reqHeaderCheck = getOptionAsBoolean(options, REQ_HEADER_CHECK);
            reqHeaderName = CollectionHelper.getMapAttr(options, REQ_HEADER_NAME);
            reqHeaderValue = CollectionHelper.getMapAttr(options, REQ_HEADER_VALUE);
            reqHeaderScore = getOptionAsInteger(options, REQ_HEADER_SCORE);
            reqHeaderInvert = getOptionAsBoolean(options, REQ_HEADER_INVERT);

        } catch (Exception e) {
            debug.error(ADAPTIVE + ".initParams : " + "Unable to initialize parameters : ", e);
        } finally {
            if (debug.messageEnabled()) {
                debug.message("Adaptive Threshold-> " + adaptiveThreshold

                        + "\nAuth Failure Check-> " + authFailureCheck
                        + "\nAuth Failure Score-> " + authFailureScore
                        + "\nAuth Failure Invert-> " + authFailureInvert

                        + "\nIP Range Check-> " + IPRangeCheck
                        + "\nIP Range Range-> " + IPRangeRange
                        + "\nIP Range Score-> " + IPRangeScore
                        + "\nIP Range Invert-> " + IPRangeInvert

                        + "\nIP History Check-> " + IPHistoryCheck
                        + "\nIP History Count-> " + IPHistoryCount
                        + "\nIP History Attribute-> " + IPHistoryAttribute
                        + "\nIP History Save-> " + IPHistorySave
                        + "\nIP History Score-> " + IPHistoryScore
                        + "\nIP History Invert-> " + IPHistoryInvert

                        + "\nKnown Cookie Check-> " + knownCookieCheck
                        + "\nKnown Cookie Name-> " + knownCookieName
                        + "\nKnown Cookie Value-> " + knownCookieValue
                        + "\nKnown Cookie Save-> " + knownCookieSave
                        + "\nKnown Cookie Score-> " + knownCookieScore
                        + "\nKnown Cookie Invert-> " + knownCookieInvert

                        + "\nDevice Cookie Check-> " + deviceCookieCheck
                        + "\nDevice Cookie Name-> " + deviceCookieName
                        + "\nDevice Cookie Save-> " + deviceCookieSave
                        + "\nDevice Cookie Score-> " + deviceCookieScore
                        + "\nDevice Cookie Invert-> " + deviceCookieInvert

                        + "\nTime Of Day Check-> " + timeOfDayCheck
                        + "\nTime Of Day Range-> " + timeOfDayRange
                        + "\nTime Of Day Invert-> " + timeOfDayInvert

                        + "\nTime Since Last Login Check-> " + timeSinceLastLoginCheck
                        + "\nTime Since Last Login Attribute: " + timeSinceLastLoginAttribute
                        + "\nTime Since Last Login Value-> " + timeSinceLastLoginValue
                        + "\nTime Since Last Login Save-> " + timeSinceLastLoginSave
                        + "\nTime Since Last Login Score-> " + timeSinceLastLoginScore
                        + "\nTime Since Last Login Invert-> " + timeSinceLastLoginInvert

                        + "\nRisk Attribute Check-> " + riskAttributeCheck
                        + "\nRisk Attribute Name-> " + riskAttributeName
                        + "\nRisk Attribute Value-> " + riskAttributeValue
                        + "\nRisk Attribute Score-> " + riskAttributeScore
                        + "\nRisk Attribute Invert-> " + riskAttributeInvert

                        + "\nGeoLocation Check-> " + geoLocationCheck
                        + "\nGeoLocation Database-> " + geoLocationDatabase
                        + "\nGeoLocation Values-> " + geoLocationValues
                        + "\nGeoLocation Score-> " + geoLocationScore
                        + "\nGeoLocation Invert-> " + geoLocationInvert

                        + "\nReq Header Check-> " + reqHeaderCheck
                        + "\nReq Header Name-> " + reqHeaderName
                        + "\nReq Header Value-> " + reqHeaderValue
                        + "\nReq Header Score-> " + reqHeaderScore
                        + "\nReq Header Invert-> " + reqHeaderInvert);
            }
        }
    }

    protected boolean getOptionAsBoolean(Map m, String i) {
        String s = null;
        s = CollectionHelper.getMapAttr(m, i);
        return Boolean.parseBoolean(s);
    }

    protected int getOptionAsInteger(Map m, String i) {
        String s = null;
        int retVal = 0;

        s = CollectionHelper.getMapAttr(m, i);
        if (s != null) {
            retVal = Integer.parseInt(s);
        }
        return retVal;
    }

    // cleanup state fields
    @Override
    public void destroyModuleState() {
        userUUID = null;
        userName = null;
        userPrincipal = null;
    }

    @Override
    public void nullifyUsedVars() {
        postAuthNMap = null;

        amAuthIdentity = null;
        clientIP = null;
        adaptiveThreshold = 1;

        authFailureCheck = false;
        authFailureScore = 1;
        authFailureInvert = false;

        IPRangeCheck = false;
        IPRangeRange = null;
        IPRangeScore = 1;
        IPRangeInvert = false;

        IPHistoryCheck = false;
        IPHistoryCount = 0;
        IPHistoryAttribute = null;
        IPHistorySave = false;
        IPHistoryScore = 1;
        IPHistoryInvert = false;

        knownCookieCheck = false;
        knownCookieName = null;
        knownCookieValue = null;
        knownCookieSave = false;
        knownCookieScore = 1;
        knownCookieInvert = false;

        deviceCookieCheck = false;
        deviceCookieName = null;
        deviceCookieSave = false;
        deviceCookieScore = 1;
        deviceCookieInvert = false;

        timeOfDayCheck = false;
        timeOfDayRange = null;
        timeOfDayInvert = false;

        timeSinceLastLoginCheck = false;
        timeSinceLastLoginAttribute = null;
        timeSinceLastLoginValue = 0;
        timeSinceLastLoginSave = false;
        timeSinceLastLoginScore = 1;
        timeSinceLastLoginInvert = false;

        riskAttributeCheck = false;
        riskAttributeName = null;
        riskAttributeValue = null;
        riskAttributeScore = 1;
        riskAttributeInvert = false;

        geoLocationCheck = false;
        geoLocationDatabase = null;
        geoLocationValues = null;
        geoLocationScore = 1;
        geoLocationInvert = false;

        reqHeaderCheck = false;
        reqHeaderName = null;
        reqHeaderValue = null;
        reqHeaderScore = 1;
        reqHeaderInvert = false;

    }

    public static String mapToString(Map<String, String> map) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (stringBuilder.length() > 0) {
                stringBuilder.append("&");
            }
            try {
                stringBuilder.append((key != null ? URLEncoder.encode(key, "UTF-8") : ""));
                stringBuilder.append("=");
                stringBuilder.append(value != null ? URLEncoder.encode(value, "UTF-8") : "");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("This method requires UTF-8 encoding support", e);
            }
        }

        return stringBuilder.toString();
    }

    public static void stringToMap(String input, Map<String, String> map) {
        String[] nameValuePairs = input.split("&");
        for (String nameValuePair : nameValuePairs) {
            String[] nameValue = nameValuePair.split("=");
            try {
                map.put(URLDecoder.decode(nameValue[0], "UTF-8"), nameValue.length > 1 ? URLDecoder.decode(
                        nameValue[1], "UTF-8") : "");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("This method requires UTF-8 encoding support", e);
            }
        }
    }

    private static synchronized DatabaseReader getLookupService(String dbLocation) {
        try {
            if (lookupService == null) {
                lookupService = new DatabaseReader.Builder(new File(dbLocation)).build();
            }
        } catch (IOException ioe) {
            //don't log the stacktrace, since it will occur on any module invocation
            debug.message(ADAPTIVE + "Unable to initialize GeoDB service" + ioe.getMessage());
        }
        return lookupService;
    }
}
