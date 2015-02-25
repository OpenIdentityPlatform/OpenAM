/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * $Id: StsConfiguration.java,v 1.2 2009/08/03 22:25:31 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.wss.provider.ProviderUtils;
import com.sun.identity.wss.provider.STSConfig;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.faces.model.SelectItem;

public class StsConfiguration implements Serializable
{
    public static final String HOSTED_STS_PROFILE_NAME = "SecurityTokenService";


    static public List<SelectItem> getSelectItems() {
        ArrayList items = new ArrayList();

        Iterator i = ProviderUtils.getAllSTSConfig().iterator();
        while( i.hasNext() ) {
            STSConfig stsConfig = (STSConfig) i.next();
            items.add(new SelectItem(stsConfig.getName()));
        }

        return items;
    }
}
