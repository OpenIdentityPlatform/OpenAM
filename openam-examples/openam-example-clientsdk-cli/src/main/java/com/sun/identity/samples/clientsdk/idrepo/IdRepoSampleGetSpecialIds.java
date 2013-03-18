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
 * $Id: IdRepoSampleGetSpecialIds.java,v 1.2 2008/06/25 05:41:14 qcheng Exp $
 *
 */

package com.sun.identity.samples.clientsdk.idrepo;

import java.io.*;
import java.util.*;

import com.iplanet.sso.SSOException;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdRepoException;


/**
 * This class 
 *
 *
 * @author 
 */
public class IdRepoSampleGetSpecialIds {

    IdRepoSampleUtils sampleUtils = null;
    AMIdentityRepository idRepo = null;

    public IdRepoSampleGetSpecialIds (AMIdentityRepository idrepo) {
        sampleUtils = new IdRepoSampleUtils();
        idRepo = idrepo;
    }

    /*
     *  The special AMIdentities are for internal use only.
     */
    public void getSpecialIds () {
        try {
            IdSearchResults isr = null;
            Set isrSet = null;
            Map isrMap = null;
            /*
             * get special role identities
             */

            isr = idRepo.getSpecialIdentities(IdType.ROLE);
            isrSet = isr.getSearchResults();
            isrMap = isr.getResultAttributes();

                processSpecialIdentity (isrMap, isrSet, "Special Roles");

            /*
             * get special user identities
             */

            sampleUtils.waitForReturn("Hit <return> to get special users: ");
            isr = idRepo.getSpecialIdentities(IdType.USER);
            isrSet = isr.getSearchResults();
            isrMap = isr.getResultAttributes();

                processSpecialIdentity (isrMap, isrSet, "Special Users");

            /*
             * get special group identities
             */

            sampleUtils.waitForReturn("Hit <return> to get special groups: ");
            isr = idRepo.getSpecialIdentities(IdType.GROUP);
            isrSet = isr.getSearchResults();
            isrMap = isr.getResultAttributes();

                processSpecialIdentity (isrMap, isrSet, "Special Groups");

                /*
             * get special agent identities
             */

            sampleUtils.waitForReturn("Hit <return> to get special agents: ");
            isr = idRepo.getSpecialIdentities(IdType.AGENT);
            isrSet = isr.getSearchResults();
            isrMap = isr.getResultAttributes();

                processSpecialIdentity (isrMap, isrSet, "Special Agents");

            /*
             * get special realm identities
             */

            sampleUtils.waitForReturn("Hit <return> to get special realms: ");
            isr = idRepo.getSpecialIdentities(IdType.REALM);
            isrSet = isr.getSearchResults();
            isrMap = isr.getResultAttributes();

                processSpecialIdentity (isrMap, isrSet, "Special Realms");

            /*
             * get special Filtered Role identities
             */

            sampleUtils.waitForReturn(
                "Hit <return> to get special filtered roles: ");
            isr = idRepo.getSpecialIdentities(IdType.FILTEREDROLE);
            isrSet = isr.getSearchResults();
            isrMap = isr.getResultAttributes();

                processSpecialIdentity (isrMap, isrSet, "Special Filtered Roles");

        } catch (IdRepoException ire) {
            System.err.println ("idRepoProcessing:IdRepoException: " +
                ire.getMessage());
        } catch (SSOException ssoe) {
            System.err.println ("idRepoProcessing:SSOException: " +
                ssoe.getMessage());
        }
        return;
    }

    /*
     *  print the Map (from IdSearchResults.getResultAttributes()) and
     *  the Set (from IdSearchResults.getSearchResults()) resulting from
     *  the AMIdentityRepository.getSpecialIdentities(IdType.xxx) call.
     */
    private void processSpecialIdentity (Map isrMap, Set isrSet, String title)
    {
        printMap (isrMap, title + " (" + isrMap.size() +")");
        System.out.println("");
        sampleUtils.waitForReturn(
            "Hit <return> to continue display of " + title + ": ");

        if (!isrSet.isEmpty()) {
            System.out.println (title + ":");
            AMIdentity am_id = null;
            for(Iterator it=isrSet.iterator(); it.hasNext();) {
                try {
                    am_id = (AMIdentity)it.next();
                    System.out.println("  For '" + am_id.getName() + "':");
                    System.out.println(
                        "    realm: " + am_id.getRealm() +
                        "\n    type: "  + am_id.getType() +
                        "\n    universalId: " + am_id.getUniversalId() +
                        "\n    active: " + am_id.isActive() +
                        "\n    exists: " + am_id.isExists() +
                        "\n");
                    } catch (IdRepoException ire) {
                        System.err.println (
                            "processSpecialIdentity:IdRepoException: " +
                                ire.getMessage());
                    } catch (SSOException ssoe) {
                        System.err.println (
                            "processSpecialIdentity:SSOException: " +
                                ssoe.getMessage());
                    }
            }
        } else {
            System.out.println ("No " + title);
        }
    }


    /*
     *  theMap:
     *    AMIdentity, Map of:
     *                String, Set of:
     *                        String
     */

    private void printMap (Map theMap, String title)
    {
        if (theMap == null) {
            System.out.println ("Null Map of " + title);
        } else if (theMap.isEmpty()) {
            System.out.println ("Map of " + title + " is empty");
        } else {
            Set keySet = theMap.keySet();
            System.out.println (title);
            Object obj = null;
            Object val = null;
            AMIdentity ami = null;
            Map vMap = null;
            Set vSet = null;
            for (Iterator it=keySet.iterator(); it.hasNext(); ) {
                obj = it.next();
                ami = (AMIdentity)obj;
                System.out.println ("  " + ami.getName());
                val = theMap.get(ami);
                if (val != null) {
                    vMap = (Map)val;
                    vSet = vMap.keySet();
                    System.out.println ("    Attributes for " +
                        ami.getName() + ":");

                    Object obj2 = null;
                    Object obj3 = null;
                    Object obj4 = null;
                    Set o3Set = null;
                    for (Iterator it2=vSet.iterator(); it2.hasNext(); ) {
                        obj2 = it2.next();
                        System.out.println ("    " + obj2);
                        obj3 = vMap.get(obj2);
                        o3Set = (Set)obj3;
                        System.out.println ("      Value(s) for " + obj2
                            + ":");
                        if (o3Set.isEmpty()) {
                            System.out.println("      [NONE]");
                        }
                        for (Iterator it3=o3Set.iterator(); it3.hasNext(); ) {
                            obj4 = it3.next();
                            System.out.println("      " + obj4);
                        }
                    }
                } else {
                    System.out.println("theMap.get(ami) rtns null");
                }
            }
        }
    }
}


