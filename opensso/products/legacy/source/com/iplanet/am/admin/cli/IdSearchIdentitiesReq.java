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
 * $Id: IdSearchIdentitiesReq.java,v 1.2 2008/06/25 05:52:29 qcheng Exp $
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
import com.sun.identity.idm.IdUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;

class IdSearchIdentitiesReq extends AdminReq {
    private Set idPatternSet = null;
    private String realmPath = null;
    private String idPattern = null;
    private IdType idType;
    private boolean recursive = false;

    /**
     * Constructs a new IdSearchIdentitiesReq.
     *
     * @param  targetDN the parent Realm DN. 
     */        
    IdSearchIdentitiesReq(String targetDN) {
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
     * sets the identity pattern Set
     *
     *  might have setIdPattern() add to the set?
     *
     * @param idSet the Set of Identity Patterns to search for
     */
    void setIdPatternSet(Set idSet) {
        idPatternSet = idSet;
    }

    /**
     * Sets the identity pattern to search for.
     *
     * @param pattern the pattern to search for.
     */
    void setIdPattern(String pattern) {
        idPattern = pattern;
    }

    /**
     * Sets the recursive flag for the search.
     *
     * @param recurs the recursive flag for the search.
     */
    void setRecursive(boolean recurs) {
        recursive = recurs;
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription117") +
            " " + targetDN);
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(SSOToken ssoToken)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("identity") + "\n" +
            bundle.getString("searchIdentities") + " " +
            idType.toString() + " " +
            bundle.getString("withpattern") + " " + idPattern + " " +
            bundle.getString("inrealm") + " " + targetDN);

        try {
            AMIdentityRepository amir =
                new AMIdentityRepository (ssoToken, realmPath);

            /*
             * set up the search for the idPattern(s) of the idType to get
             * their AMIdentity objects.
             */
            IdSearchControl isCtl = new IdSearchControl();
            isCtl.setRecursive(recursive);
            IdSearchResults isr =
                amir.searchIdentities(idType, idPattern, isCtl);
            Set srSet = isr.getSearchResults();

            if ((srSet == null) || srSet.isEmpty()) {
                String[] arg = {idPattern};
                AdminReq.writer.println("  " + MessageFormat.format(
                    bundle.getString("noMatchIds"), arg));
            } else {
                // get special users
                IdSearchResults specialUsersResults =
                    amir.getSpecialIdentities(idType);
                Set specialUsers = specialUsersResults.getSearchResults();

                for (Iterator it2 = srSet.iterator(); it2.hasNext(); ) {
                    AMIdentity amId = (AMIdentity)it2.next();
                    if (!specialUsers.contains(amId)) {
                        AdminReq.writer.println("  " + amId.getName() + " (" +
                            IdUtils.getUniversalId(amId) + ")");
                    }
                }
            }
        } catch (IdRepoException ire) {
            throw new AdminException(ire);
        } catch (SSOException ssoex) {
            throw new AdminException(ssoex);
        }
    }
}

