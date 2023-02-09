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

import jcifs.util.Hexdump;

class NetServerEnum2Response extends SmbComTransactionResponse {

    class ServerInfo1 implements FileEntry {
        String name;
        int versionMajor;
        int versionMinor;
        int type;
        String commentOrMasterBrowser;

        public String getName() {
            return name;
        }
        public int getType() {
            return (type & 0x80000000) != 0 ? SmbFile.TYPE_WORKGROUP : SmbFile.TYPE_SERVER;
        }
        public int getAttributes() {
            return SmbFile.ATTR_READONLY | SmbFile.ATTR_DIRECTORY;
        }
        public long createTime() {
            return 0L;
        }
        public long lastModified() {
            return 0L;
        }
        public long length() {
            return 0L;
        }

        public String toString() {
            return new String( "ServerInfo1[" +
                    "name=" + name +
                    ",versionMajor=" + versionMajor +
                    ",versionMinor=" + versionMinor +
                    ",type=0x" + Hexdump.toHexString( type, 8 ) +
                    ",commentOrMasterBrowser=" + commentOrMasterBrowser + "]" );
        }
    }

    private int converter, totalAvailableEntries;

    String lastName;

    NetServerEnum2Response() {
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
        ServerInfo1 e = null;

        results = new ServerInfo1[numEntries];
        for( int i = 0; i < numEntries; i++ ) {
            results[i] = e = new ServerInfo1();
            e.name = readString( buffer, bufferIndex, 16, false );
            bufferIndex += 16;
            e.versionMajor = (int)( buffer[bufferIndex++] & 0xFF );
            e.versionMinor = (int)( buffer[bufferIndex++] & 0xFF );
            e.type = readInt4( buffer, bufferIndex );
            bufferIndex += 4;
            int off = readInt4( buffer, bufferIndex );
            bufferIndex += 4;
            off = ( off & 0xFFFF ) - converter;
            off = start + off;
            e.commentOrMasterBrowser = readString( buffer, off, 48, false );

            if( log.level >= 4 )
                log.println( e );
        }
        lastName = numEntries == 0 ? null : e.name;

        return bufferIndex - start;
    }
    public String toString() {
        return new String( "NetServerEnum2Response[" +
                super.toString() +
                ",status=" + status +
                ",converter=" + converter +
                ",entriesReturned=" + numEntries +
                ",totalAvailableEntries=" + totalAvailableEntries +
                ",lastName=" + lastName + "]" );
    }
}
