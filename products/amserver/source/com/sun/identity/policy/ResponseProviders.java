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
 * $Id: ResponseProviders.java,v 1.4 2008/06/25 05:43:45 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy;

import java.util.*;

import org.w3c.dom.*;

import com.sun.identity.policy.interfaces.ResponseProvider;
import com.iplanet.sso.*;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The class <code>ResponseProviders</code> provides methods to maintain
 * a collection of <code>ResponseProvider</code> objects that 
 * apply to a policy. This class provides methods to add, replace
 * and remove <code>ResponseProvider</code> objects from this 
 * <code>ResponseProviders</code>.
 * The <code>Policy</code> object provides methods to set
 * <code>ResponseProviders</code>, which identifies response attributes that 
 * apply to the policy.
 */
public class ResponseProviders {

    private String name;
    private String description;
    private Map providers = new HashMap();

    /**
     * Constructs <code>ResponseProviders</code> object
     */
    ResponseProviders() {
    }

    /**
     * Constructor used by <code>Policy</code> to obtain
     * an instance of <code>ResponseProviders</code> from the
     * XML document
     *
     * @param rpm Response Provider Type Manager.
     * @param responseProvidersNode node that represents the Response Providers.
     */
    protected ResponseProviders(ResponseProviderTypeManager rpm, 
	Node responseProvidersNode) throws InvalidFormatException, 
	NameNotFoundException, PolicyException {
	// Check if the node name is PolicyManager.POLICY_RESP_PROVIDERS_NODE
	if (!responseProvidersNode.getNodeName().equalsIgnoreCase(
	    PolicyManager.POLICY_RESP_PROVIDERS_NODE)) {
	    if (PolicyManager.debug.warningEnabled()) {
		PolicyManager.debug.warning(
		    "invalid response providers xml blob given to construct "
			+"response providers");
	    }
	    throw (new InvalidFormatException(ResBundleUtils.rbName,
		"invalid_xml_resp_providers_root_node", null, "",
		PolicyException.RESPONSE_PROVIDER_COLLECTION));
	}

	// Get the responseProviders name
	if ((name = XMLUtils.getNodeAttributeValue(responseProvidersNode,
	    PolicyManager.NAME_ATTRIBUTE)) == null) {
	    name = "ResponseProviders:" 
		+ ServiceTypeManager.generateRandomName();
	}

	// Get the description
	if ((description = XMLUtils.getNodeAttributeValue(responseProvidersNode,
	    PolicyManager.DESCRIPTION_ATTRIBUTE)) == null) {
	    description = "";
	}

	// Get individual response providers
	Iterator providerNodes = XMLUtils.getChildNodes(
	    responseProvidersNode, PolicyManager.RESP_PROVIDER_POLICY).
	    iterator();
	while (providerNodes.hasNext()) {
	    Node providerNode = (Node) providerNodes.next();
	    String providerType = XMLUtils.getNodeAttributeValue(
		providerNode, PolicyManager.TYPE_ATTRIBUTE);
	    if (providerType == null) {
		if (PolicyManager.debug.warningEnabled()) {
		    PolicyManager.debug.warning("provider type is null");
		}
		throw (new InvalidFormatException(
		    ResBundleUtils.rbName,
		    "invalid_xml_resp_provider_root_node", null, "",
		    PolicyException.RESPONSE_PROVIDER_COLLECTION));
	    }
	    // Get the friendly name given to response provider
	    String providerName = XMLUtils.getNodeAttributeValue(
		providerNode, PolicyManager.NAME_ATTRIBUTE);

	    // Construct the ResponseProvider object
	    ResponseProvider respProvider  = 
		rpm.getResponseProvider(providerType);

	    // Get and set the properties
	    Map properties = new HashMap();
	    NodeList attrValuePairNodes = providerNode.getChildNodes();
	    int numAttrValuePairNodes = attrValuePairNodes.getLength();
	    for (int j = 0; j < numAttrValuePairNodes; j++) {
		Node attrValuePairNode = attrValuePairNodes.item(j);
		Node attributeNode 
			= XMLUtils.getChildNode(attrValuePairNode, 
			PolicyManager.ATTR_NODE);
		if ( attributeNode != null ) {
		    String name = XMLUtils.getNodeAttributeValue(attributeNode,
			    PolicyManager.NAME_ATTRIBUTE);
		    Set values = XMLUtils.getAttributeValuePair(
			    attrValuePairNode);
		    if ( ( name != null ) && ( values != null ) ) {
			properties.put(name, values);
		    }
		}
	    }
	    respProvider.setProperties(properties);

	    // Add the provider to responseProviders collection
	    addResponseProvider(providerName, respProvider);
	}
    }

