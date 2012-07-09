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
 * $Id: ReferredApplication.java,v 1.1 2009/11/19 01:02:03 veiming Exp $
 */

package com.sun.identity.entitlement;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author dennis
 */
public class ReferredApplication extends Application {
    private Map<String, Integer> mapResourcesToCount;

    ReferredApplication(
        String realm,
        String name,
        Application application,
        Set<String> res) {
        super();
        application.cloneAppl(this);
        setRealm(realm);
        this.mapResourcesToCount = new HashMap<String, Integer>();
        for (String r : res) {
            this.mapResourcesToCount.put(r, 1);
        }
        super.setResources(mapResourcesToCount.keySet());
    }

    @Override
    public void addResources(Set<String> res) {
        for (String r : res) {
            Integer cnt = mapResourcesToCount.get(r);
            if (cnt != null) {
                int nCount = cnt;
                mapResourcesToCount.put(r, ++nCount);
            } else {
                mapResourcesToCount.put(r, 1);
            }
        }
        super.setResources(mapResourcesToCount.keySet());
    }

    @Override
    public void removeResources(Set<String> res) {
        for (String r : res) {
            Integer cnt = mapResourcesToCount.get(r);
            if (cnt != null) {
                int nCount = cnt;
                if (nCount == 1) {
                    mapResourcesToCount.remove(r);
                } else {
                    mapResourcesToCount.put(r, --nCount);
                }
            }
        }
        super.setResources(mapResourcesToCount.keySet());
    }

    public boolean hasResources() {
        return !mapResourcesToCount.isEmpty();
    }
}
