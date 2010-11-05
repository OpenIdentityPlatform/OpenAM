/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IdDeleteIdentitiesReq.java,v 1.2 2008/06/25 05:52:28 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Set;

class IdDeleteIdentitiesReq extends AdminReq {
    private Set idNameSet = null;
    private String realmPath = null;
    private IdType idType;


    /**
     * Constructs a new IdDeleteIdentitiesReq.
     *
     * @param  targetDN the parent Realm DN. 
     */        
    IdDeleteIdentitiesReq(String targetDN) {
        //
        //  a "slash" format path, rather than DN...
        //
        super(targetDN);
        realmPath = targetDN;
    }


    /**
     * sets the Identity Type for this request
     *
     * @param identType the Type of the Identity
     */
    void setIdType(IdType identType) {
        idType = identType;
    }

    /**
     * sets the identity names Set
     *
     * @param idSet the Set of Identity Names to delete
     */
    void setIdNameSet(Set idSet) {
        idNameSet = idSet;
    }

    /**
     * converts this object into a string.
     *
     * @return String. 
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter); 
        prnWriter.println(AdminReq.bundle.getString("requestdescription116") +
            " " + targetDN);
        for (Iterator it = idNameSet.iterator(); it.hasNext(); ) {
            prnWriter.println(" " + (String)it.next());
        }
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(SSOToken ssoToken)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("identity") + "\n" +
            bundle.getString("deleteIdentities") + " " +
            idType.toString() + " " +
            bundle.getString("fromrealm") + " " + targetDN);
        for (Iterator it2 = idNameSet.iterator(); it2.hasNext(); ){
            AdminReq.writer.println("  " + (String)it2.next());
        }

        StringBuffer idSB = new StringBuffer();
        for (Iterator it2 = idNameSet.iterator(); it2.hasNext(); ){
            idSB.append((String)it2.next()).append(" ");
        }
        if(idSB.length() <= 0) {
            idSB.append(bundle.getString("none"));
        }

        try {
            AMIdentityRepository amir =
                new AMIdentityRepository (ssoToken, realmPath);

            /*
             *  have to do a search for the idName(s) of the
             *  idType to get their AMIdentity objects
             */
            IdSearchControl isCtl = new IdSearchControl();
            isCtl.setRecursive(false);

            /*
             * should find every Identity in the Realm, or none
             * gets deleted.
             */
            for (Iterator it = idNameSet.iterator(); it.hasNext(); ) {
                String thisName = (String)it.next();
                IdSearchResults isr =
                    amir.searchIdentities(idType, thisName, isCtl);
                Set srSet = isr.getSearchResults();
                boolean foundThis = false;
                for (Iterator it2 = srSet.iterator(); it2.hasNext(); ) {
                    AMIdentity tmpA = (AMIdentity)it2.next();
                    String tmpS = tmpA.getName();
                    if (tmpS.equals(thisName)) {
                        foundThis = true;
                        break;
                    }
                }
                if (!foundThis) {
                    throw new AdminException(
                        bundle.getString("identityDoesNotExist") + " " +
                        thisName);
                }
            }
            String[] args = new String[3];
            args[1] = idType.toString();
            args[2] = realmPath;
            for (Iterator it = idNameSet.iterator(); it.hasNext(); ) {
                String thisName = (String)it.next();
                IdSearchResults isr =
                    amir.searchIdentities(idType, thisName, isCtl);
                Set srSet = isr.getSearchResults();
                args[0] = thisName;
                doLog(args, AdminUtils.DELETE_IDENTITY_ATTEMPT);
                amir.deleteIdentities(srSet);
                doLog(args, AdminUtils.DELETE_IDENTITY);
            }
        } catch (IdRepoException ire) {
            throw new AdminException(ire);
        } catch (SSOException ssoex) {
            throw new AdminException(ssoex);
        }
    }
}