    /**
     * Constructor to obtain an instance of <code>ResponseProviders</code>
     * to hold collection of responseProviders represented as
     * <code>ResponseProvider</code>
     *
     * @param name name for the collection of <code>ResponseProvider</code>
     * @param description user friendly description for
     * the collection of <code>ResponseProvider</code>  
     */
    public ResponseProviders(String name, String description) {
	this.name = (name == null) ? 
	    ("ResponseProviders:" + ServiceTypeManager.generateRandomName()) : 
		name;
	this.description = (description == null) ?
	    "" : description;
    }

    /**
     * Returns the name of this object
     *
     * @return name of this object
     */
    public String getName() {
	return (name);
    }

    /**
     * Returns the description of this object
     *
     * @return description of this object
     */
    public String getDescription() {
	return (description);
    }

    /**
     * Sets the name of this object
     *
     * @param name name for this object
     */
    public void setName(String name) {
	this.name = (name == null) ?
	    ("ResponseProviders:" + ServiceTypeManager.generateRandomName()) : 
		name;
    }

    /**
     * Sets the description of this object
     *
     * @param description description for this object
     */
    public void setDescription(String description) {
	this.description = (description == null) ?
	    "" : description;
    }

    /**
     * Returns the names of <code>ResponseProvider</code> objects
     * contained in this object.
     *
     * @return names of <code>ResponseProvider</code> contained in
     * this object
     */
    public Set getResponseProviderNames() {
	return (providers.keySet());
    }

    /**
     * Returns the <code>ResponseProvider</code> object associated
     * with the given <code>responseProvider</code> name.
     *
     * @param responseProviderName name of the <code>ResponseProvider</code> 
     * object
     *
     * @return <code>ResponseProvider</code> object corresponding to 
     * <code>responseProvider</code> name
     *
     * @exception NameNotFoundException if a <code>ResponseProvider</code>
     * with the given name is not present
     */
    public ResponseProvider getResponseProvider(String responseProviderName)
	throws NameNotFoundException {
	ResponseProvider answer = 
	    (ResponseProvider) providers.get(responseProviderName);
	if (answer == null) {
	    String[] objs = { responseProviderName };
	    throw (new NameNotFoundException(ResBundleUtils.rbName,
		"name_not_present", objs, 
		responseProviderName, 
		PolicyException.RESPONSE_PROVIDER_COLLECTION));
	
	}
	return (answer);
    }

    /**
     * Adds a <code>ResponseProvider</code> object to this instance
     * of <code>ResponseProviders</code>. Since the name is not provided it
     * will be dynamically assigned such that it is unique within
     * this instance of the <code>ResponseProviders</code> . However if a
     * <code>ResponseProvider</code> entry with the same name already 
     * exists in the <code>ResponseProviders</code>,
     * <code>NameAlreadyExistsException</code>  will be thrown.
     *
     * @param responseProvider instance of the <code>ResponseProvider</code> 
     * object to be added 
     *
     * @exception NameAlreadyExistsException if a
     * <code>ResponseProvider</code> object is present with the same name
     */
    public void addResponseProvider(ResponseProvider responseProvider)
	throws NameAlreadyExistsException {
	addResponseProvider(null, responseProvider);
    }

    /**
     * Adds a <code>ResponseProvider</code> object to this instance
     * of <code>ResponseProviders</code>. If another 
     * <code>ResponseProvider</code> with the 
     * same  name already exists in this object
     * <code>NameAlreadyExistsException</code> will be thrown.
     *
     * @param responseProviderName name for the <code>ResponseProvider</code> 
     * instance
     * @param responseProvider instance of the <code>ResponseProvider</code> 
     * object  to be added
     *
     * @exception NameAlreadyExistsException if a
     * <code>ResponseProvider</code> object is present with the same name 
     */
    public void addResponseProvider(String responseProviderName, 
	ResponseProvider responseProvider) throws NameAlreadyExistsException {
	if (responseProviderName == null) {
	    responseProviderName = "ResponseProvider:" +
		ServiceTypeManager.generateRandomName();
	}
	if (providers.containsKey(responseProviderName)) {
	    String[] objs = { responseProviderName };
	    throw (new NameAlreadyExistsException(ResBundleUtils.rbName,
		"name_already_present", objs,
		responseProviderName, 
		PolicyException.RESPONSE_PROVIDER_COLLECTION));
	}
	providers.put(responseProviderName, responseProvider);
    }

