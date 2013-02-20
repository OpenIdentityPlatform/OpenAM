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
 * $Id: FormatterInitException.java,v 1.3 2008/06/25 05:43:36 qcheng Exp $
 *
 */



package com.sun.identity.log.handlers;

/**
 * Formatter may encounter Exceptions while processing the allField list or
 * the selectedField List. Specifically when a particular field is selected
 * and the corresponding value is either not specified or errorenous, this
 * Exception may be thrown. <p>
 * FormatterExcpeption assumes that the incoming string is a localized string.
 */
public class FormatterInitException extends com.iplanet.log.LogException {
    
    /**
     * Creates new FormatterInitalizationException
     */
    public FormatterInitException() {
        super();
    }
    /**
     * Create new FormatterException with the given string
     * @param message The Message comment string which explains the Exception.
     */
    public FormatterInitException(String message) {
        super(message);
    }
}
