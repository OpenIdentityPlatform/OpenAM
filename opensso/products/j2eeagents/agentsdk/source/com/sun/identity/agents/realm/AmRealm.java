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
 * $Id: AmRealm.java,v 1.9 2008/07/25 00:49:29 huacui Exp $
 *
 */

package com.sun.identity.agents.realm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.ISSOTokenValidator;
import com.sun.identity.agents.common.SSOValidationResult;
import com.sun.identity.agents.util.IUtilConstants;
import com.sun.identity.agents.util.TransportToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;


/**
 * The agent realm implementation that authenticates the user
 * and establishes the user principals to the container.
 */
public class AmRealm extends AmRealmBase implements IAmRealm {
    
    public AmRealm(Manager manager) {
        super(manager);
    }
    
    public void initialize() throws AgentException {
        super.initialize();
        if (isLogMessageEnabled()) {
            logMessage("AmRealm.initialize: Using IDM APIs");
        }
        initPrivilegedAttributeTypes();
        initPrivilegedAttributeTypeCases();
        initBypassPrincipalList();
        initGlobalVerificationHandler();
        initDefaultPrivilegedAttributeList();
        initSessionPrivilegedAttributeList();
        initSSOTokenValidator();
        initPrivilegedAttributeMappingEnableFlag();
        if (isPrivilegedAttributeMappingEnabled()) {
            initPrivilegedAttributeMap();
        }
        if (isLogMessageEnabled()) {
            logMessage("AmRealm.initialize: Initialized.");
        }
    }
    
    private void initPrivilegedAttributeTypes() throws AgentException {
        ArrayList types = new ArrayList();
        String[] givenTypes = getConfigurationStrings(CONFIG_FETCH_TYPE);
        try {
            if (givenTypes != null && givenTypes.length > 0) {
                for (int i=0; i<givenTypes.length; i++) {
                    String nextType = givenTypes[i];
                    if (isLogMessageEnabled()) {
                        logMessage("AmRealm.initPrivilegedAttributeTypes:"
                                + " Next configured type: "
                                + nextType);
                    }
                    IdType nextIdType = IdUtils.getType(nextType);
                    if (nextIdType != null) {
                        types.add(nextIdType);
                    } else {
                        throw new AgentException("Failed to resolve given "
                                + "privileged attribute type: " + nextType);
                    }
                }
            }
        } catch (Exception ex) {
            throw new AgentException(
                    "Failed to identify privileged attribute types", ex);
        }
        IdType[] idTypes = new IdType[types.size()];
        System.arraycopy(types.toArray(), 0, idTypes, 0, types.size());
        setPrivilegedAttributeTypes(idTypes);
    }
    
    public AmRealmAuthenticationResult authenticate(
            SSOValidationResult ssoValidationResult) {
        AmRealmAuthenticationResult result =
                AmRealmAuthenticationResult.FAILED;
        if (ssoValidationResult != null && ssoValidationResult.isValid()) {
            String userName = ssoValidationResult.getUserId();
            if (!isBypassed(userName)) {
                result = authenticateInternal(ssoValidationResult);
            } else {
                if(isLogMessageEnabled()) {
                    logMessage(
                      "AmRealm.authenticate(SSOValidationResult):"
                            + " Bypassed authentication for user: "
                      + userName);
                }
            }
        }
        return result;
    }
    
    public AmRealmAuthenticationResult authenticate(
            String userName, String transportString) {
        AmRealmAuthenticationResult result = AmRealmAuthenticationResult.FAILED;
        try {
            if(!isBypassed(userName)) {
                SSOValidationResult ssoValidationResult =
                        getSSOTokenValidator().validate(transportString);
                if (ssoValidationResult.isValid()) {
                    if (userName.equalsIgnoreCase(ssoValidationResult.getUserId())) {
                        result = authenticateInternal(ssoValidationResult);
                    } else {
                        logError("AmRealm.authenticate: Username mismatch: given: "
                                + userName + ", expected: "
                                + ssoValidationResult.getUserId() +
                                ". Denying authentication.");
                    }
                }
            } else {
                result = new AmRealmAuthenticationResult(true);
                if(isLogMessageEnabled()) {
                    logMessage("AmRealm.authenticate: Bypassed authentication"
                            + " for user: "
                            + userName);
                }
            }
        } catch (Exception ex) {
            logError("AmRealm.authenticate: failed to authenticate user: " 
                    + userName, ex);
            result = AmRealmAuthenticationResult.FAILED;
        }
        
        if (isLogMessageEnabled()) {
            logMessage("AmRealm.authenticate: user: " + userName
                    + ", authenticated: " + result.isValid()
                    + ", attributes: " + result.getAttributes());
        }
        
        return result;
    }
    
