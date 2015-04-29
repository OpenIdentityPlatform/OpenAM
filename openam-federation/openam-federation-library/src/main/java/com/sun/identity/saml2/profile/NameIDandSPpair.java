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
 * $Id: NameIDandSPpair.java,v 1.3 2008/06/25 05:47:54 qcheng Exp $
 *
 * Portions Copyrighted 2013-2015 ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import com.sun.identity.saml2.assertion.NameID;
import java.io.Serializable;

/**
 * This class represents a pair of <code>NameID</code> and its 
 * corresponding <code>SPEntityID</code>.
 */
public class NameIDandSPpair implements Serializable {

    private NameID nameID = null;
    private String spEntityID = null;

    /**
     * Default constructor for deserialization.
     */
    public NameIDandSPpair() {
    }

    /**
     * Constructor for a <code>NameIDandSPpair</code>.
     *
     * @param nameID the <code>NameID</code> object 
     * @param spEntityID the <code>SPEntityID</code> 
     */ 
    public NameIDandSPpair(NameID nameID, String spEntityID) {
        this.nameID = nameID;
        this.spEntityID = spEntityID;
    }

    /**
     * Returns the <code>SPEntityID</code>.
     *
     * @return the <code>SPEntityID</code>
     */ 
    public String getSPEntityID() {
        return spEntityID;
    }

    /**
     * Returns the <code>NameID</code>.
     *
     * @return the <code>NameID</code>
     */ 
    public NameID getNameID() {
        return nameID;
    }

    @Override
    public String toString() {
        return "NameIDandSPpair{" + "nameID=" + nameID + ", spEntityID=" + spEntityID + '}';
    }
}
