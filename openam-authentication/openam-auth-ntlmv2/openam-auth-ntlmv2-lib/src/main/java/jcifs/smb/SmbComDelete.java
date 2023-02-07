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

class SmbComDelete extends ServerMessageBlock {

    private int searchAttributes;

    SmbComDelete( String fileName ) {
        this.path = fileName;
        command = SMB_COM_DELETE;
        searchAttributes = ATTR_HIDDEN | ATTR_HIDDEN | ATTR_SYSTEM;
    }

    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        writeInt2( searchAttributes, dst, dstIndex );
        return 2;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        dst[dstIndex++] = (byte)0x04;
        dstIndex += writeString( path, dst, dstIndex );

        return dstIndex - start;
    }
    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    int readBytesWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    public String toString() {
        return new String( "SmbComDelete[" +
            super.toString() +
            ",searchAttributes=0x" + Hexdump.toHexString( searchAttributes, 4 ) +
            ",fileName=" + path + "]" );
    }
}
