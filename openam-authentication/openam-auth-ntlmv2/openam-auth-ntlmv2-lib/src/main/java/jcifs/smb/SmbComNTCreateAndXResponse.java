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

class SmbComNTCreateAndXResponse extends AndXServerMessageBlock {

    static final int EXCLUSIVE_OPLOCK_GRANTED = 1;
    static final int BATCH_OPLOCK_GRANTED     = 2;
    static final int LEVEL_II_OPLOCK_GRANTED  = 3;

    byte oplockLevel;
    int fid,
        createAction,
        extFileAttributes,
        fileType,
        deviceState;
    long creationTime,
        lastAccessTime,
        lastWriteTime,
        changeTime,
        allocationSize,
        endOfFile;
    boolean directory;
boolean isExtended;

    SmbComNTCreateAndXResponse() {
    }

    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        int start = bufferIndex;

        oplockLevel = buffer[bufferIndex++];
        fid = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        createAction = readInt4( buffer, bufferIndex );
        bufferIndex += 4;
        creationTime = readTime( buffer, bufferIndex );
        bufferIndex += 8;
        lastAccessTime = readTime( buffer, bufferIndex );
        bufferIndex += 8;
        lastWriteTime = readTime( buffer, bufferIndex );
        bufferIndex += 8;
        changeTime = readTime( buffer, bufferIndex );
        bufferIndex += 8;
        extFileAttributes = readInt4( buffer, bufferIndex );
        bufferIndex += 4;
        allocationSize = readInt8( buffer, bufferIndex );
        bufferIndex += 8;
        endOfFile = readInt8( buffer, bufferIndex );
        bufferIndex += 8;
        fileType = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        deviceState = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        directory = ( buffer[bufferIndex++] & 0xFF ) > 0;

        return bufferIndex - start;
    }
    int readBytesWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    public String toString() {
        return new String( "SmbComNTCreateAndXResponse[" +
            super.toString() +
            ",oplockLevel=" + oplockLevel +
            ",fid=" + fid +
            ",createAction=0x" + Hexdump.toHexString( createAction, 4 ) +
            ",creationTime=" + new Date( creationTime ) +
            ",lastAccessTime=" + new Date( lastAccessTime ) +
            ",lastWriteTime=" + new Date( lastWriteTime ) +
            ",changeTime=" + new Date( changeTime ) +
            ",extFileAttributes=0x" + Hexdump.toHexString( extFileAttributes, 4 ) +
            ",allocationSize=" + allocationSize +
            ",endOfFile=" + endOfFile +
            ",fileType=" + fileType +
            ",deviceState=" + deviceState +
            ",directory=" + directory + "]" );
    }
}
