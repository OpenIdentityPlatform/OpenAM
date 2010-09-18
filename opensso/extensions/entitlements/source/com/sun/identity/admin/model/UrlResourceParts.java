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
 * $Id: UrlResourceParts.java,v 1.3 2009/06/04 11:49:18 veiming Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlResourceParts implements Serializable {

    private static Pattern PART_PATTERN = Pattern.compile("(\\*)|([^\\*])+");
    private List<UrlResourcePart> urlResourceParts;

    public UrlResourceParts(UrlResource ur) {
        urlResourceParts = new ArrayList<UrlResourcePart>();

        Matcher m = PART_PATTERN.matcher(ur.getName());
        while (m.find()) {
            UrlResourcePart urp = new UrlResourcePart();
            urp.setPart(m.group(0));
            urlResourceParts.add(urp);
        }
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        for (UrlResourcePart urp : getUrlResourceParts()) {
            if (urp.getPart().equals("*")) {
                String val = urp.getValue();
                if (val == null || val.length() == 0) {
                    throw new AssertionError("part value(s) are null");
                } else {
                    b.append(val);
                }
            } else {
                b.append(urp.getPart());
            }
        }

        return b.toString();
    }

    public UrlResource getUrlResource() {
        UrlResource ur = new UrlResource();
        ur.setName(toString());

        return ur;
    }

    public List<UrlResourcePart> getUrlResourceParts() {
        return urlResourceParts;
    }

    public boolean isValid() {
        for (UrlResourcePart urp : getUrlResourceParts()) {
            if (urp.getPart().equals("*")) {
                if (urp.getValue() == null || urp.getValue().length() == 0) {
                    return false;
                }
            }
        }
        return true;
    }
}
