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
 * $Id: IdCreateIdentitiesReq.java,v 1.2 2008/06/25 05:52:28 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


class IdCreateIdentitiesReq extends AdminReq {
    private Map idAttrMap = new HashMap();
    private String realmPath = null;
    private IdType idType;


    /**
     * Constructs a new IdCreateIdentitiesReq.
     *
     * @param  targetDN the parent Realm DN. 
     */        
    IdCreateIdentitiesReq(String targetDN) {
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
     * adds the identity's name and its avPair Map to the idAttrMap.
     *
     * @param idName the Name of the Identity
     * @param idAttrMap the identity's AttributeValuePair map
     */
    void createIdCreateReq(String idName, Map attrMap) {
        idAttrMap.put(idName, attrMap);
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription115") +
            " " + targetDN);
        
        if ((idAttrMap != null) && !idAttrMap.isEmpty()) {
            Set set = idAttrMap.keySet();
            for (Iterator it=set.iterator(); it.hasNext(); ) {
                String key = (String)it.next();
                prnWriter.println(" " + key + " = ");
                Map map = (Map)idAttrMap.get(key);

                Set set2 = map.keySet();
                for (Iterator it2=set2.iterator(); it2.hasNext(); ) {
                    String ky2 = (String)it2.next();
                    prnWriter.println("   " + ky2 + " = ");
                    Set valSet2 = (Set)map.get(ky2);
                    for (Iterator it3=valSet2.iterator(); it3.hasNext(); ) {
                        String val3 = (String)it3.next();
                        prnWriter.println("     " + val3);
                    }
                }
            }
        }
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(SSOToken ssoToken)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("identity") + "\n" +
            bundle.getString("createIdentities") + idType.toString() +
            " " + bundle.getString("inrealm") + " " + targetDN);

        String[] args = {idType.toString(), realmPath};

        try {
            AMIdentityRepository amir =
                new AMIdentityRepository (ssoToken, realmPath);

            //  see if services are supported for the given IdType
            Set set = amir.getAllowedIdOperations(idType);
            if (!set.contains(IdOperation.CREATE)) {
                throw new AdminException(realmPath + " " +
                    bundle.getString("doesNotSupportCreation") + " " +
                    idType.toString());
            }

            doLog(args, AdminUtils.CREATE_IDENTITIES_ATTEMPT);
            amir.createIdentities(idType, idAttrMap);
            doLog(args, AdminUtils.CREATE_IDENTITIES);
        } catch (IdRepoException ire) {
            throw new AdminException(ire);
        } catch (SSOException ssoex) {
            throw new AdminException(ssoex);
        }
    }
}
