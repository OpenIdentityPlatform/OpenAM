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
 * $Id: ListFormatter.java,v 1.1 2009/06/25 22:39:51 farble1670 Exp $
 */

package com.sun.identity.admin;

import com.icesoft.faces.context.Resource;
import java.util.Iterator;
import java.util.List;

public class ListFormatter {
    private List list;

    public ListFormatter(List list) {
        this.list = list;
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();

        for (Iterator<Resource> i = list.iterator(); i.hasNext();) {
            b.append(i.next());
            if (i.hasNext()) {
                b.append(",");
            }

        }

        return b.toString();
    }

    public String toFormattedString() {
        StringBuffer b = new StringBuffer();

        for (Iterator<Resource> i = list.iterator(); i.hasNext();) {
            b.append(i.next());
            if (i.hasNext()) {
                b.append("\n");
            }

        }

        return b.toString();
    }
}
