/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ResponseProviderXMLBuilder.java,v 1.2 2008/06/25 05:43:09 qcheng Exp $
 *
 */

package com.sun.identity.console.property;

import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.interfaces.ResponseProvider;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ResponseProviderXMLBuilder
    extends PolicyPropertyXMLBuilderBase
{
    private ResponseProvider provider;

    /**
     * Constructs a XML builder.
     *
     * @param provider response provider object for getting property value
     *        and syntax.
     * @param model Model for getting user locale.
     */
    public ResponseProviderXMLBuilder(ResponseProvider provider, AMModel model){
        this.model = model;
        this.provider = provider;
    }

    protected List getPropertyNames() {
        return provider.getPropertyNames();
    }

    protected String getSectionLabel() {
        return "policy.responseprovider.section.label.values";
    }

    protected Syntax getPropertySyntax(String propertyName) {
        return provider.getPropertySyntax(propertyName);
    }

    protected String getDisplayName(String propertyName, Locale locale)
        throws PolicyException
    {
        return provider.getDisplayName(propertyName, locale);
    }

    protected Set getValidValues(String propertyName)
        throws PolicyException
    {
        return provider.getValidValues(propertyName);
    }
}