    private AmRealmAuthenticationResult authenticateInternal(
            SSOValidationResult ssoValidationResult) {
        AmRealmAuthenticationResult result = AmRealmAuthenticationResult.FAILED;
        SSOToken ssoToken = null;
        String userName = IUtilConstants.ANONYMOUS_USER_NAME;
        try {
            if (ssoValidationResult.isValid()) {
                userName = ssoValidationResult.getUserId();
                ssoToken = ssoValidationResult.getSSOToken();
                TransportToken token = ssoValidationResult.getTransportToken();
                IExternalVerificationHandler handler = getVerificationHandler(
                        ssoValidationResult.getApplicationName());
                if (handler.verify(userName, token, null)) {
                    HashSet attributeSet = new HashSet();
                    attributeSet.addAll(getDefaultPrivilegedAttributeSet());
                    
                    // From user profile only
                    if (isAttributeFetchEnabled()) {
                        AMIdentity user = IdUtils.getIdentity(
                                ssoValidationResult.getSSOToken());
                        if (user != null) {
                            if (isLogMessageEnabled()) {
                               String userId =
                                        getUniquePartOfUuid(
                                        IdUtils.getUniversalId(user));
                               logMessage("AmRealm.authenticateInternal: user: "
                                        + userId);
                            }
                            IdType[] types = getPrivilegedAttributeTypes();
                            for (int i=0; i<types.length; i++) {
                                Boolean toLowerCaseStat =
                                    (Boolean)getPrivilegedAttributeTypeCases().
                                        get(types[i].getName());
                                // If users did not put an entry in the map, we
                                // will cover for that case too
                                if (toLowerCaseStat == null) {
                                    toLowerCaseStat = Boolean.FALSE;
                                    // for second time
                                    getPrivilegedAttributeTypeCases().put(
                                            types[i].getName().toLowerCase(),
                                            toLowerCaseStat);
                                }
                                Set memberships = user.getMemberships(types[i]);
                                if (memberships != null &&
                                        memberships.size() > 0) {
                                    Iterator mIt = memberships.iterator();
                                    String origUUID = null;
                                    while (mIt.hasNext()) {
                                        origUUID = IdUtils.getUniversalId(
                                                    (AMIdentity) mIt.next());
                                        if (toLowerCaseStat.booleanValue()) {
                                            origUUID = origUUID.toLowerCase();
                                        }
                                        attributeSet.add(origUUID);
                                        String mappedId = getPrivilegedMappedAttribute(origUUID);
                                        attributeSet.add(mappedId);

                                        String universalId = 
                                            getUniquePartOfUuid(origUUID);
                                        if (!origUUID.equalsIgnoreCase(universalId)) {
                                            if (toLowerCaseStat.booleanValue()) {
                                                universalId = universalId.toLowerCase();
                                            }
                                            attributeSet.add(universalId);
                                            mappedId = getPrivilegedMappedAttribute(universalId);
                                            attributeSet.add(mappedId);
                                        }
                                    }
                                }
                            }
                        } else {
                            throw new AgentException("Failed to find user: "
                                    + userName + "["
                                    + ssoValidationResult.getUserPrincipal() 
                                    + "]");
                        }
                    }
                    
                    // From session only
                    if (isSessionAttributeFetchEnabled()) {
                        getMembershipsFromSessionAttributes(ssoToken, 
                                attributeSet);
                    }
                    
                    result = new AmRealmAuthenticationResult(
                            true, attributeSet);
                    
                } else {
                    if (isLogMessageEnabled()) {
                        logMessage("AmRealm.authenticateInternal:"
                                + " external verfication failed for user: " 
                                + userName);
                    }
                }
            } else {
                if (isLogMessageEnabled()) {
                    logMessage("AmRealm.authenticateInternal: session invalid"
                            + " for user: "
                            + userName);
                }
            }
            processAuthenticationResult(userName, result, ssoValidationResult);
        } catch (Exception ex) {
            logError("AmRealm.authenticateInternal: failed to authenticate"
                    + " user: " 
                    + userName, ex);
            result = AmRealmAuthenticationResult.FAILED;
        }
        
        return result;
    }
    
