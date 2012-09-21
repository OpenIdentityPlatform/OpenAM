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
 * $Id: CotDao.java,v 1.2 2009/06/24 01:41:35 asyhuang Exp $
 */
package com.sun.identity.admin.dao;

import com.sun.identity.admin.model.CotBean;
import com.sun.identity.cot.COTException;
import com.sun.identity.cot.CircleOfTrustManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CotDao implements Serializable {

    private CircleOfTrustManager cotManager;

    public List<CotBean> getCotBeans(String realm) {
        List<CotBean> cotBeans = new ArrayList();
        if (realm == null) {
            realm = "/";
        }

        try {
            CircleOfTrustManager manager = getCircleOfTrustManager();
            Set cotSet = manager.getAllCirclesOfTrust(realm);           
            for (Object cb: cotSet){
                CotBean c = new CotBean((String)cb);
                cotBeans.add(c);
             }           
        } catch (COTException e) {
            throw new RuntimeException(e);
        }
        
        return cotBeans;
    }

    private CircleOfTrustManager getCircleOfTrustManager()
            throws COTException {
        if (cotManager == null) {
            cotManager = new CircleOfTrustManager();
        }
        return cotManager;
    }
}
