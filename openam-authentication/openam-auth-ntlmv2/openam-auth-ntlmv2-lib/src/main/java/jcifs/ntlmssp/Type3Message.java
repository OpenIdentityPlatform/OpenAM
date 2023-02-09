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
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.util.HMACT64;
import jcifs.util.MD4;
import jcifs.util.RC4;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.SecureRandom;

/**
 * Represents an NTLMSSP Type-3 message.
 */
public class Type3Message extends NtlmMessage {

    static final long MILLISECONDS_BETWEEN_1970_AND_1601 = 11644473600000L;

    private static final int DEFAULT_FLAGS;

    private static final String DEFAULT_DOMAIN;

    private static final String DEFAULT_USER;

    private static final String DEFAULT_PASSWORD;

    private static final String DEFAULT_WORKSTATION;

    private static final int LM_COMPATIBILITY;

    private static final SecureRandom RANDOM = new SecureRandom();

    private byte[] lmResponse;

    private byte[] ntResponse;

    private String domain;

    private String user;

    private String workstation;

    private byte[] masterKey = null;
    private byte[] sessionKey = null;

    static {
        DEFAULT_FLAGS = NTLMSSP_NEGOTIATE_NTLM |
                (Config.getBoolean("jcifs.smb.client.useUnicode", true) ?
                        NTLMSSP_NEGOTIATE_UNICODE : NTLMSSP_NEGOTIATE_OEM);
        DEFAULT_DOMAIN = Config.getProperty("jcifs.smb.client.domain", null);
        DEFAULT_USER = Config.getProperty("jcifs.smb.client.username", null);
        DEFAULT_PASSWORD = Config.getProperty("jcifs.smb.client.password",
                null);
        String defaultWorkstation = null;
        try {
            defaultWorkstation = NbtAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) { }
        DEFAULT_WORKSTATION = defaultWorkstation;
        LM_COMPATIBILITY = Config.getInt("jcifs.smb.lmCompatibility", 3);
    }

    /**
     * Creates a Type-3 message using default values from the current
     * environment.
     */
    public Type3Message() {
        setFlags(getDefaultFlags());
        setDomain(getDefaultDomain());
        setUser(getDefaultUser());
        setWorkstation(getDefaultWorkstation());
    }

    /**
     * Creates a Type-3 message in response to the given Type-2 message
     * using default values from the current environment.
     *
     * @param type2 The Type-2 message which this represents a response to.
     */
    public Type3Message(Type2Message type2) {
        setFlags(getDefaultFlags(type2));
        setWorkstation(getDefaultWorkstation());
        String domain = getDefaultDomain();
        setDomain(domain);
        String user = getDefaultUser();
        setUser(user);
        String password = getDefaultPassword();
        switch (LM_COMPATIBILITY) {
        case 0:
        case 1:
            setLMResponse(getLMResponse(type2, password));
            setNTResponse(getNTResponse(type2, password));
            break;
        case 2:
            byte[] nt = getNTResponse(type2, password);
            setLMResponse(nt);
            setNTResponse(nt);
            break;
        case 3:
        case 4:
        case 5:
            byte[] clientChallenge = new byte[8];
            RANDOM.nextBytes(clientChallenge);
            setLMResponse(getLMv2Response(type2, domain, user, password,
                    clientChallenge));
            /*
            setNTResponse(getNTLMv2Response(type2, domain, user, password,
                    clientChallenge));
            */
            break;
        default:
            setLMResponse(getLMResponse(type2, password));
            setNTResponse(getNTResponse(type2, password));
        }
    }

