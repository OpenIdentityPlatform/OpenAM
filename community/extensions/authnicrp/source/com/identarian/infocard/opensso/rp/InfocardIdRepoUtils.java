/* The contents of this file are subject to the terms
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
 * $Id: InfocardIdRepoUtils.java,v 1.2 2009/09/15 13:27:13 ppetitsm Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2008 Patrick Petit Consulting
 */
package com.identarian.infocard.opensso.rp;

import com.identarian.infocard.opensso.rp.exception.InfocardException;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 
 * @author Patrick
 *
 */
public class InfocardIdRepoUtils {

    protected static final String INFOCARD_OBJECT_CLASS = "infocard";
    protected static final String INFOCARD_PPID_ATTRIBUTE = "ic-ppid";
    protected static final String INFOCARD_DATA_ATTRIBUTE = "ic-data";
    protected static final Set<String> INFOCARD_ATTRIBUTES = new HashSet<String>() {

        {
            add(INFOCARD_PPID_ATTRIBUTE);
            add(INFOCARD_DATA_ATTRIBUTE);
        }
    };
    private static Debug debug = Debug.getInstance(Infocard.amAuthInfocard);

    protected static void addRepoInfocardData(AMIdentity userIdentity, String ppid,
            String issuer, String password) throws InfocardException {

        Map<String, Set> attributeMap = (Map<String, Set>) Collections.EMPTY_MAP;
        Set<String> ppidAttrValue = (Set<String>) Collections.EMPTY_SET;
        Set<String> icDataAttrValue = (Set<String>) Collections.EMPTY_SET;
        InfocardIdRepoData icData;
        String fppid = getFriendlyPPID(ppid);

        try {
            attributeMap = userIdentity.getAttributes(INFOCARD_ATTRIBUTES);
            ppidAttrValue = (Set<String>) attributeMap.get(INFOCARD_PPID_ATTRIBUTE);
            if ((ppidAttrValue != null) && (!ppidAttrValue.isEmpty())) {
                if (ppidAttrValue.contains(fppid)) {
                    // This Information Card is already registered, skip
                    if (debug.messageEnabled()) {
                        debug.message(
                                "Skip attempt to duplicate Information Card record in idRepo for PPID = " + fppid);
                    }
                    return;
                }
                icDataAttrValue = (Set<String>) attributeMap.get(INFOCARD_DATA_ATTRIBUTE);
            } else {
                // No Information Card registered for that user yet
                ppidAttrValue = new HashSet<String>();
                icDataAttrValue = new HashSet<String>();
                setInfocardObjectClass(userIdentity);
            }
            ppidAttrValue.add(fppid);
            icData = new InfocardIdRepoData();
            icData.setPpid(ppid);
            byte[] encPassword = CryptoUtils.encrypt(password, icData);
            icData.setPasswordArray(encPassword);
            icData.setIssuer(issuer);
            icDataAttrValue.add(encodeRepoInfocardData(icData));
            attributeMap = new HashMap<String, Set>();
            attributeMap.put(INFOCARD_PPID_ATTRIBUTE, ppidAttrValue);
            attributeMap.put(INFOCARD_DATA_ATTRIBUTE, icDataAttrValue);
            userIdentity.setAttributes(attributeMap);
            userIdentity.store();
        } catch (IdRepoException e1) {
            throw new InfocardException("Failed to add Information Card attributes", e1);
        } catch (SSOException e2) {
            throw new InfocardException("Failed to add Information Card attributes", e2);
        }
    }

    protected static void removeRepoInfocardData(AMIdentity userIdentity, String ppid)
            throws InfocardException {

        Map<String, Set> attributeMap = Collections.EMPTY_MAP;
        Set<String> ppidAttrValue = (Set<String>) Collections.EMPTY_SET;
        Set<String> icDataAttrValue = (Set<String>) Collections.EMPTY_SET;
        String fppid = getFriendlyPPID(ppid);
        InfocardIdRepoData icData = null;

        try {
            attributeMap = userIdentity.getAttributes(INFOCARD_ATTRIBUTES);
            ppidAttrValue = (Set<String>) attributeMap.get(INFOCARD_PPID_ATTRIBUTE);
            if ((ppidAttrValue != null) && ppidAttrValue.contains(fppid)) {
                // This IC is not registered
                ppidAttrValue.remove(fppid);
            }
            icDataAttrValue = (Set<String>) attributeMap.get(INFOCARD_DATA_ATTRIBUTE);
            if ((icDataAttrValue != null) && (!icDataAttrValue.isEmpty())) {
                Iterator itr = icDataAttrValue.iterator();
                while (itr.hasNext()) {
                    String attrValue = ((String) itr.next()).trim();
                    icData = decodeRepoInfocardData(attrValue);
                    if (icData.getPpid().equals(ppid)) {
                        icDataAttrValue.remove(attrValue);
                        break;
                    }
                }
            }
            attributeMap.put(INFOCARD_PPID_ATTRIBUTE, ppidAttrValue);
            attributeMap.put(INFOCARD_DATA_ATTRIBUTE, icDataAttrValue);
            userIdentity.setAttributes(attributeMap);
            userIdentity.store();
        } catch (IdRepoException e1) {
            throw new InfocardException("Failed to read Information Card attributes", e1);
        } catch (SSOException e2) {
            throw new InfocardException("Failed to read Information Card attributes", e2);
        }
    }

