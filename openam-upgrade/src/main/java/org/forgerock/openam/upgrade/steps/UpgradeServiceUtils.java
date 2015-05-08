/*
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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.upgrade.steps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeHttpServletRequest;
import org.forgerock.openam.upgrade.UpgradeUtils;
import org.forgerock.openam.utils.IOUtils;
import org.w3c.dom.Document;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.setup.BootstrapData;
import com.sun.identity.setup.IHttpServletRequest;
import com.sun.identity.setup.JCECrypt;
import com.sun.identity.setup.ServicesDefaultValues;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * A utility class providing methods useful when dealing with SMS services in an upgrade step.
 * @since 13.0.0
 */
public class UpgradeServiceUtils {

    private static final Debug DEBUG = Debug.getInstance("amUpgrade");

    private UpgradeServiceUtils() {}

    /**
     * Loads a DOM {@code Document} for each defined service.
     * @param token The admin token.
     * @return A map of service name to the service DOM models.
     * @throws UpgradeException When the service XML cannot be loaded.
     */
    static Map<String, Document> getServiceDefinitions(SSOToken token) throws UpgradeException {
        List<String> serviceNames = new ArrayList<>();
        serviceNames.addAll(UpgradeUtils.getPropertyValues(SetupConstants.PROPERTY_FILENAME,
                SetupConstants.SERVICE_NAMES));
        Map<String, Document> newServiceDefinitions = new HashMap<>();
        String basedir = SystemProperties.get(SystemProperties.CONFIG_PATH);

        ServicesDefaultValues.setServiceConfigValues(getUpgradeHttpServletRequest(basedir, token));

        for (String serviceFileName : serviceNames) {
            boolean tagswap = true;

            if (serviceFileName.startsWith("*")) {
                serviceFileName = serviceFileName.substring(1);
                tagswap = false;
            }

            String strXML;
            try {
                strXML = IOUtils.readStream(UpgradeServiceUtils.class.getClassLoader().getResourceAsStream(serviceFileName));
            } catch (IOException ioe) {
                DEBUG.error("unable to load services file: " + serviceFileName, ioe);
                throw new UpgradeException(ioe);
            }

            if (tagswap) {
                strXML = ServicesDefaultValues.tagSwap(strXML, true);
            }

            Document serviceSchema = fetchDocumentSchema(strXML, token);

            newServiceDefinitions.put(UpgradeUtils.getServiceName(serviceSchema), serviceSchema);
        }
        return newServiceDefinitions;
    }

    private static Document fetchDocumentSchema(String xmlContent, SSOToken adminToken)
            throws UpgradeException {
        InputStream serviceStream = null;
        Document doc = null;

        try {
            serviceStream = new ByteArrayInputStream(xmlContent.getBytes());
            doc = UpgradeUtils.parseServiceFile(serviceStream, adminToken);
        } finally {
            IOUtils.closeIfNotNull(serviceStream);
        }

        return doc;
    }

    private static IHttpServletRequest getUpgradeHttpServletRequest(String basedir, SSOToken token) throws UpgradeException {
        // need to reinitialize the tag swap property map with original install params
        IHttpServletRequest requestFromFile = new UpgradeHttpServletRequest(basedir);

        try {
            Properties foo = ServerConfiguration.getServerInstance(token, WebtopNaming.getLocalServer());
            requestFromFile.addParameter(SetupConstants.CONFIG_VAR_ENCRYPTION_KEY, foo.getProperty(Constants.ENC_PWD_PROPERTY));

            String dbOption = (String) requestFromFile.getParameterMap().get(SetupConstants.CONFIG_VAR_DATA_STORE);
            boolean embedded = dbOption.equals(SetupConstants.SMS_EMBED_DATASTORE);

            if (!embedded) {
                setUserAndPassword(requestFromFile, basedir);
            }
        } catch (Exception ex) {
            DEBUG.error("Unable to initialise services defaults", ex);
            throw new UpgradeException("Unable to initialise services defaults: " + ex.getMessage());
        }

        return requestFromFile;
    }

    private static void setUserAndPassword(IHttpServletRequest requestFromFile, String basedir) throws UpgradeException {
        try {
            BootstrapData bootStrap = new BootstrapData(basedir);
            Map<String, String> data = bootStrap.getDataAsMap(0);
            requestFromFile.addParameter(SetupConstants.CONFIG_VAR_DS_MGR_DN, data.get(BootstrapData.DS_MGR));
            requestFromFile.addParameter(SetupConstants.CONFIG_VAR_DS_MGR_PWD,
                    JCECrypt.decode(data.get(BootstrapData.DS_PWD)));
        } catch (IOException ioe) {
            DEBUG.error("Unable to load directory user/password from bootstrap file", ioe);
            throw new UpgradeException("Unable to load bootstrap file: " + ioe.getMessage());
        }
    }

}
