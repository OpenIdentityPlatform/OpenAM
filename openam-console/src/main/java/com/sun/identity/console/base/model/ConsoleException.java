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
 * $Id: ConsoleException.java,v 1.3 2008/06/25 05:42:50 qcheng Exp $
 *
 */

package com.sun.identity.console.base.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This exception is thrown to signal to the view bean
 * that incorrect behavior is encountered in processing a request.
 */
public class ConsoleException extends Exception {
    private List errList = null;
 
    /**
     * Creates a Console Exception object.
     *
     * @param msg exception message.
     */
    public ConsoleException(String msg) {
        super(msg);
        errList = new ArrayList(1);
        errList.add(msg);
    }
 
    /**
     * Creates a Console Exception object.
     *
     * @param errors list of error messages.
     */
    public ConsoleException(List errors) {
        super(errors.toArray().toString());
        errList = errors;
    }
 
    /**
     * Creates a Console Exception object.
     *
     * @param t <code>Throwable</code> instance.
     */
    public ConsoleException(Throwable t) {
        super(t.getMessage());
        errList = new ArrayList(1);
        errList.add(t.getMessage());
    }
 
    /**
     * Returns error list set in the current exception.
     *
     * @return error list.
     */
    public List getErrors() {
        return (errList == null) ? Collections.EMPTY_LIST : errList;
    }
}
