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

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import com.sun.identity.shared.ldap.ber.stream.*;
import com.sun.identity.shared.ldap.client.*;
import com.sun.identity.shared.ldap.util.*;
import com.sun.identity.shared.ldap.controls.*;

/**
 * Represents arbitrary control data that can be used with a
 * a particular LDAP operation.  LDAP controls are part of version 3
 * of the LDAP protocol.
 * <P>
 *
 * LDAP controls allow you to extend the functionality of
 * an LDAP operation.  For example, you can use an LDAP control
 * for the search operation to sort search results on an LDAP server.
 * <P>
 *
 * An LDAP control can be either a <B>server control</B> or
 * a <B>client control</B>:
 * <P>
 * <UL>
 * <LI><B>Server controls</B> can be sent to the LDAP server or returned
 * by the server on any operation.
 * <LI><B>Client controls</B> are intended to affect only the client side
 * of the operation.
 * </UL>
 * <P>
 *
 * An LDAP control consists of the following information:
 * <P>
 * <UL>
 * <LI>A unique object ID (OID) that identifies the control.<P>
 * <LI>A &quot;criticality&quot; field, which indicates whether or
 * not the control is critical to the operation. (If the control is
 * critical to the operation and the server does not support the control,
 * the server should not execute the operation.)<P>
 * <LI>Data pertaining to the control.<P>
 * </UL>
 * <P>
 *
 * To determine which server controls are supported by a particular server,
 * you need to search for the root DSE (DSA-specific entry, where DSA is
 * another term for &quot;LDAP server&quot;) and find the values of the
 * <CODE>supportedControl</CODE> attribute.  This attribute contains the
 * object IDs (OIDs) of the controls supported by this server.
 * <P>
 *
 * The following section of code demonstrates how to get the list
 * of the server controls supported by an LDAP server.
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
 *     String getAttrs[] = { "supportedControl" };
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
 * supportedcontrol
 *   1.2.840.113556.1.4.473
 *   1.3.6.1.4.1.1466.29539.12
 *   1.3.6.1.4.1.42.2.27.9.5.2
 *   1.3.6.1.4.1.42.2.27.9.5.6
 *   2.16.840.1.113730.3.4.12
 *   2.16.840.1.113730.3.4.13
 *   2.16.840.1.113730.3.4.14
 *   2.16.840.1.113730.3.4.15
 *   2.16.840.1.113730.3.4.16
 *   2.16.840.1.113730.3.4.17
 *   2.16.840.1.113730.3.4.18
 *   2.16.840.1.113730.3.4.19
 *   2.16.840.1.113730.3.4.2
 *   2.16.840.1.113730.3.4.3
 *   2.16.840.1.113730.3.4.4
 *   2.16.840.1.113730.3.4.5
 *   2.16.840.1.113730.3.4.9
 * </PRE>
 * <P>
 *
 * For more information on LDAP controls, see 
 * <A HREF="http://www.ietf.org/rfc/rfc2251"
 * TARGET="_blank">RFC 2251</A>.
 * <P>
 *
 * @version 1.0
 * @see com.sun.identity.shared.ldap.LDAPv3#CLIENTCONTROLS
 * @see com.sun.identity.shared.ldap.LDAPv3#SERVERCONTROLS
 * @see com.sun.identity.shared.ldap.LDAPConnection#search(java.lang.String, int, java.lang.String, java.lang.String[], boolean)
 * @see com.sun.identity.shared.ldap.LDAPConnection#getOption
 * @see com.sun.identity.shared.ldap.LDAPConnection#setOption
 * @see com.sun.identity.shared.ldap.LDAPConnection#getResponseControls
 * @see com.sun.identity.shared.ldap.LDAPConstraints#getClientControls
 * @see com.sun.identity.shared.ldap.LDAPConstraints#getServerControls
 * @see com.sun.identity.shared.ldap.LDAPConstraints#setClientControls
 * @see com.sun.identity.shared.ldap.LDAPConstraints#setServerControls
 */
public class LDAPControl implements Cloneable, java.io.Serializable {
    static final long serialVersionUID = 5149887553272603753L;
    public final static String MANAGEDSAIT       = "2.16.840.1.113730.3.4.2";
    /* Password information sent back to client */
    public final static String PWEXPIRED         = "2.16.840.1.113730.3.4.4";
    public final static String PWEXPIRING        = "2.16.840.1.113730.3.4.5";  

