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
 * $Id: ResultsCacheUtil.java,v 1.4 2009/10/21 23:50:47 dillidorai Exp $
 *
 */


package com.sun.identity.policy.client;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.remote.PolicyEvaluationException;
import com.sun.identity.policy.remote.PolicyNotification;

import java.io.ByteArrayInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * This class provides a bridge between the end point on
 * policy client that is listening for notifications from
 * policy service and policy client cache.
*/
public class ResultsCacheUtil {

    static Debug debug = Debug.getInstance("amRemotePolicy");

    private static final String BEGIN_XML_DATA_BLOCK = "<![CDATA[";
    private static final String END_XML_DATA_BLOCK = "]]>";
    private static final String NODE_POLICY_SERVICE = "PolicyService";
    private static final String NODE_POLICY_NOTIFICATION =
            "PolicyNotification";

    /**
      * Processes notifications forwarded from the endpoint on policy 
      * client that is listening for notifications from policy service
      *
      * @param xml XML notification envelope
      * @throws PolicyEvaluationException 
      *
      */
    public static void processNotification(String xml)
            throws PolicyEvaluationException {
        if (debug.messageEnabled()) {
            debug.message("ResultsCacheUtil.processNotification():"
                    + "recieved notification xml=" + xml);
        }
        PolicyNotification pn = extractPolicyNotification(xml);
        if (pn != null) {
            ResourceResultCache.processPolicyNotification(pn);
        } else {
            debug.error("ResultsCacheUtil.processPolicyNotification():" 
                    + "PolicyNotification is null");
        }
    }

    /**
      * Processes REST notifications forwarded from the endpoint on policy 
      * client that is listening for notifications from policy service
      *
      * @param message notification as JSON string
      * @throws PolicyEvaluationException 
      *
      */
    public static void processRESTNotification(String message)
            throws PolicyEvaluationException {
        // sample json string
        // {realm: "/", privilgeName: "p1", 
        //     resources: ["http://www.sample.com/a.html", "http://www.sample.com/b.html"]}
        if (debug.messageEnabled()) {
            debug.message("ResultsCacheUtil.processRESTNotification():"
                    + "recieved notification =" + message);
        }
        if (message != null) {
            ResourceResultCache.processRESTPolicyNotification(message);
        } else {
            debug.error("ResultsCacheUtil.processRESTNotification():" 
                    + "notification message is null");
        }
    }

    /**
     * Returns the notification XML node
     *
     * @param xml XML node
     *
     * @return XML Notification node
     *
     * @throws PolicyEvaluationException 
     *
     */
    private static PolicyNotification extractPolicyNotification(String xml) 
            throws PolicyEvaluationException {
        PolicyNotification policyNotification = null; 
	try {
	    String notificationDataBlock = getNotificationDataBlock(xml);
	    if (notificationDataBlock != null) {
		Document doc = XMLUtils.getXMLDocument(
                        new ByteArrayInputStream(
                        notificationDataBlock.getBytes()));
		Node rootNode = XMLUtils.getRootNode(doc, 
                        NODE_POLICY_SERVICE);
		if (rootNode != null) {
		    Node notificationNode = XMLUtils.getChildNode(rootNode, 
                             NODE_POLICY_NOTIFICATION);
                    if (notificationNode != null) {
                        policyNotification 
                                = PolicyNotification.parseXML(notificationNode);
                    } else {
                        debug.error("ResultsCacheUtil."
                                + "extractPolicyNotification():" 
                                + "cannot find notification node");
                        throw new PolicyEvaluationException(
                                ResBundleUtils.rbName,
                                "invalid_root_element", null, null);
                    } 
		} else {
                    debug.error("ResultsCacheUtil."
                            + "extractPolicyNotification():");
                }
	    }  else {
                //null notification data block
                debug.error("ResultsCacheUtil:"
                        + "extractPolicyNotification():" 
                        + "notification data block is null");
            }
	} catch (Exception xe) {
	    debug.error("ResultsCacheUtil.extractPolicyNotification():", 
                    xe);
	    throw new PolicyEvaluationException(ResBundleUtils.rbName,
                    "xml_parsing_error", null, xe);
	} 
        return policyNotification;
    } 

    /**
     * Returns the notification data block from XML
     *
     * @param xml xml envelope
     *
     * @return Notification block 
     *
     */
    private static String getNotificationDataBlock(String xml) {
	int idx = xml.indexOf(BEGIN_XML_DATA_BLOCK);

	if (idx != -1) {
	    xml = xml.substring(idx + BEGIN_XML_DATA_BLOCK.length());
	    idx = xml.indexOf(END_XML_DATA_BLOCK);
	    if (idx != -1) {
		return xml.substring(0, idx);
	    } 
	} 
	return null;
    } 

}
