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
 * $Id: IdIsMemberReq.java,v 1.2 2008/06/25 05:52:28 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;


class IdIsMemberReq extends AdminReq {
    private String realmPath = null;
    private String idName = null;
    private IdType idType;
    private IdType targetIdType;
    private String targetIdName = null;

    /**
     * Constructs a new IdIsMemberReq.
     *
     * @param  targetDN the parent Realm DN. 
     */        
    IdIsMemberReq(String targetDN) {
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
     * sets the target's Identity Type for this request
     *
     * @param identType the Type of the Identity
     */
    void setTargetIdType(IdType identType) {
        targetIdType = identType;
    }

    /**
     * sets the Identity Name for this request
     *
     * @param identName the Name of the Identity
     */
    void setIdName(String identName) {
        idName = identName;
    }

    /**
     * sets the target Identity Name for this request
     *
     * @param identName the Name of the Identity
     */
    void setTargetIdName(String identName) {
        targetIdName = identName;
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription125") +
            " " + targetDN + "/" + targetIdName);
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(SSOToken ssoToken)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("identity") + "\n" +
            bundle.getString("identityname") + " " +
            idName + " " + bundle.getString("of") + " " +
            idType.toString() + " " +
            bundle.getString("idIsMember") + " " +
            targetIdName + " " +
            bundle.getString("of") + " " +
            targetIdType.toString() + " " +
            bundle.getString("inrealm") + " " + realmPath);


        //  see if idType can be a member of targetIdType
        Set set = idType.canBeMemberOf();
        if (!set.contains(targetIdType)) {
            throw new AdminException(idType.toString() + " " +
                bundle.getString("canNotBeMemberOf") + " " +
                targetIdType.toString());
        }

        try {
            AMIdentity aiTarget = new AMIdentity(ssoToken,
                targetIdName, targetIdType, realmPath, null);
            AMIdentity ai2use = new AMIdentity(ssoToken,
                idName, idType, realmPath, null);

            //  have both AMIdentity objects
            boolean isMem = ai2use.isMember(aiTarget);
            AdminReq.writer.println(" " + isMem);
        } catch (IdRepoException ire) {
            throw new AdminException(ire);
        } catch (SSOException ssoex) {
            throw new AdminException(ssoex);
        }
    }
}

