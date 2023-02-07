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

class NetShareEnumResponse extends SmbComTransactionResponse {

    private int converter, totalAvailableEntries;

    NetShareEnumResponse() {
    }

    int writeSetupWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int writeParametersWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int writeDataWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int readSetupWireFormat( byte[] buffer, int bufferIndex, int len ) {
        return 0;
    }
    int readParametersWireFormat( byte[] buffer, int bufferIndex, int len ) {
        int start = bufferIndex;

        status = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        converter = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        numEntries = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        totalAvailableEntries = readInt2( buffer, bufferIndex );
        bufferIndex += 2;

        return bufferIndex - start;
    }
    int readDataWireFormat( byte[] buffer, int bufferIndex, int len ) {
        int start = bufferIndex;
        SmbShareInfo e;

        useUnicode = false;

        results = new SmbShareInfo[numEntries];
        for( int i = 0; i < numEntries; i++ ) {
            results[i] = e = new SmbShareInfo();
            e.netName = readString( buffer, bufferIndex, 13, false );
            bufferIndex += 14;
            e.type = readInt2( buffer, bufferIndex );
            bufferIndex += 2;
            int off = readInt4( buffer, bufferIndex );
            bufferIndex += 4;
            off = ( off & 0xFFFF ) - converter;
            off = start + off;
            e.remark = readString( buffer, off, 128, false );

            if (log.level >= 4)
                log.println( e );
        }

        return bufferIndex - start;
    }
    public String toString() {
        return new String( "NetShareEnumResponse[" +
                super.toString() +
                ",status=" + status +
                ",converter=" + converter +
                ",entriesReturned=" + numEntries +
                ",totalAvailableEntries=" + totalAvailableEntries + "]" );
    }
}