    /**
     * Replaces an existing responseProvider object having the same name
     * with the new one. If a <code>ResponseProvider</code> with the given
     * name does not exist, <code>NameNotFoundException</code>
     * will be thrown.
     *
     * @param responseProviderName name for the responseProvider instance
     * @param responseProvider instance of the responseProvider object that will
     * replace another responseProvider object having the given name
     *
     * @exception NameNotFoundException if a responseProvider instance
     * with the given name is not present
     */
    public void replaceResponseProvider(String responseProviderName, 
	ResponseProvider responseProvider) throws NameNotFoundException {
	if (!providers.containsKey(responseProviderName)) {
	    String[] objs = { responseProviderName };
	    throw (new NameNotFoundException(ResBundleUtils.rbName,
		"name_not_present", objs,
		responseProviderName, 
		PolicyException.RESPONSE_PROVIDER_COLLECTION));
	}
	providers.put(responseProviderName, responseProvider);
    }

    /**
     * Removes the <code>ResponseProvider</code> object identified by
     * responseProvider's name.
     * If a responseProvider instance with the given
     * name does not exist, the method will return silently.
     *
     * @param responseProviderName name of the responseProvider instance that
     * will be removed from the responseProviders collection
     * @return the responseProvider that was just removed
     */
    public ResponseProvider removeResponseProvider(
	String responseProviderName) 
    {
	return (ResponseProvider)providers.remove(responseProviderName);
    }
 
    /**
     * Removes the <code>ResponseProvider</code> object identified by
     * object's <code>equals</code> method. If a responseProvider instance
     * does not exist, the method will return silently.
     *
     * @param responseProvider responseProvider object that
     * will be removed from the responseProviders collection
     * @return the responseProvider that was just removed
     */
    public ResponseProvider removeResponseProvider(
	ResponseProvider responseProvider) 
    {
	String responseProviderName = getResponseProviderName(responseProvider);
	if (responseProviderName != null) {
	    return (ResponseProvider) removeResponseProvider(
		responseProviderName);
	}
        return null;
    }

    /**
     * Returns the name associated with the given responseProvider object.
     * It uses the <code>equals</code> method on the responseProvider
     * to determine equality. If a responseProvider instance that matches
     * the given responseProvider object is not present, the method
     * returns <code>null</code>.
     *
     * @param responseProvider responseProvider object for which this method 
     * will return its associated name
     *
     * @return user friendly name given to the responseProvider object;
     * <code>null</code> if not present
     */
    public String getResponseProviderName(ResponseProvider responseProvider) {
	String responseProviderName = null;
	Iterator items = providers.keySet().iterator();
	while (items.hasNext()) {
	    responseProviderName = (String) items.next();
	    if (responseProvider.equals(providers.get(
		responseProviderName))) 
 	    {
		break;
	    }
	}
	return (responseProviderName);
    }

    /**
     * Checks if two <code>ResponseProviders</code> are identical.
     * Two responseProviders (or responseProviders collections) are identical 
     * only if both have the same set of <code>ResponseProvider</code> objects.
     *
     * @param o object against which this responseProviders object
     * will be checked for equality
     *
     * @return <code>true</code> if all the responseProviders match,
     * <code>false</code> otherwise
     */
    public boolean equals(Object o) {
	//TODO - This equals method has been aligned with
	// the logic for equals in Conditions.java but
        // dont think its giviging desired result by
        // running ResponseProviderTest.java, needs a revisit
	if (o instanceof ResponseProviders) {
	    ResponseProviders i = (ResponseProviders) o;
	    Iterator iter = providers.entrySet().iterator();
	    while (iter.hasNext()) {
		Object ss = ((Map.Entry) iter.next()).getValue();
		if (!i.providers.containsValue(ss)) {
		    return (false);
		}
	    }
	    return true;
	}
	return false;
    }

