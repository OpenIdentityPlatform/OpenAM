/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ClientDetectionDefaultImpl.java,v 1.5 2008/06/25 05:41:32 qcheng Exp $
 *
 */

package com.iplanet.services.cdm;

import com.iplanet.am.util.AMClientDetector;
import com.sun.identity.shared.debug.Debug;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;

/**
 * The <code>ClientDetectionInterface</code> interface needs to be implemented
 * by services and applications serving multiple clients, to determine the
 * client from which the request has originated. This interface detects the
 * client type from the client request.
 * @supported.all.api
 */
public class ClientDetectionDefaultImpl implements ClientDetectionInterface {

    protected static Debug debug = Debug.getInstance("amClientDetection");

    protected static DefaultClientTypesManager defCTM = 
        (DefaultClientTypesManager) 
            AMClientDetector.getClientTypesManagerInstance();

    /**
     * Creates a client detection default implementation instance.
     */
    public ClientDetectionDefaultImpl() {
    }

    /**
     * This is the method used by the interface to set the client-type.
     * <code>ClientDetectionDefaultImpl</code> currently uses the following
     * algorithm.
     * 
     * <pre>
     *  if userAgent equals a known user-agent then
     *     compare userAgent length and store the longest match
     *  if clientType not found 
     *     return the default clientType
     * </pre>
     * 
     * @param request
     *            The calling object passes in the
     *            <code>HTTPServletRequest</code>.
     * @return The string corresponding to the client type.
     * @throws ClientDetectionException
     *             if a default client type cannot be found
     */

    public String getClientType(HttpServletRequest request)
            throws ClientDetectionException {

        String httpUA = request.getHeader("user-agent");
        String clientType = null;
        int prevClientUALen = 0;

        if (debug.messageEnabled()) {
            debug.message("UserAgent : httpUA is : " + httpUA);
            debug.message("Looking in UA/PartialMatch Maps");
        }

        Client clientInstance = null;
        if ((clientInstance = defCTM.getFromUserAgentMap(httpUA)) != null) {
            //
            // Perf: We wont have to iterate thro' all clients
            //
            clientType = clientInstance.getClientType();
            if (debug.messageEnabled()) {
                debug.message("Perf: from UA Map: " + clientType);
            }
            return clientType;
        } else 
            if ((clientType = defCTM.getPartiallyMatchedClient(httpUA)) != null)

        {
            if (debug.messageEnabled()) {
                debug.message("Perf: from PartialMatch Map: " + clientType);
            }
            return clientType;
        }

        // Iterate through Clients, find and save the longest match
        int i = 0;
        Iterator knownClients = ClientsManager.getAllInstances();
        while (knownClients.hasNext()) {
            clientInstance = (Client) knownClients.next();
            i++;

            String curClientUA = clientInstance.getProperty("userAgent");
            if (curClientUA != null) {

                if (debug.messageEnabled()) {
                    debug.message("(" + i + ") Client user-agent = "
                            + curClientUA + " :: clientType = "
                            + clientInstance.getClientType());
                }

                if (userAgentCheck(httpUA, curClientUA)) {

                    // We have a match
                    String curClientType = clientInstance
                            .getProperty("clientType");

                    // Check length
                    int curClientUALen = curClientUA.length();
                    if (curClientUALen > prevClientUALen) {
                        clientType = curClientType;
                        prevClientUALen = curClientUALen;

                        if (debug.messageEnabled()) {
                            debug.message("Longest user-agent match client " +
                                    "type = " + clientType);
                        }
                    }
                }
            }
        }

        // If we don't have a single match, get the default clientType
        if (clientType == null) {
            clientType = Client.getDefaultInstance().getProperty("clientType");

            if (debug.messageEnabled()) {
                debug.message("Default client type = " + clientType);
            }
        } else {
            //
            // Found a partial map - add it so our Map,
            // so our next search is faster
            //
            defCTM.addToPartialMatchMap(httpUA, clientType);
        }

        // If we can't get the default clientType
        if (clientType == null) {

            debug.message("Unable to obtain default client type");

            throw new ClientDetectionException(CDMBundle
                    .getString("null_clientType"));
        }

        if (debug.messageEnabled()) {
            debug.message("Returning client type : " + clientType);
        }
        return clientType;
    }

    /**
     * This method contains the algorithm used to compare the 
     * <CODE>HTTPServletRequest</CODE>
     * user-agent versus the <CODE>Client</CODE> user-agent.
     * 
     * @param httpUA
     *            The HTTPServletRequest user-agent
     * @param clientUA
     *            The Client userAgent
     * @return True or false if they match
     */
    protected boolean userAgentCheck(String httpUA, String clientUA) {
        if ((httpUA == null) || (clientUA == null)) {
            return false;
        }

        if ((httpUA.equalsIgnoreCase(clientUA))
                || (httpUA.indexOf(clientUA) > -1)) {
            return true;
        } else {
            return false;
        }
    }
}
