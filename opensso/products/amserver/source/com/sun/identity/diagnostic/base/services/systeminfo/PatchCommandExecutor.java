/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PatchCommandExecutor.java,v 1.1 2008/11/22 02:24:32 ak138937 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.diagnostic.base.services.systeminfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringTokenizer;

import com.sun.identity.diagnostic.base.core.utils.GenUtils;
import com.sun.identity.diagnostic.base.core.utils.ProcessExecutor;
import com.sun.identity.shared.debug.Debug;


public class PatchCommandExecutor implements SystemInfoConstants {
    
    private ArrayList cmdList = new ArrayList();
    private ArrayList patchPresentList = new ArrayList();
    private ArrayList patchObsoletedList = new ArrayList();
    private HashMap patchIncompatiblesMap = new HashMap();
    private static String delimiter = "";
    
    public PatchCommandExecutor() {
        try {
            runPatchCmd();
        } catch(Exception ex) {
             Debug.getInstance(DEBUG_NAME).error(
                 "PatchCommandExecutor.PatchCommandExecutor: " +
                 "Exception in executing patch command", ex);
        }
    }
    
    private void runPatchCmd() throws Exception {
        cmdList = executePatchCmd();
        delimiter = ",";
        patchPresentList = createPatchPresentList();
        patchObsoletedList = createPatchObsoletedList();
        patchIncompatiblesMap = createPatchIncompatiblesMap();
    }
    
    private ArrayList executePatchCmd() throws Exception {
        ProcessExecutor executor = new ProcessExecutor(
            new String[]{"/usr/bin/showrev", "-p"});
        String[] cmdOp = executor.execute(true);
        ArrayList retList = new ArrayList();
        if (cmdOp == null) {
            return retList;
        }
        retList.addAll(Arrays.asList(cmdOp));
        return retList;
    }
    
    private String getPatchNumber(
        String opLine, 
        String srchStr
    ) {
        int indxSrch = opLine.indexOf(srchStr);
        if (indxSrch == -1) {
            return "";
        }
        int substrStart = indxSrch + srchStr.length();
        if (substrStart >= opLine.length()) {
            return "";
        }
        String patchPlus = opLine.substring(substrStart);
        StringTokenizer stok = new StringTokenizer(patchPlus," ");
        String resultStr = "";
        while (stok.hasMoreTokens()) {
            String possiblePatch = stok.nextToken();
            if (possiblePatch.indexOf(":") != -1) {
                break;
            }
            resultStr=resultStr+possiblePatch;
        }
        return resultStr;
    }
    
    /**
     * Returns the list of all patches on the system
     *
     * @return list of patches.
     */
    public ArrayList createGeneralPatchList() {
        return cmdList;
    }

    /**
     * Returns a patch from given entry
     *
     * @param patch the patch entry 
     * @param token the pattern to retrieve from patch entry 
     * @return patch from the given patch entry.
     */
    public String getPatchFromToken(String patch, String token) {
        // iterate over the patch output and get the PatchId 
        return getPatchNumber(patch, token);
    }

    /**
     * Returns a list of patches present on the system 
     *
     * @return list of patches present on system
     */
    public ArrayList createPatchPresentList() {
        ArrayList present = new ArrayList();
        
        // iterate over the cmdList and get the PatchIds 
        // which are present
        for (int j = 0; j < cmdList.size(); j++) {
            String infoString = (String) cmdList.get(j);
            String resultString = getPatchNumber(
                infoString, PATCHKEY);
            if (resultString.length() > 0) {
                present.add(resultString);
            }
        }
        return present;
    }
    