    /**
     * maps original Attribute to the one defined in agent configuration
     * properties. It helps with some cases like handling special characters
     * in original attribute.
     */
    private String getPrivilegedMappedAttribute(String originalAttribute) {
        String mappedAttribute = originalAttribute;
        if (isPrivilegedAttributeMappingEnabled()) {
            Map privilegedAttributeMap = getPrivilegedAttributeMap();
            if (privilegedAttributeMap != null &&
                    originalAttribute != null) {
                mappedAttribute = (String)privilegedAttributeMap.get(
                        originalAttribute);
            }
            if (mappedAttribute == null) {
                mappedAttribute = originalAttribute;
            }
        }
        return mappedAttribute;
    }
    
    /**
     * get memberships from SSO Session's attributes. The memberships can
     * be multiple values separated by delimiter like group1|group2|group3.
     */
    private void getMembershipsFromSessionAttributes(
            SSOToken ssoToken,
            Set attributeSet) throws SSOException {
        String[] sessionAttributes = getSessionAttributes();
        
        for (int i=0; i<sessionAttributes.length; i++) {
            String nextValue =
                    ssoToken.getProperty(sessionAttributes[i]);
            if (isLogMessageEnabled()) {
                logMessage( "AmRealm.getMembershipsFromSessionAttributes - "
                        + "Session attribute name = "
                        + sessionAttributes[i]
                        + ", Session attribute value ="
                        + nextValue);
            }
            
            if (nextValue != null &&
                    nextValue.trim().length() > 0) {
                /* handle the case Session's attribute has multiple values */
                if (nextValue.indexOf(DELIMITER) != -1) {
                    Set valueSet =
                            delimiteredStringToSet( nextValue,
                            DELIMITER);
                    Iterator iter = valueSet.iterator();
                    while (iter.hasNext()) {
                        String subValue = (String)iter.next();
                        /* handle the attribute mappings too */
                        String mappedAttribute =
                                getPrivilegedMappedAttribute(subValue);
                        attributeSet.add(mappedAttribute);
                    }
                } else {
                    String mappedAttribute =
                            getPrivilegedMappedAttribute(nextValue);
                    attributeSet.add(mappedAttribute);
                } // end of if (nextValue.indexOf
            } // end of if (nextValue
        } // end of for loop
    }
    
    /*
     * get the set of values from delimitered String.
     */
    private Set delimiteredStringToSet(String str, String delimiter) {
        Set valueSet = new HashSet();
        StringTokenizer tokenizer = new StringTokenizer(str, delimiter);
        while (tokenizer.hasMoreElements()) {
            valueSet.add(tokenizer.nextElement());
        }
        return valueSet;
    }
    
    /*
     * This function is used internally to strip of the amsdkdn part of the
     * uuid.
     */
    private String getUniquePartOfUuid(String uuid) {
        
        String uuidStripped = null;
        if ((uuid != null) && (uuid.length() > 0)) {
            int first = uuid.indexOf(AMSDKDN_DELIMITER);
            if(first != -1) {
                uuidStripped =  uuid.substring(0,first);
            } else { // fall back to original string
                uuidStripped = uuid;
            }
        }
        
        if (isLogMessageEnabled()) {
            logMessage("AmRealm.getUniquePartOfUuid: Unique part of uuid = " 
                    + uuidStripped);
        }
        return uuidStripped;
    }
    
    
    private void initSSOTokenValidator() throws AgentException {
        CommonFactory cf = new CommonFactory(getModule());
        ISSOTokenValidator validator = cf.newSSOTokenValidator();
        
        setSSOTokenValidator(validator);
    }
    
    private void initSessionPrivilegedAttributeList() {
        String [] sessionAttributeList = getConfigurationStrings(
                CONFIG_PRIVILEGED_SESSION_ATTR_LIST);
        ArrayList attributes = new ArrayList();
        if (sessionAttributeList != null && sessionAttributeList.length > 0) {
            for (int i=0; i<sessionAttributeList.length; i++) {
                String nextAttr = sessionAttributeList[i];
                if (nextAttr != null && nextAttr.trim().length() > 0) {
                    attributes.add(nextAttr);
                }
            }
        }
        
        String[] sessionAttributes = new String[attributes.size()];
        System.arraycopy(attributes.toArray(), 0, sessionAttributes, 0,
                attributes.size());
        setSessionAttributes(sessionAttributes);
    }
    
