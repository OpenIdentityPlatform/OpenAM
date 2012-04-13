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
 * $Id: FSAttributeMapper.java,v 1.3 2008/06/25 05:46:52 qcheng Exp $
 *
 */

package com.sun.identity.federation.services;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * The interface <code>FSAttributeMapper</code> is a plugin for
 * mapping the <code>Attribute</code>s  during the single sign-on at the
 * <code>ServiceProvider</code> of the Liberty protocols. The service
 * provider while it is validating the <code>Assertion</code> uses this
 * plugin to map the <code>Attribute</code>s that are passed from the 
 * Identity provider to local attributes so that the assertion consumer
 * service will populate these attributes to the application via the
 * single sign-on token.
 * </p>
 * @deprecated This SPI is deprecated.
 * @see com.sun.identity.federation.services.FSRealmAttributeMapper
 * @supported.all.api
 */ 
public interface FSAttributeMapper {

    /**
     * Returns the map of local attributes for the given list of attribute
     * statements. 
     * @param attributeStatements list of <code>AttributeStatement</code>s.
     * @param hostEntityId Hosted provider entity id.
     * @param remoteEntityId Remote provider entity id.
     * @param token Single sign-on session token.
     * @return map of attribute value pairs. This map will have the key
     *         as the attribute name and the value is the attribute value.
     */
    public Map getAttributes(
        List attributeStatements, 
        String hostEntityId,
        String remoteEntityId,
        Object token);

}