    protected static AMIdentity searchUserIdentity(
            AMIdentityRepository idRepo,
            String ppid, String userId) throws InfocardException {

        AMIdentity userIdentity = null;
        String fppid = getFriendlyPPID(ppid);

        IdType idtype = IdType.USER;
        IdSearchControl isc = new IdSearchControl();
        isc.setAllReturnAttributes(true);
        isc.setTimeOut(0);
        Map<String, Set> avMap = new HashMap<String, Set>();
        Set<String> set = new HashSet<String>();
        set.add(INFOCARD_OBJECT_CLASS);
        avMap.put("objectclass", set);
        set = new HashSet();
        set.add(fppid);
        avMap.put(INFOCARD_PPID_ATTRIBUTE, set);
        isc.setSearchModifiers(IdSearchOpModifier.OR, avMap);
        try {
            IdSearchResults results = idRepo.searchIdentities(idtype, userId, isc);
            Set idSet = results.getSearchResults();
            userIdentity = getFirstMatchingEntry(idSet, fppid);
        } catch (IdRepoException e1) {
            throw new InfocardException("Failed to search identity", e1);
        } catch (SSOException e2) {
            throw new InfocardException("Failed to search identity", e2);
        }

        return userIdentity;
    }

    protected static void removeAllRepofocardData(AMIdentity userIdentity)
            throws InfocardException {

        try {
            userIdentity.removeAttributes(INFOCARD_ATTRIBUTES);
        } catch (IdRepoException e1) {
            throw new InfocardException("Failed to remove Information Card attributes", e1);
        } catch (SSOException e2) {
            throw new InfocardException("Failed to remove Information Card attributes", e2);
        }

    }

    protected static String generateDynamicUserId(String gn, String sn) {

        StringBuffer fname = new StringBuffer();
        if (gn != null && gn.length() != 0) {
            fname.append(gn);
        }

        if (sn != null && sn.length() > 0) {
            fname.append(sn);
        }

        UUID uid = UUID.nameUUIDFromBytes(fname.toString().getBytes());
        return String.valueOf(uid);
    }

    private static AMIdentity getFirstMatchingEntry(Set idSet, String ppid)
            throws InfocardException {

        Object[] objs = idSet.toArray();
        AMIdentity userIdentity = null;
        int setsize = idSet.size();

        if (setsize > 0) {
            if (debug.messageEnabled()) {
                debug.message("getSearchResultsFirstEntry: Search returns " + setsize + " entries");
            }

            int i;
            String userID;

            AMIdentity curIdentity = null;
            for (i = 0; i < setsize; i++) {
                curIdentity = (AMIdentity) objs[i];
                userID = curIdentity.getName();
                if (debug.messageEnabled()) {
                    debug.message("\tFound " +
                            userID + " with universal ID = " + curIdentity.getUniversalId());
                }
                // Get admin users out of this
                if (userID.equalsIgnoreCase("amadmin") ||
                        userID.equalsIgnoreCase("amldapuser") ||
                        userID.equalsIgnoreCase("dsameuser") ||
                        userID.equalsIgnoreCase("amService-URLAccessAgent")) {
                    // don't mess with admin users
                    return null;
                }

                Set attributeValue = Collections.EMPTY_SET;
                try {
                    attributeValue = curIdentity.getAttribute(INFOCARD_PPID_ATTRIBUTE);
                    if ((attributeValue != null) && (!attributeValue.isEmpty())) {
                        Iterator itr = attributeValue.iterator();
                        while (itr.hasNext()) {
                            String value = ((String) itr.next()).trim();
                            if (value.equals(ppid)) {
                                userIdentity = curIdentity;
                                break;
                            }
                        }
                    }
                } catch (IdRepoException e1) {
                    throw new InfocardException("Failed to remove Information Card attributes", e1);
                } catch (SSOException e2) {
                    throw new InfocardException("Failed to remove Information Card attributes", e2);
                }

                if (userIdentity != null) {
                    break;
                }

            }
        }
        return userIdentity;
    }

