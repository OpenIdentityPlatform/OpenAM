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
 * $Id: IdRepoSampleUtils.java,v 1.5 2008/08/07 22:08:20 goodearth Exp $
 *
 */

package com.sun.identity.samples.clientsdk.idrepo;

import java.io.*;
import java.util.*;
import java.lang.Integer;
import java.security.Principal;
import javax.security.auth.callback.*;

import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.share.AuthXMLTags;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;


/**
 * This class 
 *
 *
 * @author 
 */
public class IdRepoSampleUtils {

    AuthContext lc = null;
    String userID = null;

    public IdRepoSampleUtils() {
    }

    public SSOToken realmLogin (String userid, String password, String realm)
        throws SSOException, AuthLoginException, Exception
    {
        SSOTokenManager mgr;
        String adminDN;
        String adminPassword;
        SSOToken ssoToken = null;
        AuthContext.IndexType authType = AuthContext.IndexType.MODULE_INSTANCE;

        try {
            lc = new AuthContext(realm);
        } catch (AuthLoginException le) {
            System.err.println(
                "IdRepoSampleUtils: could not get AuthContext for realm " +
                realm);
            throw le;
        }

        try {
            lc.login();
        } catch (AuthLoginException le) {
            System.err.println("IdRepoSampleUtils: Failed to start login " +
            "for default authmodule");
            throw le;
        }

        userID = userid;
        Callback[]  callbacks = null;
        Hashtable values = new Hashtable();
        values.put(AuthXMLTags.NAME_CALLBACK, userid);
        values.put(AuthXMLTags.PASSWORD_CALLBACK, password);

        while (lc.hasMoreRequirements()) {
            callbacks = lc.getRequirements();
            try {
                fillCallbacks(callbacks, values);
                lc.submitRequirements(callbacks);
            } catch (Exception e) {
                System.err.println( "Failed to submit callbacks!"); 
                e.printStackTrace();
                return null;
            }
        }

        AuthContext.Status istat = lc.getStatus();
        if (istat == AuthContext.Status.SUCCESS) {
            System.out.println("==>Authentication SUCCESSFUL for user " +
                userid);
        } else if (istat == AuthContext.Status.COMPLETED) {
            System.out.println("==>Authentication Status for user " +
                userid+ " = " + istat);
            return null;
        }

        try {
            ssoToken = lc.getSSOToken();
        } catch (Exception e) {
            System.err.println( "Failed to get SSO token!  " + e.getMessage()); 
            throw e;
        }

        return ssoToken;
    }

