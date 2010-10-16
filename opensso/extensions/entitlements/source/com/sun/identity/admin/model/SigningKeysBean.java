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
 * $Id: SigningKeysBean.java,v 1.1 2009/06/23 06:36:21 babysunil Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.dao.SigningKeysDao;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.model.SelectItem;

public class SigningKeysBean implements Serializable {

    private List<SigningKeyBean> signingKeyBeans;
    private SigningKeyBean signingKeyBean;
    private SigningKeysDao signingKeysDao;

    public void setSigningKeysDao(SigningKeysDao signingKeysDao) {
        this.signingKeysDao = signingKeysDao;
        setSigningKeyBeans(signingKeysDao.getSigningKeyBeans());
        setSigningKeyBean(signingKeyBeans.get(0));
    }

    public List<SelectItem> getSigningKeyBeanItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (SigningKeyBean skb : signingKeyBeans) {
            items.add(new SelectItem(skb, skb.getTitle()));
        }
        return items;
    }

    public static SigningKeysBean getInstance() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        SigningKeysBean signingKeysBean = (SigningKeysBean) mbr.resolve("signingKeysBean");
        return signingKeysBean;
    }

    public List<SigningKeyBean> getSigningKeyBeans() {
        return signingKeyBeans;
    }

    public void setSigningKeyBeans(List<SigningKeyBean> signingKeyBeans) {
        this.signingKeyBeans = signingKeyBeans;
    }

    public SigningKeyBean getSigningKeyBean() {
        return signingKeyBean;
    }

    public void setSigningKeyBean(SigningKeyBean signingKeyBean) {
        this.signingKeyBean = signingKeyBean;
    }
}
