/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: DefaultIDPAttributeMapper.java,v 1.1 2008/07/08 23:03:34 hengming Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS
 */

package com.sun.identity.saml2.plugins;

/**
 * This class <code>DefaultIDPAttributeMapper</code> implements the
 * <code>IDPAttributeMapper</code> to return the SAML <code>Attribute</code>
 * objects that may be inserted in the SAML Assertion.
 * This IDP attribute mapper reads the attribute map configuration defined
 * in the hosted IDP configuration and construct the SAML
 * <code>Attribute</code> objects. If the mapped values are not present in
 * the data store, this will try to read from the Single sign-on token.
 */
public class DefaultIDPAttributeMapper extends DefaultLibraryIDPAttributeMapper
{
    /**
     * Constructor
     */
    public DefaultIDPAttributeMapper() {
    }

    /**
     * Checks if dynamical profile creation or ignore profile is enabled.
     *
     * @param realm realm to check the dynamical profile creation attributes.
     * @return true if dynamical profile creation or ignore profile is enabled,
     *     false otherwise.
     */
    protected boolean isDynamicalOrIgnoredProfile(String realm) {

        return SAML2PluginsUtils.isDynamicalOrIgnoredProfile(realm);
    }
}
