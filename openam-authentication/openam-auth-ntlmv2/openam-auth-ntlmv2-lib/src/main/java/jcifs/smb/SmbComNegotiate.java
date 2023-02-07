/* jcifs smb client library in Java
 * Copyright (C) 2000  "Michael B. Allen" <jcifs at samba dot org>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jcifs.smb;

import java.io.UnsupportedEncodingException;

class SmbComNegotiate extends ServerMessageBlock {

    private static final String DIALECTS = "\u0002NT LM 0.12\u0000";

    SmbComNegotiate() {
        command = SMB_COM_NEGOTIATE;
        flags2 = DEFAULT_FLAGS2;
    }

    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        byte[] dialects;
        try {
            dialects = DIALECTS.getBytes( "ASCII" );
        } catch( UnsupportedEncodingException uee ) {
            return 0;
        }
        System.arraycopy( dialects, 0, dst, dstIndex, dialects.length );
        return dialects.length;
    }
    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    int readBytesWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    public String toString() {
        return new String( "SmbComNegotiate[" +
            super.toString() +
            ",wordCount="   + wordCount +
            ",dialects=NT LM 0.12]" );
    }
}

