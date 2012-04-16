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
 * $Id: CircleOfTrustDescriptor.java,v 1.5 2008/06/25 05:46:38 qcheng Exp $
 *
 */


package com.sun.identity.cot;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import com.sun.identity.shared.validation.ValidationException;
import com.sun.identity.shared.validation.URLValidator;

/**
 * The <code>COTDescriptor</code> class is the representation
 * of the circle of trust configuration.
 */
public class CircleOfTrustDescriptor {
    private String circleOfTrustType = null;
    private String circleOfTrustName = null;
    private String realm = null;
    private String circleOfTrustDescription = null;
    private String circleOfTrustStatus = null;
    private String writerServiceURL = null;
    private String readerServiceURL = null;
    private String idffWriterServiceURL = null;
    private String idffReaderServiceURL = null;
    private String saml2WriterServiceURL = null;
    private String saml2ReaderServiceURL = null;
    private Set    trustedProviders = null;
    // map holding protocol to set of trusted providers
    private Map    trustedProviderMap = null;
    
    /*
     * Private Constructor.
     * This constructor populates object from the attribute
     * Map received from the data store.
     *
     * @param name The name of circle of trust.
     * @param realm The realm the circle of trust resides.
     * @param attrMap The map which contains attributes of the circle
     *                      of trust.
     * @throws COTException if values in the map are invalid.
     */
    CircleOfTrustDescriptor(String name, String realm, Map attrMap)
    throws COTException {
        setCircleOfTrustName(name);
        this.realm = realm;
        setCircleOfTrustDescription(COTUtils.getFirstEntry(
                attrMap, COTConstants.COT_DESC));
        setCircleOfTrustType(COTUtils.getFirstEntry(
                attrMap,COTConstants.COT_TYPE));
        setCircleOfTrustStatus(COTUtils.getFirstEntry(
                attrMap, COTConstants.COT_STATUS));
        setWriterServiceURL(COTUtils.getFirstEntry(
                attrMap,COTConstants.COT_WRITER_SERVICE));
        setReaderServiceURL(COTUtils.getFirstEntry(
                attrMap,COTConstants.COT_READER_SERVICE));
        setSAML2WriterServiceURL(COTUtils.getFirstEntry(
                attrMap,COTConstants.COT_SAML2_WRITER_SERVICE));
        setSAML2ReaderServiceURL(COTUtils.getFirstEntry(
                attrMap,COTConstants.COT_SAML2_READER_SERVICE));
        setIDFFWriterServiceURL(COTUtils.getFirstEntry(
                attrMap,COTConstants.COT_IDFF_WRITER_SERVICE));
        setIDFFReaderServiceURL(COTUtils.getFirstEntry(
                attrMap,COTConstants.COT_IDFF_READER_SERVICE));
        setTrustedProviders((Set) 
            attrMap.get(COTConstants.COT_TRUSTED_PROVIDERS));
    }
    
    /**
     * Creates new <code>COTDescriptor</code> instance.
     *
     * @param circleOfTrustName name for the circleOfTrust
     * @param realm The realm the circle of trust resides.
     * @param circleOfTrustStatus status of the CircleOfTrust.
     * @throws COTException if <code>circleOfTrustName</code>
     *         or <code>circleOfTrustStatus</code> is invalid.
     */
    public CircleOfTrustDescriptor(String circleOfTrustName, String realm,
            String circleOfTrustStatus) throws COTException {
        setCircleOfTrustName(circleOfTrustName);
        this.realm = realm;
        setCircleOfTrustStatus(circleOfTrustStatus);
    }
    
    /**
     * Creates new <code>COTDescriptor</code> instance.
     *
     * @param circleOfTrustName name for the circleOfTrust
     * @param realm The realm this circle of trust resides.
     * @param circleOfTrustStatus status of the CircleOfTrust.
     * @param circleOfTrustDescription description for the circleOfTrust.
     * @param circleOfTrustIDFFReaderURL the reader url of the CircleOfTrust.
     * @param circleOfTrustIDFFWriterURL the writer url of the circleOfTrust.
     * @param circleOfTrustSAML2ReaderURL the reader url of the CircleOfTrust.
     * @param circleOfTrustSAML2WriterURL the writer url of the circleOfTrust.
     * @param circleOfTrustProvider the trusted providers of the CircleOfTrust.
     * @throws COTException if any input parameter is invalid.
     */
    public CircleOfTrustDescriptor(String circleOfTrustName,
            String realm,
            String circleOfTrustStatus,
            String circleOfTrustDescription,
            String circleOfTrustIDFFReaderURL,
            String circleOfTrustIDFFWriterURL,
            String circleOfTrustSAML2ReaderURL,
            String circleOfTrustSAML2WriterURL,
            Set circleOfTrustProvider)
            throws COTException {
        setCircleOfTrustName(circleOfTrustName);
        this.realm = realm;
        setCircleOfTrustStatus(circleOfTrustStatus);
        setCircleOfTrustDescription(circleOfTrustDescription);
        if (circleOfTrustIDFFReaderURL != null) {
            setIDFFReaderServiceURL(circleOfTrustIDFFReaderURL);
        }
        if (circleOfTrustIDFFWriterURL != null) {
            setIDFFWriterServiceURL(circleOfTrustIDFFWriterURL);
        }
        if (circleOfTrustSAML2ReaderURL != null) {
            setSAML2ReaderServiceURL(circleOfTrustSAML2ReaderURL);
        }
        if (circleOfTrustSAML2WriterURL != null) {
            setSAML2WriterServiceURL(circleOfTrustSAML2WriterURL);
        }
        setTrustedProviders(circleOfTrustProvider);
    }
    
