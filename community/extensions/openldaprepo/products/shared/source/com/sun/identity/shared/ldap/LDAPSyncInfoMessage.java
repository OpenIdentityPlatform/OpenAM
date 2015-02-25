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
 * $Id: LDAPSyncInfoMessage.java,v 1.1 2009/08/10 17:35:39 superpat7 Exp $
 *
 */

package com.sun.identity.shared.ldap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sun.identity.shared.ldap.ber.stream.BERElement;
import com.sun.identity.shared.ldap.ber.stream.BEROctetString;
import com.sun.identity.shared.ldap.ber.stream.BERSequence;
import com.sun.identity.shared.ldap.ber.stream.BERSet;
import com.sun.identity.shared.ldap.ber.stream.BERTag;
import com.sun.identity.shared.ldap.client.JDAPBERRfc4533Decoder;
import com.sun.identity.shared.ldap.client.opers.JDAPIntermediateResponse;

/**
 * Implements a Sync Info Message.
 * The Sync Info Message is described in RFC 4533 (LDAP Content
 * Synchronization Operation). This message is received from the LDAP server
 * when the initial synchronization is complete and moves to the persist
 * state.
 * <p>
 * The value of the Sync Info Message contains a BER-encoded syncInfoValue as
 * follows:
 * <p>
 * <pre>
 * syncInfoValue ::= CHOICE {
 *     newcookie      [0] syncCookie,
 *     refreshDelete  [1] SEQUENCE {
 *         cookie         syncCookie OPTIONAL,
 *         refreshDone    BOOLEAN DEFAULT TRUE
 *     },
 *     refreshPresent [2] SEQUENCE {
 *         cookie         syncCookie OPTIONAL,
 *         refreshDone    BOOLEAN DEFAULT TRUE
 *     },
 *     syncIdSet      [3] SEQUENCE {
 *         cookie         syncCookie OPTIONAL,
 *         refreshDeletes BOOLEAN DEFAULT FALSE,
 *         syncUUIDs      SET OF syncUUID
 *     }
 * }
 * 
 * where the contents are defined as follows:
 * 
 * cookie - typically consists of the replica id number and additional
 * information that represents an existing content synchronization request
 * along with the content of the LDAP server at a particular time 
 *
 * refreshDone - indicates that the initial synchronization phase is complete
 * and is now moving to the persist phase
 *
 * refreshDeletes - used to indicate how the LDAP server is to address the
 * situation where content synchronization cannot be achieved - to reload the
 * entire content or send an e-syncRefreshRequired result code
 * </pre>
 *
 * @see RFC4533
 */
public class LDAPSyncInfoMessage extends LDAPIntermediateResponse
{
   private static final long serialVersionUID = 1206357687316896368L;

   public static final String OID = "1.3.6.1.4.1.4203.1.9.1.4";
   
   // message choice types
   public static final int CHOICE_NEW_COOKIE = 0;
   public static final int CHOICE_REFRESH_DELETE = 1;
   public static final int CHOICE_REFRESH_PRESENT = 2;
   public static final int CHOICE_SYNC_ID_SET = 3;
   
   private int choice;
   private byte[] cookie;
   private boolean refreshDone;
   private boolean refreshDeletes;
   private List syncUuids;

   /**
    * Constructs an LDAPSyncInfoMessage.
    * @param msgid
    * @param rsp
    * @param controls
    * @throws IOException
    */
   public LDAPSyncInfoMessage( int msgid, JDAPIntermediateResponse rsp,
         LDAPControl controls[] ) throws IOException
   {
      super( msgid, rsp, controls );

      this.choice = this.decodeChoiceSelection();

      switch ( this.choice )
      {
         case CHOICE_NEW_COOKIE:
         {
            // the choice value is the cookie
            BEROctetString choiceValue = (BEROctetString)this.decodeChoiceValue();
            this.cookie = choiceValue.getValue();
            break;
         }
         case CHOICE_REFRESH_DELETE:
         case CHOICE_REFRESH_PRESENT:
         {
            final BERSequence sequence = (BERSequence)this.decodeChoiceValue();
            this.cookie = this.decodeCookie( sequence );
            this.refreshDone = this.decodeRefreshDone( sequence );
            break;
         }
         case CHOICE_SYNC_ID_SET:
         {
            final BERSequence sequence = (BERSequence)this.decodeChoiceValue();
            this.cookie = this.decodeCookie( sequence );
            this.refreshDeletes = this.decodeRefreshDeletes( sequence );
            this.syncUuids = this.decodeSyncUuids( sequence );
            break;
         }
         default:
         {
            throw new IllegalStateException(
                  "Illegal value for the choice: " + this.choice );
         }
      }
   }

