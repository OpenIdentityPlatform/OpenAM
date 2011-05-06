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
 * $Id: IClassResolver.java,v 1.3 2008/06/25 05:41:45 qcheng Exp $
 *
 */

package com.iplanet.ums;

import com.iplanet.services.ldap.AttrSet;

/**
 * Interface for a class that can resolve the Java class to instantiate for a
 * specific collection of attributes.
 * 
 * @see com.iplanet.ums.TemplateManager
 *
 * @supported.api
 */
public interface IClassResolver {
    /**
     * Resolves a set of attributes to a subclass of PersistentObject and
     * returns the class for it.
     * 
     * @param id
     *            ID of the entry
     * @param set
     *            a set of attributes of an object
     * @return a class for a corresponding object, or <code>null</code> if no
     *         class could be resolved
     *
     * @supported.api
     */
    public Class resolve(String id, AttrSet set);
}
