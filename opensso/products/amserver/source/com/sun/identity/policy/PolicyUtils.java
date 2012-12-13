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
 * $Id: PolicyUtils.java,v 1.16 2010/01/13 03:01:15 dillidorai Exp $
 *
 */
/**
 * Portions Copyrighted 2011-2012 ForgeRock Inc
 */
package com.sun.identity.policy;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.util.Crypt; //from products/shared
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.log.Logger;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.messageid.LogMessageProvider;
import com.sun.identity.log.messageid.MessageProviderFactory;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.SMSEntry;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The class <code>PolicyUtils</code> provides utility(static) methods
 * that would be used by policy pacakge
 */
public class PolicyUtils {

    public static boolean logStatus = false;

    public static final String EMPTY_STRING = "";
    public static final String NULL_STRING = "null";
    public static final String NEW_LINE = "\n";
    public static final String ADVICES_TAG_NAME = "Advices";
    public static final String ADVICES_START_TAG = "<Advices>";
    public static final String ADVICES_END_TAG = "</Advices>";

    static Debug debug = Debug.getInstance("amPolicy");
    private static LogMessageProvider msgProvider;
    private static Logger accessLogger;
    private static Logger errorLogger;
    private static Logger delegationLogger;

    static {
        String status = SystemProperties.get(Constants.AM_LOGSTATUS);
        logStatus = ((status != null) && status.equalsIgnoreCase("ACTIVE"));

        if (logStatus) {
            accessLogger = (Logger)Logger.getLogger("amPolicy.access");
            errorLogger = (Logger)Logger.getLogger("amPolicy.error");
            delegationLogger = (Logger)Logger.getLogger(
                    "amPolicyDelegation.access");
        }
    }

    static final String ENV_PARAMETERS = "EnvParameters";
    static final String GET_RESPONSE_DECISIONS = "GetResponseDecisions";
    static final String ATTRIBUTE_VALUE_PAIR = "AttributeValuePair";
    static final String ATTRIBUTE_VALUE_PAIR_BEGIN = "<AttributeValuePair>";
    static final String ATTRIBUTE_VALUE_PAIR_END = "</AttributeValuePair>";
    static final String ATTRIBUTE = "Attribute"; 
    static final String ATTRIBUTE_NAME = "name";
    static final String VALUE = "Value";
    static final String VALUE_BEGIN = "<Value>";
    static final String VALUE_END = "</Value>";
    static final String ATTRIBUTE_NAME_BEGIN = "<Attribute name=";
    static final String ATTRIBUTE_NAME_END = "/>";
    static final String CRLF = "\r\n";

    /** 
     * Adds a map to another map
     * @param mapToAdd map that needs to be added
     *        Each key should be a String
     *        Each value would be a Set of String values
     * @param toMap map the map to which the mapToAdd would be added
     *        Each key should be a String
     *        Each value would be a Set of String values
     * @return the combined map which is also the toMap
     *         The combined map is formed by replacing the values 
     *         for each key found in the addToMap to the toMap
     */         
    static Map addMapToMap(Map mapToAdd, Map toMap) {
        if ( (mapToAdd != null) && (toMap !=null) ) {
            Set keySet = mapToAdd.keySet();
            Iterator keyIter = keySet.iterator();
            while ( keyIter.hasNext() ) {
                String key = (String) keyIter.next();
                Set values = (Set) mapToAdd.get(key);
                addElementToMap(key, values, toMap);
            }
        }
        return toMap;
    }

    /** 
     * Appends a map to another map
     * @param mapToAdd map that needs to be added
     *        Each key should be a String
     *        Each value would be a Set of String values
     * @param toMap map the map to which the mapToAdd would be added
     *        Each key should be a String
     *        Each value would be a Set of String values
     * @return the combined map which is also the toMap
     *         The combined map is formed by adding the values 
     *         for each key found in the addToMap to the toMap.
     *         If a key was found both in addToMap and toMap,
     *         the new value for the key is the combined set of
     *         values for the key from the addToMap and original
     *         toMap
     */         
    public static Map appendMapToMap(Map mapToAdd, Map toMap) {
        if ( (mapToAdd != null) && (toMap !=null) ) {
            Set keySet = mapToAdd.keySet();
            Iterator keyIter = keySet.iterator();
            while ( keyIter.hasNext() ) {
                String key = (String) keyIter.next();
                Set values = (Set) mapToAdd.get(key);
                appendElementToMap(key, values, toMap);
            }
        }
        return toMap;
    }

