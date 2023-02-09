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

import java.io.UnsupportedEncodingException;

class SmbComTreeConnectAndX extends AndXServerMessageBlock {

    private static final boolean DISABLE_PLAIN_TEXT_PASSWORDS =
            Config.getBoolean( "jcifs.smb.client.disablePlainTextPasswords", true );

    private SmbSession session;
    private boolean disconnectTid = false;
    private String service;
    private byte[] password;
    private int passwordLength;
    String path;

    /* batchLimits indecies
     *
     * 0 = SMB_COM_CHECK_DIRECTORY
     * 2 = SMB_COM_CREATE_DIRECTORY
     * 3 = SMB_COM_DELETE
     * 4 = SMB_COM_DELETE_DIRECTORY
     * 5 = SMB_COM_OPEN_ANDX
     * 6 = SMB_COM_RENAME
     * 7 = SMB_COM_TRANSACTION
     * 8 = SMB_COM_QUERY_INFORMATION
     */

    /* All batch limits are single batch only until further notice
     */

    private static byte[] batchLimits = {
        1, 1, 1, 1, 1, 1, 1, 1, 0
    };

    static {
        String s;

        if(( s = Config.getProperty( "jcifs.smb.client.TreeConnectAndX.CheckDirectory" )) != null ) {
            batchLimits[0] = Byte.parseByte( s );
        }
        if(( s = Config.getProperty( "jcifs.smb.client.TreeConnectAndX.CreateDirectory" )) != null ) {
            batchLimits[2] = Byte.parseByte( s );
        }
        if(( s = Config.getProperty( "jcifs.smb.client.TreeConnectAndX.Delete" )) != null ) {
            batchLimits[3] = Byte.parseByte( s );
        }
        if(( s = Config.getProperty( "jcifs.smb.client.TreeConnectAndX.DeleteDirectory" )) != null ) {
            batchLimits[4] = Byte.parseByte( s );
        }
        if(( s = Config.getProperty( "jcifs.smb.client.TreeConnectAndX.OpenAndX" )) != null ) {
            batchLimits[5] = Byte.parseByte( s );
        }
        if(( s = Config.getProperty( "jcifs.smb.client.TreeConnectAndX.Rename" )) != null ) {
            batchLimits[6] = Byte.parseByte( s );
        }
        if(( s = Config.getProperty( "jcifs.smb.client.TreeConnectAndX.Transaction" )) != null ) {
            batchLimits[7] = Byte.parseByte( s );
        }
        if(( s = Config.getProperty( "jcifs.smb.client.TreeConnectAndX.QueryInformation" )) != null ) {
            batchLimits[8] = Byte.parseByte( s );
        }
    }

    SmbComTreeConnectAndX( SmbSession session, String path,
                                String service, ServerMessageBlock andx ) {
        super( andx );
        this.session = session;
        this.path = path;
        this.service = service;
        command = SMB_COM_TREE_CONNECT_ANDX;
    }

    int getBatchLimit( byte command ) {
        int c = (int)( command & 0xFF );
        // why isn't this just return batchLimits[c]?
        switch( c ) {
            case SMB_COM_CHECK_DIRECTORY:
                return batchLimits[0];
            case SMB_COM_CREATE_DIRECTORY:
                return batchLimits[2];
            case SMB_COM_DELETE:
                return batchLimits[3];
            case SMB_COM_DELETE_DIRECTORY:
                return batchLimits[4];
            case SMB_COM_OPEN_ANDX:
                return batchLimits[5];
            case SMB_COM_RENAME:
                return batchLimits[6];
            case SMB_COM_TRANSACTION:
                return batchLimits[7];
            case SMB_COM_QUERY_INFORMATION:
                return batchLimits[8];
        }
        return 0;
    }

    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {

        if( session.transport.server.security == SECURITY_SHARE &&
                        ( session.auth.hashesExternal ||
                        session.auth.password.length() > 0 )) {

            if( session.transport.server.encryptedPasswords ) {
                // encrypted
                password = session.auth.getAnsiHash( session.transport.server.encryptionKey );
                passwordLength = password.length;
            } else if( DISABLE_PLAIN_TEXT_PASSWORDS ) {
                throw new RuntimeException( "Plain text passwords are disabled" );
            } else {
                // plain text
                password = new byte[(session.auth.password.length() + 1) * 2];
                passwordLength = writeString( session.auth.password, password, 0 );
            }
        } else {
            // no password in tree connect
            passwordLength = 1;
        }

        dst[dstIndex++] = disconnectTid ? (byte)0x01 : (byte)0x00;
        dst[dstIndex++] = (byte)0x00;
        writeInt2( passwordLength, dst, dstIndex );
        return 4;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        if( session.transport.server.security == SECURITY_SHARE &&
                        ( session.auth.hashesExternal ||
                        session.auth.password.length() > 0 )) {
            System.arraycopy( password, 0, dst, dstIndex, passwordLength );
            dstIndex += passwordLength;
        } else {
            // no password in tree connect
            dst[dstIndex++] = (byte)0x00;
        }
        dstIndex += writeString( path, dst, dstIndex );
        try {
            System.arraycopy( service.getBytes( "ASCII" ), 0, dst, dstIndex, service.length() );
        } catch( UnsupportedEncodingException uee ) {
            return 0;
        }
        dstIndex += service.length();
        dst[dstIndex++] = (byte)'\0';

        return dstIndex - start;
    }
    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    int readBytesWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    public String toString() {
        String result = new String( "SmbComTreeConnectAndX[" +
            super.toString() +
            ",disconnectTid=" + disconnectTid +
            ",passwordLength=" + passwordLength +
            ",password=" + Hexdump.toHexString( password, passwordLength, 0 ) +
            ",path=" + path +
            ",service=" + service + "]" );
        return result;
    }
}