    private static void setInfocardObjectClass(AMIdentity userIdentity)
            throws InfocardException {

        try {
            Set attrValueSetObjectClass = userIdentity.getAttribute("objectClass");
            if ((attrValueSetObjectClass != null) &&
                    (!attrValueSetObjectClass.contains(INFOCARD_OBJECT_CLASS))) {
                attrValueSetObjectClass.add(INFOCARD_OBJECT_CLASS);
                Map<String, Set> map = new HashMap<String, Set>(2);
                map.put("ObjectClass", attrValueSetObjectClass);
                userIdentity.setAttributes(map);
            }

        } catch (IdRepoException e1) {
            throw new InfocardException("Failed to set 'infocard' Object Class", e1);
        } catch (SSOException e2) {
            throw new InfocardException("Failed to set 'infocard' Object Class", e2);
        }

    }

    private static String encodeRepoInfocardData(InfocardIdRepoData icData)
            throws InfocardException {

        byte[] sSerialized = null;

        String encodedString = null;
        ByteArrayOutputStream byteOut;

        ObjectOutputStream objOutStream;

        try {
            byteOut = new ByteArrayOutputStream();
            objOutStream = new ObjectOutputStream(byteOut);

            //convert object to byte using streams
            objOutStream.writeObject(icData);
            sSerialized = byteOut.toByteArray();

            // base 64 encoding & encrypt
            encodedString = (String) AccessController.doPrivileged(
                    new EncodeAction(Base64.encode(sSerialized).trim()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new InfocardException("Fatal internal error", e);
        }
        return encodedString;
    }

    private static InfocardIdRepoData decodeRepoInfocardData(String icDataStr)
            throws InfocardException {

        InfocardIdRepoData icData = null;

        // decrypt and then decode
        String decStr = (String) AccessController.doPrivileged(
                new DecodeAction(icDataStr));
        byte[] sSerialized = Base64.decode(decStr);

        if (sSerialized == null) {
            return null;
        }

        byte byteDecrypted[];
        ByteArrayInputStream byteIn;

        ObjectInputStream objInStream = null;
        try {
            byteDecrypted = sSerialized;
            //convert byte to object using streams
            byteIn = new ByteArrayInputStream(byteDecrypted);
            objInStream = new ObjectInputStream(byteIn);
            icData = (InfocardIdRepoData) objInStream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            throw new InfocardException("Fatal internal error", e);
        }

        return icData;
    }

    protected static InfocardIdRepoData getInfocardRepoData(AMIdentity userIdentity,
            String ppid) throws InfocardException {

        InfocardIdRepoData icData = null;
        Map<String, Set> attributeMap = (Map<String, Set>) Collections.EMPTY_MAP;
        Set<String> attributeValue = (Set<String>) Collections.EMPTY_SET;
        String fppid = getFriendlyPPID(ppid);

        try {
            attributeMap = (Map<String, Set>)userIdentity.getAttributes(INFOCARD_ATTRIBUTES);
            attributeValue = (Set<String>) attributeMap.get(INFOCARD_PPID_ATTRIBUTE);
            if ((attributeValue != null)) {
                Iterator itr = attributeValue.iterator();
                while (itr.hasNext()) {
                    String attrValue = ((String) itr.next()).trim();
                    if (attrValue.equals(fppid)) {
                        icData = readInfocardIdRepoData(attributeMap, ppid);
                        if (icData == null) {
                            // Internal error. Remove the orphan Information Card
                            removeRepoInfocardData(userIdentity, ppid);
                            throw new InfocardException(
                                    "Internal error: Inconsistent Information Card data in idRepo.");
                        }
                        break;
                    }
                }
            }
        } catch (IdRepoException e1) {
            throw new InfocardException("Failed to read Information Card attributes", e1);
        } catch (SSOException e2) {
            throw new InfocardException("Failed to read Information Card attributes", e2);
        }
        return icData;
    }

    private static InfocardIdRepoData readInfocardIdRepoData(
            Map<String, Set>attributeMap, String ppid) throws InfocardException {

        InfocardIdRepoData icData = null;

        Set<String> attributeValue = (Set<String>) Collections.EMPTY_SET;
        attributeValue = (Set<String>) attributeMap.get(INFOCARD_DATA_ATTRIBUTE);
        if ((attributeValue != null)) {
            Iterator itr = attributeValue.iterator();
            while (itr.hasNext()) {
                String attrValue = ((String) itr.next()).trim();
                icData = decodeRepoInfocardData(attrValue);
                if (icData.getPpid().equals(ppid)) {
                    byte[] passwordArray = icData.getPasswordArrray();
                    icData.setPassword(CryptoUtils.decrypt(passwordArray, icData));
                    break;
                }
            }
        }
        return icData;
    }

    private static String getFriendlyPPID(String ppid) {

        return InfocardClaims.friendlyPPID(ppid);
    }
}