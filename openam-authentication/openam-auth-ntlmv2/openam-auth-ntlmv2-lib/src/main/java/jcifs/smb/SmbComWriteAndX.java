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

class SmbComWriteAndX extends AndXServerMessageBlock {

    private static final int READ_ANDX_BATCH_LIMIT =
                            Config.getInt( "jcifs.smb.client.WriteAndX.ReadAndX", 1 );
    private static final int CLOSE_BATCH_LIMIT =
                            Config.getInt( "jcifs.smb.client.WriteAndX.Close", 1 );

    private int fid,
        remaining,
        dataLength,
        dataOffset,
        off;
    private byte[] b;
    private long offset;

private int pad;

    int writeMode;

    SmbComWriteAndX() {
        super( null );
        command = SMB_COM_WRITE_ANDX;
    }
    SmbComWriteAndX( int fid, long offset, int remaining,
                    byte[] b, int off, int len, ServerMessageBlock andx ) {
        super( andx );
        this.fid = fid;
        this.offset = offset;
        this.remaining = remaining;
        this.b = b;
        this.off = off;
        dataLength = len;
        command = SMB_COM_WRITE_ANDX;
    }

    void setParam( int fid, long offset, int remaining,
                    byte[] b, int off, int len ) {
        this.fid = fid;
        this.offset = offset;
        this.remaining = remaining;
        this.b = b;
        this.off = off;
        dataLength = len;
        digest = null; /* otherwise recycled commands
                        * like writeandx will choke if session
                        * closes in between */
    }
    int getBatchLimit( byte command ) {
        if( command == SMB_COM_READ_ANDX ) {
            return READ_ANDX_BATCH_LIMIT;
        }
        if( command == SMB_COM_CLOSE ) {
            return CLOSE_BATCH_LIMIT;
        }
        return 0;
    }
    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        dataOffset = (dstIndex - headerStart) + 26; // 26 = off from here to pad

pad = ( dataOffset - headerStart ) % 4;
pad = pad == 0 ? 0 : 4 - pad;
dataOffset += pad;

        writeInt2( fid, dst, dstIndex );
        dstIndex += 2;
        writeInt4( offset, dst, dstIndex );
        dstIndex += 4;
        for( int i = 0; i < 4; i++ ) {
            dst[dstIndex++] = (byte)0xFF;
        }
        writeInt2( writeMode, dst, dstIndex );
        dstIndex += 2;
        writeInt2( remaining, dst, dstIndex );
        dstIndex += 2;
        dst[dstIndex++] = (byte)0x00;
        dst[dstIndex++] = (byte)0x00;
        writeInt2( dataLength, dst, dstIndex );
        dstIndex += 2;
        writeInt2( dataOffset, dst, dstIndex );
        dstIndex += 2;
        writeInt4( offset >> 32, dst, dstIndex );
        dstIndex += 4;

        return dstIndex - start;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

while( pad-- > 0 ) {
    dst[dstIndex++] = (byte)0xEE;
}
        System.arraycopy( b, off, dst, dstIndex, dataLength );
        dstIndex += dataLength;

        return dstIndex - start;
    }
    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    int readBytesWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    public String toString() {
        return new String( "SmbComWriteAndX[" +
            super.toString() +
            ",fid=" + fid +
            ",offset=" + offset +
            ",writeMode=" + writeMode +
            ",remaining=" + remaining +
            ",dataLength=" + dataLength +
            ",dataOffset=" + dataOffset + "]" );
    }
}
