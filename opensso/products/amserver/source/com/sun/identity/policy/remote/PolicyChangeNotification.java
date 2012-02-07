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
 * $Id: PolicyChangeNotification.java,v 1.4 2008/06/25 05:43:53 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.remote;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.policy.ResBundleUtils;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.w3c.dom.Node;

/**
 * This <code>PolicyChangeNotification</code> class represents a 
 * PolicyChangeNotification XML document. The PolicyChangeNotification
 *  DTD is defined as the following:
 * <p>
 * <pre>
 * <!-- PolicyNotification element specifies a policy notification.
 *      There are two types of notifications that are supported.
 *      Policy Change Notification and Subject Change Notification.
 * -->
 *
 * <!ELEMENT    PolicyNotification    ( PolicyChangeNotification
 *                                     | SubjectChangeNotification ) >
 *
 *
 * <!-- PolicyChangeNotification element sends a notification to the
 *      client if a policy regarding the service has been changed.
 *      The attribute serviceName specifies the service name.
 *      The attribute type specifies the policy change type.
 *      The sub-element ResourceName specifies the name of the resource
 *      which is affected by the policy change.
 * -->
 *
 * <!ELEMENT    PolicyChangeNotification    ( ResourceName* ) >
 * <!ATTLIST    PolicyChangeNotification
 *     serviceName    NMTOKEN    #REQUIRED
 *     type           (added | modified | deleted)  "modified"
 * >
 *
 *
 * <!-- ResourceName element respresents a resource name
 * -->
 *
 * <!ELEMENT    ResourceName    ( #PCDATA ) >
 *
 * </pre>
 * <p>
 */

public class PolicyChangeNotification {

    static final String SERVICE_NAME = "serviceName";
    static final String POLICY_CHANGE_TYPE = "type";
    static final String RESOURCE_NAME = "ResourceName";
    static final String ADDED = "added";
    static final String MODIFIED = "modified";
    static final String DELETED = "deleted";
    static final String CRLF = PolicyService.CRLF;
    static Debug debug = PolicyService.debug;

    private String serviceName;
    private String changeType;
    private Set resourceNames = null;

    /** 
     * Default constructor for <code>PolicyChangeNotification</code>.
     */
    PolicyChangeNotification() {
        changeType = MODIFIED;
    }


    /**
     * Returns the service name to which this notification is sent.
     *
     * @return service name
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Sets the service name to which this notification is sent.
     *
     * @param sn Service name.
     */
    public void setServiceName(String sn) {
        serviceName = sn;
    }

    /**
     * Returns the policy change type.
     *
     * @return the policy change type.
     */
    public String getPolicyChangeType() {
        return changeType;
    }

    /**
     * Sets the policy change type.
     *
     * @param type Policy change type.
     */
    public void setPolicyChangeType(String type) {
        changeType = type;
    }

    /**
     * Returns the resource names.
     *
     * @return resource names.
     */
    public Set getResourceNames() {
        return resourceNames;
    }

    /**
     * Sets the resource names.
     *
     * @param names Resource names.
     */
    void setResourceNames(Set names) {
        resourceNames = names;
    }

    /**
     * Returns <code>PolicyChangeNotification</code> object constructed from
     * the XML.
     *
     * @param pNode the XML DOM node for the
     *        <code>PolicyChangeNotification<code> object.
     * @return constructed <code>PolicyChangeNotification</code> object.
     */
    public static PolicyChangeNotification parseXML(Node pNode)
        throws PolicyEvaluationException {
        PolicyChangeNotification pcn = new PolicyChangeNotification();

        String attr = XMLUtils.getNodeAttributeValue(pNode, SERVICE_NAME);

        if (attr == null) {
            debug.error("PolicyChangeNotification: missing attribute " +
                SERVICE_NAME);
            String objs[] = { SERVICE_NAME };
            throw new PolicyEvaluationException(ResBundleUtils.rbName,
                "missing_attribute", objs, null); 
        }

        pcn.setServiceName(attr);

        attr = XMLUtils.getNodeAttributeValue(pNode, POLICY_CHANGE_TYPE);

        if (attr == null) {
            debug.error("PolicyChangeNotification: missing attribute " +
                POLICY_CHANGE_TYPE);
            String objs[] = { POLICY_CHANGE_TYPE };
            throw new PolicyEvaluationException(ResBundleUtils.rbName, 
                "missing_attribute", objs, null); 
        }

        pcn.setPolicyChangeType(attr);
        Set nodeSet = XMLUtils.getChildNodes(pNode, RESOURCE_NAME);

        if (nodeSet == null) {
            if (debug.messageEnabled()) { 
                debug.message("PolicyChangeNotification.parseXML: " +
                    " no resource name specified");     
            }
            return pcn;
        }

        Set resNames = new HashSet();

        for (Iterator nodes = nodeSet.iterator(); nodes.hasNext(); ) {
            Node node = (Node)nodes.next();
            String name = XMLUtils.getValueOfValueNode(node);
            if (name != null) {
                resNames.add(name);
            }
        }

        pcn.setResourceNames(resNames);
        return pcn; 
    }

    /**
     * Returns a XML representation of this object.
     *
     * @return a XML representation of this object.
     */
    public String toXMLString() {
        StringBuilder xmlsb = new StringBuilder(200);
     
        xmlsb.append("<")
             .append(PolicyNotification.POLICY_CHANGE)
             .append(" ");
        xmlsb.append(SERVICE_NAME)
             .append("=\"")
             .append(serviceName)
             .append("\" ");
        xmlsb.append(POLICY_CHANGE_TYPE)
             .append("=\"")
             .append(changeType)
             .append("\">")
             .append(CRLF);
        
        if (resourceNames != null) {
            for (Iterator iter = resourceNames.iterator(); iter.hasNext(); ) {
                String resName = (String)iter.next();
                xmlsb.append("<")
                     .append(RESOURCE_NAME)
                     .append(">");
                xmlsb.append(XMLUtils.escapeSpecialCharacters(resName));
                xmlsb.append("</")
                     .append(RESOURCE_NAME)
                     .append(">")
                     .append(CRLF); 
            }
        }

        xmlsb.append("</")
             .append(PolicyNotification.POLICY_CHANGE)
             .append(">")
             .append(CRLF);
        return xmlsb.toString();
    }
}
