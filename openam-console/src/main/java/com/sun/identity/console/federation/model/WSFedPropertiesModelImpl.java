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
 * "Portions Copyrighted [year] [name of copyright owner]
 *
 * $Id: WSFedPropertiesModelImpl.java,v 1.14 2009/11/10 01:19:50 exu Exp $
 *
 * Portions copyright 2012-2016 ForgeRock AS.
 */


package com.sun.identity.console.federation.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import com.sun.identity.wsfederation.meta.WSFederationMetaSecurityUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.wsfederation.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.UriNamedClaimTypesOfferedElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.ClaimType;
import com.sun.identity.wsfederation.jaxb.wsfederation.DisplayNameType;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.wsfederation.jaxb.entityconfig.AttributeElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenIssuerEndpointElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenIssuerNameElement;
import com.sun.identity.wsfederation.jaxb.entityconfig.ObjectFactory;
import java.util.Set;
import javax.xml.bind.JAXBException;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

public class WSFedPropertiesModelImpl extends EntityModelImpl
    implements WSFedPropertiesModel
{
    private static Map GEN_DATA_MAP = new HashMap(6);
    private static Map GEN_DUAL_DATA_MAP = new HashMap(8);
    private static Map SPEX_DATA_MAP = new HashMap(28);
    private static Map IDPSTD_DATA_MAP = new HashMap(2);
    private static Map IDPEX_DATA_MAP = new HashMap(32);

    private WSFederationMetaManager metaManager = null;
    static {
        GEN_DATA_MAP.put(TF_DISPNAME, Collections.EMPTY_SET);
        GEN_DATA_MAP.put(TFTOKENISSUER_NAME, Collections.EMPTY_SET);
        GEN_DATA_MAP.put(TFTOKENISSUER_ENDPT, Collections.EMPTY_SET);
    }

    static {
        GEN_DUAL_DATA_MAP.put(TF_DISPNAME, Collections.EMPTY_SET);
        GEN_DUAL_DATA_MAP.put(TFIDPDISP_NAME, Collections.EMPTY_SET);
        GEN_DUAL_DATA_MAP.put(TFTOKENISSUER_NAME, Collections.EMPTY_SET);
        GEN_DUAL_DATA_MAP.put(TFTOKENISSUER_ENDPT, Collections.EMPTY_SET);
    }

    static {
        SPEX_DATA_MAP.put(TF_DISPNAME, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(TFSPAUTOFED_ENABLED, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(TFASSERT_SIGNED, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(TFSPAUTOFED_ATTR, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(TFASSERTEFFECT_TIME, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(TFSPACCT_MAPPER, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(TFSPATTR_MAPPER, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(TFSPAUTHCONT_MAPPER, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(TFAUTHCONTCLASS_REFMAPPING, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(TFAUTHCONT_COMPARTYPE, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(TFSPATTR_MAP, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(TFRELAY_STATE, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(TFASSERT_TIMESKEW, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(TFACCT_REALM_COOKIE, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(TFACCT_REALM_SELECTION, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(TFACCT_HOMEREALM_DISC_SERVICE, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(COT_LIST, Collections.EMPTY_SET);
        SPEX_DATA_MAP.put(WREPLY_LIST, Collections.EMPTY_SET);

    }

    static {
        IDPEX_DATA_MAP.put(TF_DISPNAME, Collections.EMPTY_SET);
        IDPEX_DATA_MAP.put(TFSIGNCERT_ALIAS, Collections.EMPTY_SET);
        IDPEX_DATA_MAP.put(TFAUTOFED_ENABLED, Collections.EMPTY_SET);
        IDPEX_DATA_MAP.put(TFIDPAUTOFED_ATTR, Collections.EMPTY_SET);
        IDPEX_DATA_MAP.put(TFIDPASSERT_TIME, Collections.EMPTY_SET);
        IDPEX_DATA_MAP.put(TFIDPAUTH_CONTMAPPER, Collections.EMPTY_SET);
        IDPEX_DATA_MAP.put(TFIDPACCT_MAPPER, Collections.EMPTY_SET);
        IDPEX_DATA_MAP.put(TFIDPATTR_MAPPER, Collections.EMPTY_SET);
        IDPEX_DATA_MAP.put(TFIDPATTR_MAP, Collections.EMPTY_SET);
        IDPEX_DATA_MAP.put(TFNAMEID_FORMAT, Collections.EMPTY_SET);
        IDPEX_DATA_MAP.put(TFNAMEID_ATTRIBUTE, Collections.EMPTY_SET);
        IDPEX_DATA_MAP.put(TFNAME_INCLU_DOMAIN, Collections.EMPTY_SET);
        IDPEX_DATA_MAP.put(TFDOMAIN_ATTRIBUTE, Collections.EMPTY_SET);
        IDPEX_DATA_MAP.put(TFUPN_DOMAIN, Collections.EMPTY_SET);
        IDPEX_DATA_MAP.put(COT_LIST, Collections.EMPTY_SET);
        IDPEX_DATA_MAP.put(WREPLY_LIST, Collections.EMPTY_SET);
    }

    static {
        // TBD-  once backend api is complete
        IDPSTD_DATA_MAP.put(TFCLAIM_TYPES, Collections.EMPTY_SET);
    }

    protected WSFederationMetaManager getWSFederationMetaManager()
        throws WSFederationMetaException
    {
        if (metaManager == null) {
            metaManager = new WSFederationMetaManager();
        }
        return metaManager;
    }

    /** Creates a new instance of WSFedPropertiesModelImpl */
    public WSFedPropertiesModelImpl(HttpServletRequest req,  Map map) {
        super(req, map);
    }

    /**
     * Returns a map with service provider attributes and values.
     *
     * @param realm to which the entity belongs.
     * @param fedId is the Federation Id otherwise known as the entity id.
     * @return attribute values of SP based on realm and fedId passed.
     * @throws AMConsoleException if unable to retreive the Service Provider
     *     attrubutes based on the realm and fedId passed.
     */
    public Map getServiceProviderAttributes(String realm, String fedId)
        throws AMConsoleException
    {
        Map SPAttributes = null;
        try {
            WSFederationMetaManager metaManager = getWSFederationMetaManager();
            SPSSOConfigElement spconfig =
                metaManager.getSPSSOConfig(realm,fedId);
            if (spconfig != null) {
                SPAttributes =  WSFederationMetaUtils.getAttributes(spconfig);
            }
        } catch (WSFederationMetaException e) {
            debug.warning(
                "WSFedPropertiesModelImpl.getServiceProviderAttributes", e);
            throw new AMConsoleException(getErrorString(e));
        }
        return (SPAttributes != null) ? SPAttributes : Collections.EMPTY_MAP;
    }

    /**
     * Returns a <code>Map</code> with identity provider attributes and values.
     *
     * @param realm to which the entity belongs.
     * @param fedId is the Federation Id otherwise known as the entity id.
     * @return attribute values of IDP based on realm and fedId passed.
     * @throws AMConsoleException if unable to retreive the Identity Provider
     *     attrubutes based on the realm and fedId passed.
     */
    public Map getIdentityProviderAttributes(String realm, String fedId)
        throws AMConsoleException
    {
        Map IDPAttributes = null;
        try {
            WSFederationMetaManager metaManager = getWSFederationMetaManager();
            IDPSSOConfigElement idpconfig =
                metaManager.getIDPSSOConfig(realm,fedId);
            if (idpconfig != null) {
                IDPAttributes = WSFederationMetaUtils.getAttributes(idpconfig);
            }
        } catch (WSFederationMetaException e) {
            debug.warning(
                "WSFedPropertiesModelImpl.getIdentityProviderAttributes", e);
            throw new AMConsoleException(e.getMessage());
        }
        return (IDPAttributes != null) ? IDPAttributes : Collections.EMPTY_MAP;
    }

    /**
     * Returns the <code>FederationElement</code> for the given realm and
     * federation ID.
     *
     * @param realm to which the entity belongs.
     * @param fedId is the Federation ID otherwise known as the entity id.
     * @return FederationElement Object for the realm and fedId passed.
     * @throws AMConsoleException if unable to retrieve the FederationElement
     *     Object.
     */
    public FederationElement getEntityDesc(String realm, String fedId)
        throws AMConsoleException
    {

        FederationElement fedElem = null;
        try {
            fedElem =
                getWSFederationMetaManager().getEntityDescriptor(
                    realm, fedId);
            if (fedElem == null) {
                throw new AMConsoleException("invalid.federation.element");
            }
        } catch (WSFederationMetaException e) {
            debug.warning("WSFedPropertiesModelImpl.getEntityDesc", e);
            throw new AMConsoleException(getErrorString(e));
        }
        return fedElem;
    }

    /**
     * Returns TokenIssuerName for the FederationElement passed.
     *
     * @param fedElem is the FederationElement Object.
     * @return TokenIssuerName for the FederationElement passed.
     */
    public String getTokenName(FederationElement fedElem) {
        String tkname = null;
        try {
            tkname =  getWSFederationMetaManager().getTokenIssuerName(
                fedElem);
        } catch (WSFederationMetaException we) {
            tkname = null;
        }
        return tkname;
    }

    /**
     * Returns TokenIssuerEndPoint for the FederationElement passed.
     *
     * @param fedElem is the FederationElement Object.
     * @return TokenIssuerEndPoint for the FederationElement passed.
     */
    public String getTokenEndpoint(FederationElement fedElem) {
        String tkEndpt = null;
        try {
            tkEndpt =  getWSFederationMetaManager().getTokenIssuerEndpoint(
                fedElem);
        } catch (WSFederationMetaException we) {
            tkEndpt = null;
        }
        return tkEndpt;
    }

    /**
     * Returns display name of claim type.
     *
     * @param fedElem is the FederationElement Object.
     * @return display name of claim type.
     */
    public String getClaimType(FederationElement fedElem) {
        List claimList = null;
        String displayName = null;
        UriNamedClaimTypesOfferedElement UriNamedclaimTypes = null;
        try {
            UriNamedclaimTypes = getWSFederationMetaManager().
                getUriNamedClaimTypesOffered(fedElem);
        } catch (WSFederationMetaException we) {
            UriNamedclaimTypes = null;
        }

        //assuming there is only 1 claim type object now
        if(UriNamedclaimTypes != null) {
            int iClaim = 0;
            int arr = 0;
            claimList = UriNamedclaimTypes.getClaimType();
            for(iClaim = 0; iClaim < claimList.size(); iClaim += 1) {
                ClaimType claimType = (ClaimType)claimList.get(iClaim);
                displayName = claimType.getDisplayName().getValue();
            }
        }
        return displayName;
    }

    /**
     * Saves the attribute values from the General page.
     *
     * @param realm to which the entity belongs.
     * @param fedId is the entity id.
     * @param idpStdValues has the General standard attribute value pairs.
     * @param role of the entity ID.
     * @param location specifies if the entity is remote or local.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    public void setGenAttributeValues(
        String realm,
        String fedId,
        Map idpStdValues,
        String role,
        String location
    ) throws AMConsoleException {
        String tknissEndPt = null;
        String tknissName = null;
        Iterator it = idpStdValues.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key.equals(TFTOKENISSUER_ENDPT)) {
                HashSet set = (HashSet) idpStdValues.get(key);
                Iterator i = set.iterator();
                while ((i !=  null)&& (i.hasNext())) {
                    tknissEndPt = (String) i.next();
                }
            } else if (key.equals(TFTOKENISSUER_NAME)) {
                HashSet set = (HashSet) idpStdValues.get(key);
                Iterator i = set.iterator();
                while ((i !=  null)&& (i.hasNext())) {
                    tknissName = (String) i.next();
                }
            } else if (key.equals(TF_DISPNAME)) {
                if (role.equals(EntityModel.SERVICE_PROVIDER)) {
                    HashSet set = (HashSet) idpStdValues.get(key);
                    // Get the current map of extended SP values
                    Map tmpMap = getExtendedValues(role, realm, fedId);
                    // Replace existing value
                    tmpMap.put(TF_DISPNAME, set);
                    setSPExtAttributeValues(realm, fedId, tmpMap, location);
                } else if (role.equals(EntityModel.IDENTITY_PROVIDER)) {
                    HashSet set = (HashSet) idpStdValues.get(key);
                    // Get the current map of extended IDP values
                    Map tmpMap = getExtendedValues(role, realm, fedId);
                    // Replace existing value
                    tmpMap.put(TF_DISPNAME, set);
                    setIDPExtAttributeValues(realm, fedId, tmpMap, location);
                } else if (role.equals(DUAL)) {
                    HashSet set = (HashSet) idpStdValues.get(key);
                    // Get the current map of extended SP values
                    Map tmpMap = getExtendedValues(EntityModel.SERVICE_PROVIDER, realm, fedId);
                    // Replace existing value
                    tmpMap.put(TF_DISPNAME, set);
                    setSPExtAttributeValues(realm, fedId, tmpMap, location);
                    // Get the current map of extended IDP values
                    tmpMap = getExtendedValues(EntityModel.IDENTITY_PROVIDER, realm, fedId);
                    set = (HashSet) idpStdValues.get(TFIDPDISP_NAME);
                    // Replace existing value
                    tmpMap.put(TF_DISPNAME, set);
                    setIDPExtAttributeValues(realm, fedId, tmpMap, location);
                }
            }
        }
        try {
            //fedElem is standard metadata federation element under the realm.
            WSFederationMetaManager metaManager = getWSFederationMetaManager();
            FederationElement fedElem =
                metaManager.getEntityDescriptor(realm, fedId);
            if (fedElem == null) {
                if (debug.warningEnabled()) {
                    debug.warning(
                        "WSFedPropertiesModelImpl.setGenAttributeValues:"+
                        " found invalid  federation element " + fedId);
                }
                throw new AMConsoleException("invalid.federation.element");
            } else {
                for (Iterator iter = fedElem.getAny().iterator();
                    iter.hasNext(); )
                {
                    Object o = iter.next();
                    if (o instanceof TokenIssuerEndpointElement) {
                        ((TokenIssuerEndpointElement)o).getAddress().
                            setValue(tknissEndPt);
                    } else if (o instanceof TokenIssuerNameElement) {
                        ((TokenIssuerNameElement)o).setValue(tknissName);
                    }
                }
                metaManager.setFederation(realm, fedElem);
            }

        } catch (WSFederationMetaException e) {
            debug.warning
                ("WSFedPropertiesModelImpl.setGenAttributeValues", e);
            throw new AMConsoleException(e.getMessage());
        }
    }

    /**
     * Saves the extended metadata attribute values for the SP.
     *
     * @param realm to which the entity belongs.
     * @param fedId is the entity id.
     * @param spExtvalues has the extended attribute value pairs of SP.
     * @param location has the information whether remote or hosted.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    public void setSPExtAttributeValues(
        String realm,
        String fedId,
        Map spExtvalues,
        String location
    ) throws AMConsoleException {
        try {
            String role = EntityModel.SERVICE_PROVIDER;
            //fed is the extended entity configuration object under the realm
            WSFederationMetaManager metaManager = getWSFederationMetaManager();
            FederationConfigElement fed =
                metaManager.getEntityConfig(realm,fedId);
            if (fed == null) {
                SPEX_DATA_MAP.put(TF_DISPNAME, Collections.EMPTY_SET);
                createExtendedObject(
                    realm, fedId, location, SERVICE_PROVIDER, SPEX_DATA_MAP);
                fed = metaManager.getEntityConfig(realm,fedId);
            }
            SPSSOConfigElement  spsso = getspsso(fed);
            if (spsso != null) {
                BaseConfigType baseConfig = (BaseConfigType)spsso;
                updateBaseConfig(baseConfig, spExtvalues, role);
            }
            //saves the attributes by passing the new fed object
            metaManager.setEntityConfig(realm,fed);
        } catch (JAXBException e) {
            debug.warning("WSFedPropertiesModelImpl.setSPExtAttributeValues",e);
            throw new AMConsoleException(e.getMessage());
        } catch (WSFederationMetaException e) {
            debug.warning("WSFedPropertiesModelImpl.setSPExtAttributeValues",e);
            throw new AMConsoleException(e.getMessage());
        }
    }

    /**
     * Saves the standard attribute values for the SP.
     *
     * @param realm to which the entity belongs.
     * @param fedId is the entity id.
     * @param idpExtValues has the extended attribute value pairs of IDP.
     * @param location has the information whether remote or hosted.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    public void setIDPExtAttributeValues(
        String realm,
        String fedId,
        Map idpExtValues,
        String location
    ) throws AMConsoleException {
        try {
            String role = EntityModel.IDENTITY_PROVIDER;
            // fed is the extended entity configuration under the realm
            WSFederationMetaManager metaManager = getWSFederationMetaManager();
            FederationConfigElement fed =
                    metaManager.getEntityConfig(realm,fedId);
            if (fed == null) {
                IDPEX_DATA_MAP.put(TF_DISPNAME, Collections.EMPTY_SET);
                createExtendedObject(
                   realm, fedId, location, IDENTITY_PROVIDER, IDPEX_DATA_MAP);
                fed = metaManager.getEntityConfig(realm,fedId);
            }
            IDPSSOConfigElement  idpsso = getidpsso(fed);
            if (idpsso != null){
                BaseConfigType baseConfig = (BaseConfigType)idpsso;
                updateBaseConfig(idpsso, idpExtValues, role);
            }

            //saves the new configuration by passing new fed element created
            metaManager.setEntityConfig(realm,fed);
        } catch (JAXBException e) {
            debug.warning(
                "WSFedPropertiesModelImpl.setIDPExtAttributeValues", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (WSFederationMetaException e) {
            debug.warning(
                "WSFedPropertiesModelImpl.setIDPExtAttributeValues", e);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Saves the standard attribute values for the IDP.
     *
     * @param entityName is entityid.
     * @param idpStdValues contain standard attribute values of idp.
     * @param realm to which the entity belongs.
     * @param idpExtValues contain extended attribute values.
     * @param location the information whether remote or hosted.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    public void setIDPSTDAttributeValues(
        String entityName,
        Map idpStdValues,
        String realm,
        Map idpExtValues,
        String location
    ) throws AMConsoleException {
        FederationElement fedElem =
                    getEntityDesc(realm, entityName);
        List claimList = null;
        ClaimType claimType = null;
        DisplayNameType displayName = null;
        String value = null;
        UriNamedClaimTypesOfferedElement UriNamedclaimTypes = null;
        try {
            UriNamedclaimTypes =
                getWSFederationMetaManager().getUriNamedClaimTypesOffered(
                    fedElem);
        } catch (WSFederationMetaException we) {
            UriNamedclaimTypes = null;
        }

        if(UriNamedclaimTypes != null) {
            int iClaim = 0;
            claimList = UriNamedclaimTypes.getClaimType();
            for(iClaim = 0; iClaim < claimList.size(); iClaim += 1) {
                claimType = (ClaimType)claimList.get(iClaim);
                displayName = claimType.getDisplayName();
            }
        }
        HashSet set = (HashSet) idpStdValues.get(
                WSFedPropertiesModel.TFCLAIM_TYPES);
        Iterator i = set.iterator();
        while ((i !=  null)&& (i.hasNext())) {
            value = (String) i.next();
        }
        if ((value.toString()).equals(
                WSFederationConstants.NAMED_CLAIM_DISPLAY_NAMES[
                    WSFederationConstants.NAMED_CLAIM_COMMONNAME])) {
            displayName.setValue(
                    WSFederationConstants.NAMED_CLAIM_DISPLAY_NAMES[
            WSFederationConstants.NAMED_CLAIM_COMMONNAME]);
            claimType.setUri(WSFederationConstants.NAMED_CLAIM_TYPES[
            WSFederationConstants.NAMED_CLAIM_COMMONNAME]);
        } else if (value.toString().equals(
                WSFederationConstants.NAMED_CLAIM_DISPLAY_NAMES[
                    WSFederationConstants.NAMED_CLAIM_EMAILADDRESS])) {
            displayName.setValue(
                WSFederationConstants.NAMED_CLAIM_DISPLAY_NAMES[
                    WSFederationConstants.NAMED_CLAIM_EMAILADDRESS]);
            claimType.setUri(
                    WSFederationConstants.NAMED_CLAIM_TYPES[
            WSFederationConstants.NAMED_CLAIM_EMAILADDRESS]);
        } else if (value.toString().equals(
                WSFederationConstants.NAMED_CLAIM_DISPLAY_NAMES[
            WSFederationConstants.NAMED_CLAIM_UPN])) {
            displayName.setValue(
                WSFederationConstants.NAMED_CLAIM_DISPLAY_NAMES[
            WSFederationConstants.NAMED_CLAIM_UPN]);
            claimType.setUri(WSFederationConstants.NAMED_CLAIM_TYPES[
            WSFederationConstants.NAMED_CLAIM_UPN]);
        }
        try {
            if (location.equals("hosted")) {
                String idp_certalias = getResult(idpExtValues, TFSIGNCERT_ALIAS);
                WSFederationMetaSecurityUtils.updateProviderKeyInfo(
                realm, entityName, idp_certalias, true);
            }
            getWSFederationMetaManager().setFederation(realm, fedElem);
        } catch (WSFederationMetaException e) {
            debug.warning
                    ("WSFedPropertiesModelImpl.setIDPSTDAttributeValues", e);
            throw new AMConsoleException(e.getMessage());
        }
    }

    /**
     * Retrieves the IDPSSOConfigElement .
     *
     * @param fed is the FederationConfigElement.
     * @return the corresponding IDPSSOConfigType Object.
     */
    private IDPSSOConfigElement getidpsso(FederationConfigElement fed) {
        List listFed = fed.getIDPSSOConfigOrSPSSOConfig();
        IDPSSOConfigElement idpsso = null;
        Iterator i = listFed.iterator();
        //TBD -- one config will have only one instance of
        //IDPSSOConfigElement ?????
        while (i.hasNext()) {
            BaseConfigType bc = (BaseConfigType) i.next();
            if (bc instanceof IDPSSOConfigElement) {
                idpsso = (IDPSSOConfigElement) bc;
                break;
            }
        }
        return idpsso;
    }

    /**
     * Retrieves the SPSSOConfigElement.
     *
     * @param fed is the FederationConfigElement.
     * @return the corresponding SPSSOConfigType Object.
     */
    private SPSSOConfigElement getspsso(FederationConfigElement fed) {
        List listFed = fed.getIDPSSOConfigOrSPSSOConfig();
        SPSSOConfigElement spsso = null;
        Iterator i = listFed.iterator();
        //TBD -- one config will have only one instance of
        //SPSSOConfigElement ?????
        while (i.hasNext()) {
            BaseConfigType bc = (BaseConfigType) i.next();
            if (bc instanceof SPSSOConfigElement) {
                spsso = (SPSSOConfigElement) bc;
                break;
            }
        }
        return spsso;
    }

    /**
     * Updates the BaseConfig Object with the map of values passed.
     *
     * @param baseConfig is the BaseConfigType object passed.
     * @param values the Map which contains the new attribute/value pairs.
     * @throws AMConsoleException if update of baseconfig object fails.
     */
    private void updateBaseConfig(
        BaseConfigType baseConfig,
        Map values,
        String role
    ) throws AMConsoleException {
        List attrList = baseConfig.getAttribute();
        if (role.equals(EntityModel.IDENTITY_PROVIDER)) {
            attrList.clear();
            baseConfig = createAttributeElement(getIDPEXDataMap(), baseConfig);
            attrList = baseConfig.getAttribute();
        } else if (role.equals(EntityModel.SERVICE_PROVIDER)) {
            attrList.clear();
            baseConfig = createAttributeElement(getSPEXDataMap(), baseConfig);
            attrList = baseConfig.getAttribute();
        }
        for (Iterator it = attrList.iterator(); it.hasNext(); ) {
            AttributeElement avpnew = (AttributeElement)it.next();
            String name = avpnew.getName();
            if (values.keySet().contains(name)) {
                Set set = (Set)values.get(name);
                if (set != null) {
                    avpnew.getValue().clear();
                    avpnew.getValue().addAll(set);
                }
            }
        }
    }


   /**
     * Creates the extended config object when it does not exist.
     * @param realm to which the entity belongs.
     * @param fedId is the entity id.
     * @param location is either hosted or remote
     * @param role is SP, IDP or SP/IDP.
     * @param keys which contain all extended attribute keys.
     * @throws WSFederationMetaException, JAXBException,
     *     AMConsoleException if saving of attribute value fails.
     */
    private void createExtendedObject(
        String realm,
        String fedId,
        String location,
        String role,
        Map keys
    ) throws WSFederationMetaException, JAXBException, AMConsoleException {
        try {
            ObjectFactory objFactory = new ObjectFactory();
            WSFederationMetaManager metaManager =
                getWSFederationMetaManager();
            FederationElement edes =
                metaManager.getEntityDescriptor(realm, fedId);
            if (edes == null) {
	        if (debug.warningEnabled()) {
                    debug.warning(
                        "WSFedPropertiesModelImpl.createExtendedObject: " +
		        "No such entity: " + fedId);
                }
                String[] data = {realm, fedId};
                throw new WSFederationMetaException("fedId_invalid", data);
            }
            FederationConfigElement eConfig =
                metaManager.getEntityConfig(realm, fedId);
            if (eConfig == null) {
                BaseConfigType bctype = null;
                FederationConfigElement ele =
                    objFactory.createFederationConfigElement();
                ele.setFederationID(fedId);
                if (location.equals("remote")) {
                    ele.setHosted(false);
                }
                List ll = ele.getIDPSSOConfigOrSPSSOConfig();
                // Decide which role EntityDescriptorElement includes
                // Right now, it is either an SP or an IdP or dual role
                if (isDualRole(edes)) {

                    //for dual role create both idp and sp config objects
                    BaseConfigType bctype_idp = null;
                    BaseConfigType bctype_sp = null;
                    bctype_idp = objFactory.createIDPSSOConfigElement();
                    bctype_idp = createAttributeElement(keys, bctype_idp);
                    bctype_sp = objFactory.createSPSSOConfigElement();
                    bctype_sp = createAttributeElement(keys, bctype_sp);
                    ll.add(bctype_idp);
                    ll.add(bctype_sp);
                } else if (role.equals(IDENTITY_PROVIDER)) {
                    bctype = objFactory.createIDPSSOConfigElement();
                    //bctype.getAttribute().add(atype);
                    bctype = createAttributeElement(keys, bctype);
                    ll.add(bctype);
                } else if (role.equals(SERVICE_PROVIDER)) {
                    bctype = objFactory.createSPSSOConfigElement();
                    bctype = createAttributeElement(keys, bctype);
                    ll.add(bctype);
                }
                metaManager.setEntityConfig(realm,ele);
            }
        } catch (JAXBException e) {
            debug.warning("WSFedPropertiesModelImpl.createExtendedObject", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (WSFederationMetaException e) {
            debug.warning("WSFedPropertiesModelImpl.createExtendedObject", e);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Returns BaseConfig object after updating with Attribute Elements.
     * @param values contain the keys for extended metadata attributes.
     * @param bconfig is the BaseConfigType object passed.
     *
     * @return BaseConfig object after updating with Attribute Elements.
     */
    private BaseConfigType createAttributeElement(
            Map values,
            BaseConfigType bconfig
            )throws AMConsoleException {
        try {
            ObjectFactory objFactory = new ObjectFactory();
            for (Iterator iter=values.keySet().iterator();
            iter.hasNext();) {
                AttributeElement avp = objFactory.createAttributeElement();
                String key = (String)iter.next();
                avp.setName(key);
                bconfig.getAttribute().add(avp);
            }
        } catch (JAXBException e) {
            debug.warning
                    ("WSFedPropertiesModelImpl.createAttributeElement", e);
            throw new AMConsoleException(e.getMessage());
        }
        return bconfig;
    }

    /**
     * Retrieves a count of the TokenIssuerEndpointElement
     *     which would help in determining whether dual role or not.
     * @param edes is the standard metadata object.
     *
     * @return count of the TokenIssuerEndpointElement
     */
    private boolean isDualRole(FederationElement edes) {
        int cnt = 0;
        boolean dual = false;
        if (edes != null) {
            for (Iterator iter = edes.getAny().iterator(); iter.hasNext(); ) {
                Object o = iter.next();
                if (o instanceof TokenIssuerEndpointElement) {
                    cnt++;
                }
            }
        }
        if (cnt > 1) {
            dual = true;
        }
        return dual;
    }

    /**
     * Returns a map of wsfed general attributes.
     *
     * @return Map of wsfed general attributes.
     */
    public Map getGenAttributes() {
        return GEN_DATA_MAP;
    }

    /**
     * Returns a map of wsfed general attribute values for dual role.
     *
     * @return Map of wsfed general attribute values for dual role.
     */
    public Map getDualRoleAttributes() {
        return GEN_DUAL_DATA_MAP;
    }

    /**
     * Returns a map of Wsfed Service Provider extended attributes.
     *
     * @return Map of Wsfed Service Provider extended attributes.
     */
    public Map getSPEXDataMap() {
        return SPEX_DATA_MAP;
    }

    /**
     * Returns a map of ext Wsfed Identity Provider extended attributes.
     *
     * @return Map of Wsfed Identity Provider extended attributes.
     */
    public Map getIDPEXDataMap() {
        return IDPEX_DATA_MAP;
    }

    /**
     * Returns a map of Wsfed Identity Provider Standard attributes.
     *
     * @return Map of Wsfed Identity Provider Standard attributes.
     */
    public Map getIDPSTDDataMap() {
        return IDPSTD_DATA_MAP;
    }

    /**
     * Returns value as a string corresponding to the key passed.
     *
     * @param map is a map of key/value pair.
     * @param value is the key passed.
     * @return value as a string corresponding to the key passed.
     */
    private String getResult(Map map, String value) {
        Set set = (Set)map.get(value);
        String val = null;
        if (set != null  && !set.isEmpty() ) {
            Iterator  i = set.iterator();
            while ((i !=  null) && (i.hasNext())) {
                val = (String)i.next();
            }
        }
        return val;
    }

    /**
     * Returns a map of extended attribute values based on the role, SP or IDP,
     * for the given realm and federation id.
     *
     * @param role Either EntityModel.SERVICE_PROVIDER or EntityModel.IDENTITY_PROVIDER
     * @param realm The realm for this entity
     * @param fedId The id of the entity
     * @return a Map of extended attribute values as Sets of values.
     * throws AMConsoleException for any issues collecting the values.
     */
    private Map getExtendedValues(String role, String realm, String fedId)
            throws AMConsoleException {

        Map result = new HashMap();
        Map map = null;

        if (EntityModel.SERVICE_PROVIDER.equals(role)) {
            map = getServiceProviderAttributes(realm, fedId);
        } else if (EntityModel.IDENTITY_PROVIDER.equals(role)) {
            map = getIdentityProviderAttributes(realm, fedId);
        }

        if (map != null) {
            Set entries = map.entrySet();
            Iterator iterator = entries.iterator();

            //the list of values is converted to a set
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next();
                result.put((String)entry.getKey(),
                        returnEmptySetIfValueIsNull(
                        convertListToSet((List)entry.getValue())));
            }
        }

        return result;
    }
}
