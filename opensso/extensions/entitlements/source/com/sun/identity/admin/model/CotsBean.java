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
 * $Id: CotsBean.java,v 1.1 2009/06/15 18:43:46 asyhuang Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.admin.dao.CotDao;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.model.SelectItem;

public class CotsBean implements Serializable {
    private List<CotBean> cotBeans;   
    private CotDao cotDao;

    public void addByName(String cotname) {
        CotBean cot = new CotBean(cotname);
        cotBeans.add(cot);
    }

    public void setCotBeans(String realm) {
        this.cotDao = new CotDao();
        setCotBeans(cotDao.getCotBeans(realm));      
    }

    public void setCotBeans(List<CotBean> cotBeans) {
        this.cotBeans = cotBeans;
    }
   
    public List<CotBean> getCotBeans() {
        return cotBeans;
    }

    public List<SelectItem> getCotBeanItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (CotBean rb : cotBeans) {
            items.add(new SelectItem(rb, rb.getName()));
        }
        return items;
    }
}