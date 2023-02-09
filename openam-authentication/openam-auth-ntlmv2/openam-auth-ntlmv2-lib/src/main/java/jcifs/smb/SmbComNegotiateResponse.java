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

import java.io.UnsupportedEncodingException;
import java.util.Date;

class SmbComNegotiateResponse extends ServerMessageBlock {

    int dialectIndex;
    SmbTransport.ServerData server;

    SmbComNegotiateResponse( SmbTransport.ServerData server ) {
        this.server = server;
    }

    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int readParameterWordsWireFormat( byte[] buffer,
                                    int bufferIndex ) {
        int start = bufferIndex;

        dialectIndex         = readInt2( buffer, bufferIndex ); bufferIndex += 2;
        if( dialectIndex > 10 ) {
            return bufferIndex - start;
        }
        server.securityMode  = buffer[bufferIndex++] & 0xFF;
        server.security      = server.securityMode & 0x01;
        server.encryptedPasswords = ( server.securityMode & 0x02 ) == 0x02;
        server.signaturesEnabled  = ( server.securityMode & 0x04 ) == 0x04;
        server.signaturesRequired = ( server.securityMode & 0x08 ) == 0x08;
        server.maxMpxCount   = readInt2( buffer, bufferIndex ); bufferIndex += 2;
        server.maxNumberVcs  = readInt2( buffer, bufferIndex ); bufferIndex += 2;
        server.maxBufferSize = readInt4( buffer, bufferIndex ); bufferIndex += 4;
        server.maxRawSize    = readInt4( buffer, bufferIndex ); bufferIndex += 4;
        server.sessionKey    = readInt4( buffer, bufferIndex ); bufferIndex += 4;
        server.capabilities  = readInt4( buffer, bufferIndex ); bufferIndex += 4;
        server.serverTime    = readTime( buffer, bufferIndex ); bufferIndex += 8;
        server.serverTimeZone = readInt2( buffer, bufferIndex ); bufferIndex += 2;
        server.encryptionKeyLength = buffer[bufferIndex++] & 0xFF;

        return bufferIndex - start;
    }
    int readBytesWireFormat( byte[] buffer,
                                    int bufferIndex ) {
        int start = bufferIndex;

        if ((server.capabilities & CAP_EXTENDED_SECURITY) == 0) {
            server.encryptionKey = new byte[server.encryptionKeyLength];
            System.arraycopy( buffer, bufferIndex,
                    server.encryptionKey, 0, server.encryptionKeyLength );
            bufferIndex += server.encryptionKeyLength;
            if( byteCount > server.encryptionKeyLength ) {
                int len = 0;
// TODO: we can use new string routine here
                try {
                    if(( flags2 & FLAGS2_UNICODE ) == FLAGS2_UNICODE ) {
                        while( buffer[bufferIndex + len] != (byte)0x00 ||
                                        buffer[bufferIndex + len + 1] != (byte)0x00 ) {
                            len += 2;
                            if( len > 256 ) {
                                throw new RuntimeException( "zero termination not found" );
                            }
                        }
                        server.oemDomainName = new String( buffer, bufferIndex,
                                len, UNI_ENCODING );
                    } else {
                        while( buffer[bufferIndex + len] != (byte)0x00 ) {
                            len++;
                            if( len > 256 ) {
                                throw new RuntimeException( "zero termination not found" );
                            }
                        }
                        server.oemDomainName = new String( buffer, bufferIndex,
                                len, ServerMessageBlock.OEM_ENCODING );
                    }
                } catch( UnsupportedEncodingException uee ) {
                    if( log.level > 1 )
                        uee.printStackTrace( log );
                }
                bufferIndex += len;
            } else {
                server.oemDomainName = new String();
            }
        } else {
            server.guid = new byte[16];
            System.arraycopy(buffer, bufferIndex, server.guid, 0, 16); 
            server.oemDomainName = new String();
            // ignore SPNEGO token for now ...
        }

        return bufferIndex - start;
    }
    public String toString() {
        return new String( "SmbComNegotiateResponse[" +
            super.toString() +
            ",wordCount="           + wordCount +
            ",dialectIndex="        + dialectIndex +
            ",securityMode=0x"      + Hexdump.toHexString( server.securityMode, 1 ) +
            ",security="            + ( server.security == SECURITY_SHARE ? "share" : "user" ) +
            ",encryptedPasswords="  + server.encryptedPasswords +
            ",maxMpxCount="         + server.maxMpxCount +
            ",maxNumberVcs="        + server.maxNumberVcs +
            ",maxBufferSize="       + server.maxBufferSize +
            ",maxRawSize="          + server.maxRawSize +
            ",sessionKey=0x"        + Hexdump.toHexString( server.sessionKey, 8 ) +
            ",capabilities=0x"      + Hexdump.toHexString( server.capabilities, 8 ) +
            ",serverTime="          + new Date( server.serverTime ) +
            ",serverTimeZone="      + server.serverTimeZone +
            ",encryptionKeyLength=" + server.encryptionKeyLength +
            ",byteCount="           + byteCount +
            ",oemDomainName="       + server.oemDomainName + "]" );
    }
}

