/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: GenericNode.java,v 1.2 2008/06/25 05:41:28 qcheng Exp $
 *
 */

package com.iplanet.dpro.parser;

import java.util.Hashtable;
import java.util.Vector;

/**
 * A generic implementation of <code>ParseOutput</code>.
 */
public class GenericNode implements ParseOutput {
    /**
     * Name of request.
     */
    public String _name;

    /**
     * Vector that has parsing elements.
     */
    public Vector _elems;

    /**
     * Parsing attributes.
     */
    public Hashtable _atts;

    /**
     * Data to be parsed.
     */
    public String _pcdata; 

    /**
     * Processes request to information key. This is called
     * by the SAX parser.
     * @param name name of request.
     * @param elems vector that has parsing elements.
     * @param atts parsing attributes.
     * @param pcdata given data to be parsed..
     */
    public void process(
        String name,
        Vector elems,
        Hashtable atts,
        String pcdata
    ) {
        _name = name;
        _elems = elems;
        _atts = atts;
        _pcdata = pcdata;
    }

    public String toString() {
        return " Name=" + _name +
            " Attrs=" + _atts.toString() +
            " Elems=" + _elems.toString() +
            " PCDATA=" + _pcdata;
    }
}
