/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openam.amsessionstore.db.opendj.setup;

import org.forgerock.i18n.LocalizableMessage;
import java.io.OutputStreamWriter;
import org.forgerock.openam.amsessionstore.common.Constants;
import org.forgerock.openam.amsessionstore.common.SystemProperties;
import org.forgerock.openam.amsessionstore.db.opendj.EmbeddedOpenDJ;
import org.forgerock.openam.amsessionstore.db.opendj.OpenDJConfig;
import static org.forgerock.openam.amsessionstore.i18n.AmsessionstoreMessages.*;

/**
 *
 * @author steve
 */
public class SetupOpenDJ {
    public static void main(String[] argv) {
        if (EmbeddedOpenDJ.isInstalled()) {
            System.out.println(DB_SETUP_ALD.get());
        } else {
            try {
                SetupProgress.setWriter(new OutputStreamWriter(System.out));
                EmbeddedOpenDJ.setup(OpenDJConfig.getOdjRoot());                

                // Determine if we are a secondary install
                if (EmbeddedOpenDJ.isMultiNode()) {
                    EmbeddedOpenDJ.setupReplication(OpenDJConfig.getOpenDJSetupMap(), 
                            ExistingServerConfig.getOpenDJSetupMap(OpenDJConfig.getExistingServerUrl(), 
                                                                   SystemProperties.get(Constants.USERNAME),
                                                                   SystemProperties.get(Constants.PASSWORD)));
                    EmbeddedOpenDJ.registerServer(OpenDJConfig.getHostUrl());
                }
                
                EmbeddedOpenDJ.shutdownServer();
            } catch (Exception ex) {
                final LocalizableMessage message = DB_SETUP_FAIL.get(ex.getMessage());
                System.err.println(message);
                System.exit(Constants.EXIT_INSTALL_FAILED);
            }
        }
    }
}
