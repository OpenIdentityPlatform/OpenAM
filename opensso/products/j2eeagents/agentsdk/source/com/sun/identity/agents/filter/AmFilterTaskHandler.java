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
 * $Id: AmFilterTaskHandler.java,v 1.4 2008/06/25 05:51:43 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import com.sun.identity.agents.arch.Manager;

/**
 * <p>
 * This abstract class that acts as the super class of all task handlers
 * that require the infrastructre services such as configuration data access
 * and log capability.
 * </p>
 */
public abstract class AmFilterTaskHandler extends AmFilterHandlerBase implements
IAmFilterTaskHandler
{
    /**
     * The constructor that takes a <code>Manager</code> intance in order
     * to gain access to the infrastructure services such as configuration
     * and log access.
     *
     * @param manager the <code>Manager</code> for the <code>filter</code>
     * subsystem.
     */
    public AmFilterTaskHandler(Manager manager) {
        super(manager);
    }
    
    /**
     * For new feature to allow session data to not be destroyed when a user 
     * authenticates to AM server and new session is created.
     * For RFE issue #763
     */
    public boolean isSessionBindingEnabled() {
        return getConfigurationBoolean(CONFIG_HTTPSESSION_BINDING, DEFAULT_CONFIG_HTTPSESSION_BINDING);
    }
    
}