    /**
     * Returns a list of patches obsoleted by current patch on system 
     *
     * @return list of obsolete patches 
     */
    public ArrayList createPatchObsoletedList() {
        delimiter=",";
        ArrayList arrayOfObsoletes = new ArrayList();
        // iterate over the cmdList and get the PatchIds 
        // which are obsoleted
        for (int j = 0; j < cmdList.size(); j++) {
            String infoString = (String) cmdList.get(j);
            String resultString = getPatchNumber(
                infoString, OBSOLETESKEY);
            if (resultString.length() > 0 ) {
                // pass delimiter as applicable
                ArrayList obsoleteIds = GenUtils.seperateStringComponents(
                   resultString, delimiter);
                for (int k = 0; k < obsoleteIds.size(); k++) {
                    String obsoleteIdstr = (String)obsoleteIds.get(k);
                    arrayOfObsoletes.add(obsoleteIdstr);
                }
            }
        }
        if (arrayOfObsoletes.isEmpty()) {
            return arrayOfObsoletes;
        }
        ArrayList cleanedList = GenUtils.resolveStringArrayListDuplicates(
            arrayOfObsoletes);
        return cleanedList;
    }

    /**
     * Returns a list of patches required on the system 
     *
     * @return list of patches required
     */
    public ArrayList createPatchRequiresList() {
        ArrayList requires = new ArrayList();
        
        // iterate over the cmdList and get the PatchIds 
        // which are required
        for (int j = 0; j < cmdList.size(); j++) {
            String infoString = (String) cmdList.get(j);
            String resultString = getPatchNumber(
                infoString, REQUIRESKEY);
            if (resultString.length() > 0) {
                requires.add(resultString);
            }
        }
        return requires;
    }

    /**
     * Returns a list of patches that are incompatible on the system 
     *
     * @return list of incompatible patches 
     */
    public ArrayList createPatchIncompatibleList() {
        ArrayList incompatible = new ArrayList();
        
        // iterate over the cmdList and get the PatchIds 
        // which are incompatible
        for (int j = 0; j < cmdList.size(); j++) {
            String infoString = (String) cmdList.get(j);
            String resultString = getPatchNumber(
                infoString, INCOMPATIBLESKEY);
            if (resultString.length() > 0) {
                incompatible.add(resultString);
            }
        }
        return incompatible;
    }

    /**
     * Returns a list of packages on the system 
     *
     * @return list of packages 
     */
    public ArrayList createPackageList() {
        ArrayList dependPackage = new ArrayList();
        
        // iterate over the cmdList and get the PatchIds 
        // which are composed of the packages
        for (int j = 0; j < cmdList.size(); j++) {
            String infoString = (String) cmdList.get(j);
            String resultString = getPatchNumber(
                infoString, PACKAGESKEY);
            if (resultString.length() > 0) {
                dependPackage.add(resultString);
            }
        }
        return dependPackage;
    }
    
    /**
     * Returns the hashmap with the hashmap containing with 
     * key having incompatibles patchids and value having 
     * corresponding id of the patch which obsoleted it.
     *
     * @return Map of incompatible patch and its obsoleted patch 
     */
    public HashMap createPatchIncompatiblesMap() {
        delimiter=",";
        HashMap incompats = new HashMap();
        //iterate over the cmdList and get the PatchIds which 
        //are incompatible with some already installed patch
        
        for (int j = 0; j < cmdList.size(); j++) {
            String infoString = (String) cmdList.get(j);
            String incompatStr = getPatchNumber(
                infoString, INCOMPATIBLESKEY);
            if (incompatStr.length() > 0) {
                String patchPresent = getPatchNumber(
                    infoString, PATCHKEY);
                ArrayList incompatIds = GenUtils.seperateStringComponents(
                    incompatStr, delimiter);
                for (int k = 0; k < incompatIds.size(); k++) {
                    String incompatPatchId = (String)incompatIds.get(k);
                    ArrayList val = (ArrayList)incompats.get(incompatPatchId);
                    if (val == null) {
                        val = new ArrayList();
                        val.add(patchPresent);
                        incompats.put(incompatPatchId,val);
                    } else {
                        val.add(patchPresent);
                        incompats.put(incompatPatchId,val);
                    }
                }
            }
        }
        return incompats;
    }
}
