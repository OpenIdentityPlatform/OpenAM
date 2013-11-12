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
 * $Id: IdRepoSampleSearchIds.java,v 1.4 2008/06/25 05:41:14 qcheng Exp $
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
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdRepoException;


/**
 * This class 
 *
 *
 * @author 
 */
public class IdRepoSampleSearchIds {

    IdRepoSampleUtils sampleUtils = null;
    AMIdentityRepository idRepo = null;

    public IdRepoSampleSearchIds (AMIdentityRepository idrepo) {
        sampleUtils = new IdRepoSampleUtils();
        idRepo = idrepo;
    }

    public void searchAMIds () {
        IdType idtype = sampleUtils.getIdType(idRepo);
        if (idtype == null) {
            return;
        }
        IdSearchControl isc = new IdSearchControl();
        isc.setAllReturnAttributes(true);
        //  recursive setting is done via the data store config

        String pattern = sampleUtils.getLine("    Enter search pattern", "*");
        try {
            IdSearchResults adRes = idRepo.searchIdentities(
                idtype, pattern, isc);
            Set adResSet = adRes.getSearchResults();
            processType(idtype, adResSet);
        IdSearchControl control = new IdSearchControl();           
        control.setAllReturnAttributes(true);
        control.setTimeOut(0);          
        Map kvPairMap = new HashMap();
        Set set = new HashSet();
        set.add("STSAgent");           
        kvPairMap.put("AgentType", set);
        control.setSearchModifiers(IdSearchOpModifier.OR, kvPairMap);
        IdSearchResults results = idRepo.searchIdentities(IdType.AGENTONLY,
            "*", control);
        Set agents = results.getSearchResults();      
        System.out.println("Listing agents for STSAgent "+agents);
        } catch (IdRepoException ire) {
            System.err.println("idRepoProcessing: IdRepoException" +
                " Searching Identities for '" +
                idtype + "' and pattern '" + pattern + "': " +
                ire.getMessage());
        } catch (SSOException ssoe) {
            System.err.println("idRepoProcessing: SSOException" +
                " Searching Identities for '" +
                idtype + "' and pattern '" + pattern + "': " +
                ssoe.getMessage());
        }

        return;
    }

