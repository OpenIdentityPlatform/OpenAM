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
 * $Id: SigningKeysDao.java,v 1.1 2009/06/23 05:56:28 babysunil Exp $
 */

package com.sun.identity.admin.dao;

import com.sun.identity.admin.model.SigningKeyBean;
import com.sun.identity.saml.xmlsig.JKSKeyProvider;
import java.io.Serializable;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class SigningKeysDao implements Serializable {

    public List<SigningKeyBean> getSigningKeyBeans() {
        try {
            List<SigningKeyBean> signingKeysBean = new ArrayList<SigningKeyBean>();
            JKSKeyProvider kp = new JKSKeyProvider();
            KeyStore ks = kp.getKeyStore();
            Enumeration e = ks.aliases();
            if (e != null) {
                while (e.hasMoreElements()) {
                    String alias = (String) e.nextElement();
                    if (ks.isKeyEntry(alias)) {
                        SigningKeyBean skb = new SigningKeyBean();
                        skb.setName(alias);
                        signingKeysBean.add(skb);
                    }
                }
            }
            return signingKeysBean;
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }


    }
}
