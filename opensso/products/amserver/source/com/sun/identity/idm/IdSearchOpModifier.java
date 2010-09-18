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
 * $Id: IdSearchOpModifier.java,v 1.3 2008/06/25 05:43:29 qcheng Exp $
 *
 */

package com.sun.identity.idm;

/**
 * This is a helper class which can be in conjunction with the
 * <code>IdSearchControl</code> class to make simple modifications to the
 * basic search performed by each plugin. The two basic modifications allowed
 * are OR and AND which are defined statically in this class.
 *
 * @supported.all.api
 */
public final class IdSearchOpModifier {
    private int sOp;

    private IdSearchOpModifier(int op) {
        sOp = op;
    }

    /**
     * The search modifier which will <code>OR</code> all the search
     * attribute-value pairs passed to <code>IdSearchControl
     * </code>
     */
    public final static IdSearchOpModifier OR = new IdSearchOpModifier(
            IdRepo.OR_MOD);

    /**
     * The search modifier which will <code>AND</code> all the search
     * attribute-value pairs passed to <code>IdSearchControl
     * </code>
     */
    public final static IdSearchOpModifier AND = new IdSearchOpModifier(
            IdRepo.AND_MOD);

    /**
     * Returns true if the object being checked is the same as this current one.
     */
    public boolean equals(Object o) {
        boolean eq = false;
        if (!(o instanceof IdSearchOpModifier)) {
            eq = false;
        } else {
            IdSearchOpModifier soperation = (IdSearchOpModifier) o;
            eq = (soperation.sOp == this.sOp);
        }
        return eq;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return ((new Integer(sOp)).hashCode());
    }

    public String toString() {
        String op = (this.sOp == IdRepo.OR_MOD) ? "OR" : "AND";
        return ("IdSearchOpModifier = " + op);
    }
}
