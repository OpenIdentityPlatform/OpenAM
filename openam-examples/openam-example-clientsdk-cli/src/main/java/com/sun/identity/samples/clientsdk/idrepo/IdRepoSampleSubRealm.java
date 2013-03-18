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
 * $Id: IdRepoSampleSubRealm.java,v 1.2 2008/06/25 05:41:14 qcheng Exp $
 *
 */

package com.sun.identity.samples.clientsdk.idrepo;

import java.io.*;
import java.util.*;

import com.sun.identity.idm.AMIdentity;

/**
 * This class 
 *
 *
 * @author 
 */
public class IdRepoSampleSubRealm {

    IdRepoSampleUtils sampleUtils = null;
    String currentRealm = null;
    static final String startingRealm = "/";

    public IdRepoSampleSubRealm (String curRealm) {
        sampleUtils = new IdRepoSampleUtils();
        currentRealm = curRealm;
    }

    public String selectSubRealm (Set currentSubRealms) {
        int i2;
        Object[] srs = currentSubRealms.toArray();
        System.out.println("SubRealms of " + currentRealm + ":");
        for (i2 = 0; i2 < srs.length; i2++) {
            System.out.println("    " + i2 + ": " +
                ((AMIdentity)srs[i2]).getRealm());
        }
        System.out.println ("    " + i2 + ": No selection");
        String ans =
            sampleUtils.getLine("Select subrealm: [0.." + srs.length + "]: ");
        int ians = sampleUtils.getIntValue(ans);
        if ((ians >= 0) && (ians < srs.length)) {
            currentRealm = (String)((AMIdentity)srs[ians]).getRealm();
        } else if (ians == srs.length) { // no selection
        } else {  // invalid selection
            System.err.println ("'" + ans + "' is invalid.");
        }
        return currentRealm;
    }

}


