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

class SmbComOpenAndXResponse extends AndXServerMessageBlock {

    int fid,
        fileAttributes,
        dataSize,
        grantedAccess,
        fileType,
        deviceState,
        action,
        serverFid;
    long lastWriteTime;

    SmbComOpenAndXResponse() {
    }

    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        int start = bufferIndex;

        fid = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        fileAttributes = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        lastWriteTime = readUTime( buffer, bufferIndex );
        bufferIndex += 4;
        dataSize = readInt4( buffer, bufferIndex );
        bufferIndex += 4;
        grantedAccess = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        fileType = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        deviceState = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        action = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        serverFid = readInt4( buffer, bufferIndex );
        bufferIndex += 6;

        return bufferIndex - start;
    }
    int readBytesWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    public String toString() {
        return new String( "SmbComOpenAndXResponse[" +
            super.toString() +
            ",fid=" + fid +
            ",fileAttributes=" + fileAttributes +
            ",lastWriteTime=" + lastWriteTime +
            ",dataSize=" + dataSize +
            ",grantedAccess=" + grantedAccess +
            ",fileType=" + fileType +
            ",deviceState=" + deviceState +
            ",action=" + action +
            ",serverFid=" + serverFid + "]" );
    }
}
