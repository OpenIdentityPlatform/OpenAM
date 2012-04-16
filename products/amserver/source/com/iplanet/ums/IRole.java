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
 * $Id: IRole.java,v 1.4 2008/06/25 05:41:45 qcheng Exp $
 *
 */

package com.iplanet.ums;

/**
 * This is a common iterface from which different roles can
 * be implemented.
 *
 * @supported.all.api
 */
public interface IRole {
    /**
     * Evaluates whether an object is member of this IRole.
     * 
     * @param po Persistent object that is being checked for membership.
     * @return <code>true</code> if the object is member of the role
     *         implementing this interface, <code>false</code> otherwise.
     * @throws UMSException if an exception occurs while determining if this
     *         role has the member.
     */
    public boolean hasMember(PersistentObject po) throws UMSException;

    /**
     * Returns the GUID of this object
     * 
     * @return the GUID of this object
     * 
     */
    public Guid getGuid();
}
