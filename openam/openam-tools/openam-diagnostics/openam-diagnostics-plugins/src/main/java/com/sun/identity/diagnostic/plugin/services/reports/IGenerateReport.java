/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IGenerateReport.java,v 1.1 2008/11/22 02:41:20 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.reports;

import com.sun.identity.diagnostic.base.core.common.DTException;


/**
 * Report Service interface for generating reports. 
 * All report generation are required to implement this interface.
 */
public interface IGenerateReport {
    /**
     * Report generation.
     *
     * @param path Path to the configuration directory.
     * @throws DTException if the exception occurs.
     */
    void generateReport(String path) throws DTException;
}

