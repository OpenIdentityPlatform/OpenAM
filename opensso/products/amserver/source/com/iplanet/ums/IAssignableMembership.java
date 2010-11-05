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
 * $Id: IAssignableMembership.java,v 1.3 2008/06/25 05:41:45 qcheng Exp $
 *
 */

package com.iplanet.ums;

/**
 * Represents a container interface common to groups and roles. It extends
 * IMembership by adding support for adding and removing members.
 * 
 * @see com.iplanet.ums.IMembership
 * @supported.all.api
 */
public interface IAssignableMembership extends IMembership {

    /**
     * Adds a member to the group. The change is saved to persistent storage.
     * 
     * @param guid
     *            globally unique identifier for the member to be added
     * @exception UMSException
     *                on failure to save to persistent storage
     */
    public void addMember(Guid guid) throws UMSException;

    /**
     * Adds a member to the group. The change is saved to persistent storage.
     * 
     * @param member
     *            Object to be added
     * @exception UMSException
     *                on failure to save to persistent storage
     */
    public void addMember(PersistentObject member) throws UMSException;

    /**
     * Adds a list of members to the group. The change is saved to persistent
     * storage.
     * 
     * @param guids
     *            list of member guids to be added as members to the group
     * @exception UMSException
     *                on failure to save to persistent storage
     */
    public void addMembers(Guid[] guids) throws UMSException;

    /**
     * Removes a member from the group. The change is saved to persistent
     * storage.
     * 
     * @param guid
     *            unique identifier for the member to be removed
     * @exception UMSException
     *                on failure to save to persistent storage
     */
    public void removeMember(Guid guid) throws UMSException;

    /**
     * Removes a member from the group. The change is saved to persistent
     * storage.
     * 
     * @param member
     *            Object to be removed
     * @exception UMSException
     *                on failure to save to persistent storage
     */
    public void removeMember(PersistentObject member) throws UMSException;

    /**
     * Removes all members of the group
     * 
     * @exception UMSException
     *                on failure to save to persistent storage
     */
    public void removeAllMembers() throws UMSException;
}
