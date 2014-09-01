/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information:
 *
 * "Portions copyright [year] [name of copyright owner]".
 *
 */
package com.sun.identity.sm.ldap;


import com.iplanet.dpro.session.exceptions.StoreException;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.common.LDAPUtils;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPSearchConstraints;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.shared.ldap.LDAPv2;
import com.sun.identity.shared.ldap.util.LDIF;
import org.forgerock.i18n.LocalizableMessage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.forgerock.openam.session.ha.i18n.AmsessionstoreMessages.DB_ENT_NOT_P;

/**
 * Public class to provide Additional Data Utilities for the CTS Directory Store
 * Implementation.  Such as verification of DIT for CTS storage.
 *
 * @author jeff.schenk@forgerock.com
 */
class CTSDataUtils {
    private static final String WEB_INF = "/WEB-INF";
    /**
     * Debug Logging
     */
    private static Debug DEBUG = SessionService.sessionDebug;
    /**
     * Define all our DIT entries which need to be verified and potentially
     * created for Validation of the CTS Service Component.
     * <p/>
     * This DIT hierarchy must exist for CTS component to operate
     * correctly.
     * <p/>
     * The Sequence of these should be in DIT order.
     */
    private final String[] CONTAINERS_TO_BE_VALIDATED;

    /**
     * Reference to Internally Shared SM Data Layer.
     */
    private final CTSPersistentStore ctsPersistentStore;

    /**
     * Default Constructor, provide CTSPersistent Store to use this utility.
     *
     * @param ctsPersistentStore - CTSPersistentStore Instance.
     */
    protected CTSDataUtils(CTSPersistentStore ctsPersistentStore) {
        this.ctsPersistentStore = ctsPersistentStore;
        CONTAINERS_TO_BE_VALIDATED = new String[8];
        CONTAINERS_TO_BE_VALIDATED[0] =
                CTSPersistentStore.getBASE_ROOT_DN();
        CONTAINERS_TO_BE_VALIDATED[1] =
                CTSPersistentStore.getTokenRoot();
        CONTAINERS_TO_BE_VALIDATED[2] =
                CTSPersistentStore.getTokenSessionHaRootDn();
        CONTAINERS_TO_BE_VALIDATED[3] =
                CTSPersistentStore.getTokenSaml2HaRootDn();
        CONTAINERS_TO_BE_VALIDATED[4] =
                CTSPersistentStore.getTokenOauth2HaRootDn();
        CONTAINERS_TO_BE_VALIDATED[5] =
                CTSPersistentStore.getSessionFailoverHaBaseDn();
        CONTAINERS_TO_BE_VALIDATED[6] =
                CTSPersistentStore.getSaml2HaBaseDn();
        CONTAINERS_TO_BE_VALIDATED[7] =
                CTSPersistentStore.getOauth2HaBaseDn();
    }

    /**
     * Define any necessary LDIF Files which can be used for the upgrade.
     */
    private static final String[] UPGRADE_LDIF_SCHEMA_FILENAMES = {
            // CTS Version 1 LDAP Schemata
            WEB_INF + "/template/ldif/oauth2/99-oauth2attributes.ldif",
            WEB_INF + "/template/ldif/sfha/cts-add-schema.ldif",
            // CTS Version 1 LDAP Indices
            WEB_INF + "/template/ldif/sfha/cts-indices-schema.ldif"
    };

    /**
     * Define any Template Files which require Tag Swapping prior to performing
     * the LDIF File.
     */
    private static final String[] UPGRADE_LDIF_DIT_FILENAMES = {
            // CTS Version 1 LDAP Container suffixes
            WEB_INF + "/template/ldif/sfha/cts-container.ldif",
    };

    /**
     * Provides an Indication if the DIT must be upgraded to provide a new
     * feature schema and/or structure.
     * <p/>
     * Performs Validation of our LDAP Connection and
     * the existence of our DIT hierarchy which this component requires.
     *
     * @return boolean - indicates if validation was successful or not.
     * @throws StoreException - Exception thrown if Error condition exists.
     */
    public boolean isDITValid() throws StoreException {
        // Iterate over our Token DIT Structure to Verify.
        Map<String, Boolean> validationResultMap = new HashMap<String, Boolean>(CONTAINERS_TO_BE_VALIDATED.length);
        for (String ditContainerSuffix : CONTAINERS_TO_BE_VALIDATED) {
            validationResultMap.put(ditContainerSuffix, doesDNExist(ditContainerSuffix));
        }
        // Now Iterate over the Validation Result Map to supply information, especially if we have
        // detected something missing...
        int validatedSuccessfully = 0;
        for (String ditContainerSuffix : validationResultMap.keySet()) {
            if (validationResultMap.get(ditContainerSuffix).booleanValue()) {
                validatedSuccessfully++;
            } else {
                final LocalizableMessage message = DB_ENT_NOT_P.get(ditContainerSuffix);
                DEBUG.message("CTSDataUtils.isDITValid" + message.toString());
            }
        } // End of KeySet for each loop...
        // Determine if we are good to go or not....
        return (CONTAINERS_TO_BE_VALIDATED.length == validatedSuccessfully);
    }

