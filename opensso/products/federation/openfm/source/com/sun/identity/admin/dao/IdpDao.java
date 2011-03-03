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
 * $Id: IdpDao.java,v 1.1 2009/12/08 02:02:30 babysunil Exp $
 */

package com.sun.identity.admin.dao;

import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.COTException;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.admin.model.WebExConfigWizardBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;


public class IdpDao implements Serializable {

    public List<SelectItem> getAvailableIdpNames(String selectedCot) {
        List<SelectItem> items = new ArrayList<SelectItem>();
        try {
            CircleOfTrustManager cotMgr = new CircleOfTrustManager();
            Set<String> entities = cotMgr.listCircleOfTrustMember("/", selectedCot, COTConstants.SAML2);
            SAML2MetaManager mgr = new SAML2MetaManager();
            if (entities != null && !entities.isEmpty()) {
                for (Iterator<String> i = entities.iterator(); i.hasNext();) {
                    String entityId = i.next();
                    EntityConfigElement elm = mgr.getEntityConfig("/", entityId);
                    if (elm.isHosted()) {
                        EntityDescriptorElement desc = mgr.getEntityDescriptor("/", entityId);
                        if (SAML2MetaUtils.getIDPSSODescriptor(desc) != null) {
                            items.add(new SelectItem(entityId));
                        }
                    }

                }
            } else if (entities == null || entities.isEmpty()){
                WebExConfigWizardBean wzbean = new WebExConfigWizardBean();
                wzbean.setCreateNew(false);
                wzbean.setChooseExistng(true);
            }
            return items;
        } catch (COTException e) {
            throw new RuntimeException(e);
        } catch (SAML2MetaException e) {
            throw new RuntimeException(e);
        }
    }
}
