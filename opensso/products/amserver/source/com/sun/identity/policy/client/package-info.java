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
 * $Id: package-info.java,v 1.2 2008/06/25 05:43:47 qcheng Exp $
 *
 */



/**
 * Provides classes necessary for a remote client to evaluate policy decisions. 
 * The classes in the package communicate with policy service 
 * to get policy decisions. 
 * Client policy API implementation maintains a local cache of policy 
 * decisions. Cache decisions are kept current either by a configurable time 
 * to live and/or notifications from policy service.
 * @supported.api
 */
package com.sun.identity.policy.client;
