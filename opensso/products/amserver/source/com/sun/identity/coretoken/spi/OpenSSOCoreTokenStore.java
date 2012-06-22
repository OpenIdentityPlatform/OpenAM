/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * If applicable, addReferral the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: OpenSSOCoreTokenStore.java,v 1.1 2009/11/19 00:07:41 qcheng Exp $
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.coretoken.spi;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TimerPool;
import com.sun.identity.coretoken.CoreTokenConstants;
import com.sun.identity.coretoken.CoreTokenUtils;
import com.sun.identity.coretoken.CoreTokenException;
import com.sun.identity.coretoken.service.TokenCleanupRunnable;
import com.sun.identity.coretoken.service.CoreTokenConfigService;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import javax.security.auth.Subject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This <code>OpenSSOCoreTokenStore</code> implements the core token store
 * using SM store.
 */
public class OpenSSOCoreTokenStore implements CoreTokenStore {
    public static String JSON_ATTR = "tokenattr";
    public static String SEARCHABLE_ATTR = "searchable";
    public static String ETAG_ATTR = "etag";
    public static final String SERVICE_DN =
        "ou=Tokens,ou=default,ou=GlobalConfig,ou=1.0,ou=" +
        CoreTokenConstants.CORE_TOKEN_STORE_SERVICE_NAME + ",ou=services,"  +
        SMSEntry.getRootSuffix();
    private static final String CONNECTOR = ".";
    // searchable attributes set
    private static Set<String> searchableAttrs =
        CoreTokenConfigService.searchableAttrs;
    // internal searchable attribute set
    private static Set<String> internalSearchableAttrs = new HashSet<String>();
    private static Set<String> internalTokenAttrs = new HashSet<String>();

    static {
        internalSearchableAttrs.add(CoreTokenConstants.TOKEN_TYPE);
        internalSearchableAttrs.add(CoreTokenConstants.TOKEN_SUBJECT);
        internalTokenAttrs.add(CoreTokenConstants.TOKEN_EXPIRY);
        internalTokenAttrs.add(CoreTokenConstants.TOKEN_ID);
        internalTokenAttrs.add(CoreTokenConstants.TOKEN_SUBJECT);
        internalTokenAttrs.add(CoreTokenConstants.TOKEN_TYPE);
        internalTokenAttrs.add(CoreTokenConstants.VERSION_TAG);

        // start cleanup thread
        long interval = CoreTokenConfigService.cleanupInt;
        TokenCleanupRunnable tcRun = new TokenCleanupRunnable(interval);
        TimerPool timerPool = SystemTimerPool.getTimerPool();
        timerPool.schedule(tcRun, new Date(((System.currentTimeMillis()
            + interval) / 1000) * 1000));
    }

    /**
     * 
     * @param subject
     * @param attributes
     * @return the created token in JSON format
     * @throws CoreTokenException
     * @throws JSONException
     */
    public String createToken(Subject subject,
        JSONObject attributes)
        throws CoreTokenException, JSONException {
        SSOToken adminToken = SubjectUtils.getSSOToken(subject);

        if (adminToken == null) {
            throw new CoreTokenException(212, null, 401);
        }

        String tokenId = null;
        try {
            // validate attribute names and convert to lower case
            attributes = validateAndToLowerCase(attributes);
            if (attributes.has(CoreTokenConstants.TOKEN_ID)) {
                throw new CoreTokenException(201, null, 409);
            }
            tokenId = UUID.randomUUID().toString();
            String dn = getCoreTokenDN(tokenId);
            SMSEntry s = new SMSEntry(adminToken, dn);

            Map<String, Set<String>> map = 
                validateAndCreateMap(tokenId, attributes);

            s.setAttributes(map);
            s.save();

            JSONObject json = new JSONObject();
            JSONArray jArray = new JSONArray();
            jArray.put(tokenId);
            json.put(CoreTokenConstants.TOKEN_ID, jArray);
            return json.toString();
        } catch (SSOException e) {
            CoreTokenUtils.debug.error("OpenSSOTokenStore.createToken", e);
            throw new CoreTokenException(202, null, e);
        } catch (SMSException e) {
            CoreTokenUtils.debug.error("OpenSSOTokenStore.createToken", e);
            throw new CoreTokenException(202, null, e);
        }
    }

