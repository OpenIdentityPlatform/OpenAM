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
 * $Id: Condition.java,v 1.2 2008/06/25 05:42:37 qcheng Exp $
 *
 */
package com.sun.identity.config.pojos.condition;

import java.io.Serializable;

/**
 * @author Victor Alfaro
 */
public class Condition implements Serializable {
    protected Integer id;
    protected String name;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Condition)) {
            return false;
        }
        Condition condition = (Condition) o;
        if (id != null ? !id.equals(condition.id) : condition.id != null) {
            return false;
        }
        /*if (name != null ? !name.equals(condition.name) : condition.name != null) {
            return false;
        }*/
        return true;
    }
}