    /** 
     * Adds a key/value pair to a map
     * @param key a String valued key
     * @param values a set of String values
     * @param toMap the map to which to add the key/value pair
     *        Each key of the map should be a String
     *        Each value of the map should be a Set of String values
     * @return the combined map which is also the toMap
     *         The combined map is formed by replacing the values 
     *         for key in the toMap with argument values
     */         
    static Map addElementToMap(String key, Set values, Map toMap) {
       if ( (key !=null) && (toMap != null) ) {
           toMap.put(key, values);
       }
       return toMap;
    }

    /** 
     * Appends a key/value pair to a map
     * @param key a String valued key
     * @param values a set of String values
     * @param toMap the map to which to append the key/value pair
     *        Each key of the map should be a String
     *        Each value of the map should be a Set of String values
     * @return the combined map which is also the toMap
     *         The combined map is formed by adding the values 
     *         for argument key to the toMap with the argument values.
     *         If the key is already present  in addToMap,
     *         the new value for the key is the combined set of
     *         values for the key from argument values and original
     *         toMap
     */         
    public static Map appendElementToMap(String key, Set values, Map toMap) {
       if ( (key !=null) && (values !=null) && (!values.isEmpty())
                && (toMap != null) ) {
           Set previousValues = (Set) toMap.get(key);
           if ( (previousValues != null) && (!previousValues.isEmpty()) ) {
               previousValues.addAll(values);
           } else {
               toMap.put(key, values);
           }
       }
       return toMap;
    }


    /** 
     *  Returns the display name for a given dn
     *  This implementation assumes the display name to be the value of
     *  the naming attribute of the entry. So, the value of the naming 
     *  attribute is the return value.
     *  @param dn dn of the entry for which to get the display name
     *  @return disaplay name for the entry, this  is same as the 
     *          value of the naming attribute of the entry
     */
    public static String getDisplayName(String dn) {
        String[] componentValues = LDAPDN.explodeDN(dn, true);
        return (componentValues.length > 0 ) ?
                componentValues[0] : "";
    }
    
    /**
     * Appends a policy decision to another policy decision.
     * i.e. Merges one policy decision to anothe policy decision.
     * @param pd1 policy decision to be added
     * @param pd2 policy decision to be merged into
     * @return merged policy decision
     * @throws PolicyException if the decision can not be merged
     */
    static PolicyDecision appendPolicyDecisionToPolicyDecision(
            PolicyDecision pd1, PolicyDecision pd2) throws PolicyException {
        Map actionDecisions = pd1.getActionDecisions();
        Iterator actionNames = actionDecisions.keySet().iterator();
        while ( actionNames.hasNext()) {
            String actionName = (String) actionNames.next();
            ActionDecision actionDecision = (ActionDecision)
            actionDecisions.get(actionName);
            pd2.addActionDecision(actionDecision);
        }
        return pd2;
    }


    /** 
     * Parses an XML node which represents a collection of 
     * the environment parameters and returns a map which contains 
     * these parameters.
     * @param pNode the XML DOM node for the environment parameters.
     * @return a map which contains the environment parameters
     * @throws PolicyException if the node can not be parsed into a map
     */

    public static Map parseEnvParameters(Node pNode)
        throws PolicyException
    {
        Node node = null;
        Set nodeSet = XMLUtils.getChildNodes(pNode, ATTRIBUTE_VALUE_PAIR);
        if (nodeSet == null) {
            debug.error("parseEnvParameters: missing element " 
                    + ATTRIBUTE_VALUE_PAIR);
            String objs[] = { ATTRIBUTE_VALUE_PAIR };
            throw new PolicyException(ResBundleUtils.rbName, 
                "missing_element", objs, null);
        }

        HashMap envParams = new HashMap();

        Iterator nodes = nodeSet.iterator();
        while (nodes.hasNext()) {
            node = (Node)nodes.next();
            String attributeName = getAttributeName(node);
            if (attributeName == null) {
                debug.error("PolicyUtils.parseEnvParameters():"
                        + " missing attribute name");
                String objs[] = { ATTRIBUTE_NAME };
                throw new PolicyException(ResBundleUtils.rbName, 
                    "missing_attribute", objs, null);
            }
            Set values = getAttributeValues(node);
            if (values == null) {
                debug.error("PolicyUtils.parseEnvParameters():"
                        + " missing attribute value");
                String objs[] = { VALUE };
                throw new PolicyException(ResBundleUtils.rbName,
                    "missing_attribute", objs, null);
            }
            envParams.put(attributeName, values);
        }
      
        return envParams;
    }


    /** 
     * Parses an XML node which represents a collection of 
     * user response attributes and returns a set which contains the
     * names of these attributes
     * @param pNode the XML DOM node for the response attributes
     * @return a set which contains the names of these attributes
     * @throws PolicyException if the node can not be parsed into a set
     */

