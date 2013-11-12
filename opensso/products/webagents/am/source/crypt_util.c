/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: crypt_util.c,v 1.7 2008/06/25 08:14:31 qcheng Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <string.h>

#if !defined(HPUX) && !defined(u_int)
typedef unsigned int u_int;
typedef unsigned char u_char;
typedef unsigned long u_long;
#endif

typedef struct {
        u_int   *xk;
        int             nr;
} rc5_ctx;

#define ROTL32(x, c)    (((x) << (c)) | ((x) >> (32 - (c))))
#define ROTR32(x, c)    (((x) >> (c)) | ((x) << (32 - (c))))

void rc5_init(rc5_ctx *, int);
void rc5_destroy(rc5_ctx *);
void rc5_key(rc5_ctx *, u_char *, int);
int rc5_encrypt(rc5_ctx *, u_char *, int);
int rc5_decrypt(rc5_ctx *, u_char *, int);
extern void encode_base64(const char*, size_t, char*);
extern int decode_base64(const char *, char *);
int encrypt_base64(const char *, char *, const char*);
int decrypt_base64(const char *, char *, const char*);

char vec[] =
	"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

void
rc5_destroy(rc5_ctx *c) {
	int i;

	for (i = 0; i < (c->nr) * 2 + 2; i++) {
		c->xk[i] = 0;
	}
	free(c->xk);
}

void
rc5_init(rc5_ctx *c, int rounds) {
	c->nr = rounds;
	c->xk = (u_int *)malloc(4 * (rounds * 2 + 2));
}

int
rc5_encrypt(rc5_ctx *c, u_char *data, int data_len) {
	u_int *sk;
	int h, i, rc;
	int n;
	int d_index;
	int blocks;
	int pad;
	u_int d0;
	u_int d1;

	pad = 8 - (data_len % 8);
	for (n = 0; n < pad; n++) {
		data[data_len + n] = 0;
	}

	data_len += pad;
	data[data_len - 1] = pad; /* store pad at end for decrypt use */
 
	blocks = data_len / 8;

	d_index = 0;
	sk = (c->xk) + 2;
	for (h = 0; h < blocks; h++) {

		d0 = data[d_index] << 24;
		d0 |= data[d_index + 1] << 16;
		d0 |= data[d_index + 2] << 8;
		d0 |= data[d_index + 3];
		d1 = data[d_index + 4] << 24;
		d1 |= data[d_index + 5] << 16;
		d1 |= data[d_index + 6] << 8;
		d1 |= data[d_index + 7];

		d0 += c->xk[0];
		d1 += c->xk[1];
		for (i = 0; i < c->nr * 2; i += 2) {
			d0 ^= d1;
			rc = d1 & 31;
			d0 = ROTL32(d0, rc);
			d0 += sk[i];

			d1 ^= d0;
			rc = d0 & 31;
			d1 = ROTL32(d1, rc);
			d1 += sk[i + 1];
		}

		/* copy back 4 byte quantities to data array... */
		data[d_index] = d0 >> 24;
		data[d_index + 1] = d0 >> 16 & 0x000000ff;
		data[d_index + 2] = d0 >> 8 & 0x000000ff;
		data[d_index + 3] = d0 & 0x000000ff;
		data[d_index + 4] = d1 >> 24;
		data[d_index + 5] = d1 >> 16 & 0x000000ff;
		data[d_index + 6] = d1 >> 8 & 0x000000ff;
		data[d_index + 7] = d1 & 0x000000ff;

		d_index += 8;
	}

	return(data_len);
}

int
rc5_decrypt(rc5_ctx *c, u_char *data, int data_len) {
	u_int *sk;
	int h, i, rc;
	int blocks;
	int d_index;
	u_int d0;
	u_int d1;

	blocks = data_len / 8;
	d_index = 0;
	sk = (c->xk) + 2;
	for (h = 0; h < blocks; h++) {

		d0 = data[d_index] << 24;
		d0 |= data[d_index + 1] << 16;
		d0 |= data[d_index + 2] << 8;
		d0 |= data[d_index + 3];
		d1 = data[d_index + 4] << 24;
		d1 |= data[d_index + 5] << 16;
		d1 |= data[d_index + 6] << 8;
		d1 |= data[d_index + 7];

		for (i = c->nr * 2 - 2; i >= 0; i-= 2) {
			d1 -= sk[i + 1];
			rc = d0 & 31;
			d1 = ROTR32(d1, rc);
			d1 ^= d0;

			d0 -= sk[i];
			rc = d1 & 31;
			d0 = ROTR32(d0, rc);
			d0 ^= d1;
		}
		d0 -= c->xk[0];
		d1 -= c->xk[1];

		/* copy back 4 byte quantities to data array... */
		data[d_index] = d0 >> 24;
		data[d_index + 1] = d0 >> 16 & 0x000000ff;
		data[d_index + 2] = d0 >> 8 & 0x000000ff;
		data[d_index + 3] = d0 & 0x000000ff;
		data[d_index + 4] = d1 >> 24;
		data[d_index + 5] = d1 >> 16 & 0x000000ff;
		data[d_index + 6] = d1 >> 8 & 0x000000ff;
		data[d_index + 7] = d1 & 0x000000ff;

		d_index += 8;
	}
	return(data_len - data[data_len - 1]);
}

