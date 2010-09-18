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
 * $Id: UserAttributesBean.java,v 1.3 2009/06/24 23:47:02 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.dao.UserAttributeDao;
import com.sun.identity.admin.handler.UserAttributesHandler;
import com.sun.identity.entitlement.ResourceAttribute;
import com.sun.identity.entitlement.UserAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections.comparators.NullComparator;
import static com.sun.identity.admin.model.AttributesBean.AttributeType.*;

public class UserAttributesBean extends AttributesBean {

    private String filter = "";
    private List<ViewAttribute> availableViewAttributes = null;

    public UserAttributesBean() {
        super();
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        if (filter == null) {
            filter = "";
        }
        NullComparator n = new NullComparator();
        if (n.compare(this.filter, filter) != 0) {
            this.filter = filter;
            availableViewAttributes = null;
        }
    }

    public List<ViewAttribute> getAvailableViewAttributes() {
        if (availableViewAttributes == null) {
            loadAvailableViewAttributes();
        }
        return availableViewAttributes;
    }

    public void loadAvailableViewAttributes() {
        UserAttributeDao uadao = UserAttributeDao.getInstance();
        availableViewAttributes = new ArrayList<ViewAttribute>();

        for (ViewAttribute va : uadao.getViewAttributes()) {
            if (filter == null || filter.length() == 0) {
                availableViewAttributes.add(va);
            } else {
                if (va.getName().toLowerCase().contains(filter.toLowerCase())) {
                    availableViewAttributes.add(va);
                } else if (va.getTitle().toLowerCase().contains(filter.toLowerCase())) {
                    availableViewAttributes.add(va);
                }
            }
        }
        availableViewAttributes.removeAll(getViewAttributes());
        Collections.sort(availableViewAttributes);
    }

    public AttributeType getAttributeType() {
        return USER;
    }

    public ViewAttribute newViewAttribute() {
        return new UserViewAttribute();
    }

    public UserAttributesBean(Set<ResourceAttribute> resourceAttributes) {
        this();

        for (ResourceAttribute ras : resourceAttributes) {
            if (!(ras instanceof UserAttributes)) {
                continue;
            }

            String key = ras.getPropertyName();
            UserViewAttribute uva = new UserViewAttribute();
            uva.setName(key);
            getViewAttributes().add(uva);
        }
    }

    public Set<ResourceAttribute> toResourceAttributesSet() {
        Set<ResourceAttribute> resAttributes = new HashSet<ResourceAttribute>();

        for (ViewAttribute va : getViewAttributes()) {
            if (!(va instanceof UserViewAttribute)) {
                continue;
            }
            UserViewAttribute uva = (UserViewAttribute) va;
            UserAttributes uas = new UserAttributes();

            uas.setPropertyName(uva.getName());
            resAttributes.add(uas);
        }

        return resAttributes;
    }

    @Override
    public void reset() {
        super.reset();
        setAttributesHandler(new UserAttributesHandler(this));
        availableViewAttributes = null;
    }
}
