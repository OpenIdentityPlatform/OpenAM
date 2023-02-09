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

class Trans2QueryFSInformation extends SmbComTransaction {

    private int informationLevel;

    Trans2QueryFSInformation( int informationLevel ) {
        command = SMB_COM_TRANSACTION2;
        subCommand = TRANS2_QUERY_FS_INFORMATION;
        this.informationLevel = informationLevel;
        totalParameterCount = 2;
        totalDataCount = 0;
        maxParameterCount = 0;
        maxDataCount = 800;
        maxSetupCount = 0;
    }

    int writeSetupWireFormat( byte[] dst, int dstIndex ) {
        dst[dstIndex++] = subCommand;
        dst[dstIndex++] = (byte)0x00;
        return 2;
    }
    int writeParametersWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        writeInt2( informationLevel, dst, dstIndex );
        dstIndex += 2;

        /* windows98 has what appears to be another 4 0's followed by the share
         * name as a zero terminated ascii string "\TMP" + '\0'
         *
         * As is this works, but it deviates from the spec section 4.1.6.6 but
         * maybe I should put it in. Wonder what NT does?
         */

        return dstIndex - start;
    }
    int writeDataWireFormat( byte[] dst, int dstIndex ) {
        return 0;
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
        return new String( "Trans2QueryFSInformation[" + super.toString() +
            ",informationLevel=0x" + Hexdump.toHexString( informationLevel, 3 ) + "]" );
    }
}
