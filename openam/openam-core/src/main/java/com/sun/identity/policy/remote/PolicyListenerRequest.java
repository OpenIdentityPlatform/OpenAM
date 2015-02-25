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
 * $Id: PolicyListenerRequest.java,v 1.3 2008/06/25 05:43:53 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.remote;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.policy.PolicyEvent;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.interfaces.PolicyListener;
import com.iplanet.services.comm.share.Notification;
import com.iplanet.services.comm.share.NotificationSet;
import com.iplanet.services.comm.server.PLLServer;
import com.iplanet.services.comm.server.SendNotificationException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import org.w3c.dom.Node;

/**
 * This <code>PolicyListenerRequest</code> class represents an 
 * AddPolicyListener XML document. The AddPolicyListener DTD is 
 * defined as the following:
 * <p>
 * <pre>
 *    <!-- AddPolicyListener element adds a policy listener to the 
 *         service to receive the policy notification.
 *         The attribute serviceName specifies the service name.
 *         The attribute notificationURL provides the notification
 *         URL for the policy server to send the notification to.
 *    -->
 *
 *    <!ELEMENT    AddPolicyListener    EMPTY >
 *    <!ATTLIST    AddPolicyListener
 *        serviceName        NMTOKEN    #REQUIRED
 *        notificationURL    CDATA      #REQUIRED
 *    >
 * </pre>
 * <p>
 */

public class PolicyListenerRequest implements PolicyListener {

    static final String POLICY_LISTENER = PolicyRequest.ADD_POLICY_LISTENER;
    static final String SERVICE_NAME = "serviceName";
    static final String NOTIFICATION_URL = "notificationURL";
    static final String CRLF = PolicyService.CRLF;
    static Debug debug = PolicyService.debug; 

    private String notificationURL = null;
    private String serviceName = null;
    
    /** 
     * Default constructor for <code>PolicyListenerRequest</code>
     */
    public PolicyListenerRequest() {
    }

    /**
     * Returns the service name on which this listener listens.
     *
     * @return service name.
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Sets the service name on which this listener listens
     *
     * @param name Service name.
     */
    public void setServiceName(String name) {
        serviceName = name;
    }

    /**
     * Returns the notification URL to which the notification is sent.
     *
     * @return notification URL
     */
    public String getNotificationURL() {
        return notificationURL;
    }

    /**
     * Sets the notification URL to which the notification is sent.
     *
     * @param url the notification URL.
     */
    public void setNotificationURL(String url) {
        notificationURL = url;
    }

    /**
     * Returns <code>PolicyListenerRequest</code> object constructed from
     * a XML.
     *
     * @param pNode the XML DOM node for the <code>PolicyListenerRequest</code>
     *        object.
     * @return constructed <code>PolicyListenerRequest</code> object.
     */
    public static PolicyListenerRequest parseXML(Node pNode)
        throws PolicyEvaluationException
    {
        PolicyListenerRequest policyListenerReq = 
                             new PolicyListenerRequest();

        String attr = XMLUtils.getNodeAttributeValue(pNode, SERVICE_NAME);

        if (attr == null) {
            debug.error("PolicyListenerRequest: missing attribute " 
                         + SERVICE_NAME);
            String objs[] = { SERVICE_NAME };
            throw new PolicyEvaluationException(ResBundleUtils.rbName,
                "missing_attribute", objs, null);
        }

        policyListenerReq.setServiceName(attr);
        attr = XMLUtils.getNodeAttributeValue(pNode, NOTIFICATION_URL);

        if (attr == null) {
            debug.error("PolicyListenerRequest: missing attribute " 
                         + NOTIFICATION_URL);
            String objs[] = { NOTIFICATION_URL };
            throw new PolicyEvaluationException(ResBundleUtils.rbName,
                "missing_attribute", objs, null);
        }
        
        policyListenerReq.setNotificationURL(attr);
        return policyListenerReq; 
    }

    /**
     *  Returns a XML representation of this object.
     *
     *  @return a XML representation of this object.
     */
    public String toXMLString()
    {
        StringBuilder xmlsb = new StringBuilder(100);
        
        xmlsb.append('<').append(POLICY_LISTENER).append(' ').
                append(SERVICE_NAME).append('=').append('\"').append(serviceName).append("\" ").
                append(NOTIFICATION_URL).append("=\"").append(notificationURL).append("\"/>").append(CRLF);
        return xmlsb.toString();
    }

    /**
     * Returns the service name on which the listener listens.
     *
     * @return service name.
     */
    public String getServiceTypeName() { 
        return serviceName;
    }

    /**
     * Handles policy change event.
     *
     * @param evt the policy event regarding the policy change.
     */
    public void policyChanged(PolicyEvent evt) {
        debug.message("PolicyListenerRequest.policyChanged()");

        if (evt == null) {
            debug.error("PolicyListenerRequest.policyChanged(PolicyEvent): " +
                "invalid policy event");  
            return;
        }

        // get the policy change type from the event
        String changeType = null;
        int type = evt.getChangeType();

        if (type == PolicyEvent.POLICY_ADDED) {
            changeType = PolicyChangeNotification.ADDED;
        } else if (type == PolicyEvent.POLICY_REMOVED) {
            changeType = PolicyChangeNotification.DELETED;
        } else {
            changeType = PolicyChangeNotification.MODIFIED;
        }

        // get the resource names from the event
        Set resourceNames = evt.getResourceNames();    
        if (debug.messageEnabled()) {
            debug.message(
                "PolicyListenerRequest.policyChanged(PolicyEvent): " +
                "resource names from the policy event : " +
                resourceNames.toString());
        }

        PolicyService ps = new PolicyService();
        PolicyNotification pn = new PolicyNotification();
        PolicyChangeNotification pcn = new PolicyChangeNotification();

        /*
         * sets the service name and resource names which are affected
         *  by this policy change notification.
         */
        pcn.setResourceNames(resourceNames);
        pcn.setPolicyChangeType(changeType);
        pcn.setServiceName(serviceName);

        pn.setNotificationType(PolicyNotification.POLICY_CHANGE_TYPE);
        pn.setPolicyChangeNotification(pcn);

        ps.setMethodID(PolicyService.POLICY_NOTIFICATION_ID);
        ps.setPolicyNotification(pn);

        /*
         * create a Notification object based on the policy change
         * notification.
         */
        Notification notification = new Notification(ps.toXMLString());
        NotificationSet set = new NotificationSet(PolicyService.POLICY_SERVICE);

        // add the notification to the notification set to be sent to the client
        set.addNotification(notification);

        if (debug.messageEnabled()) {
            debug.message(
                "PolicyListenerRequest.policyChanged(PolicyEvent): " +
                "the notification set sent is : " + set.toXMLString());
        }

        try {
            // sends the notification to the client
            PLLServer.send(new URL(notificationURL), set);
            if (debug.messageEnabled()) {
                debug.message(
                    "PolicyListenerRequest.policyChanged(PolicyEvent): " +
                    "the policy change notification has been sent to " +
                    notificationURL);
            }
        } catch (SendNotificationException e) {
            debug.error("PolicyListenerRequest.policyChanged(): " +
                "PLLServer.send() failed", e);
        } catch (MalformedURLException e) {
            debug.error("PolicyListenerRequest.policyChanged(): " +
                "PLLServer.send() failed", e);
        }
    }   
}