    /**
     * Validates and set the circle of trust name.
     */
    private void setCircleOfTrustName(String name) throws COTException {
        if (name == null || name.trim().length() <= 0 ) {
            String[] args = { name };
            throw new COTException("invalidCOTName",args);
        }
        circleOfTrustName = name;
    }
    
    /**
     * Validates and set the circle of trust type.
     */
    private void setCircleOfTrustType(String type) throws COTException {
        if ((type != null) && !COTUtils.isValidProtocolType(type)) {
            String[] data = { type };
            throw new COTException("invalidProtooclType",data);
        }
        COTUtils.debug.message("Circle of Trust type is : " + type);
        circleOfTrustType=type;
    }
    
    /**
     * Returns name of the Circle of Trust.
     *
     * @return name of the Circle of Trust.
     */
    public String getCircleOfTrustName() {
        return circleOfTrustName;
    }
    
    /**
     * Returns name of the realm the Circle of Trust resides.
     *
     * @return realm name.
     */
    public String getCircleOfTrustRealm() {
        return realm;
    }
    
    /**
     * Returns status of the Circle of Trust.
     *
     * @return status of the Circle of Trust. It can be
     *         "active" or "inactive"
     */
    public String getCircleOfTrustStatus() {
        return circleOfTrustStatus;
    }
    
    /**
     * Returns description of the Circle of Trust.
     *
     * @return description of the Circle of Trust.
     */
    public String getCircleOfTrustDescription() {
        return circleOfTrustDescription;
    }
    
    /**
     * Returns reader service URL of the Circle of Trust for ID-FF protocol.
     *
     * @return Reader service URL for ID-FF protocol.
     */
    public String getIDFFReaderServiceURL() {
        if (idffReaderServiceURL != null) {
            return idffReaderServiceURL;
        } else if ((circleOfTrustType != null) && 
            circleOfTrustType.equalsIgnoreCase(COTConstants.IDFF)) { 
            // handle legacy case
            return readerServiceURL;
        } else {
            return null;
        }
    }
 
    /**
     * Returns reader service URL of the Circle of Trust for SAMLv2 protocol.
     *
     * @return Reader service URL for SAMLv2 protocol.
     */
    public String getSAML2ReaderServiceURL() {
        if (saml2ReaderServiceURL != null) {
            return saml2ReaderServiceURL;
        } else if ((circleOfTrustType != null) && 
            circleOfTrustType.equalsIgnoreCase(COTConstants.SAML2)) { 
            // handle legacy case
            return readerServiceURL;
        } else {
            return null;
        }
    }
    
    /**
     * Returns Writer Service URL of the Circle of Trust for ID-FF protocol.
     *
     * @return the Writer service URL for ID-FF protocol.
     */
    public String getIDFFWriterServiceURL() {
        if (idffWriterServiceURL != null) {
            return idffWriterServiceURL;
        } else if ((circleOfTrustType != null) && 
            circleOfTrustType.equalsIgnoreCase(COTConstants.IDFF)) { 
            // handle legacy case
            return writerServiceURL;
        } else {
            return null;
        }
    }
    
    /**
     * Returns Writer Service URL of the Circle of Trust for SAMLv2 protocol.
     *
     * @return the Writer service URL for SAMLv2 protocol.
     */
    public String getSAML2WriterServiceURL() {
        if (saml2WriterServiceURL != null) {
            return saml2WriterServiceURL;
        } else if ((circleOfTrustType != null) && 
            circleOfTrustType.equalsIgnoreCase(COTConstants.SAML2)) { 
            // handle legacy case
            return writerServiceURL;
        } else {
            return null;
        }
    }
    
    /**
     * Sets description of the Circle of Trust.
     *
     * @param circleOfTrustDescription Description of the Circle of Trust.
     */
    public void setCircleOfTrustDescription(String circleOfTrustDescription) {
        this.circleOfTrustDescription = circleOfTrustDescription;
    }