    /**
     * Provides method to perform the upgrade process of the DIT.
     *
     * @return
     * @throws StoreException
     */
    public boolean upgradeDIT() throws StoreException {
        // *************************************************
        // Obtain our Directory Connection
        LDAPConnection ldapConnection = null;
        LDAPException lastException = null;
        try {
            ldapConnection = ctsPersistentStore.getDirectoryConnection();
            if (ldapConnection == null) {
                DEBUG.error("Unable to obtain a Directory Connection to perform Upgrade of DIT for CTS Persistent Store.");
                return false;
            }
            // *************************************************
            // For upgrading DIT from a pre-10.1.0-Xpress State.
            // Perform LDIF processing
            for (String ldifFilename : UPGRADE_LDIF_SCHEMA_FILENAMES) {
                if (!loadLDIF(ldapConnection, AMSetupServlet.getRealPath(ldifFilename))) {
                    DEBUG.error("Unable to perform Load of LDIF Schema Filename:[" + AMSetupServlet.getRealPath(ldifFilename) + "]");
                } else {
                    DEBUG.message("Load of LDIF Schema Filename:[" + AMSetupServlet.getRealPath(ldifFilename) + "], was successful.");
                }
            } // end of for each iteration over LDIF File to be run against our current CTS Backend.
            // *************************************************
            // For upgrading DIT from a pre-10.1.0-Xpress State.
            // Perform LDIF processing for any Files which require Tag Swapping
            for (String ldifFilename : UPGRADE_LDIF_DIT_FILENAMES) {
                try {
                    StringBuffer sb = AMSetupServlet.readFile(ldifFilename);
                    // Feed our LDIF to Tag Swapped Data as Input to update DIT.
                    // Right now only one element needs to be Tagged Swapped.
                    String fileContents = sb.toString().replace("@"+SetupConstants.SM_CONFIG_ROOT_SUFFIX+"@", CTSPersistentStore.getBASE_ROOT_DN());
                    DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(fileContents.getBytes()));
                    LDIF ldif = new LDIF(dataInputStream);
                    // Load the LDIF.
                    LDAPUtils.createSchemaFromLDIF(ldif, ldapConnection);
                } catch (LDAPException ldapException) {
                    lastException = ldapException;
                    DEBUG.error("Unable to perform processing of LDIF Filename:[" + ldifFilename + "], LDAP Exception: "+
                            ldapException.getMessage()+" "+ldapException.getLDAPResultCode());
                } catch (IOException ioe) {
                    DEBUG.error("Unable to perform processing of LDIF Filename:[" + ldifFilename + "], "+ioe.getMessage());
                }
            } // end of for each iteration over LDIF File to be run against our current CTS Backend.
            // *************************************************
            // Return re-validation check of upgraded DIT.
            return isDITValid();
        } finally {
            if (ldapConnection != null) {
                ctsPersistentStore.releaseDirectoryConnection(ldapConnection, lastException);
            }
        }

    }

    /**
     * Loads the LDIF changes to the directory server.
     *
     * @param ldifFilename the name of the LDIF filename.
     * @return boolean - indicator if LDIF File was processed successfully.
     */
    private boolean loadLDIF(final LDAPConnection ldapConnection, final String ldifFilename) throws StoreException {
        String classMethod = "CTSDataUtils:loadLdif : ";
        try {
            DEBUG.message(classMethod + "OpenAM CTS Upgrade Loading LDIF Filename:" + ldifFilename);
            LDIF ldif = new LDIF(ldifFilename);
            LDAPUtils.createSchemaFromLDIF(ldif, ldapConnection);
            return true;
        } catch (IOException ioe) {
            DEBUG.error(classMethod +
                    "IO Exception, Error loading ldif: " + ldifFilename + " " + ioe.getMessage());
        } catch (LDAPException le) {
            DEBUG.error(classMethod + "Error loading ldif: " + ldifFilename, le);
        }
        // LDIF File was not successfully processed.
        return false;
    }

    /**
     * Private Helper method, Does DN Exist?
     *
     * @param dn - To be Check for Existence.
     * @return boolean - indicator, "True" if DN has been Found, otherwise "False".
     */
    private boolean doesDNExist(final String dn) throws StoreException {
        LDAPConnection ldapConnection = null;
        LDAPException lastLDAPException = null;
        try {
            // Obtain a Connection.
            ldapConnection = ctsPersistentStore.getDirectoryConnection();
            // Perform the Search.
            LDAPSearchResults searchResults = ldapConnection.search(dn,
                    LDAPv2.SCOPE_BASE, CTSPersistentStore.getAnyObjectclassFilter(), CTSPersistentStore.getReturnAttrs_DN_ONLY_ARRAY(), false, new LDAPSearchConstraints());
            if ((searchResults == null) || (!searchResults.hasMoreElements())) {
                return false;
            }
            return true;
        } catch (LDAPException ldapException) {
            lastLDAPException = ldapException;
            if (ldapException.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                return false;
            }
            DEBUG.error("CTSPersistenceStore.doesDNExist:Exception Occurred, Unable to Perform method," +
                    " Directory Error Code: " + ldapException.getLDAPResultCode() +
                    " Directory Exception: " + ldapException.getLDAPErrorMessage(), ldapException);
            return false;
        } finally {
            if (ldapConnection != null) {
                // Release the Connection.
                ctsPersistentStore.releaseDirectoryConnection(ldapConnection, lastLDAPException);
            }
        }
    }
}
