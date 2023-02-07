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

class Trans2SetFileInformation extends SmbComTransaction {

    static final int SMB_FILE_BASIC_INFO = 0x101;

    private int fid;
    private int attributes;
    private long createTime, lastWriteTime;

    Trans2SetFileInformation( int fid, int attributes, long createTime, long lastWriteTime ) {
        this.fid = fid;
        this.attributes = attributes;
        this.createTime = createTime;
        this.lastWriteTime = lastWriteTime;
        command = SMB_COM_TRANSACTION2;
        subCommand = TRANS2_SET_FILE_INFORMATION;
        maxParameterCount = 6;
        maxDataCount = 0;
        maxSetupCount = (byte)0x00;
    }

    int writeSetupWireFormat( byte[] dst, int dstIndex ) {
        dst[dstIndex++] = subCommand;
        dst[dstIndex++] = (byte)0x00;
        return 2;
    }
    int writeParametersWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        writeInt2( fid, dst, dstIndex );
        dstIndex += 2;
        writeInt2( SMB_FILE_BASIC_INFO, dst, dstIndex );
        dstIndex += 2;
        writeInt2( 0, dst, dstIndex );
        dstIndex += 2;

        return dstIndex - start;
    }
    int writeDataWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        writeTime( createTime, dst, dstIndex ); dstIndex += 8;
        writeInt8( 0L, dst, dstIndex ); dstIndex += 8;
        writeTime( lastWriteTime, dst, dstIndex ); dstIndex += 8;
        writeInt8( 0L, dst, dstIndex ); dstIndex += 8;
/* Samba 2.2.7 needs ATTR_NORMAL
 */
        writeInt2( 0x80 | attributes, dst, dstIndex ); dstIndex += 2; 
                                        /* 6 zeros observed with NT */
        writeInt8( 0L, dst, dstIndex ); dstIndex += 6;

                /* Also observed 4 byte alignment but we stick
                 * with the default for jCIFS which is 2 */

        return dstIndex - start;
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
        return new String( "Trans2SetFileInformation[" + super.toString() +
            ",fid=" + fid + "]" );
    }
}
