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

class NetShareEnum extends SmbComTransaction {

    private static final String DESCR = "WrLeh\u0000B13BWz\u0000";

    NetShareEnum() {
        command = SMB_COM_TRANSACTION;
        subCommand = NET_SHARE_ENUM; // not really true be used by upper logic
        name = new String( "\\PIPE\\LANMAN" );
        maxParameterCount = 8;

//        maxDataCount = 4096; why was this set?
        maxSetupCount = (byte)0x00;
        setupCount = 0;
        timeout = 5000;
    }

    int writeSetupWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int writeParametersWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;
        byte[] descr;

        try {
            descr = DESCR.getBytes( "ASCII" );
        } catch( UnsupportedEncodingException uee ) {
            return 0;
        }

        writeInt2( NET_SHARE_ENUM, dst, dstIndex );
        dstIndex += 2;
        System.arraycopy( descr, 0, dst, dstIndex, descr.length );
        dstIndex += descr.length;
        writeInt2( 0x0001, dst, dstIndex );
        dstIndex += 2;
        writeInt2( maxDataCount, dst, dstIndex );
        dstIndex += 2;

        return dstIndex - start;
    }
    int writeDataWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int readSetupWireFormat( byte[] buffer, int bufferIndex, int len ) {
        return 0;
    }
    int readParametersWireFormat( byte[] buffer, int bufferIndex, int len ) {
        return 0;
    }
    int readDataWireFormat( byte[] buffer, int bufferIndex, int len ) {
        return 0;
    }
    public String toString() {
        return new String( "NetShareEnum[" + super.toString() + "]" );
    }
}