void
rc5_key(rc5_ctx *c, u_char *key, int keylen) {
	u_int *pk, A, B;
	int xk_len, pk_len, i, num_steps, rc;

	xk_len = c->nr * 2 + 2;
	pk_len = keylen / 4;
	if ((keylen % 4) != 0) {
		pk_len += 1;
	}

	pk = (u_int *)malloc(keylen * sizeof(u_int));

	if (pk == NULL) {
		printf("pk alloc error\n");
		exit(-1);
	}

	memset(pk, 0, keylen * 4);


	for (i = 0; i < keylen; i++) {
		pk[i] = key[i];
	}

	c->xk[0] = 0xb7e15163;
	for (i = 1; i < xk_len; i++) {
		c->xk[i] = c->xk[i - 1] + 0x9e3779b9;
	}

	if (pk_len > xk_len) {
		num_steps = 3 * pk_len;
	} else {
		num_steps = 3 * xk_len;
	}

	A = B = 0;
	for (i = 0; i < num_steps; i++) {
		A = c->xk[i % xk_len] = ROTL32(c->xk[i % xk_len] + A + B, 3);
		rc = (A + B) & 31;
		B = pk[i % pk_len] = ROTL32(pk[i % pk_len] + A + B, rc);
	}

	memset(pk, 0, keylen * 4);

	free(pk);
}



extern void
encode_base64(const char *p, size_t in_size, char *c) {
    unsigned short cx=0xffff;
    size_t loop = 0;
    size_t i;
    unsigned char in_arr[3] = {0, 0, 0};
    
    int numeq = 0;
    
    if((in_size % 3) == 1) numeq = 2;
    if((in_size % 3) == 2) numeq = 1;

    for(loop = 0; loop < in_size; loop++) {
        
        size_t idx = loop % 3;
        in_arr[idx] = p[loop];
        if(idx == 2) {
            i = (in_arr[0] & 0xff) >> 2;
            c[++cx] = vec[i];

            i = (((in_arr[0] & 0x3) << 4) | ((in_arr[1] & 0xf0) >> 4));
            c[++cx] = vec[i];

            i = ((in_arr[1] & 0xf) << 2)  | ((in_arr[2] & 0xc0) >> 6);
            c[++cx] = vec[i];

            i = (in_arr[2] & 0x3f);
            c[++cx] = vec[i];
            
            in_arr[0] = 0;
            in_arr[1] = 0;
            in_arr[2] = 0;
        }
    }
    
    if(numeq == 2) {
        c[++cx] = vec[(in_arr[0] & 0xff) >> 2];
        c[++cx] = vec[(in_arr[0] & 0x3) << 4];
        c[++cx] = '=';
        c[++cx] = '=';
    }
    
    if(numeq == 1) {
        c[++cx] = vec[(in_arr[0] & 0xff) >> 2];
        c[++cx] = vec[(((in_arr[0] & 0x3) << 4) | ((in_arr[1] & 0xf0) >> 4))];
        c[++cx] = vec[((in_arr[1] & 0xf) << 2)];
        c[++cx] = '=';
    }
    c[++cx] = '\0';
    return;
}


unsigned char
getindex(char x) {
    unsigned char i = 0xff;
    while(vec[++i] != '\0') {
        if (vec[i] == x)
            return i;
    }
    return -1;
}

