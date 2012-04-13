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
 * $Id: PatchCommandExecutorForHpux.java,v 1.1 2008/11/22 02:24:32 ak138937 Exp $
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

public class PatchCommandExecutorForHpux implements SystemInfoConstants {
    
    private ArrayList cmdList = new ArrayList();
    private ArrayList patchPresentList = new ArrayList();
    private ArrayList patchObsoletedList = new ArrayList();
    private HashMap patchIncompatiblesMap = new HashMap();
    
    public PatchCommandExecutorForHpux(){
        try {
            runPatchCmd();
        } catch (Exception ex) {
            //ignore exception
        }
    }
    
    public void runPatchCmd() throws Exception {
        int retValue = 0;
        cmdList = executePatchCmd();
        patchPresentList = createPatchPresentList();
    }
    
    private ArrayList executePatchCmd() throws Exception {
        ProcessExecutor executor = new ProcessExecutor(
            new String[]{"/usr/bin/sh", "/tmp/patch_info.sh"});
        String[] cmdOp = executor.execute(true);
        ArrayList retList = new ArrayList();
        retList.addAll(Arrays.asList(cmdOp));
        return retList;
    }
    
    private String getPatchNumber(String opLine, String srchStr) {
        int indxSrch = opLine.indexOf(srchStr);
        if (indxSrch == -1) {
            return "";
        }
        int substrStart = indxSrch+srchStr.length();
        if (substrStart >= opLine.length()) {
            return "";
        }
        String patchPlus = opLine.substring(substrStart);
        StringTokenizer stok = new StringTokenizer(patchPlus, " ");
        String resultStr = "";
        while(stok.hasMoreTokens()) {
            String possiblePatch = stok.nextToken();
            if (possiblePatch.indexOf(":") != -1) {
                break;
            }
            resultStr=resultStr+possiblePatch;
        }
        return resultStr;
    }
    
    
    public ArrayList createPatchPresentList() {
        ArrayList present = new ArrayList();
        for (int j = 0; j < cmdList.size(); j++) {
            String infoString = (String) cmdList.get(j);
            if (infoString.length() > 0) {
                present.add(infoString);
            }
        }
        return present;
    }
    
    public ArrayList createPatchObsoletedList() {
        ArrayList arrayOfObsoletes = new ArrayList();
        // iterate over the cmdList and get the PatchIds 
        // which are obsoleted
        for (int j = 0; j < cmdList.size(); j++) {
            String infoString = (String) cmdList.get(j);
            String resultString = getPatchNumber(
                infoString, OBSOLETESKEY);
            if (resultString.length() > 0) {
                ArrayList obsoleteIds = 
                    GenUtils.seperateStringComponents(resultString);
                for (int k = 0; k < obsoleteIds.size(); k++) {
                    String obsoleteIdstr = (String)obsoleteIds.get(k);
                    arrayOfObsoletes.add(obsoleteIds);
                }
            }
        }
        if (arrayOfObsoletes.isEmpty()) {
            return arrayOfObsoletes;
        }
        ArrayList cleanedList = 
            GenUtils.resolveStringArrayListDuplicates(arrayOfObsoletes);
        return cleanedList;
    }
    
    public HashMap createPatchIncompatiblesMap() {
        HashMap incompats = new HashMap();
        // iterate over the cmdList and get the PatchIds which are 
        // incompatible with some already installed patch
        for (int j = 0; j < cmdList.size(); j++) {
            String infoString = (String) cmdList.get(j);
            String incompatStr = getPatchNumber(
                infoString, INCOMPATIBLESKEY);
            if (incompatStr.length() > 0) {
                String patchPresent = getPatchNumber(infoString, PATCHKEY);
                ArrayList incompatIds = 
                    GenUtils.seperateStringComponents(incompatStr);
                for (int k = 0; k < incompatIds.size(); k++) {
                    String incompatPatchId = (String)incompatIds.get(k);
                    ArrayList val = (ArrayList)incompats.get(incompatPatchId);
                    if (val == null) {
                        val = new ArrayList();
                        val.add(patchPresent);
                        incompats.put(incompatPatchId, val);
                    } else {
                        val.add(patchPresent);
                        incompats.put(incompatPatchId, val);
                    }
                }
            }
        }
        return incompats;
    }
}
