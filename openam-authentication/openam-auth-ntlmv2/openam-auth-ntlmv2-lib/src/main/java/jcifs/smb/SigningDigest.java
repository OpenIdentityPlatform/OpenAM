package jcifs.smb;

import jcifs.util.Hexdump;
import jcifs.util.LogStream;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * To filter 0 len updates and for debugging
 */

public class SigningDigest implements SmbConstants {

    static LogStream log = LogStream.getInstance();

    private MessageDigest digest;
    private byte[] macSigningKey;
    private boolean bypass = false;
    private int updates;
    private int signSequence;

    public SigningDigest(byte[] macSigningKey, boolean bypass) throws SmbException {
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            if( log.level > 0 )
                ex.printStackTrace( log );
            throw new SmbException( "MD5", ex );
        }

        this.macSigningKey = macSigningKey;
        this.bypass = bypass;
        this.updates = 0;
        this.signSequence = 0;

        if( log.level >= 5 ) {
            log.println("macSigningKey:");
            Hexdump.hexdump( log, macSigningKey, 0, macSigningKey.length );
        }
    }

    public SigningDigest( SmbTransport transport,
                NtlmPasswordAuthentication auth ) throws SmbException {
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            if( log.level > 0 )
                ex.printStackTrace( log );
            throw new SmbException( "MD5", ex );
        }

        try {
            switch (LM_COMPATIBILITY) {
            case 0:
            case 1:
            case 2:
                macSigningKey = new byte[40];
                auth.getUserSessionKey(transport.server.encryptionKey, macSigningKey, 0);
                System.arraycopy(auth.getUnicodeHash(transport.server.encryptionKey),
                            0, macSigningKey, 16, 24);
                break;
            case 3:
            case 4:
            case 5:
                macSigningKey = new byte[16];
                auth.getUserSessionKey(transport.server.encryptionKey, macSigningKey, 0);
                break;
            default:
                macSigningKey = new byte[40];
                auth.getUserSessionKey(transport.server.encryptionKey, macSigningKey, 0);
                System.arraycopy(auth.getUnicodeHash(transport.server.encryptionKey),
                            0, macSigningKey, 16, 24);
                break;
            }
        } catch( Exception ex ) {
            throw new SmbException( "", ex );
        }
        if( log.level >= 5 ) {
            log.println( "LM_COMPATIBILITY=" + LM_COMPATIBILITY );
            Hexdump.hexdump( log, macSigningKey, 0, macSigningKey.length );
        }
    }

    public void update( byte[] input, int offset, int len ) {
        if( log.level >= 5 ) {
            log.println( "update: " + updates + " " + offset + ":" + len );
            Hexdump.hexdump( log, input, offset, Math.min( len, 256 ));
            log.flush();
        }
        if( len == 0 ) {
            return; /* CRITICAL */
        }
        digest.update( input, offset, len );
        updates++;
    }
    public byte[] digest() {
        byte[] b;

        b = digest.digest();

        if( log.level >= 5 ) {
            log.println( "digest: " );
            Hexdump.hexdump( log, b, 0, b.length );
            log.flush();
        }
        updates = 0;

        return b;
    }

    /**
     * Performs MAC signing of the SMB.  This is done as follows.
     * The signature field of the SMB is overwritted with the sequence number;
     * The MD5 digest of the MAC signing key + the entire SMB is taken;
     * The first 8 bytes of this are placed in the signature field.
     *
     * @param data The data.
     * @param offset The starting offset at which the SMB header begins.
     * @param length The length of the SMB data starting at offset. 
     */
    void sign(byte[] data, int offset, int length,
                ServerMessageBlock request, ServerMessageBlock response) {
        request.signSeq = signSequence;
        if( response != null ) {
            response.signSeq = signSequence + 1;
            response.verifyFailed = false;
        }

        try {
            update(macSigningKey, 0, macSigningKey.length);
            int index = offset + ServerMessageBlock.SIGNATURE_OFFSET;
            for (int i = 0; i < 8; i++) data[index + i] = 0;
            ServerMessageBlock.writeInt4(signSequence, data, index);
            update(data, offset, length);
            System.arraycopy(digest(), 0, data, index, 8);
            if (bypass) {
                bypass = false;
                System.arraycopy("BSRSPYL ".getBytes(), 0, data, index, 8);
            }
        } catch (Exception ex) {
            if( log.level > 0 )
                ex.printStackTrace( log );
        } finally {
            signSequence += 2;
        }
    } 

    /**
     * Performs MAC signature verification.  This calculates the signature
     * of the SMB and compares it to the signature field on the SMB itself.
     *
     * @param data The data.
     * @param offset The starting offset at which the SMB header begins.
     * @param length The length of the SMB data starting at offset. 
     */
    boolean verify(byte[] data, int offset, ServerMessageBlock response) {
        update(macSigningKey, 0, macSigningKey.length);
        int index = offset;
        update(data, index, ServerMessageBlock.SIGNATURE_OFFSET); 
        index += ServerMessageBlock.SIGNATURE_OFFSET;
        byte[] sequence = new byte[8]; 
        ServerMessageBlock.writeInt4(response.signSeq, sequence, 0); 
        update(sequence, 0, sequence.length); 
        index += 8;
        if( response.command == ServerMessageBlock.SMB_COM_READ_ANDX ) {
            /* SmbComReadAndXResponse reads directly from the stream into separate byte[] b.
             */
            SmbComReadAndXResponse raxr = (SmbComReadAndXResponse)response;
            int length = response.length - raxr.dataLength;
            update(data, index, length - ServerMessageBlock.SIGNATURE_OFFSET - 8);
            update(raxr.b, raxr.off, raxr.dataLength);
        } else {
            update(data, index, response.length - ServerMessageBlock.SIGNATURE_OFFSET - 8);
        }
        byte[] signature = digest();
        for (int i = 0; i < 8; i++) {
            if (signature[i] != data[offset + ServerMessageBlock.SIGNATURE_OFFSET + i]) {
                if( log.level >= 2 ) {
                    log.println( "signature verification failure" );
                    Hexdump.hexdump( log, signature, 0, 8 );
                    Hexdump.hexdump( log, data,
                        offset + ServerMessageBlock.SIGNATURE_OFFSET, 8 );
                }
                return response.verifyFailed = true;
            }
        }

        return response.verifyFailed = false;
    }
    public String toString() {
        return "LM_COMPATIBILITY=" + LM_COMPATIBILITY + " MacSigningKey=" + Hexdump.toHexString(macSigningKey, 0, macSigningKey.length);
    }
}