    public void logout () throws AuthLoginException {
        try {
            lc.logout();
        } catch (AuthLoginException alexc) {
            System.err.println ("IdRepoSampleUtils: logout failed for user '" +
                userID + "'");
            throw alexc;
        }
    }

    
    protected void fillCallbacks(Callback[] callbacks, Hashtable values) 
        throws Exception
    {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback nc = (NameCallback) callbacks[i];
                nc.setName((String)values.get(AuthXMLTags.NAME_CALLBACK));
            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback pc = (PasswordCallback) callbacks[i];
                pc.setPassword(((String)values.get(
                    AuthXMLTags.PASSWORD_CALLBACK)).toCharArray());
            } else if (callbacks[i] instanceof TextInputCallback) {
                TextInputCallback tic = (TextInputCallback) callbacks[i];
                tic.setText((String)values.get(
                    AuthXMLTags.TEXT_INPUT_CALLBACK));
            } else if (callbacks[i] instanceof ChoiceCallback) {
                ChoiceCallback cc = (ChoiceCallback) callbacks[i];
                cc.setSelectedIndex(Integer.parseInt((String)values.get(
                    AuthXMLTags.CHOICE_CALLBACK)));
            }
        }
    }

    public String getLine() {
        StringBuffer buf = new StringBuffer(80);
        int c;

        try {
            while ((c = System.in.read()) != -1) {
                char ch = (char)c;
                if (ch == '\r') {
                  continue;
                } 
                if (ch == '\n') {
                    break;
                }
                buf.append(ch);
            }
        } catch (IOException e) {
            System.err.println ("getLine: " + e.getMessage());
        }
        return (buf.toString());
    }

    public String getLine (String prompt) {
        System.out.print (prompt);
        return (getLine());
    }

    public String getLine (String prompt, String defaultVal) {
        System.out.print (prompt + " [" + defaultVal + "]: ");
        String tmp = getLine();
        if (tmp.length() == 0) {
            tmp = defaultVal;
        }
        return (tmp);
    }

    /*
     *  return integer value of String sVal; -1 if error
     */
    public int getIntValue (String sVal) {
        int i = -1;
        try {
            i = Integer.parseInt (sVal);
        } catch (NumberFormatException e) {
            System.err.println ("'" + sVal +
                "' does not appear to be an integer.");
        }
        return i;
    }

    /*
     *  can only create or delete AMIdentities of IdType user, agentgroup,
     *  agentonly
     */
    public IdType getIdTypeToCreateOrDelete()
    {
        IdType tType = null;
        System.out.println ("    Supported IdTypes:\n" +
            "\t0: user\n\t1: agent\n\t2: agentonly\n\t3: agentgroup\n\t4: realm\n\t5: No selection");
        String answer = getLine ("Select type: [0..3]: ");
        int i = getIntValue(answer);
        switch (i) {
            case 0:  // user
                tType = IdType.USER;
                break;
            case 1:  // agent
                tType = IdType.AGENT;
                break;
            case 2:  // agentonly
                tType = IdType.AGENTONLY;
                break;
            case 3:  // agentgroup
                tType = IdType.AGENTGROUP;
                break;
            case 4:  // realm
                tType = IdType.REALM;
                break;
            case 5:  // no selection
                break;
            default:  // invalid selection
                System.err.println(answer + " is an invalid selection.");
        }
        return tType;
    }

    /*
     *  get the IdType selected from the list of supported IdTypes for
     *  this AMIdentityRepository object.  can be "null" if no selection
     *  made.
     */
    public IdType getIdType(AMIdentityRepository idRepo) {
        IdType tType = null;
        String realmName = null;
        try {
            realmName = idRepo.getRealmIdentity().getRealm();
            Set types = idRepo.getSupportedIdTypes();
            Object[] idtypes = types.toArray();
            System.out.println("    Supported IdTypes:");
            int i = 0;
            for (i = 0; i < idtypes.length; i++) {
                tType = (IdType)idtypes[i];
                System.out.println("\t" + i + ": " + tType.getName());
            }
            System.out.println ("\t" + i + ": No selection");

            String answer = getLine ("Select type: [0.." +
                idtypes.length + "]: ");
            i = getIntValue(answer);

            tType = (IdType)idtypes[0];
            if (i == idtypes.length) {
                return (null);
            } else if ((i >= 0) && (i < idtypes.length)) {
                tType = (IdType)idtypes[i];
            } else {
                System.err.println(answer + " is an invalid selection.");
                return (null);
            }
        } catch (IdRepoException ire) {
            System.err.println("getIdType: IdRepoException" +
                " getting Supported IdTypes for '" + realmName + "': " +
                ire.getMessage());
        } catch (SSOException ssoe) {
            System.err.println("getIdType: SSOException" +
                " getting Supported IdTypes for '" + realmName + "': " +
                ssoe.getMessage());
        }
        return (tType);
    }

    /*
     *  print out elements in the Set "results".  header and trailer
     *  titling Strings.  more generic (i.e., usually expecting Strings)
     *  than other printResults(String, Set).
     */
    public void printResults (
        String header,
        Set results,
        String trailer)
    {
        if (results.isEmpty()) {
            System.out.println(header + " has no " + trailer);
        } else {
            System.out.println (header + " has " + results.size() + " " +
                trailer + ":");
            for (Iterator it = results.iterator(); it.hasNext(); ) {
                System.out.println ("    " + it.next()); 
            }
        }
        System.out.println("");
        return;
    }

    /*
     *  print out elements in the Set "results".  header and trailer
     *  titling Strings.  more generic (i.e., usually expecting Strings)
     *  than other printResults(String, Set).
     */
    public void printResultsRealm (
        String header,
        Set results,
        String trailer)
    {
        if (results.isEmpty()) {
            System.out.println(header + " has no " + trailer);
        } else {
            System.out.println (header + " has " + results.size() + " " +
                trailer + ":");
            for (Iterator it = results.iterator(); it.hasNext(); ) {
                AMIdentity amid = (AMIdentity)it.next();
                System.out.println ("    " + amid.getRealm()); 
            }
        }
        System.out.println("");
        return;
    }

    /*
     *  for the Set of IdTypes specified in "results", get and print
     *    1. the IdTypes it can be a member of
     *    2. the IdTypes it can have as members
     *    3. the IdTypes it can add to itself
     */
    public void printIdTypeResults(
        String header,
        Set results,
        String trailer)
    {
        if (results.isEmpty()) {
            System.out.println(header + " has no " + trailer);
        } else {
            System.out.println(header + " has " + results.size() + " " +
                trailer + ":");
            IdType itype = null;
            Set idSet = null;
            for (Iterator it = results.iterator(); it.hasNext(); ) {
                itype = (IdType)it.next();
                System.out.println ("    IdType " + itype.getName());
                idSet = itype.canBeMemberOf();
                printIdTypeSet("BE a member of IdType(s):", idSet);

                idSet = itype.canHaveMembers();
                printIdTypeSet("HAVE a member of IdType(s):", idSet);

                idSet = itype.canAddMembers();
                printIdTypeSet("ADD members of IdType(s):", idSet);
            }
        }
        System.out.println("");
        return;
    }

    /*
     *  used by printIdTypeResults(), above, to print out
     *  AMIdentity names of elements in the Set.
     */
    private void printIdTypeSet (
        String header,
        Set idSet)
    {
        System.out.print ("\tcan " + header);
        if (idSet.size() > 0) {
            for (Iterator it = idSet.iterator(); it.hasNext(); ) {
                System.out.print (" " + ((IdType)it.next()).getName());
            }
            System.out.print("\n");
        } else {
            System.out.println (" [NONE]");
        }
    }

    /*
     *  print the objects (String or AMIdentity.getName()) in the
     *  specified Array, and return the index of the one selected.
     *  -1 if none selected.
     */

    public int selectFromArray (
        Object[] objs,
        String hdr,
        String prompt)
    {
        AMIdentity amid = null;
        String ans = null;
        boolean isIdType = false;
        boolean isString = false;

        if (objs.length <= 0) {
            return (-1);
        }

            System.out.println(hdr);
        int i = -1;

        String objclass = objs[0].getClass().getName();
        if (objclass.indexOf("AMIdentity") >= 0) {
            isIdType = true;
        } else if (objclass.indexOf("String") >= 0) {
            isString = true;
        }

        for (i = 0; i < objs.length; i++) {
            if (isIdType) {
                amid = (AMIdentity)objs[i];
                System.out.println("\t" + i + ": " + amid.getName());
            } else if (isString) {
                System.out.println("\t" + i + ": " + (String)objs[i]);
            } else {
                System.out.println("\t" + i + ": Class = " + objclass);
            }
        }
        System.out.println("\t" + i + ": No Selection");
        ans = getLine (prompt + ": [0.." + objs.length + "]: ");
        i = getIntValue(ans);

        return i;
    }


    /*
     *  print the objects (String or AMIdentity.getName()) in the
     *  specified Set, and return the object of the one selected.
     *  null if none selected.
     */
    public Object selectFromSet (Set itemSet)
    {
        Object[] objs = itemSet.toArray();
        AMIdentity amid = null;
        AMIdentity amid2 = null;
        int setsize = itemSet.size();
        int i;
        boolean isAMId = false;
        boolean isString = false;
        String str =  null;

        if (setsize <= 0) {
            return null;
        }

        String objclass = objs[0].getClass().getName();
        if (objclass.indexOf("AMIdentity") >= 0) {
            isAMId = true;
        } else if (objclass.indexOf("String") >= 0) {
            isString = true;
        }

        if (setsize > 0) {
            System.out.println("Available selections:");
            for (i = 0; i < setsize; i++) {
                if (isAMId) {
                    amid = (AMIdentity)objs[i];
                    System.out.println("\t" + i + ": " + amid.getName());
                } else if (isString) {
                    System.out.println("\t" + i + ": " + (String)objs[i]);
                } else {
                    System.out.println("\t" + i + ": Class = " + objclass);
                }
            }
            System.out.println ("\t" + i + ": No selection");

            String answer = getLine("Select identity: [0.." + setsize + "]: ");
            int ians = getIntValue(answer); 
            if ((ians >= 0) && (ians < setsize)) {
                return (objs[ians]);
            } else if (ians == setsize) {
            } else {
                System.err.println ("'" + answer +
                    "' is invalid.");
            }
        }
        return null;
    }


    public void waitForReturn() {
        waitForReturn("Hit <return> when ready: ");
        String answer = getLine();
    }

    public void waitForReturn(String prompt) {
        System.out.print (prompt);
        String answer = getLine();
    }
}


