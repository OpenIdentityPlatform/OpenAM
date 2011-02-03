
/*
 * This software code is made available "AS IS" without warranties of any 
 * kind.  You may copy, display, modify and redistribute the software
 * code either by itself or as incorporated into your code; provided that
 * you do not remove any proprietary notices.  Your use of this software
 * code is at your own risk and you waive any claim against Amazon
 * Web Services LLC or its affiliates with respect to your use of
 * this software code. (c) Amazon Web Services LLC or its
 * affiliates.
 */


package org.bouncycastle.crypto;

import org.bouncycastle.crypto.params.KeyParameter;
import com.sun.javafx.oauth.Base64;

public class HMACSigner {
   
    public static String sign(byte[] key, byte[] data) {
        HMac hmac = new HMac(new SHA1Digest());
        byte[] buffer = new byte[hmac.getMacSize()];
        KeyParameter kp = new KeyParameter(key);
        hmac.init(kp);
        hmac.update(data, 0, data.length);
        hmac.doFinal(buffer, 0);
        
        return new String(Base64.encode(buffer));
    }
}
