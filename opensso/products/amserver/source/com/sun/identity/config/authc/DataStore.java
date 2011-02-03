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
 * $Id: DataStore.java,v 1.3 2008/06/25 05:42:32 qcheng Exp $
 *
 */
package com.sun.identity.config.authc;

import com.sun.identity.config.util.AjaxPage;

/**
 * @author Jeffrey Bermudez
 */
public class DataStore extends AjaxPage {

    public com.sun.identity.config.pojos.DataStore dataStore = null;

    public void onInit() {
        dataStore = new com.sun.identity.config.pojos.DataStore();
    }

    public void onPost() {
        dataStore.getRealm().setName(toString("realmName"));

        save(dataStore);
    }

    protected void save(com.sun.identity.config.pojos.DataStore dataStore) {
        getConfigurator().addAuthenticationStore(dataStore);
    }
}