    /**
     *
     * @param adminSubject
     * @param tokenId
     * @return token value from SM with the given tokenId
     * @throws CoreTokenException
     */
    public String readToken(Subject adminSubject, String tokenId)
        throws CoreTokenException {
        SSOToken adminToken = SubjectUtils.getSSOToken(adminSubject);

        if (adminToken == null) {
            throw new CoreTokenException(209, null, 401);
        }

        String dn = getCoreTokenDN(tokenId);

        if (!SMSEntry.checkIfEntryExists(dn, adminToken)) {
            throw new CoreTokenException(203, null, 404);
        }
        try {
            SMSEntry s = new SMSEntry(adminToken, dn);
            return getTokenAttributeValueFromSM(s, JSON_ATTR);
        } catch (SSOException ex) {
            CoreTokenUtils.debug.error("OpenSSOCoreTokenStore.read", ex);
            throw new CoreTokenException(204, null, ex);
        } catch (SMSException ex) {
            CoreTokenUtils.debug.error("OpenSSOCoreTokenStore.read", ex);
            throw new CoreTokenException(204, null, ex);
        }
    }

    /**
     *
     * @param subject
     * @param tokenId
     * @throws CoreTokenException
     * @throws JSONException
     */
    public void deleteToken(Subject subject, String tokenId)
        throws CoreTokenException {
        SSOToken adminToken = SubjectUtils.getSSOToken(subject);
        String dn = getCoreTokenDN(tokenId);

        if (adminToken == null) {
            throw new CoreTokenException(211, null, 401);
        }

        if (!SMSEntry.checkIfEntryExists(dn, adminToken)) {
            throw new CoreTokenException(203, null, 404);
        }

        try {
            SMSEntry s = new SMSEntry(adminToken, dn);
            s.delete();
        } catch (SSOException ex) {
            CoreTokenUtils.debug.error("OpenSSOCoreTokenStore.deleteToken", ex);
            throw new CoreTokenException(205, null, ex);
        } catch (SMSException ex) {
            CoreTokenUtils.debug.error("OpenSSOCoreTokenStore.deleteToken", ex);
            throw new CoreTokenException(205, null, ex);
        } 
    }

    /**
     * 
     * @param subject
     * @param queryString
     * @return JSON array of tokens matching the queryString
     * @throws CoreTokenException
     */
    public JSONArray searchTokens (Subject subject,
        String queryString) throws CoreTokenException {

        try {
            SSOToken token = SubjectUtils.getSSOToken(subject);

            if (token == null) {
                throw new CoreTokenException(216, null, 401);
            }

            JSONArray results = new JSONArray();
            if (SMSEntry.checkIfEntryExists(SERVICE_DN, token)) {
                String filter = createSearchFilter(queryString);
                Set<String> dns = SMSEntry.search(token, SERVICE_DN, filter,
                    0, 0, false, false);
                for (String dn : dns) {
                    if (!CoreTokenUtils.areDNIdentical(SERVICE_DN, dn)) {
                        String rdns[] = LDAPDN.explodeDN(dn, true);
                        if ((rdns != null) && rdns.length > 0) {
                            results.put(rdns[0]);
                        }
                    }
                }
            }
            return results;
        } catch (SMSException ex) {
            CoreTokenUtils.debug.error("OpenSSOCoreTokenStore.searchToken", ex);
            throw new CoreTokenException(215, ex);
        }
    }

