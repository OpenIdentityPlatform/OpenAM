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
 * $Id: GenericNode.java,v 1.3 2008/06/25 05:41:41 qcheng Exp $
 *
 */

package com.iplanet.services.util;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Generic node is the default node type if the client does not implement
 * ParseOutput.
 */
public class GenericNode implements ParseOutput {
    /**
     * Name of this node
     */
    public String _name;

    /**
     * Sub-node of this node
     */
    public Vector _elems;

    /**
     * Attribute values of this node
     */
    public Hashtable _atts;

    /**
     * Text value of this node
     */
    public String _pcdata;

    /**
     * Store the node information into this object.
     *
     * @param parser XML Parser instance
     * 
     * @param name
     *            the name of this node.
     * @param elems
     *            contains all the sub-nodes.
     * @param atts
     *            contains the attributes value of this node
     * @param pcdata
     *            contains text value of this node
     * @see ParseOutput
     */
    public void process(
        XMLParser parser,
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

    /**
     * Returns content of this node by String type.
     */
    public String toString() {
        return " Name=" + _name + " Attrs=" + _atts.toString() + " Elems="
                + _elems.toString() + " PCDATA=" + _pcdata;
    }
}
