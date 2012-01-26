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
import org.forgerock.openam.amsessionstore.common.Constants;
import org.forgerock.openam.amsessionstore.db.opendj.EmbeddedOpenDJ;
import org.forgerock.openam.amsessionstore.db.opendj.OpenDJConfig;
import static org.forgerock.openam.amsessionstore.i18n.AmsessionstoreMessages.*;

/**
 *
 * @author steve
 */
public class RemoveOpenDJ {
    public static void main(String[] argv) {
        if (EmbeddedOpenDJ.isInstalled()) {
            System.out.println(DB_SET_CON_ND.get());
            
            if (!EmbeddedOpenDJ.isStarted()) {
                try {
                    EmbeddedOpenDJ.startServer(OpenDJConfig.getOdjRoot());
                } catch (Exception ex) {
                    final LocalizableMessage message = DB_DJ_NO_START2.get(ex.getMessage());
                    System.err.println(message);
                    System.exit(Constants.EXIT_REMOVE_FAILED);
                }
            }
            
            try {
                EmbeddedOpenDJ.unregisterServer(OpenDJConfig.getHostUrl());
                EmbeddedOpenDJ.replicationDisable(OpenDJConfig.getOpenDJSetupMap());
                EmbeddedOpenDJ.shutdownServer();
            } catch (Exception ex) {
                final LocalizableMessage message = DB_DJ_SETUP_FAIL3.get(ex.getMessage());
                System.err.println(message);
                System.exit(Constants.EXIT_REMOVE_FAILED);
            }
        } else {
            System.out.println(DB_SET_CON_ND_NOT.get());
        }
    }    
}
