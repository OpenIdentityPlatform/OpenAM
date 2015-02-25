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
 * $Id: SimpleIterator.java,v 1.1 2009/08/19 05:40:36 veiming Exp $
 */

package com.sun.identity.entitlement.opensso;

import com.sun.identity.shared.BufferedIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SimpleIterator extends BufferedIterator {
    private LinkedList ll = new LinkedList();
    private Iterator items;

    @Override
    public void add(Object entry) {
        ll.add(entry);
    }

    @Override
    public void add(List entry) {
        ll.addAll(entry);
    }

    @Override
    public boolean hasNext() {
        if (items == null) {
            items = ll.iterator();
        }
        return (items.hasNext());
    }

    @Override
    public void isDone() {
        // do nothing
        }

    @Override
    public Object next() {
        return items.next();
    }

    @Override
    public void remove() {
        items.remove();
    }
}
