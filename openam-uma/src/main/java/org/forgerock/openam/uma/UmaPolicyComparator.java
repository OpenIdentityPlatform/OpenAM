/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import java.util.Comparator;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.resource.SortKey;

public class UmaPolicyComparator implements Comparator<UmaPolicy> {

    private final SortKey key;

    public UmaPolicyComparator(SortKey key) {
        this.key = key;
    }

    @Override
    public int compare(UmaPolicy o1, UmaPolicy o2) {
        int cmp = 0;
        if (new JsonPointer("/policyId").equals(key.getField())) {
            cmp = o1.getId().compareTo(o2.getId());
        } else if (new JsonPointer("/name").equals(key.getField())) {
            cmp = o1.getResourceSet().getName().compareTo(o2.getResourceSet().getName());
        }
        if (!key.isAscendingOrder()) {
            cmp = -cmp; // Reverse comparison for descending order
        }
        return cmp;
    }
}