    /*
     *  given a set of AMIdentities of IdType idtype, see
     *  if any AMIdentity operations are to be performed
     *  on/with them.
     */
    private void processType(IdType idtype, Set idSet)
    {
        Object[] objs = idSet.toArray();
        AMIdentity amid = null;
        AMIdentity amid2 = null;
        int setsize = idSet.size();

        int i;
        if (setsize > 0) {
            System.out.println("Search returns " + setsize +
                " entries of type " + idtype.getName() + ".");

            for (i = 0; i < setsize; i++) {
                amid = (AMIdentity)objs[i];
                System.out.println("\t" + i + ": " + amid.getName());
            }
            System.out.println ("\t" + i + ": No selection");

            String answer = sampleUtils.getLine("Select identity: [0.." +
                setsize + "]: ");

            int ians = sampleUtils.getIntValue(answer);
            try {
                if ((ians >= 0) && (ians < setsize)) {
                    amid = (AMIdentity)objs[ians];
                } else if (ians == setsize) {
                    return;
                } else {
                    System.err.println ("'" + answer +
                        "' is invalid.");
                    return;
                }

                System.out.println (" universalId for " +
                    amid.getName() + " of IdType " + idtype.getName() +
                    " = " + amid.getUniversalId());

                /*
                 * have the AMIdentity to work with in amid
                 *
                 * for IdType given, the operations allowed:
                 *
                 *  GROUP 
                 */
                if (idtype.equals(IdType.GROUP)) {
                    /*
                     * can:
                     *   get attributes
                     *   get attribute
                     *   get members (of type User)
                     */
                    System.out.println (
                        "Members of IdType User of Group '" +
                        amid.getName() + "':");
                    printMembers(amid, IdType.USER);
                    printAttrs(amid);
                } else if (idtype.equals(IdType.ROLE)) {
                    /*
                     * can:
                     *  get attributes
                     *  get attribute
                     *  get members
                     */
                    printAttrs(amid);
                } else if (idtype.equals(IdType.USER)) {
                    String thisUser = amid.getName();
                    /*
                     * can:
                     *   see if active
                     *   set active status
                     *   get attributes
                     *   get attribute
                     *   set attributes
                     *   remove attributes
                     *   store
                     *   get memberships
                     *   see if exists
                     */
                    
                    System.out.println("User '" + thisUser +
                        "' is active: " + amid.isActive());
                    if (thisUser.equalsIgnoreCase("amadmin") ||
                        thisUser.equalsIgnoreCase("amldapuser") ||
                        thisUser.equalsIgnoreCase("dsameuser") ||
                        thisUser.equalsIgnoreCase("amService-URLAccessAgent"))
                    {
                        // don't want to mess too much with these users
                        // in particular
                        System.out.println("User '" + amid.getName() +
                            "' exists: " + amid.isExists());

                        Set idtypes = amid.getType().canBeMemberOf();
                        System.out.println (amid.getName() +
                            " can have (and has) membership in identities of " +
                            "the following types:");
                        IdType idTypeToUse = null;
                        Set memberships = null;
                        for (Iterator it = idtypes.iterator(); it.hasNext(); ) {
                            idTypeToUse = (IdType)it.next();
                            System.out.println ("  can be member of " + 
                                idTypeToUse.getName());
                            memberships = amid.getMemberships(idTypeToUse);
                            printMemberships(amid, idTypeToUse, memberships);
                        }
                        printAttrs(amid);

                    } else {
                        answer = sampleUtils.getLine (
                            "Set user active, inactive, or cancel [a,i,c]: ");
                        if (answer.startsWith("a")) {
                            if (amid.isActive()) {
                                System.out.println("User '" + thisUser +
                                    "' already active");
                            } else {
                                amid.setActiveStatus(true);
                                System.out.println("User '" + thisUser +
                                    "' is active: " + amid.isActive());
                            }
                        } else if (answer.startsWith("i")) {
                            if (!amid.isActive()) {
                                System.out.println("User '" + thisUser +
                                    "' already inactive");
                            } else {
                                amid.setActiveStatus(false);
                                System.out.println("User '" + thisUser +
                                    "' is active: " + amid.isActive());
                            }
                        }

                        System.out.println("User '" + amid.getName() +
                            "' exists: " + amid.isExists());

                        Set idtypes = amid.getType().canBeMemberOf();
                        System.out.println (amid.getName() +
                            " can have (and has) membership in identities of " +
                            "the following types:");
                        IdType idTypeToUse = null;
                        Set memberships = null;
                        for (Iterator it = idtypes.iterator(); it.hasNext(); ) {
                            idTypeToUse = (IdType)it.next();
                            System.out.println ("  can be member of " + 
                                idTypeToUse.getName());
                            memberships = amid.getMemberships(idTypeToUse);
                            printMemberships(amid, idTypeToUse, memberships);
                        }
                        printAttrs(amid);

                        System.out.println ("Operations available on User '" +
                            amid.getName() + "':");
                        System.out.println (
                            "\tl: List groups or roles\n" +
                            "\td: Display attributes\n" +
                            "\ts: Set attribute\n" +
                            "\te: No selection");
                        answer = sampleUtils.getLine(
                            "Enter selection [l, d, s, e]: ");
                        if (answer.toLowerCase().startsWith("d")) {
                            printAttrs(amid);
                        } else if (answer.toLowerCase().startsWith("s")) {
                            setAttribute(amid);
                        } else if (answer.toLowerCase().startsWith("l")) {
                            listGrpOrRoleOfUser(amid);
                        } else if (answer.toLowerCase().startsWith("e")) {
                        } else {
                            System.err.println ("'" + answer + "' is invalid.");
                        }
                    }
                } else if (idtype.equals(IdType.AGENT) ||
                    idtype.equals(IdType.AGENTONLY)) {
                    /*
                     * can:
                     *   see if exists
                     *   see if active
                     *   set active status
                     *   get attributes
                     *   get attribute
                     *   set attributes
                     *   remove attributes
                     *   store
                     *   
                     */

                    String thisAgent = amid.getName();
                    System.out.println("Agent '" + thisAgent +
                        "' exists: " + amid.isExists());

                    System.out.println("Agent '" + thisAgent +
                        "' is active: " + amid.isActive());
                    answer = sampleUtils.getLine (
                        "Set agent active, inactive, or cancel [a,i,c]: ");
                    if (answer.startsWith("a")) {
                        if (amid.isActive()) {
                            System.out.println("Agent '" + thisAgent +
                                "' already active");
                        } else {
                            amid.setActiveStatus(true);
                            System.out.println("Agent '" + thisAgent +
                                "' is active: " + amid.isActive());
                        }
                    } else if (answer.startsWith("i")) {
                        if (!amid.isActive()) {
                            System.out.println("Agent '" + thisAgent +
                                "' already inactive");
                        } else {
                            amid.setActiveStatus(false);
                            System.out.println("Agent '" + thisAgent +
                                "' is active: " + amid.isActive());
                        }
                    }

                    printAttrs(amid);
                    setAttribute(amid);
                }
            } catch (IdRepoException ire) {
                System.err.println ("processType:IdRepoException: " +
                    ire.getMessage());
            } catch (SSOException ssoe) {
                System.err.println ("processType:SSOException: " +
                    ssoe.getMessage());
            }
        } else {
            System.out.println ("No identities of type '" +
                idtype.getName() + "' found to process.");
        }
    }