    /**
     * Updates a token.
     * @param subject caller subject.
     * @param tokenId token.id of the token to be updated.
     * @param eTag
     * @param newVals
     * @throws CoreTokenException
     * @throws JSONException
     */
    public void updateToken(Subject subject, String tokenId,
        String eTag, JSONObject newVals)
        throws CoreTokenException, JSONException {
        SSOToken token = SubjectUtils.getSSOToken(subject);

        if (token == null) {
            throw new CoreTokenException(210, null, 401);
        }

        String dn = null;
        try {
            dn = getCoreTokenDN(tokenId);

            if (SMSEntry.checkIfEntryExists(dn, token)) {
                SMSEntry s = new SMSEntry(token, dn);
                String tokenAttrs = getTokenAttributeValueFromSM(s, JSON_ATTR);
                JSONObject json = new JSONObject(tokenAttrs);
                checkETag(eTag, json, tokenId);

                // validate attribute names and convert to lower case
                newVals = validateAndToLowerCase(newVals);

                // token.id attribute can't be modified
                if (newVals.has(CoreTokenConstants.TOKEN_ID)) {
                    throw new CoreTokenException(221, null, 409);
                }

                // token.type attribute can't be modified
                if (newVals.has(CoreTokenConstants.TOKEN_TYPE)) {
                    throw new CoreTokenException(224, null, 409);
                }

                json = updateAttributeValues(json, newVals);
                Map<String, Set<String>> map = 
                    validateAndCreateMap(tokenId, json);

                s.setAttributes(map);
                s.save();
            } else {
                throw new CoreTokenException(203, null, 404);
            }
        } catch (SMSException e) {
            CoreTokenUtils.debug.error("OpenSSOCoreTokenStore.updateToken", e);
            throw new CoreTokenException(206, null, e);
        } catch (SSOException e) {
            CoreTokenUtils.debug.error("OpenSSOCoreTokenStore.updateToken", e);
            throw new CoreTokenException(301, null, e);
        }
    }

    /**
     * Validates token attribute name, it should not start with "token.".
     * Also convert all attribute name to lower case and return.
     * @param jObj
     * @return JSONObject with all attribute name in lower case.
     * @throws CoreTokenException
     * @throws JSONException
     */
    private JSONObject validateAndToLowerCase(JSONObject jObj)
        throws CoreTokenException, JSONException {
        if (jObj == null) {
            return null;
        }
        // TODO : check attribute name to be alphabetic, numerical
        JSONObject retObj = new JSONObject();
        Iterator<String> it = jObj.keys();
        while (it.hasNext()) {
            String key = it.next();
            String lcKey = key.toLowerCase();
            int pos = lcKey.indexOf(CONNECTOR);
            if (pos <= 0) {
                String[] args = new String[] {key};
                throw new CoreTokenException(227, args, 400);
            }
            if (!internalTokenAttrs.contains(lcKey) &&
                lcKey.startsWith("token.")) {
                String[] args = new String[] {key};
                throw new CoreTokenException(225, args, 400);
            } else {
                retObj.put(lcKey, jObj.getJSONArray(key));
            }
        }
        return retObj;
    }

    private Map<String, Set<String>> validateAndCreateMap(
        String tokenId,  JSONObject jsonAttr)
        throws JSONException, CoreTokenException {

        String tokenExpiry = null;
        if (jsonAttr.has(CoreTokenConstants.TOKEN_EXPIRY)) {
            tokenExpiry = getSingleStringValue(jsonAttr,
                CoreTokenConstants.TOKEN_EXPIRY);
        }
        // check token.expiry if exist
        if ((tokenExpiry != null) && (tokenExpiry.length() != 0)
            && CoreTokenUtils.isTokenExpired(tokenExpiry)) {
                String[] args = new String[]{tokenExpiry};
                throw new CoreTokenException(11, args, 400);
        }

        // token.type must present and must be single-valued attribute
        String tokenType = getSingleStringValue(jsonAttr,
                CoreTokenConstants.TOKEN_TYPE);

        // toke.subject could be an array and must be present
        JSONArray tokenSubject = null;
        if (jsonAttr.has(CoreTokenConstants.TOKEN_SUBJECT)) {
            tokenSubject =
                jsonAttr.getJSONArray(CoreTokenConstants.TOKEN_SUBJECT);
        } else {
            String[] args = new String[]{CoreTokenConstants.TOKEN_SUBJECT};
            throw new CoreTokenException(217, args, 400);
        }

        return getSMSAttributeMap(tokenId, tokenSubject, tokenType,
                tokenExpiry, jsonAttr);
    }

