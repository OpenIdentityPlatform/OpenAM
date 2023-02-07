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

import jcifs.util.Hexdump;

import java.util.Date;

class SmbComQueryInformationResponse extends ServerMessageBlock implements Info {

    private int fileAttributes = 0x0000;
    private long lastWriteTime = 0L;
    private long serverTimeZoneOffset;
    private int fileSize = 0;

    SmbComQueryInformationResponse( long serverTimeZoneOffset ) {
        this.serverTimeZoneOffset = serverTimeZoneOffset;
        command = SMB_COM_QUERY_INFORMATION;
    }

    public int getAttributes() {
        return fileAttributes;
    }
    public long getCreateTime() {
        return lastWriteTime + serverTimeZoneOffset;
    }
    public long getLastWriteTime() {
        return lastWriteTime + serverTimeZoneOffset;
    }
    public long getSize() {
        return fileSize;
    }
    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        if( wordCount == 0 ) {
            return 0;
        }
        fileAttributes = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        lastWriteTime = readUTime( buffer, bufferIndex );
        bufferIndex += 4;
        fileSize = readInt4( buffer, bufferIndex );
        return 20;
    }
    int readBytesWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    public String toString() {
        return new String( "SmbComQueryInformationResponse[" +
            super.toString() +
            ",fileAttributes=0x" + Hexdump.toHexString( fileAttributes, 4 ) +
            ",lastWriteTime=" + new Date( lastWriteTime ) +
            ",fileSize=" + fileSize + "]" );
    }
}

