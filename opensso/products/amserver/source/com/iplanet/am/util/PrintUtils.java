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
 * $Id: PrintUtils.java,v 1.2 2008/06/25 05:41:27 qcheng Exp $
 *
 */

package com.iplanet.am.util;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PrintUtils {

    /** Each indentation will be 2 spaces wide. */
    public static final int INDENT_WIDTH = 2;

    private PrintWriter writer;

    /*
     * Creates a new PrintUtils object and sets the PrintWriter object. @param
     * writer the PrintWriter
     */
    public PrintUtils(PrintWriter writer) {
        super();
        this.writer = writer;
    }

    /**
     * Prints AV Pairs with no indentation. this method calls the toString
     * method of the objects which are in the Map.
     * 
     * @param avPairs
     *            which contains a Map of attribute value pairs that should be
     *            printed.
     */
    public void printAVPairs(Map avPairs) {
        printAVPairs(avPairs, 0);
    }

    /**
     * Prints AV Pairs with the specified indent level. Actual indentation will
     * be (indentLevel * INDENT_WIDTH). this method calls the toString method of
     * the objects which are in the Map.
     * 
     * @param avPairs
     *            which contains a Map of attribute value pairs that should be
     *            printed
     * @param indentLevel
     *            the int value which specifies the width of the indent.
     */
    public void printAVPairs(Map avPairs, int indentLevel) {
        Set set = avPairs.keySet();
        Iterator itr = set.iterator();
        Object objAttribute;
        Object objValue;

        while (itr.hasNext()) {
            objAttribute = itr.next();
            objValue = avPairs.get(objAttribute);
            printIndent(indentLevel);
            writer.println(objAttribute.toString() + " = "
                    + objValue.toString());
        }
        writer.flush();
    }

    /**
     * Prints the contents of a Set with no indentation this method calls the
     * toString method of the objects which are in the set.
     * 
     * @param set
     *            which contains a set of objects that should be printed.
     */
    public void printSet(Set set) {
        printSet(set, 0);
    }

    /**
     * Prints the contents of a Set with the specified indent level. Actual
     * indentation will be (indentLevel * INDENT_WIDTH). this method calls the
     * toString method of the objects which are in the set.
     * 
     * @param set
     *            which contains a set of objects that should be printed
     * @param indentLevel
     *            the int value which specifies the width of the indent.
     */
    public void printSet(Set set, int indentLevel) {
        Iterator itr = set.iterator();

        while (itr.hasNext()) {
            printIndent(indentLevel);
            writer.println(itr.next().toString());
        }
        writer.flush();
    }

    /**
     * This method prints the indent based on the value of the indentLevel.
     * 
     * @param indentLevel
     *            the int value which specifies the size of the indent.
     */
    public void printIndent(int indentLevel) {
        String indent = "";
        indentLevel = indentLevel * INDENT_WIDTH;

        for (int i = 0; i < indentLevel; i++) {
            indent = indent + " ";
        }
        writer.print(indent);
    }

}
