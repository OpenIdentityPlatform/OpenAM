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

import jcifs.Config;

class SmbComReadAndX extends AndXServerMessageBlock {

    private static final int BATCH_LIMIT = Config.getInt( "jcifs.smb.client.ReadAndX.Close", 1 );

    private long offset;
    private int fid,
        openTimeout;
int maxCount, minCount, remaining;

    SmbComReadAndX() {
        super( null );
        command = SMB_COM_READ_ANDX;
        openTimeout = 0xFFFFFFFF;
    }
    SmbComReadAndX( int fid, long offset, int maxCount, ServerMessageBlock andx ) {
        super( andx );
        this.fid = fid;
        this.offset = offset;
        this.maxCount = minCount = maxCount;
        command = SMB_COM_READ_ANDX;
        openTimeout = 0xFFFFFFFF;
    }

    void setParam( int fid, long offset, int maxCount ) {
        this.fid = fid;
        this.offset = offset;
        this.maxCount = minCount = maxCount;
    }
    int getBatchLimit( byte command ) {
        return command == SMB_COM_CLOSE ? BATCH_LIMIT : 0;
    }
    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        writeInt2( fid, dst, dstIndex );
        dstIndex += 2;
        writeInt4( offset, dst, dstIndex );
        dstIndex += 4;
        writeInt2( maxCount, dst, dstIndex );
        dstIndex += 2;
        writeInt2( minCount, dst, dstIndex );
        dstIndex += 2;
        writeInt4( openTimeout, dst, dstIndex );
        dstIndex += 4;
        writeInt2( remaining, dst, dstIndex );
        dstIndex += 2;
        writeInt4( offset >> 32, dst, dstIndex );
        dstIndex += 4;

        return dstIndex - start;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    int readBytesWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    public String toString() {
        return new String( "SmbComReadAndX[" +
            super.toString() +
            ",fid=" + fid +
            ",offset=" + offset +
            ",maxCount=" + maxCount +
            ",minCount=" + minCount +
            ",openTimeout=" + openTimeout +
            ",remaining=" + remaining +
            ",offset=" + offset +
             "]" );
    }
}
