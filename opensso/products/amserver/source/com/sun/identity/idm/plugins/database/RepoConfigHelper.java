/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RepoConfigHelper.java,v 1.2 2009/12/22 19:11:54 veiming Exp $
 *
 */

package com.sun.identity.idm.plugins.database;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.util.StringTokenizer;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdOperation;

/**
 * Copied some methods from com.sun.identity.idm.plugins.ldapv3.LDAPv3Repo.java
 * which help to get attributes from the idRepoSevices.xml
 * These help in the initialize method of a plugin 
 * Probably these could be moved to a common utility since they help any idRepo
 * plugin process the configuration map built from idRepoSevices.xml attributes
 * sets for a plugin.
 **/
public class RepoConfigHelper {
    
    private Debug debug;
    
    public RepoConfigHelper(Debug debug) {
        this.debug=debug;
    }
    
    //should update this to make sure it does not print a password field,
    //maybe make a special method for password fields or another parameter 
    //like isPassword or something
    
    //Each of these methods looks returns the corresponding value for the 
    //parameter map and key.
    //they can be used any timne you need to get something from the config map
    // that was passed in to plugin thru inititalize
    
    public int getPropertyIntValue(Map configParams, String key,
            int defaultValue) {
        int value = defaultValue;
        try {
            Set valueSet = (Set) configParams.get(key);
            if (valueSet != null && !valueSet.isEmpty()) {
                value = Integer.parseInt((String) valueSet.iterator().next());
            }
        } catch (NumberFormatException nfe) {
            value = defaultValue;
        }
        if (debug.messageEnabled()) {
            debug.message("    RepoConfigHelper.getPropertyIntValue(): " + key
                    + " = " + value);
        }
        return value;
    }
    
    //input includes a default value
    public String getPropertyStringValue(Map configParams, String key,
            String defaultVal) {
        String value = getPropertyStringValue(configParams, key);
        if (value == null) {
            value = defaultVal;
        }
        return value;
    }

    public String getPropertyStringValue(Map configParams, String key) {
        String value = null;
        Set valueSet = (Set) configParams.get(key);
        if (valueSet != null && !valueSet.isEmpty()) {
            value = (String) valueSet.iterator().next();
        } else {
            if (debug.messageEnabled()) {
                debug.message("RepoConfigHelper.getPropertyStringValue failed"
                        + "to set value for:" + key);
            }
        }
        return value;
    }

    public boolean getPropertyBooleanValue(Map configParams, String key) {
        String value = getPropertyStringValue(configParams, key);
        return ((value != null) && value.equalsIgnoreCase("true"));
    }    
    
    /** 
     * ********************************************
     * parsedUserSpecifiedOps and parseInputedOps methods are copied from 
     * LDAPv3Repo.java and changed a bit to be reuseable. Changed are:
     * parsedUserSpecifiedOps
     *   --changed method to public to make accessible
     *   --changed to return a Map supportedOps INSTEAD of operate on a
     *     local field
     *   --changed debug print out
     *   --commented out dead code line Map oldSupportedOps =
     *      new HashMap(supportedOps);
     *   --commented out some code at the end which was always adding Realm
     *      support, since I dont think it is needed
     *  
     * parseInputedOps
     *  --changed method to public to make accessible
     *  --changed debug print out
     */

