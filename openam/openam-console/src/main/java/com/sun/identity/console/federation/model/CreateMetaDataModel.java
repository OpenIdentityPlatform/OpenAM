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
 * $Id: CreateMetaDataModel.java,v 1.2 2008/06/25 05:49:39 qcheng Exp $
 *
 */

package com.sun.identity.console.federation.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import java.util.Map;

public interface CreateMetaDataModel
    extends AMModel
{
    /**
     * Creates a SAMLv2 provider.
     *
     * @param realm Realm Name.
     * @param entityId Entity Id.
     * @param values   Map of property name to values.
     */
    void createSAMLv2Provider(String realm, String entityId, Map values)
        throws AMConsoleException;
    
    /**
     * Creates a IDFF provider.
     *
     * @param realm Realm Name.
     * @param entityId Entity Id.
     * @param values   Map of property name to values.
     */
    void createIDFFProvider(String realm, String entityId, Map values)
        throws AMConsoleException;

    /**
     * Creates a WS Federation provider.
     *
     * @param realm Realm Name.
     * @param entityId Entity Id.
     * @param values   Map of property name to values.
     */
    void createWSFedProvider(String realm, String entityId, Map values)
        throws AMConsoleException;
}
