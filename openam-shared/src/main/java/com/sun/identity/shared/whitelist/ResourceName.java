/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ResourceName.java,v 1.1 2009/11/24 21:42:35 madan_ranganath Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.shared.whitelist;

import org.forgerock.openam.shared.resourcename.BaseResourceName;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;

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
 */
public interface ResourceName extends BaseResourceName<ResourceMatch, MalformedURLException> {

}
