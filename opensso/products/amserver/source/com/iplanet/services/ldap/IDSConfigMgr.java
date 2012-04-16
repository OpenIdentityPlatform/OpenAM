/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IDSConfigMgr.java,v 1.2 2008/06/25 05:41:36 qcheng Exp $
 *
 */

package com.iplanet.services.ldap;

public interface IDSConfigMgr {

    /**
     * Returns the Server Group object reference for this id.
     * 
     * @param serverGroupID Server Group ID.
     * @return server group; null if no such Server group exists.
     */
    ServerGroup getServerGroup(String serverGroupID);

    /**
     * Returns the host names for this id.
     * 
     * @param serverGroupID Server Group ID.
     * @return host names.
     */
    String getHostName(String serverGroupID);

    /**
     * Returns server instance of a given authentication type.
     *
     * @param authType Authentication type.
     * @return server instance of a given authentication type.
     */
    ServerInstance getServerInstance(LDAPUser.Type authType);
}
