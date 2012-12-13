/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CommonAttributeUtils.java,v 1.2 2008/06/25 05:51:59 qcheng Exp $
 *
 */

package com.sun.identity.agents.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * An util class for common attributes 
 */
public class CommonAttributeUtils {
    
   /**
    * Merges the attribute sets present in the source map into the
    * attribute sets present in the destination map. If an attribute set
    * is found in the source map that does not exist in the destination map,
    * it gets directly added to the destination map.
    *  
    * @param destinationMap the map which will contain merged attributes
    * taken for its existing values and the provided source map.
    * @param sourceMap the map that contains attribute sets that must be 
    * merged with the attribute sets of the destination map.
    */
    public static void mergeAttributes(Map destinationMap, Map sourceMap) {
        Iterator it = sourceMap.keySet().iterator();
        while (it.hasNext()) {
            String nextName = (String) it.next();
            Set nextValue = (Set)sourceMap.get(nextName);
            if (destinationMap.containsKey(nextName)) {
                Set currentValue = (Set) destinationMap.get(nextName);
                HashSet newValue = new HashSet();
                newValue.addAll(currentValue);
                newValue.addAll(nextValue);
                destinationMap.put(nextName, newValue);
            } else {
                destinationMap.put(nextName, nextValue);
            }
        }
    }

}
