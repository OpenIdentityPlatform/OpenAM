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
 * $Id: IdRepoSampleDeleteId.java,v 1.3 2008/06/25 05:41:13 qcheng Exp $
 *
 */

package com.sun.identity.samples.clientsdk.idrepo;

import java.io.*;
import java.util.*;

import com.iplanet.sso.SSOException;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdRepoException;


/**
 * This class 
 *
 *
 * @author 
 */
public class IdRepoSampleDeleteId {

    IdRepoSampleUtils sampleUtils = null;
    AMIdentityRepository idRepo = null;

    public IdRepoSampleDeleteId (AMIdentityRepository idrepo) {
        sampleUtils = new IdRepoSampleUtils();
        idRepo = idrepo;
    }

    public void deleteAMId () {
        IdType idtype = sampleUtils.getIdTypeToCreateOrDelete();
        if (idtype == null) {
            return;
        }

        if (idtype.equals(IdType.AGENT)) {
            System.out.println("Use IdType 'agentonly' for deletion of "+
                "agents.\nOnly operation supported for IdType 'agent' is "+
                "READ");
            return;
        }

        try {
            /*
             *  get and display list of identities of idtype
             *  for reference...
             */
            IdSearchResults adRes = idRepo.searchIdentities(
                 idtype, "*", new IdSearchControl());
            Set adResSet = adRes.getSearchResults();
            System.out.println("Found " + adResSet.size() +
                " entries of type " + idtype.getName() + ".");
            AMIdentity amid = null;
            if (adResSet.size() > 0) {
                int i;
                String ans = null;
                    Object[] ids = adResSet.toArray();
                    System.out.println("AMIdentities:");
                    for (i = 0; i < ids.length; i++) {
                    amid = (AMIdentity)ids[i];
                    System.out.println("\t" + i + ": " + amid.getName());
                    }
                System.out.println("\t" + i + ": No selection");
                ans = sampleUtils.getLine (
                    "Select id: [0.." + ids.length + "]: ");
                i = sampleUtils.getIntValue(ans);
                if (i == ids.length) {
                    // no selection
                    return;
                } else if ((i < 0) || (i > ids.length)) {
                    System.err.println (ans + " is an invalid selection.");
                    return;
                }
                amid = (AMIdentity)ids[i];

                boolean doAnyway = false;
                String tmpS = amid.getName().toLowerCase();
                if (amid.getType().equals(IdType.USER)) {
                    if (tmpS.equals("dsameuser") ||
                        tmpS.equals("amldapuser") ||
                        tmpS.equals("amadmin") ||
                        tmpS.equals("amservice-urlaccessagent") ||
                        tmpS.equals("anonymous"))
                    {
                        System.out.println ("VERY BAD idea deleting user "
                            + amid.getName());
                    } else {
                        doAnyway = true;
                    }
                } else if (amid.getType().equals(IdType.REALM)) {
                    // need to select from returned set
                    // but not "/"!
                    doAnyway = true;
                } else {
                    //  no (default) AGENTs to worry about.
                    doAnyway = true;
                }
                if (doAnyway) {
                    Set tmpSet = new HashSet();
                    tmpSet.add(amid);
                    idRepo.deleteIdentities(tmpSet);
                } else {
                    System.out.println ("Not deleting " + amid.getName());
                }
            } else {
                System.out.println ("No identities of type " +
                    idtype.getName() + " found.");
            }

            /*
             *  now show the (updated) list of the
             *  identities of type idtype
             */
            adRes = idRepo.searchIdentities(
                idtype, "*", new IdSearchControl());
            adResSet = adRes.getSearchResults();

            System.out.print("    Current list of " + idtype.getName() + "s");
            if (!adResSet.isEmpty()) {
                System.out.println (":");
                for (Iterator it = adResSet.iterator(); it.hasNext(); ) {
                    System.out.println("\t" +
                        ((AMIdentity)it.next()).getName());
                }
            } else {
                System.out.println (" is empty");
            }
        } catch (IdRepoException ire) {
            System.err.println("idRepoProcessing: IdRepoException" +
                " Deleting Identity: " + ire.getMessage());
        } catch (SSOException ssoe) {
            System.err.println("idRepoProcessing: SSOException" +
                " Deleting Identity: " + ssoe.getMessage());
        }
        return;
    }
}