    public static final int LDAP_CONTROL = 1; 
    public static final int LDAP_ENTRY_CHANGE_CONTROL = 2;
    public static final int LDAP_PERSIST_SEARCH_CONTROL = 3;
    public static final int LDAP_PROXIED_AUTH_CONTROL = 4;
    public static final int LDAP_SORT_CONTROL = 5;
    public static final int LDAP_STRING_CONTROL = 6;
    public static final int LDAP_VIRTUAL_LIST_CONTROL = 7;
    public static final int LDAP_VIRTUAL_LIST_RESPONSE_CONTROL = 8;
    public static final int LDAP_PASSWORD_EXPIRED_CONTROL = 9;
    public static final int LDAP_PASSWORD_EXPIRING_CONTROL = 10;
    
    private LinkedList bytesList = null;
    private int bytesLength = 0;
    private static JDAPBERTagDecoder decoder = new JDAPBERTagDecoder();

    /**
     * Default constructor for the <CODE>LDAPControl</CODE> class.
     */
    public LDAPControl()
    {
    }

    /**
     * Constructs a new <CODE>LDAPControl</CODE> object using the
     * specified object ID (OID), &quot;criticality&quot; field, and
     * data to be used by the control.
     * <P>
     *
     * @param id the object ID (OID) identifying the control
     * @param critical <CODE>true</CODE> if the LDAP operation should be
     * cancelled when the server does not support this control (in other
     * words, this control is critical to the LDAP operation)
     * @param vals control-specific data
     * @see com.sun.identity.shared.ldap.LDAPConstraints#setClientControls
     * @see com.sun.identity.shared.ldap.LDAPConstraints#setServerControls
     */
    public LDAPControl(String id,
             boolean critical,
             byte vals[]) {
        m_oid = id;
        m_critical = critical;
        m_value = vals;
    }

    public int getType() {
        return LDAP_CONTROL;
    }
 
    /**
     * Gets the object ID (OID) of the control.
     * @return object ID (OID) of the control.
     */
    public String getID() {
        return m_oid;
    }

    /**
     * Specifies whether or not the control is critical to the LDAP operation.
     * @return <CODE>true</CODE> if the LDAP operation should be cancelled when
     * the server does not support this control.
     */
    public boolean isCritical() {
        return m_critical;
    }    
    
    /**
     * Gets the data in the control.
     * @return the data in the control as a byte array.
     */
    public byte[] getValue() {
        return m_value;
    }

    /**
     * Gets the ber representation of control.
     * @return ber representation of control.
     */
    BERElement getBERElement() {
        BERSequence seq = new BERSequence();
        seq.addElement(new BEROctetString (m_oid));
        seq.addElement(new BERBoolean (m_critical));
        if ( (m_value == null) || (m_value.length < 1) )
            seq.addElement(new BEROctetString ((byte[])null));
        else {
            seq.addElement(new BEROctetString (m_value, 0, m_value.length));
        }
        return seq;
    }

    public int getBytesSize() {
        if (bytesList == null) {
            getBytesLinkedList();
        }
        return bytesLength;
    }

    public LinkedList getBytesLinkedList() {
        if (bytesList == null) {
            bytesList = new LinkedList();
            bytesLength = 0;
            // add value
            if ((m_value == null) || (m_value.length < 1)) {
                bytesLength += LDAPRequestParser.addOctetBytes(bytesList,
                    null);
            } else {
                bytesLength += LDAPRequestParser.addOctetBytes(bytesList,
                    m_value);
            }
            bytesList.addFirst(BERElement.OCTETSTRING_BYTES);
            bytesLength++;
            // add boolean
            bytesLength += LDAPRequestParser.addBoolean(bytesList, m_critical);
            bytesList.addFirst(BERElement.BOOLEAN_BYTES);
            bytesLength++;
            // add oid
            bytesLength += LDAPRequestParser.addOctetString(bytesList, m_oid);
            bytesList.addFirst(BERElement.OCTETSTRING_BYTES);
            bytesLength++;
            // add length for the control
            byte[] tempBytes = LDAPRequestParser.getLengthBytes(bytesLength);
            bytesList.addFirst(tempBytes);
            bytesLength += tempBytes.length;
            bytesList.addFirst(BERElement.SEQUENCE_BYTES);
            bytesLength++;
        }
        return bytesList;
    }
    
