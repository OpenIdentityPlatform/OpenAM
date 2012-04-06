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
 * $Id: OrganizationalUnit.java,v 1.3 2008/06/25 05:41:45 qcheng Exp $
 *
 */

package com.iplanet.ums;

import java.security.Principal;

import com.iplanet.services.ldap.AttrSet;

/**
 * Representation of organizational unit. Such as
 * 
 * <pre>
 *   ou=people,o=vortex.com 
 *   ou=groups,o=vortex.com
 * </pre>
 * 
 * They are persistent objects and can be used as a container. For example,
 * 
 * <PRE>
 * 
 * orgUnit = (OrganizationalUnit)UMSSession.getObject( ctx, id ); orgUnit.add(
 * User1 );
 * 
 * </PRE>
 *
 * @supported.api
 */
public class OrganizationalUnit extends PersistentObject {

    /**
     * No args constructor; used to construct the right object as entries are
     * are read from persistent storage
     */
    protected OrganizationalUnit() throws UMSException {
    }

    /**
     * Constructor of OrganizationalUnit from supplied session and guid
     * identifying the organization to be constructed
     * 
     * @param session
     *            authenticated session object maintained by the Session Manager
     * @param guid
     *            globally unique identifier for the organizational unit
     */
    OrganizationalUnit(Principal principal, Guid guid) throws UMSException {
        super(principal, guid);
        verifyClass();
    }

    /**
     * Constructs an OrganizationalUnit object without a session. Unlike the
     * constructor with a session parameter , this one simply creates an object
     * in memory, using the default template. Call save() to save the object to
     * the persistent store.
     * 
     * @param attrSet
     *            attribute/value set
     */
    OrganizationalUnit(AttrSet attrSet) throws UMSException {
        this(TemplateManager.getTemplateManager().getCreationTemplate(_class,
                null), attrSet);
    }

    /**
     * Constructs a Organizational unit object without a session. Unlike
     * constructor with session, this one simply creates OrganizationalUnit
     * object in memory. Call save() to save the object to persistent storage.
     * 
     * @param template
     *            template for the OrganizationalUnit, containing required and
     *            optional attributes, and possibly default values
     * @param attrSet
     *            attribute/value set
     * @supported.api
     */
    public OrganizationalUnit(CreationTemplate template, AttrSet attrSet)
            throws UMSException {
        super(template, attrSet);
    }

    /**
     * Return name of the organizational unit
     * 
     * @return name of the organizational unit
     * @supported.api
     */
    public String getName() throws UMSException {
        return (getAttribute(getNamingAttribute()).getValue());
    }

    private static final Class _class = 
            com.iplanet.ums.OrganizationalUnit.class;
}
