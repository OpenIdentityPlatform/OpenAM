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

class SmbComSessionSetupAndX extends AndXServerMessageBlock {

    private static final int BATCH_LIMIT =
            Config.getInt( "jcifs.smb.client.SessionSetupAndX.TreeConnectAndX", 1 );
    private static final boolean DISABLE_PLAIN_TEXT_PASSWORDS =
            Config.getBoolean( "jcifs.smb.client.disablePlainTextPasswords", true );

    private byte[] lmHash, ntHash, blob = null;
    private int sessionKey, capabilities;
    private String accountName, primaryDomain;

    SmbSession session;
    Object cred;

    SmbComSessionSetupAndX( SmbSession session, ServerMessageBlock andx, Object cred ) throws SmbException {
        super( andx );
        command = SMB_COM_SESSION_SETUP_ANDX;
        this.session = session;
        this.cred = cred;

        sessionKey = session.transport.sessionKey;
        capabilities = session.transport.capabilities;

        if (session.transport.server.security == SECURITY_USER) {
            if (cred instanceof NtlmPasswordAuthentication) {
                NtlmPasswordAuthentication auth = (NtlmPasswordAuthentication)cred;

                if (auth == NtlmPasswordAuthentication.ANONYMOUS) {
                    lmHash = new byte[0];
                    ntHash = new byte[0];
                    capabilities &= ~SmbConstants.CAP_EXTENDED_SECURITY;
                } else if (session.transport.server.encryptedPasswords) {
                    lmHash = auth.getAnsiHash( session.transport.server.encryptionKey );
                    ntHash = auth.getUnicodeHash( session.transport.server.encryptionKey );
                    // prohibit HTTP auth attempts for the null session
                    if (lmHash.length == 0 && ntHash.length == 0) {
                        throw new RuntimeException("Null setup prohibited.");
                    }
                } else if( DISABLE_PLAIN_TEXT_PASSWORDS ) {
                    throw new RuntimeException( "Plain text passwords are disabled" );
                } else if( useUnicode ) {
                    // plain text
                    String password = auth.getPassword();
                    lmHash = new byte[0];
                    ntHash = new byte[(password.length() + 1) * 2];
                    writeString( password, ntHash, 0 );
                } else {
                    // plain text
                    String password = auth.getPassword();
                    lmHash = new byte[(password.length() + 1) * 2];
                    ntHash = new byte[0];
                    writeString( password, lmHash, 0 );
                }
                accountName = auth.username;
                if (useUnicode)
                    accountName = accountName.toUpperCase();
                primaryDomain = auth.domain.toUpperCase();
            } else if (cred instanceof byte[]) {
                blob = (byte[])cred;
            } else {
                throw new SmbException("Unsupported credential type");
            }
        } else if (session.transport.server.security == SECURITY_SHARE) {
            if (cred instanceof NtlmPasswordAuthentication) {
                NtlmPasswordAuthentication auth = (NtlmPasswordAuthentication)cred;
                lmHash = new byte[0];
                ntHash = new byte[0];
                accountName = auth.username;
                if (useUnicode)
                    accountName = accountName.toUpperCase();
                primaryDomain = auth.domain.toUpperCase();
            } else {
                throw new SmbException("Unsupported credential type");
            }
        } else {
            throw new SmbException("Unsupported");
        }
    }

    int getBatchLimit( byte command ) {
        return command == SMB_COM_TREE_CONNECT_ANDX ? BATCH_LIMIT : 0;
    }
    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        writeInt2( session.transport.snd_buf_size, dst, dstIndex );
        dstIndex += 2;
        writeInt2( session.transport.maxMpxCount, dst, dstIndex );
        dstIndex += 2;
        writeInt2( session.transport.VC_NUMBER, dst, dstIndex );
        dstIndex += 2;
        writeInt4( sessionKey, dst, dstIndex );
        dstIndex += 4;
        if (blob != null) {
            writeInt2( blob.length, dst, dstIndex );
            dstIndex += 2;
        } else {
            writeInt2( lmHash.length, dst, dstIndex );
            dstIndex += 2;
            writeInt2( ntHash.length, dst, dstIndex );
            dstIndex += 2;
        }
        dst[dstIndex++] = (byte)0x00;
        dst[dstIndex++] = (byte)0x00;
        dst[dstIndex++] = (byte)0x00;
        dst[dstIndex++] = (byte)0x00;
        writeInt4( capabilities, dst, dstIndex );
        dstIndex += 4;

        return dstIndex - start;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        if (blob != null) {
            System.arraycopy(blob, 0, dst, dstIndex, blob.length );
            dstIndex += blob.length;
        } else {
            System.arraycopy( lmHash, 0, dst, dstIndex, lmHash.length );
            dstIndex += lmHash.length;
            System.arraycopy( ntHash, 0, dst, dstIndex, ntHash.length );
            dstIndex += ntHash.length;
    
            dstIndex += writeString( accountName, dst, dstIndex );
            dstIndex += writeString( primaryDomain, dst, dstIndex );
        }
        dstIndex += writeString( session.transport.NATIVE_OS, dst, dstIndex );
        dstIndex += writeString( session.transport.NATIVE_LANMAN, dst, dstIndex );

        return dstIndex - start;
    }
    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    int readBytesWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    public String toString() {
        String result = new String( "SmbComSessionSetupAndX[" +
            super.toString() +
            ",snd_buf_size=" + session.transport.snd_buf_size +
            ",maxMpxCount=" + session.transport.maxMpxCount +
            ",VC_NUMBER=" + session.transport.VC_NUMBER +
            ",sessionKey=" + sessionKey +
            ",lmHash.length=" + (lmHash == null ? 0 : lmHash.length) +
            ",ntHash.length=" + (ntHash == null ? 0 : ntHash.length) +
            ",capabilities=" + capabilities +
            ",accountName=" + accountName +
            ",primaryDomain=" + primaryDomain +
            ",NATIVE_OS=" + session.transport.NATIVE_OS +
            ",NATIVE_LANMAN=" + session.transport.NATIVE_LANMAN + "]" );
        return result;
    }
}