    /**
     * Sets reader service URL.
     *
     * @param readerServiceURL reader service URL.
     * @throws COTException if <code>readerServiceURL</code>
     *         is not an URL.
     */
    private void setReaderServiceURL(String readerServiceURL)
    throws COTException {
        if ((readerServiceURL != null) &&
                (readerServiceURL.trim().length() > 0)){
            URLValidator validator = URLValidator.getInstance();
            try {
                validator.validate(readerServiceURL);
            } catch (ValidationException e) {
                throw new COTException("invalidReaderUrl",null);
            }
        }

        this.readerServiceURL = readerServiceURL;
    }

    /**
     * Sets reader service URL for ID-FF protocol.
     *
     * @param readerServiceURL reader service URL for ID-FF protocol.
     * @throws COTException if <code>readerServiceURL</code>
     *         is not an URL.
     */
    public void setIDFFReaderServiceURL(String readerServiceURL)
    throws COTException {
        if ((readerServiceURL != null) &&
                (readerServiceURL.trim().length() > 0)){
            URLValidator validator = URLValidator.getInstance();
            try {
                validator.validate(readerServiceURL);
            } catch (ValidationException e) {
                throw new COTException("invalidReaderUrl",null);
            }
        }
        
        this.idffReaderServiceURL = readerServiceURL;
    }
    
    /**
     * Sets reader service URL for SMALv2 protocol.
     *
     * @param readerServiceURL reader service URL of SAMLv2 protocol.
     * @throws COTException if <code>readerServiceURL</code>
     *         is not an URL.
     */
    public void setSAML2ReaderServiceURL(String readerServiceURL)
    throws COTException {
        if ((readerServiceURL != null) &&
                (readerServiceURL.trim().length() > 0)){
            URLValidator validator = URLValidator.getInstance();
            try {
                validator.validate(readerServiceURL);
            } catch (ValidationException e) {
                throw new COTException("invalidReaderUrl",null);
            }
        }
        
        this.saml2ReaderServiceURL = readerServiceURL;
    }
    
    /**
     * Sets writer service URL.
     *
     * @param writerServiceURL writer service URL of the Circle of Trust.
     * @throws COTException if <code>writerServiceURL</code>
     *         is not an URL.
     */
    private void setWriterServiceURL(String writerServiceURL)
    throws COTException {
        if ((writerServiceURL != null) &&
                (writerServiceURL.trim().length() > 0)
                ){
            URLValidator validator = URLValidator.getInstance();
            try {
                validator.validate(writerServiceURL);
            } catch (ValidationException e) {
                throw new COTException("invalidWriterUrl",null);
            }
        }
        
        this.writerServiceURL = writerServiceURL;
    }

    /**
     * Sets writer service URL for IDFF protocol.
     *
     * @param writerServiceURL writer service URL of the Circle of Trust.
     * @throws COTException if <code>writerServiceURL</code>
     *         is not an URL.
     */
    public void setIDFFWriterServiceURL(String writerServiceURL)
    throws COTException {
        if ((writerServiceURL != null) &&
                (writerServiceURL.trim().length() > 0)
                ){
            URLValidator validator = URLValidator.getInstance();
            try {
                validator.validate(writerServiceURL);
            } catch (ValidationException e) {
                throw new COTException("invalidWriterUrl",null);
            }
        }

        this.idffWriterServiceURL = writerServiceURL;
    }

    /**
     * Sets writer service URL for SAML2 protocol.
     *
     * @param writerServiceURL writer service URL of the Circle of Trust.
     * @throws COTException if <code>writerServiceURL</code>
     *         is not an URL.
     */
    public void setSAML2WriterServiceURL(String writerServiceURL)
    throws COTException {
        if ((writerServiceURL != null) &&
                (writerServiceURL.trim().length() > 0)
                ){
            URLValidator validator = URLValidator.getInstance();
            try {
                validator.validate(writerServiceURL);
            } catch (ValidationException e) {
                throw new COTException("invalidWriterUrl",null);
            }
        }

        this.saml2WriterServiceURL = writerServiceURL;
    }
 
    /**
     * Sets status of a circle of trust.
     *
     * @param circleOfTrustStatus the circle of trust status. The valid value
     *        for status is "active" or "inactive".
     * @throws COTException if <code>circleOfTrustStatus</code>
     *         is blank or it is not "active" or "inactive"
     */
    public void setCircleOfTrustStatus(String circleOfTrustStatus)
    throws COTException {
        if (circleOfTrustStatus !=null &&
                (circleOfTrustStatus.equalsIgnoreCase(COTConstants.ACTIVE ) ||
                circleOfTrustStatus.equalsIgnoreCase(COTConstants.INACTIVE))) {
            this.circleOfTrustStatus = circleOfTrustStatus;
        } else {
            throw new COTException("invalidCotStatus", null);
        }
    }
    
