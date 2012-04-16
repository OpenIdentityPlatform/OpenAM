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
 * $Id: IDPPExtension.java,v 1.2 2008/06/25 05:47:17 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.idpp.plugin;

import com.sun.identity.liberty.ws.idpp.common.*;
import java.util.List;


/**
 * The class <code>IDPPExtension</code> is an interface for extension
 * attributes in each <code>IDPPContainer</code>. Each container can be extended
 * to any attributes that are not defined by the liberty personal profile
 * service by implementing this interface. Each container extension plugin
 * can be configurable from the service configuration.
 * @supported.all.api
 */ 

public interface IDPPExtension {

    /**
     * Gets the list of extension <code>JAXB</code> attribute objects. These
     * <code>JAXB</code> Elements must be of the type 
     * <code>com.sun.identity.liberty.ws.idpp.plugin.jaxb.PPISExtensionElement
     * </code>
     * @return list of <code>PPISExtensionElement</code> <code>JAXB</code>
     * Objects.
     * @exception IDPPException.
     */
    public List getExtAttributes() throws IDPPException;

}
