/* jcifs smb client library in Java
 * Copyright (C) 2008  "Michael B. Allen" <jcifs at samba dot org>
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

import jcifs.ntlmssp.NtlmFlags;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.util.Encdec;
import jcifs.util.Hexdump;
import jcifs.util.LogStream;

/**
For initiating NTLM authentication (including NTLMv2). If you want to add NTLMv2 authentication support to something this is what you want to use. See the code for details. Note that JCIFS does not implement the acceptor side of NTLM authentication.
 */

public class NtlmContext {


    NtlmPasswordAuthentication auth;
    int ntlmsspFlags;
    String workstation;
    boolean isEstablished = false;
    byte[] serverChallenge = null;
    byte[] signingKey = null;
    String netbiosName = null;
    int state = 1;
    LogStream log;

    public NtlmContext(NtlmPasswordAuthentication auth, boolean doSigning) {
        this.auth = auth;
        this.ntlmsspFlags = ntlmsspFlags |
                NtlmFlags.NTLMSSP_REQUEST_TARGET |
                NtlmFlags.NTLMSSP_NEGOTIATE_NTLM2 |
                NtlmFlags.NTLMSSP_NEGOTIATE_128;
        if (doSigning) {
            this.ntlmsspFlags |= NtlmFlags.NTLMSSP_NEGOTIATE_SIGN |
                NtlmFlags.NTLMSSP_NEGOTIATE_ALWAYS_SIGN |
                NtlmFlags.NTLMSSP_NEGOTIATE_KEY_EXCH;
        }
        this.workstation = Type1Message.getDefaultWorkstation();
        log = LogStream.getInstance();
    }

    public String toString() {
        String ret = "NtlmContext[auth=" + auth +
            ",ntlmsspFlags=0x" + Hexdump.toHexString(ntlmsspFlags, 8) +
            ",workstation=" + workstation +
            ",isEstablished=" + isEstablished +
            ",state=" + state +
            ",serverChallenge=";
        if (serverChallenge == null) {
            ret += "null";
        } else {
            ret += Hexdump.toHexString(serverChallenge, 0, serverChallenge.length * 2);
        }
        ret += ",signingKey=";
        if (signingKey == null) {
            ret += "null";
        } else {
            ret += Hexdump.toHexString(signingKey, 0, signingKey.length * 2);
        }
        ret += "]";
        return ret;
    }

    public boolean isEstablished() {
        return isEstablished;
    }
    public byte[] getServerChallenge()
    {
        return serverChallenge;
    }
    public byte[] getSigningKey()
    {
        return signingKey;
    }
    public String getNetbiosName()
    {
        return netbiosName;
    }

    private String getNtlmsspListItem(byte[] type2token, int id0)
    {
        int ri = 58;

        for ( ;; ) {
            int id = Encdec.dec_uint16le(type2token, ri);
            int len = Encdec.dec_uint16le(type2token, ri + 2);
            ri += 4;
            if (id == 0 || (ri + len) > type2token.length) {
                break;
            } else if (id == id0) {
                try {
                    return new String(type2token, ri, len, SmbConstants.UNI_ENCODING);
                } catch (java.io.UnsupportedEncodingException uee) {
                    break;
                }
            }
            ri += len;
        }

        return null;
    }
    public byte[] initSecContext(byte[] token, int offset, int len) throws SmbException {
        switch (state) {
            case 1:
                Type1Message msg1 = new Type1Message(ntlmsspFlags, auth.getDomain(), workstation);
                token = msg1.toByteArray();

                if (log.level >= 4) {
                    log.println(msg1);
                    if (log.level >= 6)
                        Hexdump.hexdump(log, token, 0, token.length);
                }

                state++;
                break;
            case 2:
                try {
                    Type2Message msg2 = new Type2Message(token);

                    if (log.level >= 4) {
                        log.println(msg2);
                        if (log.level >= 6)
                            Hexdump.hexdump(log, token, 0, token.length);
                    }

                    serverChallenge = msg2.getChallenge();
                    ntlmsspFlags &= msg2.getFlags();

//                  netbiosName = getNtlmsspListItem(token, 0x0001);

                    Type3Message msg3 = new Type3Message(msg2,
                                auth.getPassword(),
                                auth.getDomain(),
                                auth.getUsername(),
                                workstation,
                                ntlmsspFlags);
                    token = msg3.toByteArray();

                    if (log.level >= 4) {
                        log.println(msg3);
                        if (log.level >= 6)
                            Hexdump.hexdump(log, token, 0, token.length);
                    }

                    if ((ntlmsspFlags & NtlmFlags.NTLMSSP_NEGOTIATE_SIGN) != 0)
                        signingKey = msg3.getMasterKey();

                    isEstablished = true;
                    state++;
                    break;
                } catch (Exception e) {
                    throw new SmbException(e.getMessage(), e);
                }
            default:
                throw new SmbException("Invalid state");
        }
        return token;
    }
}
