/* jcifs smb client library in Java
 * Copyright (C) 2002  "Michael B. Allen" <jcifs at samba dot org>
 *                 "Eric Glass" <jcifs at samba dot org>
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

package jcifs.ntlmssp;

import jcifs.Config;
import jcifs.netbios.NbtAddress;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Represents an NTLMSSP Type-2 message.
 */
public class Type2Message extends NtlmMessage {

    private static final int DEFAULT_FLAGS;

    private static final String DEFAULT_DOMAIN;

    private static final byte[] DEFAULT_TARGET_INFORMATION;

    private byte[] challenge;

    private String target;

    private byte[] context;

    private byte[] targetInformation;

    static {
        DEFAULT_FLAGS = NTLMSSP_NEGOTIATE_NTLM |
                (Config.getBoolean("jcifs.smb.client.useUnicode", true) ?
                        NTLMSSP_NEGOTIATE_UNICODE : NTLMSSP_NEGOTIATE_OEM);
        DEFAULT_DOMAIN = Config.getProperty("jcifs.smb.client.domain", null);
        byte[] domain = new byte[0];
        if (DEFAULT_DOMAIN != null) {
            try {
                domain = DEFAULT_DOMAIN.getBytes(UNI_ENCODING);
            } catch (IOException ex) { }
        }
        int domainLength = domain.length;
        byte[] server = new byte[0];
        try {
            String host = NbtAddress.getLocalHost().getHostName();
            if (host != null) {
                try {
                    server = host.getBytes(UNI_ENCODING);
                } catch (IOException ex) { }
            }
        } catch (UnknownHostException ex) { }
        int serverLength = server.length;
        byte[] targetInfo = new byte[(domainLength > 0 ? domainLength + 4 : 0) +
                (serverLength > 0 ? serverLength + 4 : 0) + 4];
        int offset = 0;
        if (domainLength > 0) {
            writeUShort(targetInfo, offset, 2);
            offset += 2;
            writeUShort(targetInfo, offset, domainLength);
            offset += 2;
            System.arraycopy(domain, 0, targetInfo, offset, domainLength);
            offset += domainLength;
        }
        if (serverLength > 0) {
            writeUShort(targetInfo, offset, 1);
            offset += 2;
            writeUShort(targetInfo, offset, serverLength);
            offset += 2;
            System.arraycopy(server, 0, targetInfo, offset, serverLength);
        }
        DEFAULT_TARGET_INFORMATION = targetInfo;
    }

    /**
     * Creates a Type-2 message using default values from the current
     * environment.
     */
    public Type2Message() {
        this(getDefaultFlags(), null, null);
    }

    /**
     * Creates a Type-2 message in response to the given Type-1 message
     * using default values from the current environment.
     *
     * @param type1 The Type-1 message which this represents a response to.
     */
    public Type2Message(Type1Message type1) {
        this(type1, null, null);
    }

    /**
     * Creates a Type-2 message in response to the given Type-1 message.
     *
     * @param type1 The Type-1 message which this represents a response to.
     * @param challenge The challenge from the domain controller/server.
     * @param target The authentication target.
     */
    public Type2Message(Type1Message type1, byte[] challenge, String target) {
        this(getDefaultFlags(type1), challenge, (type1 != null &&
                target == null && type1.getFlag(NTLMSSP_REQUEST_TARGET)) ?
                        getDefaultDomain() : target);
    }

    /**
     * Creates a Type-2 message with the specified parameters.
     *
     * @param flags The flags to apply to this message.
     * @param challenge The challenge from the domain controller/server.
     * @param target The authentication target.
     */
    public Type2Message(int flags, byte[] challenge, String target) {
        setFlags(flags);
        setChallenge(challenge);
        setTarget(target);
        if (target != null) setTargetInformation(getDefaultTargetInformation());
    }

    /**
     * Creates a Type-2 message using the given raw Type-2 material.
     *
     * @param material The raw Type-2 material used to construct this message.
     * @throws IOException If an error occurs while parsing the material.
     */
    public Type2Message(byte[] material) throws IOException {
        parse(material);
    }

    /**
     * Returns the challenge for this message.
     *
     * @return A <code>byte[]</code> containing the challenge.
     */
    public byte[] getChallenge() {
        return challenge;
    }

    /**
     * Sets the challenge for this message.
     *
     * @param challenge The challenge from the domain controller/server.
     */
    public void setChallenge(byte[] challenge) {
        this.challenge = challenge;
    }

    /**
     * Returns the authentication target.
     *
     * @return A <code>String</code> containing the authentication target.
     */
    public String getTarget() {
        return target;
    }

    /**
     * Sets the authentication target.
     *
     * @param target The authentication target.
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Returns the target information block.
     *
     * @return A <code>byte[]</code> containing the target information block.
     * The target information block is used by the client to create an
     * NTLMv2 response.
     */ 
    public byte[] getTargetInformation() {
        return targetInformation;
    }

    /**
     * Sets the target information block.
     * The target information block is used by the client to create
     * an NTLMv2 response.
     * 
     * @param targetInformation The target information block.
     */
    public void setTargetInformation(byte[] targetInformation) {
        this.targetInformation = targetInformation;
    }

    /**
     * Returns the local security context.
     *
     * @return A <code>byte[]</code> containing the local security
     * context.  This is used by the client to negotiate local
     * authentication.
     */
    public byte[] getContext() {
        return context;
    }