    /**
     * Creates a Type-3 message in response to the given Type-2 message.
     *
     * @param type2 The Type-2 message which this represents a response to.
     * @param password The password to use when constructing the response.
     * @param domain The domain in which the user has an account.
     * @param user The username for the authenticating user.
     * @param workstation The workstation from which authentication is
     * taking place.
     */
    public Type3Message(Type2Message type2, String password, String domain,
            String user, String workstation, int flags) {
        setFlags(flags | getDefaultFlags(type2));
        if (workstation == null)
            workstation = getDefaultWorkstation();
        setWorkstation(workstation);
        setDomain(domain);
        setUser(user);

        switch (LM_COMPATIBILITY) {
        case 0:
        case 1:
            if ((getFlags() & NTLMSSP_NEGOTIATE_NTLM2) == 0) {
                setLMResponse(getLMResponse(type2, password));
                setNTResponse(getNTResponse(type2, password));
            } else {
                // NTLM2 Session Response

                byte[] clientChallenge = new byte[24];
                RANDOM.nextBytes(clientChallenge);
                java.util.Arrays.fill(clientChallenge, 8, 24, (byte)0x00);

// NTLMv1 w/ NTLM2 session sec and key exch all been verified with a debug build of smbclient

                byte[] responseKeyNT = NtlmPasswordAuthentication.nTOWFv1(password);
                byte[] ntlm2Response = NtlmPasswordAuthentication.getNTLM2Response(responseKeyNT,
                            type2.getChallenge(),
                            clientChallenge);

                setLMResponse(clientChallenge);
                setNTResponse(ntlm2Response);

                if ((getFlags() & NTLMSSP_NEGOTIATE_SIGN) == NTLMSSP_NEGOTIATE_SIGN) {
                    byte[] sessionNonce = new byte[16];
                    System.arraycopy(type2.getChallenge(), 0, sessionNonce, 0, 8);
                    System.arraycopy(clientChallenge, 0, sessionNonce, 8, 8);

                    MD4 md4 = new MD4();
                    md4.update(responseKeyNT);
                    byte[] userSessionKey = md4.digest();

                    HMACT64 hmac = new HMACT64(userSessionKey);
                    hmac.update(sessionNonce);
                    byte[] ntlm2SessionKey = hmac.digest();

                    if ((getFlags() & NTLMSSP_NEGOTIATE_KEY_EXCH) != 0) {
                        masterKey = new byte[16];
                        RANDOM.nextBytes(masterKey);

                        byte[] exchangedKey = new byte[16];
                        RC4 rc4 = new RC4(ntlm2SessionKey);
                        rc4.update(masterKey, 0, 16, exchangedKey, 0);
/* RC4 was not added to Java until 1.5u7 so let's use our own for a little while longer ...
                        try {
                            Cipher rc4 = Cipher.getInstance("RC4");
                            rc4.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(ntlm2SessionKey, "RC4"));
                            rc4.update(masterKey, 0, 16, exchangedKey, 0);
                        } catch (GeneralSecurityException gse) {
                            throw new RuntimeException("", gse);
                        }
*/

                        setSessionKey(exchangedKey);
                    } else {
                        masterKey = ntlm2SessionKey;
                        setSessionKey(masterKey);
                    }
                }
            }
            break;
        case 2:
            byte[] nt = getNTResponse(type2, password);
            setLMResponse(nt);
            setNTResponse(nt);
            break;
        case 3:
        case 4:
        case 5:
            byte[] responseKeyNT = NtlmPasswordAuthentication.nTOWFv2(domain, user, password);

            byte[] clientChallenge = new byte[8];
            RANDOM.nextBytes(clientChallenge);
            setLMResponse(getLMv2Response(type2, domain, user, password, clientChallenge));

            byte[] clientChallenge2 = new byte[8];
            RANDOM.nextBytes(clientChallenge2);
            setNTResponse(getNTLMv2Response(type2, responseKeyNT, clientChallenge2));

            if ((getFlags() & NTLMSSP_NEGOTIATE_SIGN) == NTLMSSP_NEGOTIATE_SIGN) {
                HMACT64 hmac = new HMACT64(responseKeyNT);
                hmac.update(ntResponse, 0, 16); // only first 16 bytes of ntResponse
                byte[] userSessionKey = hmac.digest();

                if ((getFlags() & NTLMSSP_NEGOTIATE_KEY_EXCH) != 0) {
                    masterKey = new byte[16];
                    RANDOM.nextBytes(masterKey);

                    byte[] exchangedKey = new byte[16];
                    RC4 rc4 = new RC4(userSessionKey);
                    rc4.update(masterKey, 0, 16, exchangedKey, 0);
/* RC4 was not added to Java until 1.5u7 so let's use our own for a little while longer ...
                    try {
                        Cipher rc4 = Cipher.getInstance("RC4");
                        rc4.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(userSessionKey, "RC4"));
                        rc4.update(masterKey, 0, 16, exchangedKey, 0);
                    } catch (GeneralSecurityException gse) {
                        throw new RuntimeException("", gse);
                    }
*/

                    setSessionKey(exchangedKey);
                } else {
                    masterKey = userSessionKey;
                    setSessionKey(masterKey);
                }
            }

            break;
        default:
            setLMResponse(getLMResponse(type2, password));
            setNTResponse(getNTResponse(type2, password));
        }
    }

