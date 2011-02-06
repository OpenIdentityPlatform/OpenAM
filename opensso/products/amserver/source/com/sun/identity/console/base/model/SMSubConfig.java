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
 * $Id: SMSubConfig.java,v 1.2 2008/06/25 05:42:50 qcheng Exp $
 *
 */

package com.sun.identity.console.base.model;
import java.io.Serializable;

/* - NEED NOT LOG - */

/**
 * Sub Configuration Display Object.
 */
public class SMSubConfig
    implements Serializable {
    private String id;
    private String name;
    private String type;

    private SMSubConfig() {
    }

    /**
     * Creates an instance of <code>SMSubConfig</code>.
     *
     * @param id ID of the sub configuration.
     * @param name Name of the sub configuration.
     * @param type Type of the sub configuration.
     */
    public SMSubConfig(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    /**
     * Returns ID.
     *
     * @return ID.
     */
    public String getID() {
        return id;
    }

    /**
     * Returns name.
     *
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns type.
     *
     * @return type.
     */
    public String getType() {
        return type;
    }
}
