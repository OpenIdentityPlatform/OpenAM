/* jcifs smb client library in Java
 * Copyright (C) 2000  "Michael B. Allen" <jcifs at samba dot org>
 *                             Gary Rambo <grambo aventail.com>
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

class NetServerEnum2 extends SmbComTransaction {

    static final int SV_TYPE_ALL         = 0xFFFFFFFF;
    static final int SV_TYPE_DOMAIN_ENUM = 0x80000000;

    static final String[] DESCR = {
        "WrLehDO\u0000B16BBDz\u0000",
        "WrLehDz\u0000B16BBDz\u0000",
    };

    String domain, lastName = null;
    int serverTypes;

    NetServerEnum2( String domain, int serverTypes ) {
        this.domain = domain;
        this.serverTypes = serverTypes;
        command = SMB_COM_TRANSACTION;
        subCommand = NET_SERVER_ENUM2; // not really true be used by upper logic
        name = "\\PIPE\\LANMAN";

        maxParameterCount = 8;
        maxDataCount = 16384;
        maxSetupCount = (byte)0x00;
        setupCount = 0;
        timeout = 5000;
    }

    void reset( int key, String lastName ) {
        super.reset();
        this.lastName = lastName;
    }

    int writeSetupWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int writeParametersWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;
        byte[] descr;
        int which = subCommand == NET_SERVER_ENUM2 ? 0 : 1;

        try {
            descr = DESCR[which].getBytes( "ASCII" );
        } catch( UnsupportedEncodingException uee ) {
            return 0;
        }

        writeInt2( subCommand & 0xFF, dst, dstIndex );
        dstIndex += 2;
        System.arraycopy( descr, 0, dst, dstIndex, descr.length );
        dstIndex += descr.length;
        writeInt2( 0x0001, dst, dstIndex );
        dstIndex += 2;
        writeInt2( maxDataCount, dst, dstIndex );
        dstIndex += 2;
        writeInt4( serverTypes, dst, dstIndex );
        dstIndex += 4;
        dstIndex += writeString( domain.toUpperCase(), dst, dstIndex, false );
        if( which == 1 ) {
            dstIndex += writeString( lastName.toUpperCase(), dst, dstIndex, false );
        }

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
        return new String( "NetServerEnum2[" + super.toString() +
                ",name=" + name +
                ",serverTypes=" + (serverTypes == SV_TYPE_ALL ?
                        "SV_TYPE_ALL" : "SV_TYPE_DOMAIN_ENUM" ) +
                "]" );
    }
}
