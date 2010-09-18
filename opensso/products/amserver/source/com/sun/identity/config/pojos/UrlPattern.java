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
 * $Id: UrlPattern.java,v 1.2 2008/06/25 05:42:36 qcheng Exp $
 *
 */
package com.sun.identity.config.pojos;

import java.io.Serializable;

/**
 * @author Victor Alfaro
 */
public class UrlPattern implements Serializable {
    private Integer id;
    private String pattern;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof UrlPattern)) {
            return false;
        }
        UrlPattern that = (UrlPattern) o;
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        /*if (pattern != null ? !pattern.equals(that.pattern) : that.pattern != null) {
            return false;
        }*/
        return true;
    }
}
