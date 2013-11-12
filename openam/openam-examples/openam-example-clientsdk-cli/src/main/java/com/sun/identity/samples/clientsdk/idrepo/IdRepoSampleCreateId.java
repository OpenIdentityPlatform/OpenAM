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
 * $Id: IdRepoSampleCreateId.java,v 1.15 2008/09/24 19:50:10 goodearth Exp $
 *
 */

package com.sun.identity.samples.clientsdk.idrepo;


import java.io.*;
import java.util.*;

import com.iplanet.sso.SSOException;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;


/**
 * This class 
 *
 *
 * @author 
 */
public class IdRepoSampleCreateId {

    IdRepoSampleUtils sampleUtils = null;
    AMIdentityRepository idRepo = null;
    private static final String AGENT_TYPE_ATTR = "AgentType"; 
    private static final String WSP_ENDPOINT = "WSPEndpoint"; 

    public IdRepoSampleCreateId (AMIdentityRepository idrepo) {
        sampleUtils = new IdRepoSampleUtils();
        idRepo = idrepo;
    }

    public void createAMId () {
        /*
         *  just create the identities with this selection.
         *  to "exercise" identities, use the
         *  "search identities" selection to find one to
         *  exercise.
         */
        IdType idtype = sampleUtils.getIdTypeToCreateOrDelete();
        if (idtype == null) {
            return;
        }

        IdType tmpIdtype = idtype;
        if (idtype.equals(IdType.AGENT)) {
            tmpIdtype = IdType.AGENTONLY;
        }

        try {
            /*
             * get and display list of identities of idtype
             * for reference...
             */
            IdSearchResults adRes =
                idRepo.searchIdentities(tmpIdtype, "*", new IdSearchControl());
            Set adResSet = adRes.getSearchResults();
            if (!adResSet.isEmpty()) {
                System.out.println("    Current list of " +
                    idtype.getName() + "s:");
                for (Iterator it = adResSet.iterator(); it.hasNext(); ) {
                    System.out.println("\t" +
                        ((AMIdentity)it.next()).getName());
                }
            } else {
                System.out.println("    No " + idtype.getName() + "s found.");
            }

            String idName = sampleUtils.getLine("Enter idName to create: ");
            Map attrs = new HashMap();
            Set vals = new HashSet();
            AMIdentity tmpId = null;

            if (idtype.equals(IdType.AGENT)) {
                String tmpS = sampleUtils.getLine(idName + "'s password: ");
                vals = new HashSet();
                vals.add(tmpS);
                attrs.put("userpassword", vals);

                tmpId = idRepo.createIdentity(IdType.AGENTONLY, idName, attrs);
                idtype = IdType.AGENTONLY;
            } else if (idtype.equals(IdType.AGENTONLY) || 
                idtype.equals(IdType.AGENTGROUP)) { 

                String tmpS = sampleUtils.getLine(idName + "'s agentType: ");
                vals.add(tmpS);
                attrs.put(AGENT_TYPE_ATTR, vals);
                tmpS = sampleUtils.getLine(idName + "'s password: ");
                vals = new HashSet();
                vals.add(tmpS);
                attrs.put("userpassword", vals);

                tmpId = idRepo.createIdentity(idtype, idName, attrs);
            } else if (idtype.equals(IdType.USER)) {
                String tmpS = sampleUtils.getLine(idName + "'s password: ");
                vals.add(tmpS);
                attrs.put("userpassword", vals);
                vals = new HashSet();
                tmpS =  sampleUtils.getLine(idName + "'s last name: ");
                vals.add(tmpS);
                attrs.put("sn", vals);
                vals = new HashSet();
                vals.add(idName + " " + tmpS);
                attrs.put("cn", vals);
                vals = new HashSet();
                vals.add(idName);
                attrs.put("givenname", vals); // "full name"
                tmpId = idRepo.createIdentity(IdType.USER, idName, attrs);
            } else if (idtype.equals(IdType.REALM)) {
                String tmpS = sampleUtils.getLine(
                                idName + " active/inactive [a,i]: ");
                String actVal = "Active";
                if (tmpS.startsWith("i")) {
                    actVal = "Inactive";
                }
                vals.add(actVal);
                attrs.put("sunOrganizationStatus", vals);
                tmpId = idRepo.createIdentity(IdType.REALM, idName, attrs);
            }

            //  identity should exist, since it was just created
            if (tmpId != null) {
                System.out.println("    Created " +
                idtype.getName() + " identity '" +
                idName + "' isExists = " + tmpId.isExists());

                //  now show list of the identities of type created
                adRes = idRepo.searchIdentities(
                        idtype, "*", new IdSearchControl());
                adResSet = adRes.getSearchResults();

                if (!adResSet.isEmpty()) {
                    System.out.println("    Current list of " +
                        idtype.getName() + "s:");
                    for (Iterator it = adResSet.iterator(); it.hasNext(); ) {
                        System.out.println("\t" +
                            ((AMIdentity)it.next()).getName());
                    }
                } else {
                    System.out.println("    Odd, no " +
                        idtype.getName() + "s found.");
                }
                IdSearchControl WSCcontrol = new IdSearchControl();
                String providerName = idName;
                WSCcontrol.setAllReturnAttributes(true);
                IdSearchResults WSCresults = idRepo.searchIdentities(
                    IdType.AGENTONLY, providerName, WSCcontrol);
                Set agents = WSCresults.getSearchResults();
                System.out.println("WSC Agents before removeMember: " + agents); 
                if (idtype.equals(IdType.AGENTONLY) || 
                    idtype.equals(IdType.AGENTGROUP)) {
                    String POLLINT = "com.sun.am.policy.am.polling.interval";
                    //  now get the attributes of the identities of type created
                    Map attrMap = null;
                    String name = null;
                    String type = null;
                    name = tmpId.getName();
                    attrMap = tmpId.getAttributes();
                    if (!attrMap.isEmpty()) {
                        Set keySet = attrMap.keySet();
                        for (Iterator it = keySet.iterator(); it.hasNext(); ) {
                            String key = (String)it.next();
                            if (key.equalsIgnoreCase(POLLINT)) { 
                                System.out.println("Value before " +
                                    "removeAttribute: "
                                    + POLLINT + "=" + attrMap.get(POLLINT));
                                Set attrNameSet = null;
                                attrNameSet = new HashSet();
                                attrNameSet.add(key);
                                System.out.println("Attribute to remove :"+key);
                                tmpId.removeAttributes(attrNameSet);
                            }
                            if (key.equalsIgnoreCase("userpassword")) { 
                                System.out.println("Value check for pwd " +
                                    "userpassword =" + 
                                    attrMap.get("userpassword"));
                            }
                        }
                    } else {
                        System.out.println (name + " has no attributes.");
                    }

                    // get/check after remove
                    attrMap = tmpId.getAttributes();
                    if (!attrMap.isEmpty()) {
                        System.out.println ("Has key after removeAttribute : "+
                            POLLINT + " : true/false :" +
                                attrMap.keySet().contains(POLLINT));
                        if (attrMap.keySet().contains(POLLINT)) {
                            System.out.println ("Value after removeAttribute : "+
                                POLLINT + "=" + attrMap.get(POLLINT));
                        }
                    }

                    vals = new HashSet();
                    vals.add("WebAgent");
                    attrs.put(AGENT_TYPE_ATTR, vals);
                    AMIdentity agroupIdentity = null; 

                    if (!((tmpId.getType()).equals(IdType.AGENTGROUP))) {
                        System.out.println("\nChecking membership operations");
                        IdSearchResults res = 
                            idRepo.searchIdentities(IdType.AGENTGROUP,
                                "myagrp", new IdSearchControl());
                        Set resSet = res.getSearchResults();
                        if (!resSet.isEmpty()) {
                            Iterator iter = resSet.iterator();
                            if (iter.hasNext()) {
                                agroupIdentity = (AMIdentity) iter.next();
                            }
                        } else {
                            agroupIdentity = idRepo.createIdentity(
                                IdType.AGENTGROUP, "myagrp", attrs);
                        }

                        // Test for getMembers()
                        System.out.println("Obtained agent group =" + 
                            agroupIdentity.getName());
                        System.out.println("\nAdding member to agent group: " + 
                            tmpId.getName());
                        agroupIdentity.addMember(tmpId);
                        System.out.println("\nGetting member from agent "+
                            "group: " + 
                            agroupIdentity.getMembers(IdType.AGENTONLY));

                        // Test for getMemberships()
                        Set agentgroupsOfAgent = 
                            tmpId.getMemberships(IdType.AGENTGROUP);
                        System.out.println("Agent's agentGroup memberships = ");
                        Iterator agiter = agentgroupsOfAgent.iterator();
                        while (agiter.hasNext() ){
                            AMIdentity id = (AMIdentity) agiter.next();
                            System.out.println("AgentGroup of agent = " + 
                                id.getName());
                            System.out.println("AgentGroup of agent "+
                                "isExists: " + id.isExists());
                        }

                        System.out.println("\nRemoving member from agent "+
                            "group: " + tmpId.getName());
                            
                        agroupIdentity.removeMember(tmpId);
                        System.out.println("\nAfter removeMember : Getting "+
                            "member from agent group: " + 
                            agroupIdentity.getMembers(IdType.AGENTONLY));

                    }
                }
                IdSearchControl WSCcnt = new IdSearchControl();
                WSCcnt.setAllReturnAttributes(true);
                IdSearchResults WSCres = idRepo.searchIdentities(
                    IdType.AGENTONLY, providerName, WSCcnt);
                Set wscagents = WSCres.getSearchResults();
                System.out.println("WSC Agents after removeMember: " + 
                    wscagents); 

                // Test for avpairs filter while searching..
                IdSearchControl avcontrol = new IdSearchControl();
                avcontrol.setAllReturnAttributes(true);
                avcontrol.setTimeOut(0);           
                Map kvPairMap = new HashMap();
                Set avset = new HashSet();
                avset.add("WSCAgent");           
                kvPairMap.put(AGENT_TYPE_ATTR, avset);

                avcontrol.setSearchModifiers(IdSearchOpModifier.OR, kvPairMap);

                IdSearchResults avresults = 
                    idRepo.searchIdentities(IdType.AGENTONLY,
                        "*", avcontrol);
                Set avagents = avresults.getSearchResults();
                System.out.println("WSC Agents with avpairs as filter: " + 
                    avagents); 

                // Test : Search for WSPAgent type with its WSP End point 
                // attribute value known. This search should not return 
                // multiple WSP profies instead should return just one that 
                // has given WSP end point attribute value.

                IdSearchControl wspcontrol = new IdSearchControl();
                wspcontrol.setAllReturnAttributes(true);
                wspcontrol.setTimeOut(0);

                Map wspkvPairMap = new HashMap();
                Set wspset = new HashSet();
                wspset.add("WSPAgent");
                wspkvPairMap.put(AGENT_TYPE_ATTR, wspset);

                wspset = new HashSet();
                //String endpoint = "default";
                String endpoint = "testendpoint1";
                wspset.add(endpoint);
                wspkvPairMap.put(WSP_ENDPOINT, wspset);

                wspcontrol.setSearchModifiers(IdSearchOpModifier.OR, 
                    wspkvPairMap);
                IdSearchResults wspresults = 
                    idRepo.searchIdentities(IdType.AGENTONLY,"*", wspcontrol);
               
                Set wspagents = wspresults.getSearchResults();
                System.out.println("WSP Agents with avpairs as filter: " + 
                    wspagents); 
            }
        } catch (IdRepoException ire) {
            System.err.println("idRepoProcessing IdRepoException " +
                "creating '" + idtype + "': " + ire.getMessage());
        } catch (SSOException ssoe) {
            System.err.println("idRepoProcessing: SSOException " +
                "creating '" + idtype + "': " + ssoe.getMessage());
        }

        return;
    }

}


