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
 * $Id: IdEventListener.java,v 1.3 2008/06/25 05:43:28 qcheng Exp $
 *
 */

package com.sun.identity.idm;

/**
 * <p>
 * Represents the event listener interface that consumers of this API should
 * implement and register with the AMIdentityRepository to receive
 * notifications.
 *
 * @supported.all.api
 */
public interface IdEventListener {
    /**
     * This method is called back for all identities that are modified in a
     * repository.
     * 
     * @param universalId
     *            Universal Identifier of the identity.
     */
    public void identityChanged(String universalId);

    /**
     * This method is called back for all identities that are deleted from a
     * repository. The universal identifier of the identity is passed in as an
     * argument
     * 
     * @param universalId
     *            Univerval Identifier
     */
    public void identityDeleted(String universalId);

    /**
     * This method is called for all identities that are renamed in a
     * repository. The universal identifier of the identity is passed in as an
     * argument
     * 
     * @param universalId
     *            Universal Identifier
     */
    public void identityRenamed(String universalId);

    /**
     * The method is called when all identities in the repository are changed.
     * This could happen due to a organization deletion or permissions change
     * etc
     * 
     */
    public void allIdentitiesChanged();
}