    /**
     * Sets the local security context.  This is used by the client
     * to negotiate local authentication.
     *
     * @param context The local security context.
     */
    public void setContext(byte[] context) {
        this.context = context;
    }

    public byte[] toByteArray() {
        try {
            String targetName = getTarget();
            byte[] challenge = getChallenge();
            byte[] context = getContext();
            byte[] targetInformation = getTargetInformation();
            int flags = getFlags();
            byte[] target = new byte[0];
            if ((flags & NTLMSSP_REQUEST_TARGET) != 0) {
                if (targetName != null && targetName.length() != 0) {
                    target = (flags & NTLMSSP_NEGOTIATE_UNICODE) != 0 ?
                            targetName.getBytes(UNI_ENCODING) :
                            targetName.toUpperCase().getBytes(getOEMEncoding());
                } else {
                    flags &= (0xffffffff ^ NTLMSSP_REQUEST_TARGET);
                }
            }
            if (targetInformation != null) {
                flags |= NTLMSSP_NEGOTIATE_TARGET_INFO;
                // empty context is needed for padding when t.i. is supplied.
                if (context == null) context = new byte[8];
            }
            int data = 32;
            if (context != null) data += 8;
            if (targetInformation != null) data += 8;
            byte[] type2 = new byte[data + target.length +
                    (targetInformation != null ? targetInformation.length : 0)];
            System.arraycopy(NTLMSSP_SIGNATURE, 0, type2, 0, 8);
            writeULong(type2, 8, 2);
            writeSecurityBuffer(type2, 12, data, target);
            writeULong(type2, 20, flags);
            System.arraycopy(challenge != null ? challenge : new byte[8], 0,
                    type2, 24, 8);
            if (context != null) System.arraycopy(context, 0, type2, 32, 8);
            if (targetInformation != null) {
                writeSecurityBuffer(type2, 40, data + target.length,
                        targetInformation);
            }
            return type2;
        } catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage());
        }
    }

    public String toString() {
        String target = getTarget();
        byte[] challenge = getChallenge();
        byte[] context = getContext();
        byte[] targetInformation = getTargetInformation();

        return "Type2Message[target=" + target +
            ",challenge=" + (challenge == null ? "null" : "<" + challenge.length + " bytes>") +
            ",context=" + (context == null ? "null" : "<" + context.length + " bytes>") +
            ",targetInformation=" + (targetInformation == null ? "null" : "<" + targetInformation.length + " bytes>") +
            ",flags=0x" + jcifs.util.Hexdump.toHexString(getFlags(), 8) + "]";
    }

    /**
     * Returns the default flags for a generic Type-2 message in the
     * current environment.
     *
     * @return An <code>int</code> containing the default flags.
     */
    public static int getDefaultFlags() {
        return DEFAULT_FLAGS;
    }

    /**
     * Returns the default flags for a Type-2 message created in response
     * to the given Type-1 message in the current environment.
     *
     * @return An <code>int</code> containing the default flags.
     */
    public static int getDefaultFlags(Type1Message type1) {
        if (type1 == null) return DEFAULT_FLAGS;
        int flags = NTLMSSP_NEGOTIATE_NTLM;
        int type1Flags = type1.getFlags();
        flags |= ((type1Flags & NTLMSSP_NEGOTIATE_UNICODE) != 0) ?
                NTLMSSP_NEGOTIATE_UNICODE : NTLMSSP_NEGOTIATE_OEM;
        if ((type1Flags & NTLMSSP_REQUEST_TARGET) != 0) {
            String domain = getDefaultDomain();
            if (domain != null) {
                flags |= NTLMSSP_REQUEST_TARGET | NTLMSSP_TARGET_TYPE_DOMAIN;
            }
        }
        return flags;
    }

    /**
     * Returns the default domain from the current environment.
     *
     * @return A <code>String</code> containing the domain.
     */
    public static String getDefaultDomain() {
        return DEFAULT_DOMAIN;
    }

    public static byte[] getDefaultTargetInformation() {
        return DEFAULT_TARGET_INFORMATION;
    }

    private void parse(byte[] material) throws IOException {
        for (int i = 0; i < 8; i++) {
            if (material[i] != NTLMSSP_SIGNATURE[i]) {
                throw new IOException("Not an NTLMSSP message.");
            }
        }
        if (readULong(material, 8) != 2) {
            throw new IOException("Not a Type 2 message.");
        }
        int flags = readULong(material, 20);
        setFlags(flags);
        String target = null;
        byte[] bytes = readSecurityBuffer(material, 12);
        if (bytes.length != 0) {
            target = new String(bytes,
                    ((flags & NTLMSSP_NEGOTIATE_UNICODE) != 0) ?
                            UNI_ENCODING : getOEMEncoding());
        }
        setTarget(target);
        for (int i = 24; i < 32; i++) {
            if (material[i] != 0) {
                byte[] challenge = new byte[8];
                System.arraycopy(material, 24, challenge, 0, 8);
                setChallenge(challenge);
                break;
            }
        }
        int offset = readULong(material, 16); // offset of targetname start
        if (offset == 32 || material.length == 32) return;
        for (int i = 32; i < 40; i++) {
            if (material[i] != 0) {
                byte[] context = new byte[8];
                System.arraycopy(material, 32, context, 0, 8);
                setContext(context);
                break;
            }
        }
        if (offset == 40 || material.length == 40) return;
        bytes = readSecurityBuffer(material, 40);
        if (bytes.length != 0) setTargetInformation(bytes);
    }

}