    /*
     *  get and print the attributes for the AMIdentity specified.
     */
    private void printAttrs (AMIdentity amid) {
        Map attrMap = null;
        String name = null;
        String type = null;
        try {
            name = amid.getName();
            type = amid.getType().getName();
            attrMap = amid.getAttributes();
            if (!attrMap.isEmpty()) {
                Set keySet = attrMap.keySet();
                Set valSet = null;
                System.out.println(type + ":" + name + "'s Attributes:");
                for (Iterator it = keySet.iterator(); it.hasNext(); ) {
                    String key = (String)it.next();
                    System.out.print ("    attr '" + key + "' ");
                    valSet = (Set)attrMap.get(key);
                    if (valSet.size() > 0) {
                        System.out.println ("=");
                        for (Iterator it2=valSet.iterator(); it2.hasNext(); ) {
                            System.out.println ("\t" + (String)it2.next());
                        }
                    } else {
                        System.out.println ("has no values.");
                    }
                }
            } else {
                System.out.println (name + " has no attributes.");
            }
        } catch (IdRepoException ire) {
            System.err.println("printAttrs:IdRepoException: " +
                ire.getMessage());
        } catch (SSOException ssoe) {
            System.err.println("printAttrs:SSOException: " +
                ssoe.getMessage());
        }
    }


    /*
     *  print members of the specified IdType in the AMIdentity object
     *  specified.
     */
    private void printMembers (AMIdentity amid, IdType typeToGet)
    {
        try {
            IdType amidType = amid.getType();
            String amidTypeName = amidType.getName(); 
            Iterator it = amid.getMembers(typeToGet).iterator();
            if (it.hasNext()) {
                System.out.println(amidTypeName + " " + amid.getName() +
                    "'s members:");
                while (it.hasNext()) {
                    System.out.println("   " +
                        ((AMIdentity)it.next()).getName());
                }
            } else {
                System.out.println(amidType + " " + amid.getName() +
                    " has no members.");
            }
        } catch (IdRepoException ire) {
            System.err.println("printMembers:IdRepoException: " +
                ire.getMessage());
        } catch (SSOException ssoe) {
            System.err.println("printMembers:SSOException: " +
                ssoe.getMessage());
        }
    }


