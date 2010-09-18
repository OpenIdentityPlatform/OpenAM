/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1999 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s): 
 */
package com.sun.identity.shared.ldap;

import java.util.*;
import com.sun.identity.shared.ldap.util.*;
import java.io.*;

/**
 * Represents a distinguished name in LDAP.
 * <P>
 *
 * You can use objects of this class to split a distinguished name
 * (DN) into its individual components.  You can also escape the
 * characters in a DN.
 * <P>
 *
 * @version 1.0
 */
public class LDAPDN {

    /**
     * Returns the individual components of a distinguished name (DN).
     * @param dn distinguished name of which you want to get the components.
     * @param noTypes if <CODE>true</CODE>, returns only the values of the
     * components and not the names (such as 'cn=')
     * @return an array of strings representing the components of the DN.
     * @see com.sun.identity.shared.ldap.LDAPDN#explodeRDN(java.lang.String, boolean)
     */
    public static String[] explodeDN (String dn, boolean noTypes) {
        return explodeDN(new DN(dn), noTypes);
    }

    public static String[] explodeDN (DN dn, boolean noTypes) {
        return dn.explodeDN(noTypes);
    }

    /**
     * Returns the individual components of a relative distinguished name (RDN).
     * @param rdn relative distinguished name of which you want to get the components.
     * @param noTypes if <CODE>true</CODE>, returns only the values of the
     * components and not the names (such as 'cn=')
     * @return an array of strings representing the components of the RDN.
     * @see com.sun.identity.shared.ldap.LDAPDN#explodeDN(java.lang.String, boolean)
     */
    public static String[] explodeRDN (String rdn, boolean noTypes) {
        return explodeRDN(new RDN(rdn), noTypes);
    }

    public static String[] explodeRDN (RDN rdn, boolean noTypes) {
        if ( noTypes ) {
            return rdn.getValues();
        } else {
            String[] str = new String[1];
            str[0] = rdn.toString();
            return str;
        }
    }

    /**
     * Returns the RDN after escaping the characters specified
     * by <CODE>com.sun.identity.shared.ldap.util.DN.ESCAPED_CHAR</CODE>.
     * <P>
     *
     * @param rdn the RDN to escape
     * @return the RDN with the characters escaped.
     * @see com.sun.identity.shared.ldap.util.DN#ESCAPED_CHAR
     * @see com.sun.identity.shared.ldap.LDAPDN#unEscapeRDN(java.lang.String)
     */
    public static String escapeRDN(String rdn) {

        RDN name = new RDN(rdn);
        String[] val = name.getValues();
        if (val == null)
            return rdn;

        StringBuffer[] buffer = new StringBuffer[val.length];
        StringBuffer retbuf = new StringBuffer();
        String[] types = name.getTypes();

        for (int j = 0; j < val.length; j++ ) {
            buffer[j] = new StringBuffer(val[j]);

            int i=0;
            while (i<buffer[j].length()) {
                if (isEscape(buffer[j].charAt(i))) {
                    buffer[j].insert(i, '\\');
                    i++;
                }
                
                i++;
            }

            retbuf.append( ((retbuf.length() > 0) ? " + " : "") + types[j] + "=" +
                           ( new String( buffer[j] ) ) );
        }

        return new String( retbuf );
    }

    /**
     * Escape a string to be used as RDN value.
     * Incriminated characters are prepended with a \
     * @param str source string
     * @return RDN-safe string
     */
    public static String escapeValue( String str ) {
        StringBuffer retbuf = new StringBuffer();
        for( int i = 0; i < str.length(); ++i ) {
            char current_char = str.charAt( i );
            if( isEscape( current_char ) ) {
                retbuf.append( '\\' );
            }
            retbuf.append( current_char );
        }
        return retbuf.toString();
    }
    /**
     * Unescape a string from a RDN value.
     * \ prefixing escape characters are removed.
     * @param str RDN source string
     * @return original string
     */
    public static String unEscapeValue( String str ) {
        StringBuffer retbuf = new StringBuffer();
        for( int i = 0; i < str.length(); ++i ) {
            char current_char = str.charAt( i ); 
            if( '\\' ==  current_char ) {
                ++i;
                if( i == str.length() ) {
                    return "";
                }
                current_char = str.charAt( i );
            }
            retbuf.append( current_char );
        }
        return retbuf.toString();
    }
    
    /**
     * Returns the RDN after unescaping any escaped characters.
     * For a list of characters that are typically escaped in a
     * DN, see <CODE>com.sun.identity.shared.ldap.LDAPDN.ESCAPED_CHAR</CODE>.
     * <P>
     *
     * @param rdn the RDN to unescape
     * @return the unescaped RDN.
     * @see com.sun.identity.shared.ldap.util.DN#ESCAPED_CHAR
     * @see com.sun.identity.shared.ldap.LDAPDN#escapeRDN(java.lang.String)
     */
    public static String unEscapeRDN(String rdn) {
        RDN name = new RDN(rdn);
        String[] vals = name.getValues();
        if ( (vals == null) || (vals.length < 1) )
            return rdn;

        StringBuffer buffer = new StringBuffer(vals[0]);
        StringBuffer copy = new StringBuffer();
        int i=0;
        while (i<buffer.length()) {
            char c = buffer.charAt(i++);
            if (c != '\\') {
                copy.append(c);
            }
            else { // copy the escaped char following the back slash
                if (i<buffer.length()) {
                    copy.append(buffer.charAt(i++));
                }
            }
        }

        return name.getTypes()[0]+"="+(new String(copy));
    }

    /** 
     * Normalizes the dn.
     * @param dn the DN to normalize
     * @return the normalized DN
     */
    public static String normalize(String dn) {
        return normalize(new DN(dn));
    }

    public static String normalize(DN dn) {
        return dn.toString();
    }
    
    /** 
     * Compares two dn's for equality.
     * @param dn1 the first dn to compare
     * @param dn2 the second dn to compare
     * @return true if the two dn's are equal
     */
    public static boolean equals(String dn1, String dn2) {
        return normalize(dn1).equals(normalize(dn2));
    }

    private static boolean isEscape(char c) {
        for (int i=0; i<DN.ESCAPED_CHAR.length; i++)
            if (c == DN.ESCAPED_CHAR[i])
                return true;
        return false;
    }
}
