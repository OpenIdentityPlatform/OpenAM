/*
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
 * $Id: DelegationResourceNameSplitter.java,v 1.1 2009/06/13 04:38:55 arviranga Exp $
 */

package com.sun.identity.entitlement.util;

import com.sun.identity.entitlement.ResourceSearchIndexes;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author aravind
 */
public class DelegationResourceNameSplitter implements ISearchIndex {

    public ResourceSearchIndexes getIndexes(String resource) {
        Set<String> set = new HashSet<String>();
        set.add("all");
        return (new ResourceSearchIndexes(set, set, set));
    }
}