    private Map<String, Set<String>> getSMSAttributeMap(String tokenId,
        JSONArray tokenSubject, String tokenType,
        String tokenExpiry, JSONObject jsonAttr) throws JSONException {
        Map<String, Set<String>> attrMap =
            getSearchableAttribute(tokenSubject, tokenType, jsonAttr);
        // add special attributes
        JSONArray jArray = new JSONArray();
        jArray.put(tokenId);
        jsonAttr.put(CoreTokenConstants.TOKEN_ID, jArray);
        jsonAttr.put(CoreTokenConstants.TOKEN_SUBJECT, tokenSubject);
        jArray = new JSONArray();
        jArray.put(tokenType);
        jsonAttr.put(CoreTokenConstants.TOKEN_TYPE, jArray);
        jArray = new JSONArray();
        jArray.put(tokenExpiry);
        jsonAttr.put(CoreTokenConstants.TOKEN_EXPIRY, jArray);
        jArray = new JSONArray();
        // ETag must in quota
        jArray.put("\"" + UUID.randomUUID().toString() + "\"");
        jsonAttr.put(CoreTokenConstants.VERSION_TAG, jArray);
        Set<String> set = new HashSet<String>();
        set.add(JSON_ATTR + "=" + jsonAttr.toString());
        attrMap.put(SMSEntry.ATTR_KEYVAL, set);
        return attrMap;
    }

    private Map<String, Set<String>> getSearchableAttribute(
        JSONArray tokenSubject,
        String tokenType, JSONObject jo) throws JSONException {
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        Set<String> values = new HashSet<String>();
        for (Iterator i = jo.keys(); i.hasNext(); ) {
            String key = ((String)i.next()).toLowerCase();
            if (!searchableAttrs.contains(key)) {
                continue;
            }
            JSONArray arr = (JSONArray)jo.getJSONArray(key);
            for (int j = 0; j < arr.length(); j++) {
                values.add(SEARCHABLE_ATTR + "=" + key
                    + "=" + arr.getString(j));
            }
        }
        values.add(CoreTokenConstants.TOKEN_TYPE + "=" + tokenType);
        int valLen = tokenSubject.length();
        for (int i =  0; i < valLen; i++) {
            values.add("token.subject=" + tokenSubject.getString(i));
        }
        result.put(SMSEntry.ATTR_XML_KEYVAL, values);

        Set<String> setObjectClass = new HashSet<String>(4);
        result.put(SMSEntry.ATTR_OBJECTCLASS, setObjectClass);
        // add object classes
        setObjectClass.add(SMSEntry.OC_TOP);
        setObjectClass.add(SMSEntry.OC_SERVICE_COMP);
        
        return result;
    }

    public static String getCoreTokenDN(String tokenId) {
        return "ou=" + tokenId + "," + SERVICE_DN;
    }

    private String getTokenAttributeValueFromSM(SMSEntry s, String attrName) {
        Map<String, Set<String>> map = s.getAttributes();
        Set<String> attrVals = map.get(SMSEntry.ATTR_KEYVAL);
        return getTokenAttributeValue(attrVals, attrName);
    }

    private String getTokenAttributeValue(Set<String> values, String keyName) {
        if ((values != null) && !values.isEmpty()) {
            for (Iterator<String> i = values.iterator(); i.hasNext(); ) {
                String value = i.next();
                if (value.startsWith(keyName + "=")) {
                    // found the JSON-encoded attribute value
                    return value.substring(keyName.length() + 1);
                }
            }
        }
        // return empty JSON values
        return "{}";
    }