    public static Set parseResponseAttributes(Node pNode)
        throws PolicyException
    {
        Set nodeSet = XMLUtils.getChildNodes(pNode, ATTRIBUTE);
        if (nodeSet == null) {
            debug.error("parseResponseAttributes: "
                    + " missing element " + ATTRIBUTE);
            String objs[] = { ATTRIBUTE };
            throw new PolicyException(ResBundleUtils.rbName,
                "missing_element", objs, null);
        }

        HashSet attrs = new HashSet();
        Node node = null; 
        Iterator nodes = nodeSet.iterator();
        while (nodes.hasNext()) {
            node = (Node)nodes.next();
            String attrName = XMLUtils.getNodeAttributeValue(node, 
                    ATTRIBUTE_NAME);
            if (attrName == null) {
                debug.error("parseResponseAttributes: "
                        + " missing attribute " + ATTRIBUTE_NAME);
                String objs[] = { ATTRIBUTE_NAME };
                throw new PolicyException(ResBundleUtils.rbName,
                    "missing_attribute", objs, null);
            }
            attrs.add(attrName);
        }

        return attrs;
    }

            
    /** 
     * Parses an XML node which represents Attribute-Value pairs
     * and returns a map of such values.
     * @param pNode the XML DOM node containing Attribute-Value pairs
     * as child nodes.
     * @return a map which contains Attribute-Value pairs
     */

    public static Map parseAttributeValuePairs(Node pNode) {
        Node node = null;
        Set nodeSet = XMLUtils.getChildNodes(pNode, ATTRIBUTE_VALUE_PAIR);
        if (nodeSet == null) {
            debug.error("parseAttribiteValuePairs: "
                    +"missing element " + ATTRIBUTE_VALUE_PAIR);
            return null;
        }

        HashMap attrValuePairs = new HashMap();

        Iterator nodes = nodeSet.iterator();
        while (nodes.hasNext()) {
            node = (Node)nodes.next();
            String attributeName = getAttributeName(node);
            if (attributeName == null) {
                debug.error("PolicyUtils.parseAttribiteValuePairs"
                        +"():missing attribute name");
                return null;
            }
            Set values = getAttributeValues(node);
            if (values == null) {
                debug.error("PolicyUtils.parseAttribiteValuePairs"
                        +"():missing attribute value");
                return null;
            }
            attrValuePairs.put(attributeName, values);
        }
      
        return attrValuePairs;
    }

    /** 
     * Parses an XML node which represents an 
     * AttributeValuePair and returns the attribute name.
     * @param pNode the XML DOM node for an AttributeValuePair
     * @return the attribute name of the AttributeValuePair
     */

    public static String getAttributeName(Node pNode) 
    {
        Node node = XMLUtils.getChildNode(pNode, ATTRIBUTE);
        if (node == null) {
            debug.error("PolicyUtils.getAttributeName(): "
                +"missing element " + ATTRIBUTE);
            return null;
        }

        String attrName = XMLUtils.getNodeAttributeValue(node, ATTRIBUTE_NAME);
        if (attrName == null) {
            debug.error("PolicyUtils.getAttributeName(): "
                +"missing attribute " + ATTRIBUTE_NAME + " for element " 
                + ATTRIBUTE);
            return null;
        }

        return attrName;
    }


    /** 
     * Parses an XML node which represents an 
     * AttributeValuePair and returns the attribute values.
     * @param pNode the XML DOM node for an AttributeValuePair
     * @return the set of attribute values of the AttributeValuePair
     */

    public static Set getAttributeValues(Node pNode)
    {
        Set nodeSet = XMLUtils.getChildNodes(pNode, VALUE);
        if (nodeSet == null) {
            debug.error("PolicyUtils.getAttributeValues() : "
                +"missing element " + VALUE);
            return null;
        }

        Iterator nodes = nodeSet.iterator();
            
        HashSet values = new HashSet();

        while (nodes.hasNext()) {
            Node node = (Node)nodes.next();
            String value = XMLUtils.getValueOfValueNode(node);
            if (value != null) {
                values.add(value);
            }
            else {
                values.add("");
            }
        }
        return values;
    }



    /**
     *  Converts a map which stores a set of 
     *  environment parameters into its XML string representation.
     *  @param envMap a map respresents a collection of the parameters 
     *  @return its XML string representation
     */

