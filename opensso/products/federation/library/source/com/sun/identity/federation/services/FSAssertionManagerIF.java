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
 * $Id: FSAssertionManagerIF.java,v 1.4 2008/06/25 05:46:51 qcheng Exp $
 *
 */

package com.sun.identity.federation.services;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.sun.identity.federation.common.FSRemoteException;

/**
 * JAX-RPC interface for AssertionManager to make it remoteable.
 */
public interface FSAssertionManagerIF extends Remote {
    public void checkForLocal() throws RemoteException;
    
    /**
     * Returns assertion associated with the artifact.
     * @param metaAlias hosted provider meta alias. 
     * @param artifact assertion artifact.
     * @param destID The destination site requesting the assertion using
     *  the artifact.
     * @return The Assertion referenced to by artifact.
     * @exception FSRemoteException, RemoteException If an error occurred during
     *  the process
     */
    public String getAssertion(
        String metaAlias, String artifact, String destID)
        throws FSRemoteException, RemoteException;
    
    /**
     * Returns the destination id the artifact is created for.
     * @param metaAlias hosted provider meta alias
     * @param artifact assertion artifact string
     * @return destination id
     * @exception FSRemoteException, RemoteException if error occurred.
     */
    public String getDestIdForArtifact(String metaAlias, String artifact)
        throws FSRemoteException, RemoteException;

    /**
     * Checks if the user exists.
     * @param userDN user ID
     * @param metaAlias hosted provider's meta alias
     * @return <code>true</code> if the user exists; <code>false</code> 
     *  otherwise.
     * @exception FSRemoteException,RemoteException if error occurred.
     */
    public boolean isUserExists(String userDN, String metaAlias)
        throws FSRemoteException, RemoteException;

    /**
     * Returns the error status of a given artifact.
     * @param metaAlias hosted provider's meta alias
     * @param artifact
     * @return status encoded in XML
     */
    public String getErrorStatus( String metaAlias, String artifact )
        throws FSRemoteException, RemoteException;
}
