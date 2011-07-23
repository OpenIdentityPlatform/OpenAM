
/*
  * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  *
  * - Redistributions of source code must retain the above copyright
  *   notice, this list of conditions and the following disclaimer.
  *
  * - Redistribution in binary form must reproduce the above copyright
  *   notice, this list of conditions and the following disclaimer in
  *   the documentation and/or other materials provided with the
  *   distribution.
  *
  * Neither the name of Sun Microsystems, Inc. or the names of
  * contributors may be used to endorse or promote products derived
  * from this software without specific prior written permission.
  *
  * This software is provided "AS IS," without a warranty of any
  * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
  * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
  * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
  * DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
  * DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN
  * OR ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA,
  * OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
  * PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF
  * LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE,
  * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
  *
  * You acknowledge that Software is not designed, licensed or
  * intended for use in the design, construction, operation or
  * maintenance of any nuclear facility.
  */ 

/*
 * FortressModulePrincipal.java
 *
 * Created on March 27, 2007, 10:34 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.aspace.ftress.authentication.modules.ftressOTP;


import java.io.*;
import java.security.Principal;
/**
 *
 * @author mc116350
 */
public class FortressModulePrincipal implements Principal, Serializable{
    
    /** Creates a new instance of FortressModulePrincipal */
    public FortressModulePrincipal() {
    }
    
        /**
     * @serial
     */
    private String name;
    
    public FortressModulePrincipal(String name) {
	if (name == null)
	    throw new NullPointerException("illegal null input");

	this.name = name;
    }

    /**
     * Return the principal username for this 
     * <code>FortressModulePrincipal</code>.
     * <p>
     *
     * @return the principal username for this 
     * <code>FortressModulePrincipal</code>
     */
    public String getName() {
	return name;
    }

    /**
     * Return a string representation of 
     * this <code>FortressModulePrincipal</code>.
     * <p>
     * @return a string representation of 
     * this <code>FortressModulePrincipal</code>.
     */
    public String toString() {
	return("FortressModulePrincipal:  " + name);
    }

    /**
     * Compares the specified Object with this 
     * <code>FortressModulePrincipal</code> for equality.  
     * Returns true if the given object is also a
     * <code>FortressModulePrincipal</code> and the two FortressModulePrincipal
     * have the same username.
     *
     * <p>
     *
     * @param o Object to be compared for equality with this
     *		<code>FortressModulePrincipal</code>.
     *
     * @return true if the specified Object is equal equal to this
     *		<code>FortressModulePrincipal</code>.
     */
    public boolean equals(Object o) {
	if (o == null)
	    return false;

        if (this == o)
            return true;
 
        if (!(o instanceof FortressModulePrincipal))
            return false;
        FortressModulePrincipal that = (FortressModulePrincipal)o;

	if (this.getName().equals(that.getName()))
	    return true;
	return false;
    }
 
    
    
    /**
     * Return a hash code for this <code>FortressModulePrincipal</code>.
     *
     * <p>
     *
     * @return a hash code for this <code>FortressModulePrincipal</code>.
     */
    public int hashCode() {
	return name.hashCode();
    }
    
    
}
