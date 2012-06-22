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
 * $Id: SMDiscoveryServiceModel.java,v 1.2 2008/06/25 05:49:46 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.sun.identity.console.base.model.AMServiceProfileModel;
import java.util.Set;

/* - NEED NOT LOG - */

public interface SMDiscoveryServiceModel
    extends AMServiceProfileModel
{
    /**
     * Returns resource offering entry stored in the model map for a given
     * type.
     *
     * @param dynamic value to indicate if it is a dynamic or not.
     * @return resource offering entry stored in the model map for a given
     * type.
     */
    Set getDiscoEntry(boolean dynamic);

    /**
     * Returns provider resource ID mapper attribute value.
     *
     * @return provider resource ID mapper attribute value.
     */
    Set getProviderResourceIdMapper();
}
