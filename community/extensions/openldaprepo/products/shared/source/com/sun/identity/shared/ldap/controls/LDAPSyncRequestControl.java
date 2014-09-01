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
 * $Id: LDAPSyncRequestControl.java,v 1.1 2009/08/10 17:35:44 superpat7 Exp $
 *
 */

package com.sun.identity.shared.ldap.controls;

import java.io.IOException;
import com.sun.identity.shared.ldap.ber.stream.BERBoolean;
import com.sun.identity.shared.ldap.ber.stream.BEREnumerated;
import com.sun.identity.shared.ldap.ber.stream.BEROctetString;
import com.sun.identity.shared.ldap.ber.stream.BERSequence;
import com.sun.identity.shared.ldap.LDAPControl;


/**
 * Implements the Sync Request Control as described in RFC 4533 (LDAP Content
 * Synchronization Operation). Refer to the RFC for full details.
 * <p>
 * The value of the Sync Info Message contains a BER-encoded syncInfoValue as
 * follows:
 * <pre>
 *    syncRequestValue ::= SEQUENCE {
 *        mode ENUMERATED {
 *            -- 0 unused
 *            refreshOnly       (1),
 *            -- 2 reserved
 *            refreshAndPersist (3)
 *        },
 *        cookie     syncCookie OPTIONAL,
 *        reloadHint BOOLEAN DEFAULT FALSE
 *    }
 * 
 * where the contents are defined as follows:
 *
 * mode - defines the synchronization mode of operation
 *
 * cookie - an initial cookie equal to "rid=XXX" where XXX is 000 to 999 is
 * required to identify the request - the cookie subsequently returned by the
 * search response can be then used to re-initiate a content sync to a
 * specific LDAP server state
 *
 * reloadHint - used to tell the LDAP server how to address the situation
 * where content synchronization cannot be achieved - to reload the entire
 * content or send an e-syncRefreshRequired result code
 * </pre>
 * 
 * @see RFC4533
 */
public class LDAPSyncRequestControl extends LDAPControl {

    private static final long serialVersionUID = 3330968375873702228L;
    
    /**
     * The OID of the Sync Request Control.
     */
    public static final String OID = "1.3.6.1.4.1.4203.1.9.1.1";
    
    // control mode types
    // mode 0 is unused, mode 2 is reserved
    public static final int MODE_REFRESH_ONLY = 1;
    public static final int MODE_REFRESH_AND_PERSIST = 3;
    
    private int mode;
    private byte [] cookie;
    boolean reloadHint = false;

    /**
     * Default constructor
     */
    public LDAPSyncRequestControl()
    {
        super(OID, true, null);
    }
    
    /**
     * Constructs a SyncRequestControl.
     * @param mode specifies the mode of operation of the sync request
     * @param cookie the cookie with which to initiate the request
     * @param reloadHint - tells LDAP server how to address the situation
     * where content synchronization cannot be achieved
     * @throws IOException if the control cannot be constructed
     */
    public LDAPSyncRequestControl( final int mode, final byte cookie[],
          final boolean reloadHint )
    {
       super( OID, false, null );

       this.mode = mode;
       this.cookie = cookie;
       this.reloadHint = reloadHint;

       // this control is always critical
       m_critical = true;
       m_value = generateEncodedValue();
    }

    /**
     * Returns the mode value.
     * @return the mode
     */
    public int getMode()
    {
       return this.mode;
    }

    /**
     * Sets the mode value.
     * @param mode - the mode value
     */
    public void setMode(final int mode)
    {
       this.mode = mode;
    }
    
    /**
     * Returns the cookie value.
     * @return the cookie
     */
    public byte[] getCookie()
    {
       return this.cookie;
    }

    /**
     * Sets the cookie value.
     * @param cookie - the cookie value
     */
    public void setCookie(final byte[] cookie)
    {
       this.cookie = cookie;
    }
    
    /**
     * Returns the reloadHint value.
     * @return the reloadHint
     */
    public boolean isReloadHint()
    {
       return this.reloadHint;
    }
    
    /**
     * Sets the reloadHint value.
     * @param reloadHint - the reloadHint value
     */
    public void setReloadHint(final boolean reloadHint)
    {
       this.reloadHint = reloadHint;
    }
    
    /*
     * Encodes the control value in BER format.
     */
    private byte[] generateEncodedValue()
    {
       final BERSequence sequence = new BERSequence();
       sequence.addElement( new BEREnumerated( this.mode ) );
       if ( cookie != null )
       {
          sequence.addElement( new BEROctetString( cookie ) );
       }

       if ( reloadHint )
       {
          sequence.addElement( new BERBoolean( reloadHint ) );
       }

       /* return a byte array */
       return flattenBER( sequence );
    }
}
