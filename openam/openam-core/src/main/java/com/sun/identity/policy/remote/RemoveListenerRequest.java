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
 * $Id: RemoveListenerRequest.java,v 1.3 2008/06/25 05:43:54 qcheng Exp $
 *
 */

package com.sun.identity.policy.remote;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.policy.ResBundleUtils;
import org.w3c.dom.Node;

/**
 * This <code>RemoveListenerRequest</code> class represents an 
 * RemovePolicyListener XML document. The RemovePolicyListener DTD is 
 * defined as the following:
 * <p>
 * <pre>
 *    <!-- RemovePolicyListener element removes a policy listener 
 *          from the service.
 *          The attribute serviceName specifies the service name.
 *          The attribute notificationURL provides the notification
 *          URL for which the corresponding policy listener needs to
 *          be removed.
 *     -->
 *
 *      <!ELEMENT    RemovePolicyListener    EMPTY >
 *     <!ATTLIST    RemovePolicyListener
 *         serviceName        NMTOKEN    #REQUIRED
 *         notificationURL    CDATA      #REQUIRED
 *     >
 * </pre>
 * <p>
 */
public class RemoveListenerRequest {

    static final String REMOVE_LISTENER = PolicyRequest.REMOVE_POLICY_LISTENER;
    static final String SERVICE_NAME = "serviceName";
    static final String NOTIFICATION_URL = "notificationURL";
    static final String CRLF = PolicyService.CRLF;
    static Debug debug = PolicyService.debug; 

    private String notificationURL = null;
    private String serviceName = null;
    
    /** 
     * Default constructor for <code>RemoveListenerRequest</code>.
     */
    public RemoveListenerRequest() {
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
     * Sets the service name on which this listener listens.
     *
     * @param name the service name.
     */
    public void setServiceName(String name) {
        serviceName = name;
    }

    /**
     * Returns the notification URL to which the notification is sent.
     *
     * @return notification URL.
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
     * Returns a <code>RemoveListenerRequest</code> object constructed from
     * a XML.
     *
     * @param pNode the XML DOM node for the <code>RemoveListenerRequest</code>
     *        object.
     * @return constructed <code>RemoveListenerRequest</code> object.
     */
    public static RemoveListenerRequest parseXML(Node pNode)
        throws PolicyEvaluationException
    {
        RemoveListenerRequest removeListenerReq = new RemoveListenerRequest();
        String attr = XMLUtils.getNodeAttributeValue(pNode, SERVICE_NAME);

        if (attr == null) {
            debug.error("RemoveListenerRequest: missing attribute " +
                SERVICE_NAME);
            String objs[] = { SERVICE_NAME };
            throw new PolicyEvaluationException(ResBundleUtils.rbName,
                "missing_attribute",objs, null);
        }

        removeListenerReq.setServiceName(attr);

        attr = XMLUtils.getNodeAttributeValue(pNode, NOTIFICATION_URL);
        if (attr == null) {
            debug.error("RemoveListenerRequest: missing attribute " 
                         + NOTIFICATION_URL);
            String objs[] = { NOTIFICATION_URL };
            throw new PolicyEvaluationException(ResBundleUtils.rbName,
                "missing_attribute", objs, null);
        }
        
        removeListenerReq.setNotificationURL(attr);
        return removeListenerReq; 
    }

    /**
     * Returns a XML representation of this object.
     *
     * @return a XML representation of this object.
     */
    public String toXMLString() {
        return "<" + REMOVE_LISTENER + " " +
            SERVICE_NAME + "=\"" + serviceName + "\" " +
            NOTIFICATION_URL + "=\"" + notificationURL +
            "\"/>" + CRLF;
    }
}
