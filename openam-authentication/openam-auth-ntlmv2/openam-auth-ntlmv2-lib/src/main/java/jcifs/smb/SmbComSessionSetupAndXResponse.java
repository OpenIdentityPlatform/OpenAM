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

class SmbComSessionSetupAndXResponse extends AndXServerMessageBlock {

    private String nativeOs = "";
    private String nativeLanMan = "";
    private String primaryDomain = "";

    boolean isLoggedInAsGuest;
    byte[] blob = null;

    SmbComSessionSetupAndXResponse( ServerMessageBlock andx ) {
        super( andx );
    }

    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        int start = bufferIndex;
        isLoggedInAsGuest = ( buffer[bufferIndex] & 0x01 ) == 0x01 ? true : false;
        bufferIndex += 2;
        if (extendedSecurity) {
            int blobLength = readInt2(buffer, bufferIndex);
            bufferIndex += 2;
            blob = new byte[blobLength];
        }
        return bufferIndex - start;
    }
    int readBytesWireFormat( byte[] buffer, int bufferIndex ) {
        int start = bufferIndex;

        if (extendedSecurity) {
            System.arraycopy(buffer, bufferIndex, blob, 0, blob.length);
            bufferIndex += blob.length;
        }
        nativeOs = readString( buffer, bufferIndex );
        bufferIndex += stringWireLength( nativeOs, bufferIndex );
        nativeLanMan = readString( buffer, bufferIndex, start + byteCount, 255, useUnicode );
        bufferIndex += stringWireLength( nativeLanMan, bufferIndex );
        if (!extendedSecurity) {
            primaryDomain = readString(buffer, bufferIndex, start + byteCount, 255, useUnicode);
            bufferIndex += stringWireLength(primaryDomain, bufferIndex);
        }

        return bufferIndex - start;
    }
    public String toString() {
        String result = new String( "SmbComSessionSetupAndXResponse[" +
            super.toString() +
            ",isLoggedInAsGuest=" + isLoggedInAsGuest +
            ",nativeOs=" + nativeOs +
            ",nativeLanMan=" + nativeLanMan +
            ",primaryDomain=" + primaryDomain + "]" );
        return result;
    }
}