    private void initDefaultPrivilegedAttributeList() {
        String[] defaultAttributeList = getConfigurationStrings(
                CONFIG_DEFAULT_PRIVILEGE_ATTR_LIST);
        
        if (defaultAttributeList != null && defaultAttributeList.length >0) {
            for (int i=0; i<defaultAttributeList.length; i++) {
                String nextAttr = defaultAttributeList[i];
                if (nextAttr != null && nextAttr.trim().length() > 0) {
                    getDefaultPrivilegedAttributeSet().add(nextAttr);
                }
            }
        }
        
        if (isLogMessageEnabled()) {
            logMessage("AmRealm.initDefaultPrivilegedAttributeList:"
                    + " Default privileged attribute set: "
                    + getDefaultPrivilegedAttributeSet());
        }
    }
    
    private void initPrivilegedAttributeMappingEnableFlag() {
        _privilegedAttributeMappingEnabled = getConfigurationBoolean(
                CONFIG_PRIVILEGED_ATTRIBUTE_MAPPING_ENABLED,
                DEFAULT_PRIVILEGED_ATTRIBUTE_MAPPING_ENABLED
                );
        if (isLogMessageEnabled()) {
            logMessage(
                    "AmRealm.initPrivilegedAttributeMappingEnableFlag:" 
                    + " Using privileged attribute mapping enabled flag: "
                    + _privilegedAttributeMappingEnabled);
        }
    }
    
    private void initPrivilegedAttributeMap() {
        _privilegedAttributeMap = getConfigurationMap(
                CONFIG_PRIVILEGED_ATTRIBUTE_MAPPING);
        if (isLogMessageEnabled()) {
            logMessage("AmRealm.initPrivilegedAttributeMap: privileged"
                    + " attribute mapping: "
                    + _privilegedAttributeMap);
        }
    }
    
    private boolean isPrivilegedAttributeMappingEnabled() {
        return _privilegedAttributeMappingEnabled;
    }
    
    private Map getPrivilegedAttributeMap() {
        return _privilegedAttributeMap;
    }
    
