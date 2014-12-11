/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ResourceName.java,v 1.2 2008/06/25 05:43:47 qcheng Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.policy.interfaces;

import java.util.Map;
import java.util.Set;
import com.sun.identity.policy.ResourceMatch;
import com.sun.identity.policy.PolicyException;
import org.forgerock.openam.shared.resourcename.BaseResourceName;

/**
 * The interface <code>ResourceName</code> provides
 * methods to determine the hierarchy of resource names.
 * It provides methods to compare resources, get sub resources etc.
 * Also it provides an interface to determine the service
 * type to which it be used. Service developers could
 * provide an implementation of this interface that will
 * determine its hierarchy during policy evaluation and
 * also its display in the GUI. A class that implements
 * this interface must have a empty constructor.
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public interface ResourceName extends BaseResourceName<ResourceMatch, PolicyException> {

}