    /**
     * Creates a Type-3 message with the specified parameters.
     *
     * @param flags The flags to apply to this message.
     * @param lmResponse The LanManager/LMv2 response.
     * @param ntResponse The NT/NTLMv2 response.
     * @param domain The domain in which the user has an account.
     * @param user The username for the authenticating user.
     * @param workstation The workstation from which authentication is
     * taking place.
     */
    public Type3Message(int flags, byte[] lmResponse, byte[] ntResponse,
            String domain, String user, String workstation) {
        setFlags(flags);
        setLMResponse(lmResponse);
        setNTResponse(ntResponse);
        setDomain(domain);
        setUser(user);
        setWorkstation(workstation);
    }

    /**
     * Creates a Type-3 message using the given raw Type-3 material.
     *
     * @param material The raw Type-3 material used to construct this message.
     * @throws IOException If an error occurs while parsing the material.
     */
    public Type3Message(byte[] material) throws IOException {
        parse(material);
    }

    /**
     * Returns the LanManager/LMv2 response.
     *
     * @return A <code>byte[]</code> containing the LanManager response.
     */
    public byte[] getLMResponse() {
        return lmResponse;
    }

    /**
     * Sets the LanManager/LMv2 response for this message.
     *
     * @param lmResponse The LanManager response.
     */
    public void setLMResponse(byte[] lmResponse) {
        this.lmResponse = lmResponse;
    }

    /**
     * Returns the NT/NTLMv2 response.
     *
     * @return A <code>byte[]</code> containing the NT/NTLMv2 response.
     */
    public byte[] getNTResponse() {
        return ntResponse;
    }

    /**
     * Sets the NT/NTLMv2 response for this message.
     *
     * @param ntResponse The NT/NTLMv2 response.
     */
    public void setNTResponse(byte[] ntResponse) {
        this.ntResponse = ntResponse;
    }

    /**
     * Returns the domain in which the user has an account.
     *
     * @return A <code>String</code> containing the domain for the user.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Sets the domain for this message.
     *
     * @param domain The domain.
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * Returns the username for the authenticating user.
     *
     * @return A <code>String</code> containing the user for this message.
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the user for this message.
     *
     * @param user The user.
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Returns the workstation from which authentication is being performed.
     *
     * @return A <code>String</code> containing the workstation.
     */
    public String getWorkstation() {
        return workstation;
    }

    /**
     * Sets the workstation for this message.
     *
     * @param workstation The workstation.
     */
    public void setWorkstation(String workstation) {
        this.workstation = workstation;
    }

    /**
     * The real session key if the regular session key is actually
     * the encrypted version used for key exchange.
     *
     * @return A <code>byte[]</code> containing the session key.
     */
    public byte[] getMasterKey() {
        return masterKey;
    }

    /**
     * Returns the session key.
     *
     * @return A <code>byte[]</code> containing the session key.
     */
    public byte[] getSessionKey() {
        return sessionKey;
    }

    /**
     * Sets the session key.
     *
     * @param sessionKey The session key.
     */
    public void setSessionKey(byte[] sessionKey) {
        this.sessionKey = sessionKey;
    }

