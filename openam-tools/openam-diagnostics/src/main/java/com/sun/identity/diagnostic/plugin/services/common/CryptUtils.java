/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: CryptUtils.java,v 1.1 2008/11/22 02:41:19 ak138937 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.diagnostic.plugin.services.common;

public class CryptUtils {
    
    private static String vec = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    
    private static int decodeBase64(final char[] c, char[] p) {
        int len = c.length;
        int i = len;
        int px = -1;
        int loop = len;
        int cmpr = 0;
        char[] inArr = {'\0', '\0', '\0', '\0'};
        short numeq = 0;
        while (i >= 0 && c[--i] == '=') {
            ++numeq;
        }
        if (numeq != 0) {
            loop = len - 4;
        }
        for (i = 0; i < loop; ++i) {
            cmpr = vec.indexOf(c[i]);
            if (cmpr == -1) {
                p[++px] = '\0';
                return px;
            }
            inArr[i % 4] = (char) cmpr;
            if (i % 4 == 3) {
                p[++px] = (char) (((inArr[0] & 0x003f) << 2) | ((inArr[1] & 0x0030) >> 4));
                p[++px] = (char) (((inArr[1] & 0x000f) << 4) | ((inArr[2] & 0x003c) >> 2));
                p[++px] = (char) (((inArr[2] & 0x0003) << 6) | (inArr[3] & 0x003f));
            }
        }
        if (loop != len) {
            cmpr = vec.indexOf(c[i]);
            if (cmpr == -1) {
                p[0] = '\0';
                return 0;
            }
            inArr[0] = (char) cmpr;
            cmpr = vec.indexOf(c[++i]);
            if (cmpr == -1) {
                p[0] = '\0';
                return 0;
            }
            inArr[1] = (char) cmpr;
            if (numeq == 2) {
                p[++px] = (char) (((inArr[0] & 0x003f) << 2) | ((inArr[1] & 0x0030) >> 4));
            }
            if(numeq == 1) {
                cmpr = vec.indexOf(c[++i]);
                if (cmpr == -1) {
                    p[0] = '\0';
                    return 0;
                }
                inArr[2] = (char) cmpr;
                p[++px] = (char) (((inArr[0] & 0x3f) << 2) | ((inArr[1] & 0x30) >> 4));
                p[++px] = (char) (((inArr[1]  & 0xf) << 4) | ((inArr[2] & 0x3c) >> 2));
            }
        }
        p[++px] = '\0';
        return px;
    }
    
    public static int decryptBase64(
        final char[] encryptBase, 
        char[] base64DecBuffer, 
        final char[] key
    ) {
        char[] buffer = new char[7];
        int outlen = 0;
        int decodeLen = 0;
        buffer[0] = key[0];
        buffer[1] = key[1];
        decodeLen = decodeBase64(encryptBase, base64DecBuffer);
        buffer[2] = key[2];
        buffer[3] = key[3];
        if (decodeLen > 0) {
            buffer[4] = key[4];
            buffer[5] = key[5];
            buffer[6] = key[6];
            RC5Ctx c = new RC5Ctx(12);
            c.key(buffer);
            
            // Decrpypt password will be atleast smaller than the base64 encrypt
            outlen = c.decrypt(base64DecBuffer, decodeLen);
            return 0;
        }
        return 1;
    }
    
    private static class RC5Ctx {
        private int nr;
        private int[] xk;
        
        public RC5Ctx(int rounds) {
            nr = rounds;
            xk = new int[4 * (rounds * 2 + 2)];
        }
        
        public void key(char[] key) {
            int[] pk;
            int A, B, keylen;
            int xk_len, pk_len, num_steps, rc;
            keylen = key.length;
            xk_len = nr * 2 + 2;
            pk_len = keylen/ 4;
            if ((keylen % 4) != 0) {
                pk_len++;
            }
            pk = new int[keylen];
            System.arraycopy(key, 0, pk, 0, keylen);
            xk[0] = 0xb7e15163;
            for (int i = 1; i < xk_len; i++) {
                xk[i] = xk[i - 1] + 0x9e3779b9;
            }
            if (pk_len > xk_len) {
                num_steps = 3 * pk_len;
            } else {
                num_steps = 3 * xk_len;
            }
            A = B = 0;
            for (int i = 0; i < num_steps; i++) {
                A = xk[i % xk_len] = ROTL32(xk[i % xk_len] + A + B, 3);
                rc = (A + B) & 31;
                B = pk[i % pk_len] = ROTL32(pk[i % pk_len] + A + B, rc);
            }
            for (int i = 0; i < keylen; i++) {
                pk[i] = 0;
            }
        }
        
        public int decrypt(char[] data, int data_len) {
            int rc;
            int blocks;
            int d_index;
            int d0;
            int d1;
            blocks = data_len / 8;
            d_index = 0;
            for (int i = 0; i < blocks; i++) {
                d0 = data[d_index] << 24;
                d0 |= data[d_index + 1] << 16;
                d0 |= data[d_index + 2] << 8;
                d0 |= data[d_index + 3];
                d1 = data[d_index + 4] << 24;
                d1 |= data[d_index + 5] << 16;
                d1 |= data[d_index + 6] << 8;
                d1 |= data[d_index + 7];
                
                for (int j = nr * 2 - 2; j >= 0; j-= 2) {
                    d1 -= xk[j + 3];
                    rc = (int) (d0 & 31);
                    d1 = ROTR32((int) d1, rc);
                    d1 ^= d0;
                    d0 -= xk[j + 2];
                    rc = (int) (d1 & 31);
                    d0 = ROTR32((int) d0, rc);
                    d0 ^= d1;
                }
                d0 -= xk[0];
                d1 -= xk[1];
                /* copy back 4 byte quantities to data array... */
                data[d_index] = (char) (d0 >>> 24);
                data[d_index + 1] = (char) (d0 >>> 16 & 0x000000ff);
                data[d_index + 2] = (char) (d0 >>> 8 & 0x000000ff);
                data[d_index + 3] = (char) (d0 & 0x000000ff);
                data[d_index + 4] = (char) (d1 >>> 24);
                data[d_index + 5] = (char) (d1 >>> 16 & 0x000000ff);
                data[d_index + 6] = (char) (d1 >>> 8 & 0x000000ff);
                data[d_index + 7] = (char) (d1 & 0x000000ff);
                
                d_index += 8;
            }
            return (data.length - data[data.length - 1]);
        }
        
        private int ROTL32(int x, int c) {
            return (((x) << (c)) | ((x) >>> (32 - (c))));
        }
        
        private int ROTR32(int x, int c) {
            return (((x) >>> (c)) | ((x) << (32 - (c))));
        }
    }
}