    /*
     *  print memberships of IdType specified in the Set provided.
     */
    private void printMemberships(AMIdentity amid, IdType idTypeToUse,
        Set memberships)
    {
        if ((memberships == null) || (memberships.size() == 0)) {
            System.out.println("  " + amid.getName() +
                " has no memberships in identities of IdType " +
                idTypeToUse.getName());
            return;
        }

        Iterator it = memberships.iterator();
        if (it.hasNext()) {
            System.out.println("  " + amid.getName() +
                " has membership in identities of IdType " +
                idTypeToUse.getName() + " named: ");
            while (it.hasNext()) {
                System.out.println("   " +
                    ((AMIdentity)it.next()).getName());
            }
        } else {
            System.out.println("  " + amid.getName() +
                " has no memberships in identities of IdType " +
                idTypeToUse.getName());
        }
    }

    /*
     *  get and print the attributes for the specified AMIdentity object.
     *  select the attribute to set, and value to which to set it, set it,
     *  then retrieve and print its value for verification.
     */
    private void setAttribute (AMIdentity amid)
    {
        Map attrMap = null;
        String name = null;
        String type = null;
        try {
            name = amid.getName();
            type = amid.getType().getName();
            attrMap = amid.getAttributes();
            if (!attrMap.isEmpty()) {
                Set keySet = attrMap.keySet();
                Object[] attrArray = keySet.toArray();

                int i = sampleUtils.selectFromArray (attrArray, type + ":" +
                    name + "'s Attributes",
                    "Select attribute to set");

                String attrToSet = null;
                if ((i >= 0) && (i < keySet.size())) {
                    attrToSet = (String)attrArray[i];
                    System.out.println("To set attribute " + attrToSet);
                    Set oldVal = amid.getAttribute(attrToSet);
                    System.out.print("  Current value = [");
                    if (oldVal.isEmpty()) {
                        System.out.println ("Empty]");
                    } else {
                        for (Iterator it=oldVal.iterator(); it.hasNext(); ) {
                            System.out.print((String)it.next());
                            if (it.hasNext()) {
                                System.out.print(" ");
                            }
                        }
                        System.out.println ("]");
                    }
                    String newVal = sampleUtils.getLine(
                        "New value for " + attrToSet + ": ");
                    Map nattrs = new HashMap();
                    Set valSet = new HashSet();
                    valSet.add(newVal);
                    nattrs.put(attrToSet, valSet);
                    amid.setAttributes(nattrs);
                    amid.store();

                    /*
                     *  get attr value again to verify it changed
                     */
                    oldVal = amid.getAttribute(attrToSet);
                    System.out.print ("  Updated value = [");
                    if (oldVal.isEmpty()) {
                        System.out.println ("Empty]");
                    } else {
                        for (Iterator it=oldVal.iterator(); it.hasNext(); ) {
                            System.out.print((String)it.next());
                            if (it.hasNext()) {
                                System.out.print (" ");
                            }
                        }
                        System.out.println ("]");
                    }
                }
            } else {
                System.out.println (name + " has no attributes.");
            }
        } catch (IdRepoException ire) {
            System.err.println("setAttrs:IdRepoException: " +
                ire.getMessage());
        } catch (SSOException ssoe) {
            System.err.println("setAttrs:SSOException: " +
                ssoe.getMessage());
        }
    }

    /*
     *  list groups or roles for the specified user
     */
    private void listGrpOrRoleOfUser (AMIdentity amid)
    {
        IdType typeToGet = null;
        Set tSet = null;
        String hdr = null;

        String ans = sampleUtils.getLine(
            "Groups, or Roles to list [g, r]: ");
        String ans2 = ans.toLowerCase();

        try {
            String trlr = null;
            hdr = "User " + amid.getName();
            if (ans2.startsWith("g")) {  // list groups
                tSet = amid.getMemberships(IdType.GROUP);
                trlr = "group memberships";
            } else if (ans2.startsWith("r")) {  // list roles
                tSet = amid.getMemberships(IdType.ROLE);
                trlr = "role memberships";
            }
            sampleUtils.printResults (hdr, tSet, trlr);
        } catch (IdRepoException ire) {
            System.err.println("listGrpOrRoleOfUser:IdRepoException: " +
                ire.getMessage());
        } catch (SSOException ssoe) {
            System.err.println("listGrpOrRoleOfUser:SSOException: " +
                ssoe.getMessage());
        }
    }
}


