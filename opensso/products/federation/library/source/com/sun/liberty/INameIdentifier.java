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
 * $Id: INameIdentifier.java,v 1.2 2008/06/25 05:48:17 qcheng Exp $
 *
 */


package com.sun.liberty;

/**
 * This is an interface which aids in creation of <code>NameIdentifier</code>.
 * Each hosted provider can specify the implementation class 
 * to be used for the generation of the <code>NameIdentifier</code>.
 */
public interface INameIdentifier {
    /**
     * Returns a String whoes value is the <code>NameIdentifier</code>
     * @return the NameIdentifier
    */
    public String createNameIdentifier();

}
