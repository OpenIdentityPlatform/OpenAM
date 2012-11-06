/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.upgrade;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.Server;
import com.iplanet.services.ldap.ServerGroup;
import com.sun.identity.setup.EmbeddedOpenDS;
import com.sun.identity.setup.IHttpServletRequest;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.SMSEntry;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 *
 * @author steve
 */
public class UpgradeHttpServletRequest implements IHttpServletRequest {
    protected Locale locale;
    protected Map<String, String> parameters;
    
    public UpgradeHttpServletRequest(String baseDir) 
    throws UpgradeException {
        parameters = new HashMap<String, String>();
        initialize(baseDir);
    }
    
    private void initialize(String baseDir)
    throws UpgradeException {
        parameters.put(SetupConstants.CONFIG_VAR_DATA_STORE,
                EmbeddedOpenDS.isStarted() ? SetupConstants.SMS_EMBED_DATASTORE : SetupConstants.SMS_DS_DATASTORE);
        parameters.put(SetupConstants.CONFIG_VAR_BASE_DIR, baseDir);
        parameters.put(SetupConstants.CONFIG_VAR_SERVER_URI, getContextPath());
        parameters.put(SetupConstants.CONFIG_VAR_SERVER_URL, getServerURL());
        //workaround for ServicesDefaultValues#validatePassword
        parameters.put(SetupConstants.CONFIG_VAR_DS_MGR_PWD, "********");
        parameters.put(SetupConstants.CONFIG_VAR_ADMIN_PWD, "********");
        parameters.put(SetupConstants.CONFIG_VAR_CONFIRM_ADMIN_PWD, "********");
        parameters.put(SetupConstants.CONFIG_VAR_AMLDAPUSERPASSWD, "********!");
        parameters.put(SetupConstants.CONFIG_VAR_AMLDAPUSERPASSWD_CONFIRM, "********!");
        parameters.put(SetupConstants.CONFIG_VAR_SERVER_HOST, SystemProperties.get(Constants.AM_SERVER_HOST));
        try {
            ServerGroup sg = DSConfigMgr.getDSConfigMgr().getServerGroup("sms");
            Server server = (Server) sg.getServersList().iterator().next();
            parameters.put(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_HOST, server.getServerName());
            parameters.put(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_PORT, Integer.toString(server.getPort()));
            parameters.put(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_SSL, server.getConnectionType().toString());
        } catch (LDAPServiceException ldapse) {
            UpgradeUtils.debug.error("Unable to get SMS LDAP configuration!");
            throw new UpgradeException(ldapse);
        }
        parameters.put(SetupConstants.CONFIG_VAR_ROOT_SUFFIX, SMSEntry.getRootSuffix());
    }
    
    public Locale getLocale() {
        return locale;
    }

    public void addParameter(String parameterName, Object parameterValue) {
        parameters.put(parameterName, (String) parameterValue);
    }

    @Override
    public String getContextPath() {
        return SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
    }

    public Map getParameterMap() {
        return parameters;
    }

    public String getHeader(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private String getServerURL() {
        return SystemProperties.get(Constants.AM_SERVER_PROTOCOL) + "://" + SystemProperties.get(Constants.AM_SERVER_HOST) + ":" + SystemProperties.get(Constants.AM_SERVER_PORT) + getContextPath();
    }
}