    private IExternalVerificationHandler getVerificationHandler(String appName)
    throws AgentException {
        IExternalVerificationHandler result = (IExternalVerificationHandler)
        getVerificationHandlers().get(appName);
        
        if (result == null) {
            synchronized (this) {
                result = (IExternalVerificationHandler)
                getVerificationHandlers().get(appName);
                if (result == null) {
                    String className = (String) getConfigurationMap(
                            CONFIG_VERIFICATION_HANDLERS).get(appName);
                    
                    boolean appHandlerFound = false;
                    if (className != null && className.trim().length() > 0) {
                        try {
                            result = (IExternalVerificationHandler)
                            Class.forName(className).newInstance();
                            
                            getVerificationHandlers().put(appName, result);
                            appHandlerFound = true;
                        } catch (Exception ex) {
                            throw new AgentException(
                                "Unable to load verification handler for app: "
                                + appName, ex);
                            
                        }
                    }
                    if (!appHandlerFound) {
                        result = getGlobalVerificationHandler();
                        getVerificationHandlers().put(appName, result);
                        
                        if (isLogMessageEnabled()) {
                            logMessage("AmRealm.getVerificationHandler: Unable"
                                    + " to find verification handler for app: "
                                    + appName
                                    + ", using global handler");
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    private IExternalVerificationHandler getGlobalVerificationHandler() {
        return _globalVerificationHandler;
    }
    
    private void initGlobalVerificationHandler() throws AgentException {
        try {
            String className = getResolver().getGlobalVerificationHandlerImpl();
            _globalVerificationHandler = (IExternalVerificationHandler)
            Class.forName(className).newInstance();
            
            if (isLogMessageEnabled()) {
                logMessage("AmRealm.initGlobalVerificationHandler: Global"
                        + " verification handler set to: "
                        + _globalVerificationHandler);
            }
            
        } catch (Exception ex) {
            throw new AgentException(
                    "Unable to initialize global verification handler", ex);
        }
    }
    
    private void initBypassPrincipalList() {
        String[] bypassList = getConfigurationStrings(CONFIG_BYPASS_USER_LIST);
        if (bypassList != null && bypassList.length > 0) {
            for (int i=0; i<bypassList.length; i++) {
                if (bypassList[i] != null 
                        && bypassList[i].trim().length() > 0) {
                    getBypassPrincipalSet().add(bypassList[i]);
                }
            }
        }
    }
    
    private void initPrivilegedAttributeTypeCases() {
        
        Map privAttrTypeCasesMap = getConfigurationMap(
                CONFIG_PRIVILEGED_ATTR_CASE);
        if (privAttrTypeCasesMap != null) {
            Iterator iter = privAttrTypeCasesMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry me = (Map.Entry)iter.next();
                String key = (String)me.getKey();
                String val = (String)me.getValue();
                if ((key != null) && (key.length() > 0)) {
                    if (val != null) {
                        Boolean caseStat = Boolean.valueOf(val);
                        // We need to convert attr types to lower cases so
                        // that they match with IdType.getName()
                        getPrivilegedAttributeTypeCases().put(
                                key.toLowerCase(),caseStat);
                    }
                }
            }
        }
    }
    
    private boolean isAttributeFetchEnabled() {
        return getPrivilegedAttributeTypes().length > 0;
    }
    
    private HashSet getBypassPrincipalSet() {
        return _bypassPrincipalSet;
    }
    
    private boolean isBypassed(String userName) {
        boolean result = false;
        if(getBypassPrincipalSet().contains(userName)) {
            result = true;
        }
        return result;
    }
    
    
    private Hashtable getVerificationHandlers() {
        return _verificationHandlers;
    }
    
    private HashSet getDefaultPrivilegedAttributeSet() {
        return _defaultPrivilegedAttributeSet;
    }
    
    private ISSOTokenValidator getSSOTokenValidator() {
        return _ssoTokenValidator;
    }
    
    private void setSSOTokenValidator(ISSOTokenValidator validator) {
        _ssoTokenValidator = validator;
    }
    
    private void setPrivilegedAttributeTypes(IdType[] types) {
        _privilegedAttributeTypes = types;
        
        if (isLogMessageEnabled()) {
            StringBuffer buff = new StringBuffer(
                    "AmRealm.setPrivilegedAttributeTypes: Configured"
                    + " Attribute Types:");
            buff.append(IUtilConstants.NEW_LINE);
            for (int i=0; i<types.length; i++) {
                buff.append("[").append(i).append("]: ");
                buff.append(types[i].getName()).append(IUtilConstants.NEW_LINE);
            }
            buff.append("Total Configugured Attribute Types: " + types.length);
            logMessage(buff.toString());
        }
    }
    
    private IdType[] getPrivilegedAttributeTypes() {
        return _privilegedAttributeTypes;
    }
    
    private boolean isSessionAttributeFetchEnabled() {
        boolean result = false;
        String[] sessionAttributes = getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.length > 0) {
            result = true;
        }
        return result;
    }
    
    private String[] getSessionAttributes() {
        return _sessionAttributes;
    }
    
    private void setSessionAttributes(String[] attributes) {
        _sessionAttributes = attributes;
        if (isLogMessageEnabled()) {
            StringBuffer buff = new StringBuffer("");
            for (int i=0; i<attributes.length; i++) {
                buff.append(" ").append(attributes[i]);
                if (i !=attributes.length -1) {
                    buff.append(",");
                }
            }
            logMessage("AmRealm.setSessionAttributes: Session attributes: " 
                    + buff.toString());
        }
    }
    
    private HashMap getPrivilegedAttributeTypeCases() {
        return _privilegedAttributeTypeCases;
    }
    
    /*
     * Delimiter to strip off amsdkdn part of the uuid
     */
    public static String AMSDKDN_DELIMITER = ",amsdkdn=";
    
    /*
     * Delimiter for session attribute e.g. group1|group2|group3...
     * It should be the same as DELIMITER from SessionPropertyCondition.java.
     */
    private static final String DELIMITER = "|";
    
    private HashSet _bypassPrincipalSet = new HashSet();
    private IExternalVerificationHandler _globalVerificationHandler;
    private Hashtable _verificationHandlers = new Hashtable();
    private HashSet _defaultPrivilegedAttributeSet = new HashSet();
    private ISSOTokenValidator _ssoTokenValidator;
    private IdType[] _privilegedAttributeTypes;
    private HashMap _privilegedAttributeTypeCases = new HashMap();
    private String[] _sessionAttributes;
    private Map _privilegedAttributeMap = null;
    private boolean _privilegedAttributeMappingEnabled;
}

