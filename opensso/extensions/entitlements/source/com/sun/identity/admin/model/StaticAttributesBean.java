/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: StaticAttributesBean.java,v 1.6 2009/06/04 11:49:17 veiming Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.handler.StaticAttributesHandler;
import com.sun.identity.entitlement.ResourceAttribute;
import com.sun.identity.entitlement.StaticAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static com.sun.identity.admin.model.AttributesBean.AttributeType.*;

public class StaticAttributesBean extends AttributesBean {

    public StaticAttributesBean() {
        super();
    }

    public ViewAttribute newViewAttribute() {
        return new StaticViewAttribute();
    }

    public AttributeType getAttributeType() {
        return STATIC;
    }

    public StaticAttributesBean(Set<ResourceAttribute> resourceAttributes) {
        this();

        for (ResourceAttribute ras: resourceAttributes) {
            if (!(ras instanceof StaticAttributes)) {
                continue;
            }

            String key = ras.getPropertyName();
            for (String val : ras.getPropertyValues()) {
                StaticViewAttribute sva = new StaticViewAttribute();
                sva.setName(key);
                sva.setValue(val);
                getViewAttributes().add(sva);
            }
        }
    }

    public Set<ResourceAttribute> toResourceAttributesSet() {
        Map<String,ResourceAttribute> resAttributes = new HashMap<String,ResourceAttribute>();

        for (ViewAttribute va: getViewAttributes()) {
            if (!(va instanceof StaticViewAttribute)) {
                continue;
            }
            StaticViewAttribute sva = (StaticViewAttribute)va;
            StaticAttributes sas = (StaticAttributes)resAttributes.get(sva.getName());
            if (sas == null ){
                sas = new StaticAttributes();
                sas.setPropertyName(sva.getName());
                sas.setPropertyValues(new HashSet());
                resAttributes.put(sas.getPropertyName(), sas);
            }
            sas.getPropertyValues().add(sva.getValue());
        }

        return new HashSet(resAttributes.values());
    }

    @Override
    public void reset() {
        super.reset();
        setAttributesHandler(new StaticAttributesHandler(this));
    }
}
