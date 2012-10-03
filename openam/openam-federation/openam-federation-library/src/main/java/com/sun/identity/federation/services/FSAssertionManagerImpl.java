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
 * $Id: FSAssertionManagerImpl.java,v 1.5 2008/11/10 22:56:58 veiming Exp $
 *
 */

package com.sun.identity.federation.services;

import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSRemoteException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.message.FSAssertion;
import com.sun.identity.federation.message.FSAssertionArtifact;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.protocol.AssertionArtifact;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.shared.encode.Base64;

import java.util.List;

/**
 * This class implements interface <code>FSAssertionManagerIF</code>.
 */
public class FSAssertionManagerImpl implements FSAssertionManagerIF {

    // Flag used to check if service is available locally
    protected static boolean isLocal;

    /**
     * Turns on the local flag.
     */
    public void checkForLocal() {
        isLocal = true;
    }

    /**
     * Returns assertion associated with the artifact.
     * @param metaAlias hosted provider's meta alias.
     * @param artifact assertion artifact.
     * @param destID The destination site requesting the assertion using
     *  the artifact.
     * @return The Assertion referenced to by artifact.
     * @exception FSRemoteException, RemoteException If an error occurred during     *  the process
     */
    public String getAssertion(
        String metaAlias, String artifact, String destID)
        throws FSRemoteException 
    {
        try {
            FSAssertionManager assertionManager = 
                FSAssertionManager.getInstance(metaAlias);
            FSAssertion a = (FSAssertion)assertionManager.getAssertion(
                new FSAssertionArtifact(artifact),
                SAMLUtils.byteArrayToString(Base64.decode(destID)));
            if (a == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionManagerImpl: Unable to " +
                        "get assertion from Artifact: " + artifact);
                }
                return (null);
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSAssertionManagerImpl: Assertion got from " +
                    "assertionManager.getAssertion: " + 
                    a.toXMLString(true, true));
            }
            return (a.toXMLString(true, true));
        } catch(Exception e) {
            throw new FSRemoteException(e.getMessage());
        }        
    }
    
    /**
     * Returns the destination id the artifact is created for.
     * @param metaAlias hosted provider's meta alias
     * @param artifact assertion artifact string
     * @return destination id
     * @exception FSRemoteException if error occurred.
     */
    public String getDestIdForArtifact(String metaAlias, String artifact)
        throws FSRemoteException 
    {
       try {
            FSAssertionManager assertionManager = 
                FSAssertionManager.getInstance(metaAlias);
            String destID = assertionManager.getDestIdForArtifact(
                new FSAssertionArtifact(artifact));
            if (destID == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionManagerImpl: Unable to " +
                        "get destination ID from remote : " );
                }
            }
            return destID;
        } catch(Exception e) {
            throw new FSRemoteException(e.getMessage());
        }        
    }

    /**
     * Checks if the user exists.
     * @param userDN user ID
     * @param metaAlias provider's Meta Alias.
     * @return <code>true</code> if the user exists; <code>false</code>
     *  otherwise.
     * @exception FSRemoteException,RemoteException if error occurred.
     */
    public boolean isUserExists(String userDN, String metaAlias)
        throws FSRemoteException
    {
        try {
            FSSessionManager sessionMgr = FSSessionManager.getInstance(
                metaAlias);
            synchronized(sessionMgr) {
                FSUtils.debug.message("About to call getSessionList");
                List sessionList = sessionMgr.getSessionList(userDN);
                if (sessionList == null) {
                    FSUtils.debug.message("AMC:isUserExists:List is empty");
                    return false;
                } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "AMC:isUserExists: List is not empty User found: "
                            + userDN);
                    }
                    return true;
                }
            }
        } catch(Exception e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("AMC.isUserExists:", e);
            }
            throw new FSRemoteException(e.getMessage());
        }
    }

    public String getErrorStatus( String metaAlias, String artifact) 
        throws FSRemoteException{
        try {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionManagerImpl.getErrStatus(" 
                        + metaAlias + ", " + artifact );
            }
            AssertionArtifact aa = new FSAssertionArtifact(artifact);
            Status s = FSAssertionManager.getInstance( metaAlias )
                .getErrorStatus( aa );
            if ( null != s )
                return s.toString( true, true );
        } catch (FSMsgException e) {
            FSUtils.debug.error( "getErrStatus: FSMsgException:" 
                    + e.getMessage() );
            throw new FSRemoteException(e.getMessage());
        } catch (FSException e) {
            FSUtils.debug.error( "getErrStatus: FSException:" 
                    + e.getMessage() );
            throw new FSRemoteException(e.getMessage());
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message( "getErrStatus: returning null" );
        }
        return null;
    }

}
