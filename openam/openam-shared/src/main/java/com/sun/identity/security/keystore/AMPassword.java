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
 * $Id: AMPassword.java,v 1.2 2008/06/25 05:52:58 qcheng Exp $
 *
 */


package com.sun.identity.security.keystore;

import java.io.UnsupportedEncodingException;

/**
 * Stores a password.  <code>clear</code> should be
 * called when the password is no longer needed so that the sensitive
 * information is not left in memory.
 * <p>A <code>AMPassword</code> can be used as a hard-coded
 * <code>AMCallbackHandler</code>.
 * @see AMCallbackHandler
 */
public class AMPassword extends AMCallbackHandler implements  Cloneable,
        java.io.Serializable
    {
    /**
     * Don't use this if you aren't Password.
     */
    private AMPassword() {
        cleared = true;
    }

    /**
     * Creates a Password from a char array, then wipes the char array.
     * @param pw A char[] containing the password.  This array will be
     *      cleared (set to zeroes) by the constructor.
     */
    public AMPassword(char[] pw) {
        int i;
        int length = pw.length;

        cleared = false;

        password = new char[length];
        System.arraycopy(pw, 0, password, 0, length);
    }

    /**
     * Clones the password.  The resulting clone will be completely independent
     * of the parent, which means it will have to be separately cleared.
     * @return the cloned AMPassword
     */
    public synchronized Object clone() {
        AMPassword dolly = new AMPassword();

        dolly.password = (char[]) password.clone();
        dolly.cleared = cleared;
        return dolly;
    }
    
    /**
     * Returns the char array underlying this password. It must not be
     * modified in any way.
     * @return password in char array
     */
    public char[] getChars() {
        return password;
    }

    /**
     * Returns a null-terminated byte array that is the byte-encoding of
     * this password.
     * The returned array is a copy of the password.
     * The caller is responsible for wiping the returned array,
     * for example using <code>wipeChars</code>.
     * @return the copy of password in byte array
     */
    public byte[] getByteCopy() {
        return charToByte( (char[])password.clone() );
    }

    /**
     * Converts a char array to a null-terminated byte array using a standard
     * encoding, which is currently UTF8. The caller is responsible for
     * clearing the copy (with <code>wipeBytes</code>, for example).
     *
     * @param charArray A character array, which should not be null. It will
     *        be wiped with zeroes.
     * @return A copy of the charArray, converted from Unicode to UTF8. It
     *         is the responsibility of the caller to clear the output byte
     *         array; *	<code>wipeBytes</code> is ideal for this purpose.
     * @see org.mozilla.jss.util.Password#wipeBytes
     */
    public static byte[] charToByte(char[] charArray) {
	byte bytearray[] = null;
	try {
	    if (charArray != null) {
	        bytearray = (byte[]) new String(charArray).getBytes("UTF-8");
	    }
	} catch (UnsupportedEncodingException e) {}
	
	return bytearray;
    }
}
