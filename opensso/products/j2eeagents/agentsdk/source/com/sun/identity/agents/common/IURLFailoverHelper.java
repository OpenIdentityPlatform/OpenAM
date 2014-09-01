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
 * $Id: IURLFailoverHelper.java,v 1.3 2008/06/25 05:51:40 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.common;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.filter.AmFilterRequestContext;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * The interface for URLFailoverHelper
 */
public interface IURLFailoverHelper {
    public abstract void initialize(
            boolean probeEnabled, 
            boolean isPrioritized, 
            long timeout,
            String[] urlList,
            Map<String, Set<String>> conditionalUrls) throws AgentException;

    public String getAvailableURL(AmFilterRequestContext ctx) throws AgentException;
    public abstract String getAvailableURL(HttpServletRequest req) throws AgentException;
}