    /* 
     * Note, this ignores the case of input from user specified list
     */
    public Map parsedUserSpecifiedOps(Set userSpecifiedOpsSet) {

        // parse each entry, string, based syntax:
        // idType=idOperation,idOperation ...
        // if the idType is within my type and op then add it.
        if (debug.messageEnabled()) {
            debug.message("RepoConfigHelper.parsedUserSpecifiedOps entry:"
                    + " userSpecifiedOpsSet:"+ userSpecifiedOpsSet);
        }
        IdType idTypeRead = null;
        Set opsREAD = null;
        //this oldSupportedOps seems to be dead code ????        
        //Map oldSupportedOps = new HashMap(supportedOps);
        
        //I added this which it will use and return at end
        Map supportedOps = new HashMap();
        
        //supportedOps.clear(); //I commented this line out
        Iterator it = userSpecifiedOpsSet.iterator();
        while (it.hasNext()) {
            idTypeRead = null;
            Set opsRead = null;
            String curr = (String) it.next();
            StringTokenizer st = new StringTokenizer(curr, "= ,");
            if (st.hasMoreTokens()) {
                String idtypeToken = st.nextToken(); // read the type.
                if (debug.messageEnabled()) {
                    debug.message("    idtypeToken:" + idtypeToken);
                }
                if (idtypeToken.equalsIgnoreCase("user")) {
                    idTypeRead = IdType.USER;
                    opsRead = parseInputedOps(st, true);
                } else if (idtypeToken.equalsIgnoreCase("group")) {
                    idTypeRead = IdType.GROUP;
                    opsRead = parseInputedOps(st, false);
                } else if (idtypeToken.equalsIgnoreCase("agent")) {
                    idTypeRead = IdType.AGENT;
                    opsRead = parseInputedOps(st, false);
                } else if (idtypeToken.equalsIgnoreCase("role")) {
                    idTypeRead = IdType.ROLE;
                    opsRead = parseInputedOps(st, false);
                } else if (idtypeToken.equalsIgnoreCase("filteredrole")) {
                    idTypeRead = IdType.FILTEREDROLE;
                    opsRead = parseInputedOps(st, false);                    
                } else if (idtypeToken.equalsIgnoreCase("realm")) {
                    idTypeRead = IdType.REALM;
                    opsRead = parseInputedOps(st, true);
                } else {
                    idTypeRead = null; // unknown or unsupported type.
                }
            } // else a blank line.

            if ((idTypeRead != null) && (opsRead != null)
                    && (!opsRead.isEmpty())) {
                supportedOps.put(idTypeRead, opsRead);
                if (debug.messageEnabled()) {
                    debug.message("RepoConfigHelper.parsedUserSpecifiedOps"
                            + " called supportedOps:" + supportedOps 
                            + "; idTypeRead:" + idTypeRead
                            + "; opsRead:" + opsRead);
                }
            }

        } // while
        
        
        
        // always added the "realm=service" so services can be added to realm.
        /**
         * Dont think you need to do this realm stuff always.
         * Commented out for now since just supporting users right now
        Set realmSrv = (Set) supportedOps.get(IdType.REALM);
        if (realmSrv == null) {
            realmSrv = new HashSet();
        }
        realmSrv.add(IdOperation.SERVICE);
        supportedOps.put(IdType.REALM, realmSrv);
        **/
        return supportedOps;
    }
    
    /*
     * Note, this ignores the case of input from user specified list and 
     * replaces values with values in some defined constants
     *
     * @param boolean supportService seems to only be used when idOpToken 
     *     is service. This flag is set by caller since some IdTypes can not 
     *     support service so they want to make sure they do not set it ????
     */
    public Set parseInputedOps(StringTokenizer st, boolean supportService) {
        // read op from st.
        Set opsReadSet = new HashSet();
        while (st.hasMoreTokens()) {
            String idOpToken = st.nextToken();
            if (idOpToken.equalsIgnoreCase("read")) {
                opsReadSet.add(IdOperation.READ);
            } else if (idOpToken.equalsIgnoreCase("edit")) {
                opsReadSet.add(IdOperation.EDIT);
            } else if (idOpToken.equalsIgnoreCase("create")) {
                opsReadSet.add(IdOperation.CREATE);
            } else if (idOpToken.equalsIgnoreCase("delete")) {
                opsReadSet.add(IdOperation.DELETE);
            } else if (idOpToken.equalsIgnoreCase("service")) {
                if (supportService) {
                    opsReadSet.add(IdOperation.SERVICE);
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("RepoConfigHelper.parseInputedOps done: opsReadSet:"
                    + opsReadSet);
        }
        return opsReadSet;
    }
}

