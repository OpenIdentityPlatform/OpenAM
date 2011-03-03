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
 * $Id: PolicyNotification.java,v 1.3 2008/06/25 05:43:53 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.remote;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.policy.ResBundleUtils;
import org.w3c.dom.Node;

/**
 * This <code>PolicyNotification</code> class represents a PolicyNotification
 * XML document. The PolicyNotification DTD is defined as the following:
 * <p>
 * <pre>
 * <!-- PolicyNotification element specifies a policy notification.
 *      The attribute notificationId is used for identifying the
 *      notification.
 * -->
 *
 * <!ELEMENT    PolicyNotification    ( PolicyChangeNotification ) >
 * <!ATTLIST    PolicyNotification
 *     notificationId        CDATA        #REQUIRED
 * -->
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
public class PolicyNotification {

    static final String POLICY_CHANGE = "PolicyChangeNotification";
    static final String NOTIFICATION_ID = "notificationId";
    static final String CRLF = PolicyService.CRLF;
    static Debug debug = PolicyService.debug;

    public static final int POLICY_CHANGE_TYPE = 1;

    private int notificationType = 0;
    private String notificationId = "0";
    private PolicyChangeNotification policyChangeNotification = null;

    /** 
     * Default constructor for <code>PolicyNotification</code>
     */
    PolicyNotification() {
    }

    /**
     * Returns the notification type of this notification.
     *
     * @return notification type.
     */
    public int getNotificationType() {
        return notificationType;
    }

    /**
     * Sets the notification type of this notification.
     *
     * @param nt the notification type.
     */ 
    void setNotificationType(int nt) {
        notificationType = nt;
    }

    /**
     * Returns the notification Id of this notification.
     *
     * @return notification Id.
     */
    public String getNotificationId() {
        return notificationId;
    }

    /**
     * Sets the notification Id of this notification.
     *
     * @param nId the notification Id.
     */
    void setNotificationId(String nId) {
        notificationId = nId;
    }

    /**
     * Returns the notification Id of this notification.
     *
     * @return notification Id.
     */
    public PolicyChangeNotification getPolicyChangeNotification() {
        return policyChangeNotification;
    }

    /**
     * Sets the policy change notification.
     *
     * @param pcn the policy change notification.
     */
    void setPolicyChangeNotification(PolicyChangeNotification pcn) {
        policyChangeNotification = pcn;
    }

    /**
     * Returns a <code>PolicyNotification</code> object constructed from a XML.
     *
     * @param pNode the XML DOM node for the <code>PolicyNotification</code>
     *        object.
     * @return constructed <code>PolicyNotification</code> object.
     * @throws PolicyEvaluationException if <code>PolicyNotification</code>
     *         object cannot be constructed.
     */
    public static PolicyNotification parseXML(Node pNode)
        throws PolicyEvaluationException
    {
        PolicyNotification policyNotification = new PolicyNotification();
        String attr = XMLUtils.getNodeAttributeValue(pNode, NOTIFICATION_ID);

        if (attr == null) {
            debug.error("PolicyResponse.parseXML: missing attribute " +
                NOTIFICATION_ID);
            String objs[] = { NOTIFICATION_ID };
            throw new PolicyEvaluationException(ResBundleUtils.rbName,
                "missing_attribute", objs, null);
        }
        policyNotification.setNotificationId(attr);
       
        Node node = XMLUtils.getChildNode(pNode, POLICY_CHANGE);
        if (node != null) {
            PolicyChangeNotification pcn = PolicyChangeNotification.parseXML(
                node);
            policyNotification.setPolicyChangeNotification(pcn);
            policyNotification.setNotificationType(POLICY_CHANGE_TYPE);
            return policyNotification;
        }

        /* We reach here. This means the notification element is 
         * missing. Log error and return.
         */ 
        debug.error("PolicyNotification: missing either " +
            POLICY_CHANGE + " element in the xml");
        String objs[] = { POLICY_CHANGE };
        throw new PolicyEvaluationException(ResBundleUtils.rbName,
            "missing_element", objs, null);
    }


    /**
     * Returns a XML representation of this object.
     *
     * @return a XML representation of this object.
     */
    public String toXMLString() {
        StringBuilder xmlsb = new StringBuilder(1000);

        xmlsb.append("<")
             .append(PolicyService.POLICY_NOTIFICATION)
             .append(" ")
             .append(NOTIFICATION_ID)
             .append("=\"")
             .append(notificationId)
             .append("\">")
             .append(CRLF);
                                                                                
        if (notificationType == POLICY_CHANGE_TYPE) {
            xmlsb.append(policyChangeNotification.toXMLString());
        } else {
            debug.error(
                "PolicyNotification.toXMLString(): unknown notification type");
        }
                                                                                
        xmlsb.append("</")
             .append(PolicyService.POLICY_NOTIFICATION)
             .append(">")
             .append(CRLF);
        return xmlsb.toString();
    }
} 
