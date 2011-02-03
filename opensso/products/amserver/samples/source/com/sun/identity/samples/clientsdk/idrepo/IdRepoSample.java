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
 * $Id: IdRepoSample.java,v 1.2 2008/06/25 05:41:13 qcheng Exp $
 *
 */

package com.sun.identity.samples.clientsdk.idrepo;

import java.io.*;
import java.util.*;
import java.lang.Integer;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.OrganizationConfigManager;

/**
 * This class 
 *
 *
 * @author 
 */
public class IdRepoSample {
    static final String DEF_USERNAME = "amAdmin";
    static final String DEF_USERPWD = "openssoxxx";
    static final String DEF_REALM = "/";

    SSOToken ssoToken = null;
    String currentRealm = DEF_REALM;    // string rep of current realm
    Set currentSubRealms = null;        // subrealms of currentRealm
    AMIdentityRepository idRepo = null;        // idrepo for currentRealm

    IdRepoSampleUtils sampleUtils = null;

    public IdRepoSample() {
        sampleUtils = new IdRepoSampleUtils();
    }

    private int printIdRepoMenu() {
        System.out.println ("\nCurrently in realm '" + currentRealm + "'.");
        sampleUtils.printResultsRealm ("Realm '" + currentRealm + "'",
            currentSubRealms, "subrealms");
        System.out.println (
            "  AMIdentityRepository operations\n" +
            "\t0:  Select (sub)Realm           1:  Create Identity\n" +
            "\t2:  Delete Identity             3:  Get Allowed Operations\n" +
            "\t4:  Get Supported IdTypes       5:  Search/Select Identities\n" +
            "\t6:  Return to / realm           7:  Exit\n");
        String sval = sampleUtils.getLine ("Enter selection: ");
        return (sampleUtils.getIntValue(sval));
    }

    /*
     *  for the current Realm, get:
     *    1. its AMIdentityRepository object
     *    2. its AMIdentity (via getRealmIdentity())
     *    3. realm for the AMIdentity (via getRealm())
     *    4. name for the AMIdentity (via getName())
     *    5. its subrealms (via
     *         OrganizationConfigManager.getSubOrganizationNames())
     */

    private void doCurrentRealm () {
        String currentAMIdName = null;
        String currentRealmAMIdName = null;
        try {
            idRepo = new AMIdentityRepository(ssoToken, currentRealm);
            AMIdentity currentRealmAMId = idRepo.getRealmIdentity();
            currentRealmAMIdName = currentRealmAMId.getRealm();
            currentAMIdName = currentRealmAMId.getName();
        } catch (IdRepoException ire) {
            System.err.println(
                "doCurrentRealm:IdRepoException getting AMIdentityRepository" +
                " object for '" + currentRealm + "': " + ire.getMessage());
            System.exit(7);
        } catch (SSOException sse) {
            System.err.println(
                "doCurrentRealm: SSOException getting AMIdentityRepository" +
                " object for '" + currentRealm + "': " + sse.getMessage());
            System.exit(8);
        }

        System.out.println ("AMIdentity realm name for realm '" +
            currentRealm + "' is '" + currentRealmAMIdName + "'");

        System.out.println ("getting subrealms");
        try {
            currentSubRealms = (idRepo.searchIdentities(IdType.REALM,
                "*", new IdSearchControl())).getSearchResults();
        } catch (SSOException ssoe) {
            System.err.println (
                "doCurrentRealm: SSOException getting subrealms for '" +
                currentRealm + "': " + ssoe.getMessage());
        } catch (IdRepoException ire) {
            System.err.println (
                "doCurrentRealm: IdRepoException getting subrealms for '" +
                currentRealm + "': " + ire.getMessage());
        }

        sampleUtils.printResultsRealm("Realm '" + currentRealm + "'",
            currentSubRealms, "subrealms");
    }

    /*
     *  start of IdRepo processing.  have the starting realm name.
     *  get an SSOToken, and start processing requests.
     */

