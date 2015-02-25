/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS Inc. All Rights Reserved
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
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted [2010-2012] [ForgeRock AS]
 *
 */
package com.iplanet.dpro.session.service;

import com.sun.identity.sm.model.AMRecord;
import com.sun.identity.sm.model.FAMRecord;

import java.io.Serializable;

/**
 * MockInternalSession Test POJO.
 *
 * @author jeff.schenk@forgerock.com
 * @version 10.1
 * @since <pre>Aug 29, 2012</pre>
 */
public class MockInternalSession implements Serializable {
    private static final long serialVersionUID = 101L;   //  10.1

    private AMRecord amRecord;

    private FAMRecord famRecord;

    public MockInternalSession() {
    }

    public AMRecord getAmRecord() {
        return amRecord;
    }

    public void setAmRecord(AMRecord amRecord) {
        this.amRecord = amRecord;
    }

    public FAMRecord getFamRecord() {
        return famRecord;
    }

    public void setFamRecord(FAMRecord famRecord) {
        this.famRecord = famRecord;
    }

}