    /** 
     * Associates a class with an oid. This class must be an extension of 
     * <CODE>LDAPControl</CODE>, and should implement the <CODE>LDAPControl(
     * String oid, boolean critical, byte[] value)</CODE> constructor to 
     * instantiate the control. 
     * @param oid the string representation of the oid
     * @param controlClass the class that instantatiates the control associated
     * with oid
     * @exception com.sun.identity.shared.ldap.LDAPException If the class parameter is not
     * a subclass of <CODE>LDAPControl</CODE> or the class parameter does not
     * implement the <CODE>LDAPControl(String oid, boolean critical, byte[] value)
     * </CODE> constructor.
     */
    public static void register(String oid, Class controlClass) throws 
        LDAPException {

        if (controlClass == null) {
	    return;
        }

	// 1. make sure controlClass is a subclass of LDAPControl
	Class superClass = controlClass;
	while (superClass != LDAPControl.class && superClass != null) {
	    superClass = superClass.getSuperclass();
	}

	if (superClass == null) 
	    throw new LDAPException("controlClass must be a subclass of " +
				    "LDAPControl", LDAPException.PARAM_ERROR);

	// 2. make sure controlClass has the proper constructor
	Class[] cparams = { String.class, boolean.class, byte[].class };
	try {
	    controlClass.getConstructor(cparams);
	} catch (NoSuchMethodException e) {
	    throw new LDAPException("controlClass does not implement the " +
				    "correct contstructor", 
				    LDAPException.PARAM_ERROR);
	}

        // 3. check if the hash table exists
        if (m_controlClassHash == null) {
            m_controlClassHash = new Hashtable();
	}

	// 4. add the controlClass
        m_controlClassHash.put(oid, controlClass);
    }

    /**
     * Returns the <CODE>Class</CODE> that has been registered to oid.
     * @param oid a String that associates the control class to a control
     * @return a <CODE>Class</CODE> that can instantiate a control of the
     * type specified by oid.
     * @see com.sun.identity.shared.ldap.LDAPControl#register
     *
     */
    protected static Class lookupControlClass(String oid) {
        if (m_controlClassHash == null) {
	    return null;
        }
      
	return (Class)m_controlClassHash.get(oid);
    }
    
    /**
     * Returns a <CODE>LDAPControl</CODE> object instantiated by the Class
     * associated by <CODE>LDAPControl.register</CODE> to the oid. If
     * no Class is found for the given control, or an exception occurs when 
     * attempting to instantiate the control, a basic <CODE>LDAPControl</CODE>
     * is instantiated using the parameters. 
     * @param oid the oid of the control to instantiate
     * @param critical <CODE>true</CODE> if this is a critical control
     * @param value the byte value for the control
     * @return a newly instantiated <CODE>LDAPControl</CODE>.
     * @see com.sun.identity.shared.ldap.LDAPControl#register
     */ 
  protected static LDAPControl createControl(String oid, boolean critical,
                                             byte[] value) {
        
	Class controlClass = lookupControlClass(oid);
	
	if (controlClass == null) {
	    return new LDAPControl(oid, critical, value);
	}

	Class[] cparams = { String.class, boolean.class, byte[].class };
	Constructor creator = null;
	try {
	    creator = controlClass.getConstructor(cparams);
	} catch (NoSuchMethodException e) {
	    //shouldn't happen, but...
	    System.err.println("Caught java.lang.NoSuchMethodException while" +
			       " attempting to instantiate a control of type " +
			       oid);
	    return new LDAPControl(oid, critical, value);
	}
	
	Object[] oparams = { oid, new Boolean(critical), value } ;
	LDAPControl returnControl = null;
	try {
	    returnControl = (LDAPControl)creator.newInstance(oparams);
	} catch (Exception e) {
	    String eString = null;
	    if (e instanceof InvocationTargetException) {
	        eString = ((InvocationTargetException)
		          e).getTargetException().toString();
	    } else {
	        eString = e.toString();
	    }
	
	    System.err.println("Caught " + eString + " while attempting to" +
			       " instantiate a control of type " +
			       oid);
	    returnControl = new LDAPControl(oid, critical, value);
   	}

	return returnControl;
    }