    private void idRepoProcessing()
    {
        /*
         *  get:
         *    1. userid (default "amadmin")
         *    2. userid password (default "openssoxxx")
         *    3. starting realm (default "/")
         */
        String userSID = sampleUtils.getLine("Userid", DEF_USERNAME);
        String userPWD = sampleUtils.getLine("Userid " + userSID +
            "'s password", DEF_USERPWD);
        String realmName = sampleUtils.getLine("Realm", DEF_REALM);

        //  login and get the SSOToken

        try {
            ssoToken = sampleUtils.realmLogin(userSID, userPWD, realmName);
        } catch (SSOException ssoe) {
            System.err.println ("idRepoProcessing: could not get SSOToken: " +
                ssoe.getMessage());
            System.exit(3);
        } catch (AuthLoginException ale) {
            System.err.println ("idRepoProcessing: could not authenticate: " +
                ale.getMessage());
            System.exit(4);
        } catch (Exception e) {
            System.err.println (
                "idRepoProcessing: exception getting SSOToken: " +
                e.getMessage());
            System.exit(5);
        }

        /*
         *  retrieve some information about the current realm, if
         *  we can as the userid specified.
         */

        currentRealm = realmName;
        doCurrentRealm();

        int i = -1;
        boolean doMore = true;
        String ans = null;
        int ians = -1;

        while (doMore) {
            i = printIdRepoMenu();
            switch (i) {
                case 0:  // select (sub)realm
                    IdRepoSampleSubRealm issr =
                        new IdRepoSampleSubRealm (currentRealm);
                    String nextSubRealm =
                        issr.selectSubRealm (currentSubRealms);
                    if (nextSubRealm != currentRealm) {
                        currentRealm = nextSubRealm;
                        try {
                            idRepo = new AMIdentityRepository(ssoToken,
                                currentRealm);
                        } catch (IdRepoException ire) {
                            System.err.println(
                                "idRepoProcessing: IdRepoException getting " +
                                "AMIdentityRepository object for '" +
                                currentRealm + "': " +
                                ire.getMessage());
                            break;
                        } catch (SSOException ssoe) {
                            System.err.println(
                                "idRepoProcessing: SSOException getting " +
                                "AMIdentityRepository object for '" +
                                currentRealm + "': " +
                                ssoe.getMessage());
                        }
                        doCurrentRealm();
                    }
                    break;

                case 1:  // create identity
                    IdRepoSampleCreateId isci =
                        new IdRepoSampleCreateId (idRepo);
                    isci.createAMId();
                    break;

                case 2:  // delete identity
                    IdRepoSampleDeleteId isdi =
                        new IdRepoSampleDeleteId (idRepo);
                    isdi.deleteAMId();
                    break;

                case 3:  // get allowed id operations
                    try {
                        Set types = idRepo.getSupportedIdTypes();
                        IdType itype = null;
                        Set ops = null;
                        for (Iterator it = types.iterator(); it.hasNext(); ) {
                            itype = (IdType)it.next();
                            ops = idRepo.getAllowedIdOperations (itype);
                            sampleUtils.printResults ("IdType '" +
                                itype.getName() + "'",
                                ops, "allowed Identity Operations");
                        }
                    } catch (IdRepoException ire) {
                        System.err.println (
                            "idRepoProcessing:IdRepoException: " +
                                ire.getMessage());
                    } catch (SSOException ssoe) {
                        System.err.println (
                            "idRepoProcessing:SSOException: " +
                                ssoe.getMessage());
                    }
                    break;

                case 4:  // get supported IdTypes
                    try {
                        Set types = idRepo.getSupportedIdTypes();
                        sampleUtils.printIdTypeResults("This deployment",
                            types, "supported IdTypes");
                    } catch (IdRepoException ire) {
                        System.err.println (
                            "idRepoProcessing:IdRepoException: " +
                                ire.getMessage());
                    } catch (SSOException ssoe) {
                        System.err.println (
                            "idRepoProcessing:SSOException: " +
                                ssoe.getMessage());
                    }
                    break;

                case 5:  // search/select Identities
                    IdRepoSampleSearchIds issi =
                        new IdRepoSampleSearchIds (idRepo);
                    issi.searchAMIds();
                    break;

                case 6:  // return to '/' realm
                    currentRealm = DEF_REALM;
                    doCurrentRealm();
                    break;

                case 7:  // exit
                    doMore = false;
                    break;

                default:
                    System.err.println ("Invalid selection; try again.");
            }
        }

        try {
            sampleUtils.logout();
        } catch (AuthLoginException alexc) {
            System.err.println ("idRepoProcessing: logout failed for user '" +
                userSID + "'");
            alexc.printStackTrace();
            System.exit(10);
        }
        System.out.println ("idRepoProcessing: user '" + userSID +
            "' logged out");
    }

    public static void main(String[] args) {
        IdRepoSample idRS = new IdRepoSample();
        idRS.idRepoProcessing();

        System.exit(0);
    }
}




