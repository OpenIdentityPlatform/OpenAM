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
 * $Id: ObjConfBase.java,v 1.2 2008/06/25 05:54:40 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.sjsws;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.FileUtils;

import java.util.Map;

/**
 * Configures SWS server instance's obj.conf
 */

public class ObjConfBase implements 
    InstallConstants, IConstants, IConfigKeys {
    
    protected boolean configureObjConf(IStateAccess stateAccess) {
        boolean status = false;
        String objFile = 
            (String)stateAccess.get(STR_KEY_SWS_OBJ_FILE);
        try {

            int line = FileUtils.getLastOccurence(
                objFile, "PathCheck fn=find-index", true, false, false);

            int line1 = FileUtils.getLastOccurence(
                objFile, "PathCheck fn=\"find-index\"", true, false, false);

            if ( line != -1 ) {
                FileUtils.insertLineByNumber(
                    objFile, line +1, "PathCheck fn=validate_session_policy");
            } else if ( line1 != -1 ) {
                FileUtils.insertLineByNumber(
                    objFile, line1 +1, "PathCheck fn=validate_session_policy");
            }

            status = true;
        } catch (Exception exc) {
            Debug.log("ObjConfBase.configureObjConf() - " +
                    "Exception occurred while adding the Agent entry from " +
                    "file '" + objFile + "'. ", exc);

        }

        return status;
    }

    protected boolean unconfigureObjConf(IStateAccess stateAccess) {
        boolean status = false;
        String objFile = 
            (String)stateAccess.get(STR_KEY_SWS_OBJ_FILE);

        try {
            int i = FileUtils.getFirstOccurence(
                objFile, "PathCheck fn=validate_session_policy",
                true, false, false);

            if (i != -1) {
                FileUtils.removeLinesByNum(objFile, i, 1);
                status = true;
            }
        } catch (Exception exc) {
            Debug.log("ObjConfBase.unconfigureObjConf() - " +
                    "Exception occurred while adding the Agent entry from " +
                    "file '" + objFile + "'. ", exc);
        }
        return status;
    }

}