    /**
     * Returns a <CODE>LDAPControl</CODE> object instantiated by the Class
     * associated by <CODE>LDAPControl.register</CODE> to the oid. If
     * no Class is found for the given control, or an exception occurs when 
     * attempting to instantiate the control, a basic <CODE>LDAPControl</CODE>
     * is instantiated using the parameters.
     * @param el the <CODE>BERElement</CODE> containing the control
     * @return a newly instantiated <CODE>LDAPControl</CODE>.
     * @see com.sun.identity.shared.ldap.LPAPControl#register
     * 
     * Note:
     * This code was extracted from <CODE>JDAPControl(BERElement el)</CODE>
     * constructor.
     */ 
  static LDAPControl parseControl(BERElement el) {
        BERSequence s = (BERSequence)el;
        String oid = null;
        boolean critical = false;
        byte[] value = null;
        try{
            oid = new String(((BEROctetString)s.elementAt(0)).getValue(), "UTF8");
        } catch(Throwable x) {}
        
        BERElement obj = (BERElement) s.elementAt(1);
        if (obj.getType() == BERElement.BOOLEAN) {
            critical = ((BERBoolean)obj).getValue();
        }            
        else {
            value = ((BEROctetString)obj).getValue();
        }            

        if (s.size() >= 3) {
            value = ((BEROctetString)s.elementAt(2)).getValue();
        }
        
        return createControl(oid, critical, value);
  }
      

  /**
     * Instantiates all of the controls contained within the LDAP message 
     * fragment specified by data and returns them in an <CODE>LDAPControl</CODE>
     * array. This fragment can be either the entire LDAP message or just the 
     * control section of the message.
     * <P>
     * If an exception occurs when instantiating a control, that control is 
     * returned as a basic <CODE>LDAPControl</CODE>.
     * @param data the LDAP message fragment in raw BER format
     * @return an <CODE>LDAPControl</CODE> array containing all of the controls
     * from the message fragment.
     * @exception java.lang.IOException If the data passed to this method
     * is not a valid LDAP message fragment.
     * @see com.sun.identity.shared.ldap.LDAPControl#register
     */
    public static LDAPControl[] newInstance(byte[] data) throws IOException {
        
        int[] bread = { 0 };
  	BERElement el = BERElement.getElement(decoder,
                                          new ByteArrayInputStream(data),
                                          bread);

	LDAPControl[] jc = null;
	try {
	    // see if data is a LDAP message
	    LDAPMessage msg = LDAPMessage.parseMessage(el);
	    return msg.getControls();
	} catch (IOException e) {
  	    // that didn't work; let's see if its just the controls 
	    BERTag tag = (BERTag)el;
	    if ( tag.getTag() == (BERTag.CONSTRUCTED|BERTag.CONTEXT|0) ) {
	        BERSequence controls = (BERSequence)tag.getValue();
		jc = new LDAPControl[controls.size()];
		for (int i = 0; i < controls.size(); i++) {
		    jc[i] = parseControl(controls.elementAt(i));
		}
	    }
	}
	
	return jc;
	    
    }

    /**
     * Creates a copy of the control.
     * @return copy of the control.
     */
    public Object clone() {
        byte[] vals = null;
        if ( m_value != null ) {
            vals = new byte[m_value.length];
            for( int i = 0; i < m_value.length; i++ )
                vals[i] = m_value[i];
        }
        LDAPControl control = new LDAPControl( m_oid, m_critical, vals );

        return control;
    }

    /**
     * Create a "flattened" BER encoding from a BER,
     * and return it as a byte array.
     * @param ber a BER encoded sequence
     * @return the byte array of encoded data.
     */
    protected byte[] flattenBER( BERSequence ber ) {
        /* Suck out the data and return it */
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            ber.write( outStream );
        } catch ( IOException e ) {
            return null;
        }
        return outStream.toByteArray();
    }

    /**
     * Return a string representation of the control for debugging
     *
     * @return a string representation of the control.
     */
    public String toString() {
        if (getID().equals(MANAGEDSAIT)) {
            return "{MANAGEDSITControl: isCritical=" + isCritical() + "}";
        }
        String s = getID() + ' ' + isCritical();
        if ( m_value != null ) {
            s += ' ' + LDIF.toPrintableString( m_value );
        }
        return "LDAPControl {" + s + '}';
    }

    private String m_oid;
    protected boolean m_critical = false;
    protected byte[] m_value = null;
    static private Hashtable m_controlClassHash = null;
    static {
        try {
            LDAPControl.register( LDAPPasswordExpiringControl.EXPIRING,
                                  LDAPPasswordExpiringControl.class );
            LDAPControl.register( LDAPPasswordExpiredControl.EXPIRED,
                                  LDAPPasswordExpiredControl.class );
            LDAPControl.register( LDAPEntryChangeControl.ENTRYCHANGED,
                                  LDAPEntryChangeControl.class );
            LDAPControl.register( LDAPSortControl.SORTRESPONSE,
                                  LDAPSortControl.class );
            LDAPControl.register( LDAPVirtualListResponse.VIRTUALLISTRESPONSE,
                                  LDAPVirtualListResponse.class );
        } catch (LDAPException e) {
        }
    }
}