    /**
     * Checks if the ETag matches the one in the token store.
     * @param eTag ETag to be checked.
     * @param s SMSEntry object of the token entry to be checked.
     * @throws CoreTokenException if the ETag does not match.
     */
    private void checkETag(String eTag, JSONObject json, String tokenId)
        throws JSONException, CoreTokenException {
        String tokenType = getSingleStringValue(json,
            CoreTokenConstants.TOKEN_TYPE);
        if (CoreTokenConfigService.noETagEnfTypes.contains(tokenType)) {
            // no need to check eTag
            return;
        }
        if (CoreTokenUtils.debug.messageEnabled()) {
            CoreTokenUtils.debug.message("OpenSSOCoreTokenStore.checkETag: " +
                "check etag for token type " + tokenType + ", ETag=" + eTag);
        }
        if ((eTag == null) || (eTag.length() == 0)) {
            throw new CoreTokenException(220, null, 409);
        }
        String tokenETag = getSingleStringValue(json,
            CoreTokenConstants.VERSION_TAG);
        if ((tokenETag == null) || !tokenETag.equals(eTag)) {
            Object[] args = {eTag};
            throw new CoreTokenException(208, args, 412);
        }
    }

    private JSONObject updateAttributeValues(
        JSONObject json, JSONObject newVals)
        throws JSONException {
        Iterator<String> keys = newVals.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONArray valArray = newVals.getJSONArray(key);
            // remove old value
            json.remove(key);
            // add new value if it is not empty
            if (valArray.length() != 0) {
                json.put(key, valArray);
            }
        }
        return json;
    }

    /**
     *
     * @param jObj
     * @param attrName
     * @return
     * @throws JSONException if unable to get attribute values.
     * @throws CoreTokenException if the attribute is not found or the
     *    attributes contains more than one value.
     */
    private String getSingleStringValue(JSONObject jObj, String attrName)
        throws JSONException, CoreTokenException {
        JSONArray values = null;
        if (jObj.has(attrName)) {
            values = jObj.getJSONArray(attrName);
        } else {
            String[] args = new String[]{attrName};
            throw new CoreTokenException(222, args, 400);
        }
        if (values.length() != 1) {
            String[] args = new String[]{attrName};
            throw new CoreTokenException(226, args, 400);
        } else {
            return values.getString(0);
        }
    }

    private String createSearchFilter(String query) throws CoreTokenException {
        if ((query == null) || (query.length() == 0)) {
            throw new CoreTokenException(218, null, 400);
        }
        StringTokenizer attrs = new StringTokenizer(query, "&");
        StringBuilder sb = new StringBuilder(100);
        sb.append("(&");
        while (attrs.hasMoreTokens()) {
            String attr = (String) attrs.nextToken();
            int pos = attr.indexOf("=");
            if (pos == -1) {
                String[] args = new String[] {attr};
                throw new CoreTokenException(219, args);
            }
            String key = attr.substring(0, pos);
            if ((key == null) || (key.length() == 0)) {
                String[] args = new String[] {attr};
                throw new CoreTokenException(219, args, 400);
            }
            // change attribute name to lower case as all attribute
            // names are converted to lower case before saving
            String lcKey = key.toLowerCase();
            if (!searchableAttrs.contains(lcKey) &&
                !internalSearchableAttrs.contains(lcKey)) {
                String[] args = new String[]{key};
                throw new CoreTokenException(223, args, 400);
            }
            String value = attr.substring(pos + 1);
            if ((value == null) || (value.length() == 0)) {
                String[] args = new String[] {attr};
                throw new CoreTokenException(219, args, 400);
            }
            sb.append("(").append(SMSEntry.ATTR_XML_KEYVAL + "=");
            if (searchableAttrs.contains(lcKey)) {
                sb.append(SEARCHABLE_ATTR).append("=");
            }
            sb.append(lcKey).append("=").append(value)
              .append(")");
        }
        sb.append(")");
        if (CoreTokenUtils.debug.messageEnabled()) {
            CoreTokenUtils.debug.message("OpenSSOCoreTokenStore." +
                "createSearchFilter, filter is " + sb.toString());
        }
        return sb.toString();
    }
}
