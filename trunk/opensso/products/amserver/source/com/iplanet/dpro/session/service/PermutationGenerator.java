/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PermutationGenerator.java,v 1.2 2008/06/25 05:41:30 qcheng Exp $
 *
 */

package com.iplanet.dpro.session.service;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Utility class to generate uniformly distributed random permutations of
 * integers in the range 0..size Permutation generation deterministically
 * produces the same result if given the same seed value This class is used to
 * implement enhanced request routing scheme as described in
 * http://is.red.iplanet.com/failover/s1as7/IS62-SessionFailover.sxw
 */

public class PermutationGenerator {
    private short[] state;

    private Random random;

    private int iter = 0;

    /**
     * Constructor
     * 
     * @param seed
     *            seed used by RNG
     * @param size
     *            permutation size
     */
    public PermutationGenerator(String seed, int size) {
        byte[] bytes = seed.getBytes();
        long longSeed = 0;
        for (int i = 0; i < bytes.length; i += 8) {
            longSeed += bytesToLong(bytes, i);
        }
        init(longSeed, size);
    }

    /**
     * Constructs<code>PermutationGenerator</code>
     * 
     * @param seed
     *            seed used by RNG
     * @param size
     *            permutation size
     */

    public PermutationGenerator(long seed, int size) {
        init(seed, size);
    }

    /**
     * Initializes the internal generator state
     * 
     * @param seed
     *            seed used by RNG
     * @param size
     *            permutation size
     */
    private void init(long seed, int size) {
        random = new Random();
        random.setSeed(seed);
        state = new short[size];
        for (int i = 0; i < state.length; i++) {
            state[i] = (short) i;
        }
    }

    /**
     * Returns permutation element at a given position Throws
     * 
     * @param pos
     *            permutation element position
     * @return permutation element value
     * @exception IndexOutOfBoundsException
     *                if index is less than 0 or greater than permutation size
     *                specified in constructor
     */
    public short itemAt(int pos) {
        if (pos < 0 || pos > state.length) {
            throw new IndexOutOfBoundsException();
        }
        for (; iter <= pos; ++iter) {
            int r = random.nextInt(state.length - iter) + iter;
            short tmp = state[r];
            state[r] = state[iter];
            state[iter] = tmp;
        }
        return state[pos];
    }

    /*
     * Utility used to convert string-based seed value into long
     */
    static private long bytesToLong(byte[] bytes, int offset) {
        int bound = offset + Math.min(bytes.length - offset, 8);
        long result = 0;
        for (int i = offset; i < bound; ++i) {
            result = (result << 8) | bytes[i];
        }
        return result;
    }

    /*
     * Test method
     */
    public static void main(String[] args) throws Exception {

        byte[] test = { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5,
                (byte) 6, (byte) 7, (byte) 8 };

        System.err.println("Long=" + Long.toHexString(bytesToLong(test, 0)));

        int size = 2;
        int[][] hist = new int[size][];
        for (int i = 0; i < hist.length; ++i) {
            hist[i] = new int[size];
        }

        for (int s = 0; s < 10000; ++s) {
            String seed = String.valueOf(new SecureRandom().nextLong());
            System.err.print(seed + ": ");
            PermutationGenerator perm = new PermutationGenerator(seed, size);
            int total = 0;
            for (int i = 0; i < size; i++) {
                int item = perm.itemAt(i);
                System.err.print(item + " ");
                ++hist[i][item];
                total += item;
            }
            if (total != ((size - 1) * size) / 2) {
                throw new Exception("permutation error");
            }
            System.err.println("");
        }

        for (int k = 0; k < hist.length; k++) {
            for (int l = 0; l < hist[k].length; l++) {
                System.err.print(hist[k][l] + " ");
            }
            System.err.println("");
        }
    }
}