    public byte[] toByteArray() {
        try {
            int flags = getFlags();
            boolean unicode = (flags & NTLMSSP_NEGOTIATE_UNICODE) != 0;
            String oem = unicode ? null : getOEMEncoding();
            String domainName = getDomain();
            byte[] domain = null;
            if (domainName != null && domainName.length() != 0) {
                domain = unicode ?
                        domainName.getBytes(UNI_ENCODING) :
                                domainName.getBytes(oem);
            }
            int domainLength = (domain != null) ? domain.length : 0;
            String userName = getUser();
            byte[] user = null;
            if (userName != null && userName.length() != 0) {
                user = unicode ? userName.getBytes(UNI_ENCODING) :
                        userName.toUpperCase().getBytes(oem);
            }
            int userLength = (user != null) ? user.length : 0;
            String workstationName = getWorkstation();
            byte[] workstation = null;
            if (workstationName != null && workstationName.length() != 0) {
                workstation = unicode ?
                        workstationName.getBytes(UNI_ENCODING) :
                                workstationName.toUpperCase().getBytes(oem);
            }
            int workstationLength = (workstation != null) ?
                    workstation.length : 0;
            byte[] lmResponse = getLMResponse();
            int lmLength = (lmResponse != null) ? lmResponse.length : 0;
            byte[] ntResponse = getNTResponse();
            int ntLength = (ntResponse != null) ? ntResponse.length : 0;
            byte[] sessionKey = getSessionKey();
            int keyLength = (sessionKey != null) ? sessionKey.length : 0;
            byte[] type3 = new byte[64 + domainLength + userLength +
                    workstationLength + lmLength + ntLength + keyLength];
            System.arraycopy(NTLMSSP_SIGNATURE, 0, type3, 0, 8);
            writeULong(type3, 8, 3);
            int offset = 64;
            writeSecurityBuffer(type3, 12, offset, lmResponse);
            offset += lmLength;
            writeSecurityBuffer(type3, 20, offset, ntResponse);
            offset += ntLength;
            writeSecurityBuffer(type3, 28, offset, domain);
            offset += domainLength;
            writeSecurityBuffer(type3, 36, offset, user);
            offset += userLength;
            writeSecurityBuffer(type3, 44, offset, workstation);
            offset += workstationLength;
            writeSecurityBuffer(type3, 52, offset, sessionKey);
            writeULong(type3, 60, flags);
            return type3;
        } catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage());
        }
    }

    public String toString() {
        String user = getUser();
        String domain = getDomain();
        String workstation = getWorkstation();
        byte[] lmResponse = getLMResponse();
        byte[] ntResponse = getNTResponse();
        byte[] sessionKey = getSessionKey();

        return "Type3Message[domain=" + domain +
            ",user=" + user +
            ",workstation=" + workstation +
            ",lmResponse=" + (lmResponse == null ? "null" : "<" + lmResponse.length + " bytes>") +
            ",ntResponse=" + (ntResponse == null ? "null" : "<" + ntResponse.length + " bytes>") +
            ",sessionKey=" + (sessionKey == null ? "null" : "<" + sessionKey.length + " bytes>") +
            ",flags=0x" + jcifs.util.Hexdump.toHexString(getFlags(), 8) + "]";
    }

    /**
     * Returns the default flags for a generic Type-3 message in the
     * current environment.
     *
     * @return An <code>int</code> containing the default flags.
     */
    public static int getDefaultFlags() {
        return DEFAULT_FLAGS;
    }

    /**
     * Returns the default flags for a Type-3 message created in response
     * to the given Type-2 message in the current environment.
     *
     * @return An <code>int</code> containing the default flags.
     */
    public static int getDefaultFlags(Type2Message type2) {
        if (type2 == null) return DEFAULT_FLAGS;
        int flags = NTLMSSP_NEGOTIATE_NTLM;
        flags |= ((type2.getFlags() & NTLMSSP_NEGOTIATE_UNICODE) != 0) ?
                NTLMSSP_NEGOTIATE_UNICODE : NTLMSSP_NEGOTIATE_OEM;
        return flags;
    }

    /**
     * Constructs the LanManager response to the given Type-2 message using
     * the supplied password.
     *
     * @param type2 The Type-2 message.
     * @param password The password.
     * @return A <code>byte[]</code> containing the LanManager response.
     */
    public static byte[] getLMResponse(Type2Message type2, String password) {
        if (type2 == null || password == null) return null;
        return NtlmPasswordAuthentication.getPreNTLMResponse(password,
                type2.getChallenge());
    }

    public static byte[] getLMv2Response(Type2Message type2,
            String domain, String user, String password,
                    byte[] clientChallenge) {
        if (type2 == null || domain == null || user == null ||
                password == null || clientChallenge == null) {
            return null;
        }
        return NtlmPasswordAuthentication.getLMv2Response(domain, user,
                password, type2.getChallenge(), clientChallenge);
    }
    public static byte[] getNTLMv2Response(Type2Message type2,
                    byte[] responseKeyNT,
                    byte[] clientChallenge) {
        if (type2 == null || responseKeyNT == null || clientChallenge == null) {
            return null;
        }
        long nanos1601 = (System.currentTimeMillis() + MILLISECONDS_BETWEEN_1970_AND_1601) * 10000L;
        return NtlmPasswordAuthentication.getNTLMv2Response(responseKeyNT,
                    type2.getChallenge(),
                    clientChallenge,
                    nanos1601,
                    type2.getTargetInformation());
    }

    /**
     * Constructs the NT response to the given Type-2 message using
     * the supplied password.
     *
     * @param type2 The Type-2 message.
     * @param password The password.
     * @return A <code>byte[]</code> containing the NT response.
     */
    public static byte[] getNTResponse(Type2Message type2, String password) {
        if (type2 == null || password == null) return null;
        return NtlmPasswordAuthentication.getNTLMResponse(password,
                type2.getChallenge());
    }

    /**
     * Returns the default domain from the current environment.
     *
     * @return The default domain.
     */
    public static String getDefaultDomain() {
        return DEFAULT_DOMAIN;
    }

    /**
     * Returns the default user from the current environment.
     *
     * @return The default user.
     */
    public static String getDefaultUser() {
        return DEFAULT_USER;
    }

    /**
     * Returns the default password from the current environment.
     *
     * @return The default password.
     */
    public static String getDefaultPassword() {
        return DEFAULT_PASSWORD;
    }

    /**
     * Returns the default workstation from the current environment.
     *
     * @return The default workstation.
     */
    public static String getDefaultWorkstation() {
        return DEFAULT_WORKSTATION;
    }

    private void parse(byte[] material) throws IOException {
        for (int i = 0; i < 8; i++) {
            if (material[i] != NTLMSSP_SIGNATURE[i]) {
                throw new IOException("Not an NTLMSSP message.");
            }
        }
        if (readULong(material, 8) != 3) {
            throw new IOException("Not a Type 3 message.");
        }
        byte[] lmResponse = readSecurityBuffer(material, 12);
        int lmResponseOffset = readULong(material, 16);
        byte[] ntResponse = readSecurityBuffer(material, 20);
        int ntResponseOffset = readULong(material, 24);
        byte[] domain = readSecurityBuffer(material, 28);
        int domainOffset = readULong(material, 32);
        byte[] user = readSecurityBuffer(material, 36);
        int userOffset = readULong(material, 40);
        byte[] workstation = readSecurityBuffer(material, 44);
        int workstationOffset = readULong(material, 48);
        int flags;
        String charset;
        byte[] _sessionKey = null;
        if (lmResponseOffset == 52 || ntResponseOffset == 52 ||
                domainOffset == 52 || userOffset == 52 ||
                        workstationOffset == 52) {
            flags = NTLMSSP_NEGOTIATE_NTLM | NTLMSSP_NEGOTIATE_OEM;
            charset = getOEMEncoding();
        } else {
            _sessionKey = readSecurityBuffer(material, 52);
            flags = readULong(material, 60);
            charset = ((flags & NTLMSSP_NEGOTIATE_UNICODE) != 0) ?
                UNI_ENCODING : getOEMEncoding();
        }
        setSessionKey(_sessionKey);
        setFlags(flags);
        setLMResponse(lmResponse);
        setNTResponse(ntResponse);
        setDomain(new String(domain, charset));
        setUser(new String(user, charset));
        setWorkstation(new String(workstation, charset));
    }
}
