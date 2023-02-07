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

class SmbComNTCreateAndX extends AndXServerMessageBlock {

    // share access specified in SmbFile

    // create disposition

    /* Creates a new file or supersedes the existing one
     */

    static final int FILE_SUPERSEDE    = 0x0;

    /* Open the file or fail if it does not exist
     * aka OPEN_EXISTING
     */

    static final int FILE_OPEN         = 0x1;

    /* Create the file or fail if it does not exist
     * aka CREATE_NEW
     */

    static final int FILE_CREATE       = 0x2;

    /* Open the file or create it if it does not exist
     * aka OPEN_ALWAYS
     */

    static final int FILE_OPEN_IF      = 0x3;

    /* Open the file and overwrite it's contents or fail if it does not exist
     * aka TRUNCATE_EXISTING
     */

    static final int FILE_OVERWRITE    = 0x4;

    /* Open the file and overwrite it's contents or create it if it does not exist
     * aka CREATE_ALWAYS (according to the wire when calling CreateFile)
     */

    static final int FILE_OVERWRITE_IF = 0x5;


    // create options
    static final int FILE_WRITE_THROUGH           = 0x00000002;
    static final int FILE_SEQUENTIAL_ONLY         = 0x00000004;
    static final int FILE_SYNCHRONOUS_IO_ALERT    = 0x00000010;
    static final int FILE_SYNCHRONOUS_IO_NONALERT = 0x00000020;

    // security flags
    static final int SECURITY_CONTEXT_TRACKING = 0x01;
    static final int SECURITY_EFFECTIVE_ONLY   = 0x02;

    private int rootDirectoryFid,
        extFileAttributes,
        shareAccess,
        createDisposition,
        createOptions,
        impersonationLevel;
    private long allocationSize;
    private byte securityFlags;
    private int namelen_index;

int flags0, desiredAccess;

    SmbComNTCreateAndX( String name, int flags,
                int access,
                int shareAccess,
                int extFileAttributes,
                int createOptions,
                ServerMessageBlock andx ) {
        super( andx );
        this.path = name;
        command = SMB_COM_NT_CREATE_ANDX;

        desiredAccess = access;
        desiredAccess |= FILE_READ_DATA | FILE_READ_EA | FILE_READ_ATTRIBUTES;

        // extFileAttributes
        this.extFileAttributes = extFileAttributes;

        // shareAccess
        this.shareAccess = shareAccess;

        // createDisposition
        if(( flags & SmbFile.O_TRUNC ) == SmbFile.O_TRUNC ) {
            // truncate the file
            if(( flags & SmbFile.O_CREAT ) == SmbFile.O_CREAT ) {
                // create it if necessary
                createDisposition = FILE_OVERWRITE_IF;
            } else {
                createDisposition = FILE_OVERWRITE;
            }
        } else {
            // don't truncate the file
            if(( flags & SmbFile.O_CREAT ) == SmbFile.O_CREAT ) {
                // create it if necessary
                if ((flags & SmbFile.O_EXCL ) == SmbFile.O_EXCL ) {
                    // fail if already exists
                    createDisposition = FILE_CREATE;
                } else {
                    createDisposition = FILE_OPEN_IF;
                }
            } else {
                createDisposition = FILE_OPEN;
            }
        }

        if ((createOptions & 0x0001) == 0) {
            this.createOptions = createOptions | 0x0040;
        } else {
            this.createOptions = createOptions;
        }
        impersonationLevel = 0x02; // As seen on NT :~)
        securityFlags = (byte)0x03; // SECURITY_CONTEXT_TRACKING | SECURITY_EFFECTIVE_ONLY
    }

    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        dst[dstIndex++] = (byte)0x00;
        // name length without counting null termination
        namelen_index = dstIndex;
        dstIndex += 2;
        writeInt4( flags0, dst, dstIndex );
        dstIndex += 4;
        writeInt4( rootDirectoryFid, dst, dstIndex );
        dstIndex += 4;
        writeInt4( desiredAccess, dst, dstIndex );
        dstIndex += 4;
        writeInt8( allocationSize, dst, dstIndex );
        dstIndex += 8;
        writeInt4( extFileAttributes, dst, dstIndex );
        dstIndex += 4;
        writeInt4( shareAccess, dst, dstIndex );
        dstIndex += 4;
        writeInt4( createDisposition, dst, dstIndex );
        dstIndex += 4;
        writeInt4( createOptions, dst, dstIndex );
        dstIndex += 4;
        writeInt4( impersonationLevel, dst, dstIndex );
        dstIndex += 4;
        dst[dstIndex++] = securityFlags;

        return dstIndex - start;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        int n;
        n = writeString( path, dst, dstIndex );
        writeInt2( ( useUnicode ? path.length() * 2 : n ), dst, namelen_index );
        return n;
    }
    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    int readBytesWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    public String toString() {
        return new String( "SmbComNTCreateAndX[" +
            super.toString() +
            ",flags=0x" + Hexdump.toHexString( flags0, 2 ) +
            ",rootDirectoryFid=" + rootDirectoryFid +
            ",desiredAccess=0x" + Hexdump.toHexString( desiredAccess, 4 ) +
            ",allocationSize=" + allocationSize +
            ",extFileAttributes=0x" + Hexdump.toHexString( extFileAttributes, 4 ) +
            ",shareAccess=0x" + Hexdump.toHexString( shareAccess, 4 ) +
            ",createDisposition=0x" + Hexdump.toHexString( createDisposition, 4 ) +
            ",createOptions=0x" + Hexdump.toHexString( createOptions, 8 ) +
            ",impersonationLevel=0x" + Hexdump.toHexString( impersonationLevel, 4 ) +
            ",securityFlags=0x" + Hexdump.toHexString( securityFlags, 2 ) +
            ",name=" + path + "]" );
    }
}
