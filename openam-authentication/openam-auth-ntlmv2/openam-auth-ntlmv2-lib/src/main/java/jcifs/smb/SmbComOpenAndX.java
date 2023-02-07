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
import jcifs.util.Hexdump;

import java.util.Date;

class SmbComOpenAndX extends AndXServerMessageBlock {

    // flags (not the same as flags constructor argument)
    private static final int FLAGS_RETURN_ADDITIONAL_INFO = 0x01;
    private static final int FLAGS_REQUEST_OPLOCK         = 0x02;
    private static final int FLAGS_REQUEST_BATCH_OPLOCK   = 0x04;

    // Access Mode Encoding for desiredAccess
    private static final int SHARING_COMPATIBILITY           = 0x00;
    private static final int SHARING_DENY_READ_WRITE_EXECUTE = 0x10;
    private static final int SHARING_DENY_WRITE              = 0x20;
    private static final int SHARING_DENY_READ_EXECUTE       = 0x30;
    private static final int SHARING_DENY_NONE               = 0x40;

    private static final int DO_NOT_CACHE  = 0x1000; // bit 12
    private static final int WRITE_THROUGH = 0x4000; // bit 14

    private static final int OPEN_FN_CREATE = 0x10;
    private static final int OPEN_FN_FAIL_IF_EXISTS = 0x00;
    private static final int OPEN_FN_OPEN = 0x01;
    private static final int OPEN_FN_TRUNC = 0x02;

    private static final int BATCH_LIMIT = Config.getInt( "jcifs.smb.client.OpenAndX.ReadAndX", 1 );

    int flags,
        desiredAccess,
        searchAttributes,
        fileAttributes,
        creationTime,
        openFunction,
        allocationSize;

    // flags is NOT the same as flags member

    SmbComOpenAndX( String fileName, int access, int flags, ServerMessageBlock andx ) {
        super( andx );
        this.path = fileName;
        command = SMB_COM_OPEN_ANDX;

        desiredAccess = access & 0x3;
        if( desiredAccess == 0x3 ) {
            desiredAccess = 0x2; /* Mmm, I thought 0x03 was RDWR */
        }
        desiredAccess |= SHARING_DENY_NONE;
        desiredAccess &= ~0x1; // Win98 doesn't like GENERIC_READ ?! -- get Access Denied.

        // searchAttributes
        searchAttributes = ATTR_DIRECTORY | ATTR_HIDDEN | ATTR_SYSTEM;

        // fileAttributes
        fileAttributes = 0;

        // openFunction
        if(( flags & SmbFile.O_TRUNC ) == SmbFile.O_TRUNC ) {
            // truncate the file
            if(( flags & SmbFile.O_CREAT ) == SmbFile.O_CREAT ) {
                // create it if necessary
                openFunction = OPEN_FN_TRUNC | OPEN_FN_CREATE;
            } else {
                openFunction = OPEN_FN_TRUNC;
            }
        } else {
            // don't truncate the file
            if(( flags & SmbFile.O_CREAT ) == SmbFile.O_CREAT ) {
                // create it if necessary
                if(( flags & SmbFile.O_EXCL ) == SmbFile.O_EXCL ) {
                    // fail if already exists
                    openFunction = OPEN_FN_CREATE | OPEN_FN_FAIL_IF_EXISTS;
                } else {
                    openFunction = OPEN_FN_CREATE | OPEN_FN_OPEN;
                }
            } else {
                openFunction = OPEN_FN_OPEN;
            }
        }
    }

    int getBatchLimit( byte command ) {
        return command == SMB_COM_READ_ANDX ? BATCH_LIMIT : 0;
    }
    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        writeInt2( flags, dst, dstIndex );
        dstIndex += 2;
        writeInt2( desiredAccess, dst, dstIndex );
        dstIndex += 2;
        writeInt2( searchAttributes, dst, dstIndex );
        dstIndex += 2;
        writeInt2( fileAttributes, dst, dstIndex );
        dstIndex += 2;
        creationTime = 0;
        writeInt4( creationTime, dst, dstIndex );
        dstIndex += 4;
        writeInt2( openFunction, dst, dstIndex );
        dstIndex += 2;
        writeInt4( allocationSize, dst, dstIndex );
        dstIndex += 4;
        for( int i = 0; i < 8; i++ ) {
            dst[dstIndex++] = 0x00;
        }

        return dstIndex - start;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        if( useUnicode ) {
            dst[dstIndex++] = (byte)'\0';
        }
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
        return new String( "SmbComOpenAndX[" +
            super.toString() +
            ",flags=0x" + Hexdump.toHexString( flags, 2 ) +
            ",desiredAccess=0x" + Hexdump.toHexString( desiredAccess, 4 ) +
            ",searchAttributes=0x" + Hexdump.toHexString( searchAttributes, 4 ) +
            ",fileAttributes=0x" + Hexdump.toHexString( fileAttributes, 4 ) +
            ",creationTime=" + new Date( creationTime ) +
            ",openFunction=0x" + Hexdump.toHexString( openFunction, 2 ) +
            ",allocationSize=" + allocationSize +
            ",fileName=" + path + "]" );
    }
}
