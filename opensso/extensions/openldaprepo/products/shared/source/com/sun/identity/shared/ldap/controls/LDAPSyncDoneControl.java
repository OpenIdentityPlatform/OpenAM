/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * Portions Copyrighted 2009 Nortel
 *
 * $Id: LDAPSyncDoneControl.java,v 1.1 2009/08/10 17:35:43 superpat7 Exp $
 *
 */

package com.sun.identity.shared.ldap.controls;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.sun.identity.shared.ldap.LDAPControl;
import com.sun.identity.shared.ldap.ber.stream.BERElement;
import com.sun.identity.shared.ldap.ber.stream.BERSequence;
import com.sun.identity.shared.ldap.ber.stream.BEROctetString;
import com.sun.identity.shared.ldap.client.JDAPBERTagDecoder;

/**
 * Implements the Sync Done Control as described in RFC 4533 (LDAP Content
 * Synchronization Operation). This control is received from the LDAP server.
 * Refer to the RFC for full details.
 * <p>
 * The value of the Sync Done Control contains a BER-encoded syncDoneValue as
 * follows:
 * <pre>
 *       syncDoneValue ::= SEQUENCE {
 *           cookie          syncCookie OPTIONAL,
 *           refreshDeletes  BOOLEAN DEFAULT FALSE
 *     }
 *
 * where the contents are defined as follows:
 * 
 * cookie - typically consists of the rid number and additional information
 * that represents an existing content synchronization request along with
 * the content of the LDAP server at a particular time 
 *
 * refreshDeletes - used to indicate how the LDAP server is to address the situation
 * where content synchronization cannot be achieved - to reload the entire content
 * or send an e-syncRefreshRequired result code
 * </pre>
 * 
 * @see RFC4533
 *
 */
public class LDAPSyncDoneControl extends LDAPControl
{
   private static final long serialVersionUID = 5569669182856955580L;

   /**
    * The OID of the Sync Done Control.
    */
   public static final String OID = "1.3.6.1.4.1.4203.1.9.1.3";

   private byte [] cookie;

   private boolean refreshDeletes;

   /**
    * Constructs a SyncDoneControl.
    * @param oid the OID of the SyncDoneControl
    * @param critical indicates if this control must be supported by the
    * LDAP server
    * @param value the control value
    * @throws IOException if the control cannot be created
    */
   public LDAPSyncDoneControl( final String oid, final boolean critical,
         final byte[] value ) throws IOException
   {
      super( oid, critical, value );

      // The cookie is optional for this control and the default for
      // refreshDeletes (i.e. when not present) is false.
      if ( this.m_value == null || this.m_value.length == 0 )
      {
         this.cookie = null;
         this.refreshDeletes = false;
      }
      else
      {
         final ByteArrayInputStream inStream =
               new ByteArrayInputStream(this.m_value);
         final JDAPBERTagDecoder decoder = new JDAPBERTagDecoder();
         final int[] numBytesRead = new int[1];
         numBytesRead[0] = 0;
         final BERSequence sequence = (BERSequence)BERElement.getElement(
               decoder, inStream, numBytesRead);

         this.cookie = this.decodeCookie( sequence );
         this.refreshDeletes = this.decodeRefreshDeletes( sequence );
      }
   }

   /**
    * Get the cookie value.
    * @return the cookie value
    */
   public byte[] getCookie()
   {
      return this.cookie;
   }

   /**
    * Get the refreshDeletes value.
    * @return the refreshDeletes value
    */
   public boolean isRefreshDeletes()
   {
      return this.refreshDeletes;
   }
   
   /**
    * Decode the cookie field from the provided ASN1Sequence.
    * @param asn1Seq the ASN.1 sequence
    * @return the decoded cookie
    */
   private byte [] decodeCookie( final BERSequence sequence )
   {
      byte [] cookie = null;

      final int cookiePosition = 0;
      if ( BERElement.OCTETSTRING == sequence.elementAt(
            cookiePosition ).getType() )
      {
         final BEROctetString cookieElement =
               (BEROctetString)sequence.elementAt( cookiePosition );
         cookie = cookieElement.getValue();
      }

      return cookie;
   }
   
   /**
    * Decode the refreshDeletes field from the provided ASN1Sequence.
    * @param asn1Seq the ASN.1 sequence
    * @return the decoded refreshDeletes value
    */
   private boolean decodeRefreshDeletes( final BERSequence sequence )
   {
      boolean refreshDeletes = false;

      for ( int i = 0; i < sequence.size(); i++ )
      {
         // If there is a boolean present, then it is the refreshDeletes field
         if ( BERElement.BOOLEAN == sequence.elementAt(i).getType() )
         {
            // The LDAP specification (RFC 2251) states that when a default
            // boolean value is defined then the field must be omitted when its
            // value is equal to the default. Therefore since refreshDeletes has
            // a defined default of false, the field will only be present when
            // it has a value of true.
            refreshDeletes = true;
         }
      }
      
      return refreshDeletes;
   }
}
