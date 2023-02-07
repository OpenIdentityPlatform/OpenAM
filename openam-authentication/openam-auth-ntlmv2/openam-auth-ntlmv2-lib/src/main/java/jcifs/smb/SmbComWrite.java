/* jcifs smb client library in Java
 * Copyright (C) 2003  "Michael B. Allen" <jcifs at samba dot org>
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

class SmbComWrite extends ServerMessageBlock {

    private int fid,
        count,
        offset,
        remaining,
        off;
    private byte[] b;

    SmbComWrite() {
        super();
        command = SMB_COM_WRITE;
    }
    SmbComWrite( int fid, int offset, int remaining, byte[] b, int off, int len ) {
        this.fid = fid;
        this.count = len;
        this.offset = offset;
        this.remaining = remaining;
        this.b = b;
        this.off = off;
        command = SMB_COM_WRITE;
    }

    void setParam( int fid, long offset, int remaining,
                    byte[] b, int off, int len ) {
        this.fid = fid;
        this.offset = (int)(offset & 0xFFFFFFFFL);
        this.remaining = remaining;
        this.b = b;
        this.off = off;
        count = len;
        digest = null; /* otherwise recycled commands
                        * like writeandx will choke if session
                        * closes in between */
    }
    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        writeInt2( fid, dst, dstIndex );
        dstIndex += 2;
        writeInt2( count, dst, dstIndex );
        dstIndex += 2;
        writeInt4( offset, dst, dstIndex );
        dstIndex += 4;
        writeInt2( remaining, dst, dstIndex );
        dstIndex += 2;

        return dstIndex - start;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        dst[dstIndex++] = (byte)0x01; /* BufferFormat */
        writeInt2( count, dst, dstIndex ); /* DataLength? */
        dstIndex += 2;
        System.arraycopy( b, off, dst, dstIndex, count );
        dstIndex += count;

        return dstIndex - start;
    }
    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    int readBytesWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    public String toString() {
        return new String( "SmbComWrite[" +
            super.toString() +
            ",fid=" + fid +
            ",count=" + count +
            ",offset=" + offset +
            ",remaining=" + remaining + "]" );
    }
}
