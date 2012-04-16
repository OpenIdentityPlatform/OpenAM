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
 * $Id: AttributeMappingsDao.java,v 1.1 2009/06/23 05:56:28 babysunil Exp $
 */

package com.sun.identity.admin.dao;

import com.sun.identity.admin.model.SamlV2ViewAttribute;
import com.sun.identity.console.base.model.AMAdminUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AttributeMappingsDao implements Serializable {

    public List<SamlV2ViewAttribute> getViewAttributes() {
        List<SamlV2ViewAttribute> samlviewAttributes = new ArrayList<SamlV2ViewAttribute>();

        Set userAttrNames = AMAdminUtils.getUserAttributeNames();
        userAttrNames.remove("iplanet-am-user-account-life");
        for (Object s : userAttrNames) {
            SamlV2ViewAttribute va = new SamlV2ViewAttribute();
            //va.setName(s.toString());
            va.setValue(s.toString());
            samlviewAttributes.add(va);
        }

        return samlviewAttributes;
    }
}