    public static String envParametersToXMLString(Map envMap)
    {
        StringBuilder xmlSB = new StringBuilder(1000);
        
        xmlSB.append('<').append(ENV_PARAMETERS).append('>').append(CRLF);

        Set keySet = envMap.keySet();
        Iterator keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String name = (String)keyIter.next();
            Set values = (Set)envMap.get(name);
            xmlSB.append(attributeValuePairToXMLString(name, values));
        }
        xmlSB.append("</").append(ENV_PARAMETERS).append('>').append(CRLF);
        return xmlSB.toString();
    } 

    /**
     *  Converts a set which stores a set of 
     *  response attribute names into its XML string representation.
     *  @param attrs a set of response attribute names
     *  @return XML string representation of set of attributes
     */

    public static String responseAttributesToXMLString(Set attrs)
    {
        StringBuilder xmlSB = new StringBuilder(1000);
        
        xmlSB.append("<" + GET_RESPONSE_DECISIONS + ">" + CRLF);

        Iterator names = attrs.iterator(); 
        while (names.hasNext()) {
            String name = (String)names.next();
            xmlSB.append("<").append(ATTRIBUTE).append(" ").append(ATTRIBUTE_NAME).append("=\"").
                    append(XMLUtils.escapeSpecialCharacters(name)).append("\"/>").append(CRLF);
        }
        xmlSB.append("</" + GET_RESPONSE_DECISIONS + ">" + CRLF);
        return xmlSB.toString();
    } 

    /**
     *  Converts a map 
     *  to its XML string representation.
     *  @param envMap a map that has String valued keys. Value corresponding
     *         to each key should be a set of String(s).
     *  @return its XML string representation of env map
     */

    public static String mapToXMLString(Map envMap)
    {
        StringBuilder xmlSB = new StringBuilder(1000);
        Set keySet = envMap.keySet();
        Iterator keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String name = (String)keyIter.next();
            Set values = (Set)envMap.get(name);
            xmlSB.append(attributeValuePairToXMLString(name, values));
        }
        return xmlSB.toString();
    } 


    /**
     *  Converts an attribute value pair into 
     *  its XML string representation.
     *  @param name the attribute name of the attribute value pair
     *  @param values the attribute values of the attribute value pair
     *  @return XML string representation of attribue value pair
     */

    public static String attributeValuePairToXMLString(String name, Set values)
    {
        StringBuilder xmlSB = new StringBuilder(1000);
     
        xmlSB.append('<').append(ATTRIBUTE_VALUE_PAIR).append('>').append(CRLF);
        xmlSB.append('<').append(ATTRIBUTE).append(' ').append(ATTRIBUTE_NAME).
                append("=\"").append(XMLUtils.escapeSpecialCharacters(name)).append("\"/>").append(CRLF);
        
        if ( values != null ) {
            Iterator itr = values.iterator();
            while (itr.hasNext()) {
                String value = (String)itr.next();
                xmlSB.append("<" + VALUE + ">" );
                xmlSB.append(XMLUtils.escapeSpecialCharacters(value));
                xmlSB.append("</" + VALUE + ">" + CRLF);
            }
        }

        xmlSB.append("</" + ATTRIBUTE_VALUE_PAIR + ">" + CRLF);

        return xmlSB.toString();
    }

    /**
     * Return a quoted string
     * Surrounds a string on either side with double quote and returns
     * the quoted string
     * @param s string to be quoted
     * @return quoted string
     */
    public static String quote(String s) {
        if ( s == null ) {
            s= "";
        }
        return "\"" + s + "\"";
    }

    /**
     * Return a quoted string, quoting an <code>int</code>.
     * Converts an <code>int</code> to string and quotes it on either side 
     * with double quote and returns the quoted string
     * @param i <code>int</code> to be quoted
     * @return quoted string
     */
    public static String quote(int i) {
        return quote(Integer.toString(i));
    }

    /**
     * Return a quoted string, quoting a <code>long</code>.
     * Converts a <code>long</code> to string and quotes it 
     * on either side 
     * with double quote and returns the quoted string
     * @param l <code>long</code> to be quoted
     * @return quoted string
     */
    public static String quote(long l) {
        return quote(Long.toString(l));
    }

    /**
     * Logs an access message
     * @param msgIdName name of message id
     * @param data array of data to be logged
     * @param token session token of the user who did the operation
     * that triggered this logging
     */
    public static void logAccessMessage(
        String msgIdName,
        String data[],
        SSOToken token
    ) throws SSOException {
        logAccessMessage(msgIdName, data, token, null);
    }

    public static void logAccessMessage(
        String msgIdName,
        String data[],
        SSOToken token,
        String serviceType
    ) throws SSOException {
        try {
            if (msgProvider == null) {
                msgProvider = MessageProviderFactory.getProvider("Policy");
            }
        } catch (IOException e) {
            debug.error("PolicyUtils.logAccessMessage()", e);
            debug.error("PolicyUtils.logAccessMessage():" 
                    + "disabling logging");
            logStatus = false;
        }
        if ((accessLogger != null) && (msgProvider != null)) {
            LogRecord lr = msgProvider.createLogRecord(msgIdName, data, token);
            if (lr != null) {
                SSOToken ssoToken = (SSOToken)AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
                if (serviceType != null && serviceType.equals(
                        DelegationManager.DELEGATION_SERVICE)) {
                    delegationLogger.log(lr, ssoToken);
                } else {
                    accessLogger.log(lr, ssoToken);
                }
            }
        }
    }

    /**
     * Logs an error message
     * @param msgIdName name of message id
     * @param data array of data to be logged
     * @param token session token of the user who did the operation
     * that triggered this logging
     */
    public static void logErrorMessage(
        String msgIdName,
        String data[],
        SSOToken token
    ) throws SSOException {
        try {
            if (msgProvider == null) {
                msgProvider = MessageProviderFactory.getProvider("Policy");
            }
        } catch (IOException e) {
            debug.error("PolicyUtils.logErrorMessage()", e);
            debug.error("PolicyUtils.logAccessMessage():" 
                    + "disabling logging");
            logStatus = false;
        }
        if ((errorLogger != null) && (msgProvider != null)) {
            LogRecord lr = msgProvider.createLogRecord(msgIdName, data, token);
            if (lr != null) {
                SSOToken ssoToken = (SSOToken)AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
                errorLogger.log(lr, ssoToken);
            }
        }
    }

    /**
     * Returns the LDAP server host used by Access Manager SDK stored
     * in <code>serverconfig.xml</code> file.
     * For multiple hosts, the returned value is a space-delimited list
     * of hosts.
     *
     * @return the LDAP server host used by Access Manager SDK. Returns null
     *         if unable to get the host.
     */
    public static String getISDSHostName() {
        try {
            DSConfigMgr mgr = DSConfigMgr.getDSConfigMgr();
            return mgr.getHostName(DSConfigMgr.DEFAULT);
        } catch (LDAPServiceException e) {
            debug.error(
                "Unable to get LDAP server host from DSConfigMgr: ", e);
            return null;
        }
    }

    /**
     * Checks if the <code>hostName</code> is the same as
     * the one used by OpenSSO SDK.
     *
     * @param hostName host name to compared against AM SDK config store host
     *
     * @return true if <code>hostName</code> is the same as the one used by
     *         OpenSSO SDK, false otherwise
     * @throws PolicyException if host names comparision did not succeed
     */
    public static boolean isLocalDS(String hostName) throws PolicyException {
        if (hostName == null) {
            throw new PolicyException(ResBundleUtils.rbName,
                "invalid_ldap_server_host", null, null);
        }
        return (hostName.equalsIgnoreCase(PolicyConfig.ISDS_HOST));
    }

    /**
     * Constructs a search filter used in subject evaluation.
     * If aliasEnabled is true, the user aliases will also be used
     * to construct the search filter.
     *
     * @param token SSO token
     * @param userRDNAttrName naming attribute
     * @param userName the value of the user name
     * @param aliasEnabled if true, user alias list will be used to construct
     *        the search filter
     *
     * @return search filter
     *
     * @throws SSOException if there is error when trying to retrieve
     *         token properties
     */
    public static String constructUserFilter(SSOToken token,
        String userRDNAttrName, String userName, boolean aliasEnabled)
        throws SSOException {

        StringBuilder userFilter = new StringBuilder();
        if (aliasEnabled) {
            String principalsString = token.getProperty("Principals");
            if (principalsString != null) {
                StringTokenizer st = new StringTokenizer(principalsString, "|");
                while (st.hasMoreTokens()) {
                    String principalName = (String) st.nextToken();
                    DN ldapDN = new DN(principalName);
                    if (ldapDN.isDN()) {
                        String[] components = ldapDN.explodeDN(true);
                        if (components == null || components.length < 1) {
                            continue;
                        }
                        String userID = components[0];
                            
                        if (!userID.equalsIgnoreCase(userName)) {
                            userFilter.append("(").append(userRDNAttrName)
                                .append("=").append(userID).append(")");
                        }
                    }
                }
            }
        }
        if (userFilter.length() == 0) {
            // if alias is disabled or no alias found from token
            userFilter.append("(").append(userRDNAttrName)
                .append("=").append(userName).append(")");
        } else {
            userFilter.insert(0, "(|");
            userFilter.append("(").append(userRDNAttrName)
                .append("=").append(userName).append("))");
        }
        if (debug.messageEnabled()) {
            debug.message(
                "PolicyUtils.constructUserFilter(): filter: " +
                    userFilter.toString());
        }
        return userFilter.toString();
    }

    /**
     * Removes policy rules defined for a service.
     * All the policy rules defined for a service in the system 
     * are removed.
     * @param token session token of the user doing the operation
     * @param serviceName name of the service
     */
    public static void removePolicyRules(SSOToken token ,String serviceName) 
            throws SSOException,AMException {
        try {
             AMStoreConnection dpStore = new AMStoreConnection(token);
             PolicyManager pm = new PolicyManager(token);
             String org = pm.getOrganizationDN();
             AMOrganization rootOrg 
                    = (AMOrganization)dpStore.getOrganization(org);
             String dn,policyName,ruleName; 
             DN rootDN,tmpDN;
             Set policyNames;
             Policy p;
             Rule rule,ruleDeleted;
             Iterator iter,ruleItr,levelItr;
             Map policyDNs = new HashMap();
             Map levelDNs = new HashMap(); 
             TreeMap sortedDNs; 
               
             rootDN = new DN(SMSEntry.getRootSuffix());
             Map avPair = new HashMap();
             Set value = new HashSet(); 
             value.add("iPlanetAMPolicyConfigService");
             avPair.put(AMConstants.SERVICE_STATUS_ATTRIBUTE,value);
             Set subOrgs = null;
             subOrgs = rootOrg.searchSubOrganizations(
                               "*",avPair,AMConstants.SCOPE_SUB);
     
             for (Iterator iterOrg = subOrgs.iterator(); iterOrg.hasNext();) {
                  dn = (String)iterOrg.next();
                  PolicyManager pmSubOrg = new PolicyManager(token,dn);
                  policyNames = pmSubOrg.getPolicyNames(); 
                  iter = policyNames.iterator();
                  while (iter.hasNext()) {
                         policyName = (String)iter.next();
                         p = pmSubOrg.getPolicy(policyName);
                         if (!p.isReferralPolicy()) {
                             ruleItr = p.getRuleNames().iterator();
                             while (ruleItr.hasNext()) {
                                    ruleName = (String) ruleItr.next();
                                    rule = p.getRule(ruleName);
                                    if ((rule.getServiceTypeName()) 
                                            .equalsIgnoreCase(serviceName)){
                                        if (PolicyManager.
                                            debug.messageEnabled()) 
                                        {
                                            debug.message(
                                            "PolicyUtils.removePolicyRules():"+
                                            "policy: " + policyName +",rule: "
                                            +ruleName);
                                        }
                                        ruleDeleted = p.removeRule(ruleName);
                                        if (ruleDeleted != null ) {
                                            pmSubOrg.replacePolicy(p);
                                        }
                                    }
                              }
            
                          } else {
                              //store the policies corresponding to DNs 
                              if(policyDNs.containsKey(dn)) {
                                 ((Vector)policyDNs.get(dn)).add(policyName); 
                              } else {
                                 Vector policies = new Vector();
                                 policies.add(policyName);
                                 policyDNs.put(dn,policies);
                              }
                              //store DNs corresponding to levels wrt root
                              tmpDN = new DN (dn);
                              String levelDiff = String.valueOf(rootDN
                                   .countRDNs()-tmpDN.countRDNs());
                              if(levelDNs.containsKey(levelDiff)) {
                                 ((Vector)levelDNs.get(levelDiff)).add(dn);
                              } else {
                                  Vector DNs = new Vector ();
                                  DNs.add(dn);
                                  levelDNs.put(levelDiff,DNs);
                              }
                          }
                  }
             }

             sortedDNs = new TreeMap(levelDNs);
             levelItr = sortedDNs.keySet().iterator();
             while (levelItr.hasNext()) {
                    String level = (String)levelItr.next();
                    Vector vDNs =  (Vector)sortedDNs.get(level);
                    for (int i = 0; i < vDNs.size(); i++){
                         dn = (String) vDNs.get(i);
                         PolicyManager pmRefOrg = new PolicyManager(token,dn);  
                         Vector vPolicies = (Vector)policyDNs.get(dn);  
                         for (int j = 0;j < vPolicies.size(); j++) {
                              policyName = (String) vPolicies.get(j);
                              p = pmRefOrg.getPolicy(policyName);
                              ruleItr = p.getRuleNames().iterator();
                              while (ruleItr.hasNext()) {
                                     ruleName = (String) ruleItr.next();
                                     rule = p.getRule(ruleName);
                                     if ((rule.getServiceTypeName())
                                        .equalsIgnoreCase(serviceName)) {
                                         if (debug.messageEnabled(
                                            )) {
                                             debug.message(
                                             "PolicyUtils.removePolicyRules():"+
                                             "referral policy: " + policyName +
                                             ",rule: "+ruleName);
                                          }
                                         ruleDeleted = p.removeRule(ruleName);
                                         if (ruleDeleted != null ) {
                                             pmRefOrg.replacePolicy(p);
                                         }
                                      }
                              }
                         }
                     }
              
             }
            
            
        } catch (PolicyException pe){
             debug.error(
                 "PolicyUtils.removePolicyRules():" ,pe);
       }
    }

    /**
     * Parses a string into a set using the specified delimiter
     * @param str string to be parsed
     * @param delimiter delimiter used in the string
     * @return the parsed set
     */
    public static Set delimStringToSet(String str, String delimiter) {
        Set valSet = new HashSet();
        StringTokenizer st = new StringTokenizer(str, delimiter);
        while (st.hasMoreTokens()) {
            valSet.add(st.nextToken().trim());
        }
        return valSet;
    }

    /**
     * Returns a display string for an LDAP distinguished name.
     *
     * @param strDN distinguished name.
     * @return display string for the LDAP distinguished name.
     */
    public static String getDNDisplayString(String strDN) {
        String displayString = null;
        /*
         * Given a value of cn=Accounting Managers,ou=groups,dc=iplanet,dc=com,
         * this method returns com > iplanet > groups > Accounting Managers
         */
        DN dn = new DN(strDN);
        if (!dn.isDN()) {
            displayString = strDN;
        } else {
            StringBuilder buff = new StringBuilder(1024);
            List rdns = dn.getRDNs();
            for (ListIterator iter = rdns.listIterator(rdns.size());
                iter.hasPrevious();) {
                RDN rdn = (RDN) iter.previous();
                buff.append(rdn.getValues()[0]);

                if (iter.hasPrevious()) {
                    buff.append(" > ");
                }
            }
            displayString = buff.toString();
        }
        return displayString;
    }
   
    /**
     * Parses an XML string representation of policy advices and 
     * returns a Map of advices.  The keys of returned map would be advice name 
     * keys. Each key is a String object. The values against each key is a 
     * Set of String(s) of advice values
     *
     * @param advicesXML XML string representation of policy advices conforming
     * to the following DTD. The input string may not be validated against the 
     * dtd for performance reasons.  

         <!-- This DTD defines the Advices that could be included in
        ActionDecision nested in PolicyDecision. Agents would post this
        Advices to authentication service URL

        Unique Declaration name for DOCTYPE tag:
                  "iPlanet Policy Advices Interface 1.0 DTD"
        -->


        <!ELEMENT    AttributeValuePair    (Attribute, Value*) >


        <!-- Attribute defines the attribute name i.e., a configuration
             parameter.
        -->
        <!ELEMENT    Attribute     EMPTY >
        <!ATTLIST    Attribute 
              name    NMTOKEN    #REQUIRED 
        >


        <!-- Value element represents a value string.
        -->
        <!ELEMENT    Value    ( #PCDATA ) >


        <!-- Advices element provides some additional info which may help the 
             client could use to influence the policy decision
        -->
        <!ELEMENT    Advices   ( AttributeValuePair+ ) >

     *
     * @return the map of policy advices parsed from the passed in advicesXML
     *         If the passed in advicesXML is null, null would be returned

     * @throws PolicyException if there is any error parsing the passed in
     *                         advicesXML
     */
    public static Map parseAdvicesXML(String advicesXML) 
            throws PolicyException {

        if(debug.messageEnabled()) {
            debug.message("PolicyUtils.parseAdvicesXML():"
                    + " entering, advicesXML= " + advicesXML);
        }

        Map advices = null;
        if (advicesXML != null) {
            Document document = XMLUtils.toDOMDocument(advicesXML, 
                    debug);
            if (document != null) {
                Node advicesNode 
                        = XMLUtils.getRootNode(document, ADVICES_TAG_NAME);
                if (advicesNode != null) {
                    advices = XMLUtils.parseAttributeValuePairTags(
                            advicesNode);
                } else {
                    if(debug.messageEnabled()) {
                        debug.message(
                                "PolicyUtils.parseAdvicesXML():"
                                + " advicesNode is null");
                    }
                }
            } else {
                if(debug.messageEnabled()) {
                    debug.message(
                            "PolicyUtils.parseAdvicesXML():"
                            + " document is null");
                }
            }
        }

        if(debug.messageEnabled()) {
            debug.message("PolicyUtils.parseAdvicesXML():"
                    + " returning, advices= " + advices);
        }

        return advices;
    }

   
    /** 
     * Returns XML string representation of a <code>Map</code> of policy advices
     * @param advices <code>Map</code> of policy advices
     * @return XML string representation of policy advices
     * @throws PolicyException if there is any error while converting
     */
    public static String advicesToXMLString(Map advices) 
            throws PolicyException {

        String advicesXML = null;
        StringBuilder sb = new StringBuilder(200);
        sb.append(ADVICES_START_TAG).append(NEW_LINE);
        if (advices != null) {
            sb.append(mapToXMLString(advices));
        }
        sb.append(ADVICES_END_TAG).append(NEW_LINE);
        advicesXML = sb.toString();

        return advicesXML;
    }

    /**
     * Checks if principal name and uuid are same in the session
     * @param token session token
     * @return <code>true</code> if the principal name and uuid 
     * are same in the session. Otherwise, <code>false</code>
     * @throws SSOException if the session token is not valid
     */
    static public boolean principalNameEqualsUuid(SSOToken token) 
            throws SSOException {
        String principalName = token.getPrincipal().getName();
        String uuid = token.getProperty(
                com.sun.identity.shared.Constants.UNIVERSAL_IDENTIFIER);
        return principalName.equals(uuid);
    }
    
    /**
     * Creates policy objects given an input stream of policy XML which
     * confines to <code>com/sun/identity/policy/policyAdmin.dtd</code>.
     *
     * @param pm Policy manager.
     * @param xmlPolicies Policy XML input stream.
     * @throws PolicyException if policies cannot be created.
     * @throws SSOException if Single Sign On token used to create policy
     *         manager is no longer valid.
     */
    public static void createPolicies(PolicyManager pm, InputStream xmlPolicies)
            throws PolicyException, SSOException {
        // Overload common method
        createOrReplacePolicies(pm, xmlPolicies, false);
    }

    /**
     * Creates or replaces policy objects given an input stream of policy XML 
     * which confines to <code>com/sun/identity/policy/policyAdmin.dtd</code>.
     *
     * @param pm Policy manager.
     * @param xmlPolicies Policy XML input stream.
     * @param replace True if the policies should be replaced, otherwise create.
     * @throws PolicyException if policies cannot be updated.
     * @throws SSOException if Single Sign On token used to update policy
     *         manager is no longer valid.
     */
    public static void createOrReplacePolicies(PolicyManager pm, InputStream xmlPolicies, boolean replace)
        throws PolicyException, SSOException {
        try {
            DocumentBuilder builder = XMLUtils.getSafeDocumentBuilder(true);
            builder.setErrorHandler(new ValidationErrorHandler());
            Element topElement =builder.parse(xmlPolicies).getDocumentElement();
            NodeList childElements = topElement.getChildNodes();
            int len = childElements.getLength();
            for (int i = 0; i < len; i++) {
                Node node = childElements.item(i);
                if ((node != null) && (node.getNodeType() == Node.ELEMENT_NODE)
                ) {
                    if (replace) {
                        pm.replacePolicy(new Policy(pm, node));
                    } else {
                        pm.addPolicy(new Policy(pm, node));
                    }
                }
            }
        } catch (IOException e) {
            throw new PolicyException(e);
        } catch (SAXException e) {
            throw new PolicyException(e);
        } catch (ParserConfigurationException e) {
            throw new PolicyException(e);
        }
    }

    /** 
     * Returns deep copy of a <code>Map</Map>
     * The passed in <code>Map</code> should have <code>String</code> 
     * object as keys and <code>Set</code> of <code>String</code> 
     * objects as values 
     *
     * @param map <code>Map</code> that needs to be copied
     * @return a deep copy of passed in <code>Map</code>
     */
    public static Map cloneMap(Map map) {
        Map clonedMap = null;
        if (map != null) {
            clonedMap = new HashMap();
            Set keys = map.keySet();
            Iterator keysIter = keys.iterator();
            while (keysIter.hasNext()) {
                Object key = keysIter.next();
                Object value = map.get(key);
                if (value instanceof Set) {
                    Set values = new HashSet();
                    values.addAll((Set)value);
                    clonedMap.put(key, values);
                } else {
                    clonedMap.put(key, value);
                }
            }
        }
        return clonedMap;
    }

    public static String encrypt(String plainText) {
        if (plainText != null) {
            return Crypt.encode(plainText);
        } else {
            return plainText;
        }
    }

    public static String decrypt(String encryptedText) {
        if (encryptedText != null) {
            return Crypt.decode(encryptedText);
        } else {
            return encryptedText;
        }
    }

}
