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
 * $Id: Marshaller.java,v 1.2 2008/06/25 05:43:34 qcheng Exp $
 *
 */

package com.sun.identity.idsvcs.rest;

import java.io.Writer;

/**
 * Generic interface to support streaming of output through the servlet.
 */
public interface Marshaller {
    /**
     * Write out an object to the writer. Its basic 'Serialization' for the
     * various format that the SecurityHandler now supports.
     * 
     * @param wrt the object is written to this.
     * @param value object to write to the writer provided.
     * @throws Exception whenever thers a problem.
     */
    void marshall(Writer wrt, Object value) throws Exception;
}
