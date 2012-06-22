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
 * $Id: IDFFNameIdentifierMapper.java,v 1.3 2008/06/25 05:47:12 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco.plugins;

import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfo;
import com.sun.identity.federation.accountmgmt.FSAccountManager;
import com.sun.identity.federation.accountmgmt.FSAccountMgmtException;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.message.common.EncryptedNameIdentifier;
import com.sun.identity.liberty.ws.util.ProviderManager;
import com.sun.identity.liberty.ws.util.ProviderUtil;
import java.security.Key;
import java.util.Map;
import java.util.List;

/**
 * The class <code>IDFFNameIdentifierMapper</code> is an implementation
 * of <code>NameIdentifierMapper</code> for Liberty ID-FF providers.
 * <p>
 *
 */
public class IDFFNameIdentifierMapper implements NameIdentifierMapper {

    /**
     * Returns mapped <code>NameIdentifier</code> for specified user.   
     * This is used by Discovery Service to generate correct 
     * <code>NameIdentifier</code> when creating credentials for remote
     * service provider. A <code>NameIdentifier</code> in encrypted format
     * will be returned if the mapped <code>NameIdentifier</code> is
     * different from the original <code>NameIdentifier</code>, this
     * is to prevent the <code>NameIdentifier</code> to be revealed
     * to a proxy service provider. 
     * @param spProviderID Provider ID of the service provider to which
     *     the <code>NameIdentifier</code> needs to be mapped. 
     * @param idpProviderID Provider ID of the identifier provider.
     * @param nameId The <code>NameIdentifier</code> needs to be mapped. 
     * @param userID The user whose mapped <code>NameIdentifier</code> will 
     *     be returned. The value is the universal identifier of the user.
     * @return the mapped <code>NameIdentifier</code> for specified user, 
     *     return null if unable to map the <code>NameIdentifier</code>,
     *     return original name identifier if no need to mapp the
     *     <code>NameIdentifier</code>.
     */

    public NameIdentifier getNameIdentifier(String spProviderID, 
        String idpProviderID, NameIdentifier nameId, String userID) {
        try {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("IDFFNameIdentifierMapper, enter " +
                    "spProviderID=" + spProviderID + ", idpProviderID=" +
                    idpProviderID + ", userID=" + userID);
                if (nameId != null) {
                    FSUtils.debug.message("IDFFNameIdentifierMapper, enter " +
                        "name identifier=" + nameId.toString());
                }
            }

            if ((spProviderID == null) || (idpProviderID == null) ||
                (userID == null)) {
                return null;
            }
            if (spProviderID.equals(idpProviderID)) {
                // same entity, this is for the case of discovery service as IDP
                return nameId;
            }

            if (nameId != null) {
                String nameQualifier = nameId.getNameQualifier();
                if ((nameQualifier != null) && 
                    nameQualifier.equals(spProviderID)) {
                    // current name id is intended for the spProviderID 
                    return nameId;
                }
            }

            IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
            String metaAlias = metaManager.getIDPDescriptorConfig(
                "/", idpProviderID).getMetaAlias();
            
            FSAccountManager fsaccountmgr = 
                FSAccountManager.getInstance(metaAlias);
            FSAccountFedInfo accountinfo = 
                fsaccountmgr.readAccountFedInfo(userID, spProviderID);
            if (accountinfo != null) {
                NameIdentifier ni = accountinfo.getLocalNameIdentifier();
                FSUtils.debug.message("IDFFNameIdentifierMapper : new Ni");
                ProviderManager pm = ProviderUtil.getProviderManager();
                if (pm != null) {
                    Key encKey = pm.getEncryptionKey(spProviderID);
                    if (encKey != null) {
                        // encrypt this name identifier as it will be
                        // passed down through a proxy WSC
                        return EncryptedNameIdentifier
                            .getEncryptedNameIdentifier(
                            ni, spProviderID, encKey,
                            pm.getEncryptionKeyAlgorithm(spProviderID),
                            pm.getEncryptionKeyStrength(spProviderID));
                    } else {
                        return ni;
                    }
                } else {
                    return ni; 
                }
            } else {
                return nameId;
            }
        } catch (FSAccountMgmtException e) {
            // the federation info might not be there, just ignore
            FSUtils.debug.message("IDFFNameIdentifierMapper, account error", e);
        } catch (FSException e) {
            // the federation info might not be there, just ignore
            FSUtils.debug.message("IDFFNameIdentifierMapper, encrypt error", e);
        } catch (IDFFMetaException e) {
            // the provider might not be a IDFF provider, just ignore
            FSUtils.debug.message("IDFFNameIdentifierMapper, meta error", e);
        }
        return null;
    }
} 