extern int 
decode_base64(const char *c, char *p) {
    int len = 0;
    int i =0;
    int px =0;
    int loop = 0;
    int cmpr = 0;
    unsigned char in_arr[4] = {0, 0, 0, 0};
    unsigned short numeq = 0;

    len = (int) strlen(c);
    i = len;
    px = -1;
    loop = len;
    
    while(i >= 0 && c[--i] == '=') ++numeq;
    if(numeq != 0) loop = len - 4;
    
    for(i = 0; i < loop; ++i) {
	cmpr = getindex(c[i]);
        if (cmpr == -1) {
            p[++px] = '\0';
	    return px;
        }
	in_arr[i%4] = (unsigned char)cmpr;
        if(i % 4 == 3) {
            p[++px] = ((in_arr[0] & 0x3f) << 2) | ((in_arr[1] & 0x30) >> 4);
            p[++px] = ((in_arr[1]  & 0xf) << 4) | ((in_arr[2] & 0x3c) >> 2);
            p[++px] = ((in_arr[2] & 0x3) << 6) | ((in_arr[3] & 0x3f));
        }
    }

    if(loop != len) {
        cmpr = getindex(c[i]);
        if (cmpr == -1) {
            p[0] = '\0';
	    return 0;
        }
        in_arr[0] = (unsigned char)cmpr;

        cmpr = getindex(c[++i]);
        if (cmpr == -1) {
            p[0] = '\0';
	    return 0;
        }
        in_arr[1] = (unsigned char)cmpr;
        
        if(numeq == 2) {
            p[++px] = ((in_arr[0] & 0x3f) << 2) | ((in_arr[1] & 0x30) >> 4);
        }
        
        if(numeq == 1) {
	    cmpr = getindex(c[++i]);
            if (cmpr == -1) {
                p[0] = '\0';
	        return 0;
            }
	    in_arr[2] = (unsigned char)cmpr;
            p[++px] = ((in_arr[0] & 0x3f) << 2) | ((in_arr[1] & 0x30) >> 4);
            p[++px] = ((in_arr[1]  & 0xf) << 4) | ((in_arr[2] & 0x3c) >> 2);
        }
    }
    p[++px] = '\0';
    return px;
}

/**
  * Decrypt with rc5 and turn into base64
  *
  * @param encrypted base 64 password
  * @return decrypted cleartext password
  *
*/
int decrypt_base64(const char *encrptbase, char *base64_dec_buffer, 
        const char* key)
{

    char buffer[7] = "";
    rc5_ctx c;
    int outlen = 0;
    int decode_len = 0;

    buffer[0] = key[0];
    buffer[1] = key[1];

    decode_len = decode_base64(encrptbase, base64_dec_buffer);

    buffer[2] = key[2];
    buffer[3] = key[3];

    if(decode_len > 0){

        buffer[4] = key[4];
        buffer[5] = key[5];
        buffer[6] = key[6];

        rc5_init(&c, 12);
        rc5_key(&c, (u_char *)buffer, 7);

        // Decrpypt password will be atleast smaller than the base64 encrypt
        outlen = rc5_decrypt(&c, (u_char *)base64_dec_buffer, decode_len);

        rc5_destroy(&c);

        return 0;
    }

    return 1;
}

/**
  * Encrypt with rc5 and turn into base 64
  * @param cleartext password
  * @return encrypted base 64 password
  *
*/
int encrypt_base64(const char *password, char *enc_passwd, 
        const char* key)
{

    char buffer[7] = "";
    rc5_ctx c;
    size_t passwordlen = 0;
    int outlen = 0;
    
    buffer[0] = key[0];
    buffer[1] = key[1];

    passwordlen = strlen(password);

    buffer[2] = key[2];
    buffer[3] = key[3];

    if(passwordlen > 0){

        buffer[4] = key[4];
        buffer[5] = key[5];
        buffer[6] = key[6];

        rc5_init(&c, 12);
        rc5_key(&c, (u_char *)buffer, 7);

        outlen = rc5_encrypt(&c, (u_char *)password, (int) passwordlen + 1);
        encode_base64(password, outlen, enc_passwd);

        rc5_destroy(&c);

        return 0;
    }

    return 1;
}


#if !defined(AM_BUILDING_LIB)
/**
  * Main function that can be invoked from command line 
  * Usage amencrypt <password>
  * output : base64 encrypted password
*/
int
main(int argc, char *argv[]) {

    int retVal = 0;
    char encryptpasswd[1024] = "";
	char origpasswd[1024] = "";
    char keystr[8] = "";

    if(argc != 3){
#ifndef WINNT
		printf("Usage : crypt_util <password> <key>\n");
#else
		printf("Usage : cryptit <password> <key>\n");        
#endif
        exit(1);
    } else if (argc == 3) {
        if((argv[1] != NULL) && (argv[2] != NULL)) {
            strcpy(origpasswd, argv[1]);
            strncpy(keystr,argv[2],7);
            keystr[7]='\0';
            retVal = encrypt_base64(origpasswd, encryptpasswd, keystr);
            printf("%s\n", encryptpasswd);
        } else {

            printf("Invalid password/key Input\n");
#ifndef WINNT
            printf("Usage : crypt_util <password> <key>\n");
#else
            printf("Usage : cryptit <password> <key>\n");
#endif
            exit(1);
        }
    }

    return retVal;
} 
#endif /* AM_BUILDING_LIB */