   /**
    * Get the cookie value.
    * @return the cookie
    */
   public byte[] getCookie()
   {
      return this.cookie;
   }

   /**
    * Get the refreshDeletes value. 
    * @return the value of refreshDeletes
    */
   public boolean getRefreshDeletes()
   {
      return this.refreshDeletes;
   }
   
   /**
    * Get the refreshDone value. 
    * @return the value of refreshDone
    */
   public boolean getRefreshDone()
   {
      return this.refreshDone;
   }

   /**
    * Get the syncUuids.
    * @return the list of syncuUids
    */
   public List getSyncUuids()
   {
      return this.syncUuids;
   }
  
   /*
    * Decodes the choice content from the message content.
    */
   private BERTag decodeChoiceContent() throws IOException
   {
      final ByteArrayInputStream inStream =
            new ByteArrayInputStream(this.getValue());
      final JDAPBERRfc4533Decoder decoder = new JDAPBERRfc4533Decoder();
      final int[] numBytesRead = new int[1];
      numBytesRead[0] = 0;
      return (BERTag)BERElement.getElement(
            decoder, inStream, numBytesRead);
   }
   
   /*
    * Decodes the choice value from the message content.
    */
   private BERElement decodeChoiceValue() throws IOException
   {
      final BERTag content = this.decodeChoiceContent();
      return content.getValue();
   }
   
   /*
    * Decodes the choice selection from the message.
    */
   private int decodeChoiceSelection() throws IOException
   {
      final byte[] messageValue = this.getValue();
      final int choiceTag = messageValue[0] & 0xff;
      int choice = 0;
      switch(choiceTag)
      {
         // CHOICE [0]
         case 0xa0:
         {
            choice = 0;
            break;
         }
         // CHOICE [1]
         case 0xa1:
         {
            choice = 1;
            break;
         }
         // CHOICE [2]
         case 0xa2:
         {
            choice = 2;
            break;
         }
         // CHOICE [3]
         case 0xa3:
         {
            choice = 3;
            break;
         }
         default:
         {
            throw new IOException("Illegal CHOICE tag: " + choiceTag);
         }
      }
      return choice;
   }

   /*
    * Decode the cookie field from the sequence.
    *
    * This method requires that the sequence is associated with either
    * the refreshDelete, refreshPresent, or syncIdSet choice.
    */
   private byte [] decodeCookie( final BERSequence sequence )
   {
      byte [] cookie = null;
      
      final int cookiePosition = 0;
      final BERElement element = sequence.elementAt( cookiePosition );
      if ( BERElement.OCTETSTRING == element.getType() )
      {
         cookie = ((BEROctetString)element).getValue();
      }

      return cookie;
   }
   
   /*
    * Decode the refreshDone field from the sequence.
    *
    * This method requires that the sequence is associated with either
    * the refreshDelete or refreshPresent choice.
    */
   private boolean decodeRefreshDone( final BERSequence sequence )
   {
      boolean refreshDone = true;

      for ( int i = 0; i < sequence.size(); i++ )
      {
         // If there is a boolean present, then it is the refreshDone field
         if ( BERElement.BOOLEAN == sequence.elementAt(i).getType() )
         {
            // The LDAP specification (RFC 2251) states that when a default
            // boolean value is defined then the field must be omitted when
            // its value is equal to the default. Therefore since refreshDone
            // has a defined default of true, the field will only be present
            // when it has a value of false.
            refreshDone = false;
         }
      }
      
      return refreshDone;
   }
   
   /*
    * Decode the refreshDeletes field from the sequence.
    *
    * This method requires that the sequence is associated with the
    * syncIdSet choice.
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
            // boolean value is defined then the field must be omitted
            // when its value is equal to the default. Therefore since
            // refreshDeletes has a defined default of false, the field will
            // only be present when it has a value of true.
            refreshDeletes = true;
         }
      }
      
      return refreshDeletes;
   }
   
   /*
    * Decode the list of syncUuids from the sequence.
    *
    * This method requires that the sequence is associated with the
    * syncIdSet choice.
    */
   private List decodeSyncUuids( final BERSequence sequence )
   {
      List syncUuids = null;

      for ( int i = 0; i < sequence.size(); i++ )
      {
         // The "set of" is the syncUuids field
         if ( BERElement.SET == sequence.elementAt(i).getType() )
         {
            final BERSet set = (BERSet)(sequence.elementAt(i));
            syncUuids = this.extractElements(set);
         }
      }
      
      return syncUuids;
   }
   
   /*
    * Extract the elements from the BERSet.
    */
   private List extractElements(final BERSet set)
   {
      final List elements = new ArrayList();
      for ( int i = 0; i < set.size(); i++ )
      {
         elements.add(set.elementAt(i));
      }
      
      return elements;
   }
}
