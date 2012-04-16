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
 * $Id: LDAPSyncStateControl.java,v 1.1 2009/08/10 17:35:44 superpat7 Exp $
 *
 */

package com.sun.identity.shared.ldap.controls;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.sun.identity.shared.ldap.LDAPControl;
import com.sun.identity.shared.ldap.ber.stream.BERElement;
import com.sun.identity.shared.ldap.ber.stream.BEREnumerated;
import com.sun.identity.shared.ldap.ber.stream.BEROctetString;
import com.sun.identity.shared.ldap.ber.stream.BERSequence;
import com.sun.identity.shared.ldap.client.JDAPBERTagDecoder;

/**
 * Implements the Sync State Control as described in RFC 4533 (LDAP Content
 * Synchronization Operation). This control is received from the LDAP server.
 * Refer to the RFC for full details.
 * <p>
 * The value of the Sync State Control contains a BER-encoded syncStateValue as
 * follows:
 * <pre>
 *    syncStateValue ::= SEQUENCE {
 *        state ENUMERATED {
 *            present (0),
 *            add (1),
 *            modify (2),
 *            delete (3)
 *        },
 *        entryUUID syncUUID,
 *        cookie    syncCookie OPTIONAL
 *    }
 *
 * where the contents are defined as follows:
 *
 * state - indicates the operation that was performed
 *
 * entryUuid - a unique identifier representing the LDAP entry 
 *
 * cookie - typically consists of the rid number and additional information
 * that represents an existing content synchronization request along with
 * the content of the LDAP server at a particular time 
 * </pre>
 *
 * @see RFC4533
 *
 */
public class LDAPSyncStateControl extends LDAPControl
{
   private static final long serialVersionUID = -7342878441071369476L;

   /**
    * The OID of the Sync State Control.
    */
   public static final String OID = "1.3.6.1.4.1.4203.1.9.1.2";

   // state types
   public static final int STATE_PRESENT = 0;
   public static final int STATE_ADD = 1;
   public static final int STATE_MODIFY = 2;
   public static final int STATE_DELETE = 3;
   
   private int state;
   private byte [] entryUuid;
   private byte [] cookie;

   private static final int STATE_ELEMENT_POSITION = 0;
   private static final int ENTRY_UUID_ELEMENT_POSITION = 1;
   private static final int COOKIE_ELEMENT_POSITION = 2;

   /**
    * Constructs a SyncStateControl.
    * @param oid the OID of the Sync State Control
    * @param critical indicates if this control must be supported by the
    * LDAP server
    * @param value the control value
    * @throws IOException if the control cannot be created
    */
   public LDAPSyncStateControl( final String oid, final boolean critical,
         final byte[] value ) throws IOException
   {
      super( oid, critical, value );

      final ByteArrayInputStream inStream = new ByteArrayInputStream(this.m_value);
      final JDAPBERTagDecoder decoder = new JDAPBERTagDecoder();
      final int[] numBytesRead = new int[1];
      numBytesRead[0] = 0;

      final BERSequence sequence = (BERSequence)BERElement.getElement(
            decoder, inStream, numBytesRead);

      this.state = this.decodeState( sequence );
      this.entryUuid = this.decodeEntryUuid( sequence );
      this.cookie = this.decodeCookie( sequence );
   }

   /**
    * Returns the cookie value.
    * @return the cookie value
    */
   public byte[] getCookie()
   {
      return this.cookie;
   }

   /**
    * Returns the entryUuid value.
    * @return the entryUuid value
    */
   public byte[] getEntryUuid()
   {
      return this.entryUuid;
   }

   /**
    * Returns the state value.
    * @return the state value
    */
   public int getState()
   {
      return this.state;
   }

   /*
    * Decodes the state field from the supplied sequence.
    */
   private int decodeState( final BERSequence sequence )
   {
      final BEREnumerated stateElement =
               (BEREnumerated) sequence.elementAt( STATE_ELEMENT_POSITION );

      final int state = stateElement.getValue();

      switch(state)
      {
         case STATE_PRESENT:
         case STATE_ADD:
         case STATE_MODIFY:
         case STATE_DELETE:
         {
            // legal value for the state
            break;
         }
         default:
         {
            throw new IllegalStateException( "Illegal state value: " + state );            
         }
      }
      
      return state;
   }

   /*
    * Decodes the entryUuid field from the supplied sequence.
    */
   private byte [] decodeEntryUuid( final BERSequence sequence )
   {
      final BEROctetString entryUuidElement =
            (BEROctetString)sequence.elementAt( ENTRY_UUID_ELEMENT_POSITION );
      return entryUuidElement.getValue();
   }

   /*
    * Decodes the cookie field from the supplied sequence.
    * Returns null if no cookie is present.
    */
   private byte [] decodeCookie( final BERSequence sequence )
   {
      byte [] cookie = null;

      if ( this.isCookiePresent( sequence ) )
      {
         final BEROctetString cookieElement =
               (BEROctetString)sequence.elementAt( COOKIE_ELEMENT_POSITION );
         cookie = cookieElement.getValue();
      }

      return cookie;
   }

   /*
    * Determines if a cookie is present in the supplied sequence.
    */
   private boolean isCookiePresent( final BERSequence sequence )
   {
      // The cookie element - when present - is the last element 
      return ( sequence.size() == ( COOKIE_ELEMENT_POSITION + 1 ) );
   }
}