    /**
     * Returns a deep copy of this object with the identical
     * set of <code>ResponseProvider</code> objects
     *
     * @return a deep copy of this object 
     */
    public Object clone() {
	ResponseProviders answer = null;
	try {
	    answer = (ResponseProviders) super.clone();
	} catch (CloneNotSupportedException se) {
            answer = new ResponseProviders();
        }
	answer.name = name;
	answer.description = description;
	answer.providers = new HashMap();
	Iterator items = providers.keySet().iterator();
	while (items.hasNext()) {
	    Object item = items.next();
	    ResponseProvider responseProvider = (ResponseProvider) 
		providers.get(item);
	    answer.providers.put(item, responseProvider.clone());
	}
	return (answer);
    }

    /**
     * Returns response decision evalutating this object
     * The effective result is union of all attributes defined in all
     * <code>ResponseProvider</code> objects contained in this object
     *
     * @param token single sign on token of the user
     * @param env a map of key/value pairs containing any information 
     *            that could be used by each contraint to evaluate
     *            the allow/deny result
     * @return <code>Map</code> of attribute value pairs.
     *
     * @throws PolicyException if an error occured 
     * @throws SSOException if the token is invalid
     */
    Map getResponseProviderDecision(SSOToken token,Map env) 
            throws PolicyException, SSOException {
	HashMap attrsMap = new HashMap();
	Iterator items = providers.entrySet().iterator();
	while (items.hasNext()) {
	    ResponseProvider responseProvider = (ResponseProvider)
		((Map.Entry) items.next()).getValue();
	    Map respProviderMap = responseProvider.
		getResponseDecision(token, env);
            PolicyUtils.appendMapToMap(respProviderMap,attrsMap);
        }
	return attrsMap;
    }

    /**
     * Returns XML string representation of this object
     *
     * @return xml string representation of this object
     */
    public String toString() {
	return (toXML());
    }

    protected String toXML() {
	StringBuilder sb = new StringBuilder(100);
	sb.append("\n").append(RESPONSE_PROVIDERS_ELEMENT_BEGIN)
	    .append(XMLUtils.escapeSpecialCharacters(name))
            .append(RESPONSE_PROVIDERS_DESCRIPTION)
	    .append(XMLUtils.escapeSpecialCharacters(description))
            .append("\">");
	Iterator items = providers.keySet().iterator();
	while (items.hasNext()) {
	    String responseProviderName = (String) items.next();
	    ResponseProvider responseProvider = 
		(ResponseProvider) providers.get(responseProviderName);
	    sb.append("\n").append(RESPONSE_PROVIDER_ELEMENT)
		.append(XMLUtils.escapeSpecialCharacters(responseProviderName))
		.append(RESPONSE_PROVIDER_TYPE)
		.append(XMLUtils.escapeSpecialCharacters(
                        ResponseProviderTypeManager.
			responseProviderTypeName(responseProvider)))
		.append("\">\n");
	    // Add attribute values pairs
	    Map properties = responseProvider.getProperties();
	    if (properties != null) {
                sb.append(PolicyUtils.mapToXMLString(properties));
	    }
	    sb.append(RESPONSE_PROVIDER_ELEMENT_END);
	}
	sb.append("\n").append(RESPONSE_PROVIDERS_ELEMENT_END);
	return (sb.toString());		
    }

    /**
     * Returns the number of <code>ResponseProvider</code> elements in this
     * </code>ResponseProviders</code> object
     *
     * @return the number of <code>ResponseProvider</code> elements in this
     *           </code>ResponseProviders</code> object
     */
    int size() {
        return providers.size();
    }

    // Private variables to construct the XML document
    private static String RESPONSE_PROVIDERS_ELEMENT_BEGIN = 
	"<ResponseProviders name=\"";
    private static String RESPONSE_PROVIDERS_DESCRIPTION = 
	"\" description=\"";
    private static String RESPONSE_PROVIDERS_ELEMENT_END = 
	"</ResponseProviders>";
    private static String RESPONSE_PROVIDER_ELEMENT = 
	"<ResponseProvider name=\"";
    private static String RESPONSE_PROVIDER_TYPE = "\" type=\"";
    private static String RESPONSE_PROVIDER_ELEMENT_END = "</ResponseProvider>";
}
