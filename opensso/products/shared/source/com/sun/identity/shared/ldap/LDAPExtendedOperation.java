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

/**
 * Version 3 of the LDAP protocol include the means to define additional
 * operations (&quot;extended operations&quot;) beyond the standard LDAP
 * operations. An LDAP v3 client can send an extended operation request,
 * identifying the operation by its unique object ID (OID).  The server
 * receives the request and if OID corresponds to an operation supported
 * by the server, the server procoess the request and sends an extended
 * operation response back to the client.
 * <P>
 *
 * Objects of this class can be used to represent extended operation
 * requests (sent by your client) or extended operation responses
 * (returned by an LDAP v3 server).
 * <P>
 *
 * To determine which extended operations are supported by a server,
 * you need to search for the root DSE (DSA-specific entry, where DSA is
 * another term for &quot;LDAP server&quot;) and find the values of the
 * <CODE>supportedExtension</CODE> attribute.  This attribute contains the
 * object IDs (OIDs) of the extended operations supported by this server.
 * <P>
 *
 * The following section of code demonstrates how to get the list
 * of the extended operations supported by an LDAP server.
 * <P>
 *
 * <PRE>
 * public static void main( String[] args )
 * {
 *   LDAPConnection ld = new LDAPConnection();
 *   try {
 *     String MY_HOST = "localhost";
 *     int MY_PORT = 389;
 *     ld.connect( MY_HOST, MY_PORT );
 *     try {
 *       ld.authenticate( 3, "cn=myApp,ou=Directory Apps", "23skidoo" );
 *     } catch( LDAPException e ) {
 *       System.out.println( "LDAP server does not support v3." );
 *       ld.disconnect();
 *       System.exit(1);
 *     }
 *
 *     String MY_FILT = "(objectclass=*)";
 *     String MY_BASE = "";
 *     String getAttrs[] = { "supportedExtension" };
 *     LDAPSearchResults res = ld.search( MY_BASE,
 *       LDAPConnection.SCOPE_BASE, MY_FILT, getAttrs, false );
 *
 *     while ( res.hasMoreElements() ) {
 *       LDAPEntry findEntry = (LDAPEntry)res.nextElement();
 *       LDAPAttributeSet findAttrs = findEntry.getAttributeSet();
 *       Enumeration enumAttrs = findAttrs.getAttributes();
 *
 *         while ( enumAttrs.hasMoreElements() ) {
 *           LDAPAttribute anAttr = (LDAPAttribute)enumAttrs.nextElement();
 *           String attrName = anAttr.getName();
 *           System.out.println( attrName );
 *           Enumeration enumVals = anAttr.getStringValues();
 *
 *           while ( enumVals.hasMoreElements() ) {
 *             String aVal = ( String )enumVals.nextElement();
 *             System.out.println( "\t" + aVal );
 *           }
 *         }
 *      }
 *   }
 *   catch( LDAPException e ) {
 *     System.out.println( "Error: " + e.toString() );
 *   }
 *   try {
 *     ld.disconnect();
 *   }
 *   catch( LDAPException e ) {
 *     System.exit(1);
 *   }
 *   System.exit(0);
 * }
 * </PRE>
 * <P>
 *
 * If you compile and run this example against an LDAP server that
 * supports v3 of the protocol, you might receive the following results:
 * <P>
 *
 * <PRE>
 * supportedextension
 *   1.3.6.1.4.1.42.2.27.9.6.1
 *   1.3.6.1.4.1.42.2.27.9.6.11
 *   1.3.6.1.4.1.42.2.27.9.6.12
 *   1.3.6.1.4.1.42.2.27.9.6.13
 *   1.3.6.1.4.1.42.2.27.9.6.14
 *   1.3.6.1.4.1.42.2.27.9.6.15
 *   1.3.6.1.4.1.42.2.27.9.6.16
 *   1.3.6.1.4.1.42.2.27.9.6.17
 *   1.3.6.1.4.1.42.2.27.9.6.18
 *   1.3.6.1.4.1.42.2.27.9.6.19
 *   1.3.6.1.4.1.42.2.27.9.6.2
 *   1.3.6.1.4.1.42.2.27.9.6.21
 *   1.3.6.1.4.1.42.2.27.9.6.22
 *   1.3.6.1.4.1.42.2.27.9.6.3
 *   1.3.6.1.4.1.42.2.27.9.6.4
 *   1.3.6.1.4.1.42.2.27.9.6.5
 *   1.3.6.1.4.1.42.2.27.9.6.6
 *   1.3.6.1.4.1.42.2.27.9.6.7
 *   1.3.6.1.4.1.42.2.27.9.6.8
 *   1.3.6.1.4.1.42.2.27.9.6.9
 *   1.3.6.1.4.1.4203.1.11.3
 *   2.16.840.1.113730.3.5.3
 *   2.16.840.1.113730.3.5.4
 *   2.16.840.1.113730.3.5.5
 *   2.16.840.1.113730.3.5.6
 *   2.16.840.1.113730.3.5.7
 *   2.16.840.1.113730.3.5.8
 * </PRE>
 * <P>
 *
 * For more information on extended operations, see
 * <A HREF="http://www.ietf.org/rfc/rfc2251.txt"
 * TARGET="_blank">RFC 2251</A>.
 * <P>
 *
 * @version 1.0
 * @see com.sun.identity.shared.ldap.LDAPConnection#extendedOperation(com.sun.identity.shared.ldap.LDAPExtendedOperation)
 * @see com.sun.identity.shared.ldap.LDAPConnection#search(java.lang.String, int, java.lang.String, java.lang.String[], boolean)
 *
 */
public class LDAPExtendedOperation implements java.io.Serializable {

    static final long serialVersionUID = 4010382829133611945L;

    /**
     * Construct an object
     * @param oid identifier for the particular operation
     * @param vals operation-specific data
     */
    public LDAPExtendedOperation( String oid, byte[] vals ) {
        m_oid = oid;
        m_vals = vals;
    }

    /**
     * Get the identifier for this operation.
     * @return oid identifier for the particular operation.
     */
    public String getID() {
        return m_oid;
    }

    /**
     * Get the data for this operation.
     * @return vals operation-specific data.
     */
    public byte[] getValue() {
        return m_vals;
    }

    private String m_oid;
    private byte[] m_vals;
}