    /**
     * Sets trusted providers of a circle of trust.
     *
     * @param circleOfTrustProvider A set of trusted providers
     */
    public void setTrustedProviders(Set circleOfTrustProvider) {
        trustedProviderMap = Collections.unmodifiableMap(
            COTUtils.trustedProviderSetToProtocolMap(
                circleOfTrustProvider, realm));
        trustedProviders = Collections.unmodifiableSet(
            COTUtils.trustedProviderProtocolMapToSet(trustedProviderMap));
    }
    
    /**
     * Returns a set of trusted providers in the circle of trust.
     *
     * @return a set of trusted providers in the circle of trust, or null
     * if there is no trusted providers in the circle of trust.
     */
    public Set getTrustedProviders() {
        if (trustedProviders == null) {
            return null;
        } else {
            Set ret = new HashSet();
            ret.addAll(trustedProviders);
            return ret;
        }
    }
    
    /**
     * Returns a set of trusted providers in the circle of trust for a specific
     * protocol.
     * @param protocol name of the federation protocol
     * @return a set of trusted providers in the circle of trust, or null if 
     * such entity does not exist.
     */
    public Set getTrustedProviders(String protocol) {
        if ((trustedProviderMap == null) || (protocol == null)) {
            return null;
        } else {
            Set tmp = (Set) trustedProviderMap.get(protocol);
            if (tmp != null) {
                // handle legacy case
                if ((circleOfTrustType == null) 
                    || circleOfTrustType.equalsIgnoreCase(protocol)) {
                    Set ret = new HashSet();
                    ret.addAll(tmp);
                    return ret;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }
    
    /**
     * Adds entity identifier to trusted providers set within the
     * circle of trust.
     *
     * @param entityID the entity id of a provider .
     * @param protocol name of protocol for the entity.
     * @return true if the set did not already contain the entityID.
     */
    public boolean add(String entityID, String protocol) throws COTException {
        if (!COTUtils.isValidProtocolType(protocol)) {
            throw new COTException("invalidProtocolType", null);
        }
        if ((trustedProviderMap == null) 
            || (trustedProviderMap.get(protocol) == null) 
            || !((Set) trustedProviderMap.get(protocol)).contains(entityID)) {
            Set ret = new HashSet();
            if (trustedProviders != null) {
                ret.addAll(trustedProviders);
            }
            boolean result = ret.add(entityID + COTConstants.DELIMITER 
                + protocol);
            setTrustedProviders(ret);
            return result;
        } else {
            throw new COTException("entityExistsInCot", null);
        }
    }
    
    /**
     * Removes member from the trusted provider set within this circle
     * of trust.
     *
     * @param entityID The entity id of a provider.
     * @param protocol name of protocol for the entity.
     * @return true if the set contained the entityID.
     */
    public boolean remove(String entityID, String protocol) {
        if (!COTUtils.isValidProtocolType(protocol)) {
            return false;
        }
        if ((trustedProviderMap == null) ||
            (trustedProviderMap.get(protocol) == null) ||
            !((Set) trustedProviderMap.get(protocol)).contains(entityID)) {
            return false;
        }
        Set ret = new HashSet();
        if (trustedProviders != null) {
            ret.addAll(trustedProviders);
        }
        boolean result = ret.remove(entityID + COTConstants.DELIMITER 
            + protocol);
        if (!result) {
            // handle legacy case
            result = ret.remove(entityID);
        }
        setTrustedProviders(ret);
        return result;
    }
    
    /**
     * Returns attributes of this object into a map.
     */
    protected Map getAttributes() {
        Map attrMap = new HashMap();
        COTUtils.fillEntriesInSet(
                attrMap,
                COTConstants.COT_DESC,
                circleOfTrustDescription);
        COTUtils.fillEntriesInSet(
                attrMap,
                COTConstants.COT_STATUS,
                circleOfTrustStatus);
        COTUtils.fillEntriesInSet(
                attrMap,
                COTConstants.COT_SAML2_WRITER_SERVICE,
                saml2WriterServiceURL);
        COTUtils.fillEntriesInSet(
                attrMap,
                COTConstants.COT_IDFF_WRITER_SERVICE,
                idffWriterServiceURL);
        COTUtils.fillEntriesInSet(
                attrMap,
                COTConstants.COT_SAML2_READER_SERVICE,
                saml2ReaderServiceURL);
        COTUtils.fillEntriesInSet(
                attrMap,
                COTConstants.COT_IDFF_READER_SERVICE,
                idffReaderServiceURL);
        attrMap.put(COTConstants.COT_TRUSTED_PROVIDERS, trustedProviders);
        return attrMap;
    }
}
