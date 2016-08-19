/*
* The contents of this file are subject to the terms of the Common Development and
* Distribution License (the License). You may not use this file except in compliance with the
* License.
*
* You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
* specific language governing permission and limitations under the License.
*
* When distributing Covered Software, include this CDDL Header Notice in each file and include
* the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
* Header, with the fields enclosed by brackets [] replaced by your own identifying
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2016 ForgeRock AS.
*/
package org.forgerock.openam.utils;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;

import org.forgerock.util.Reject;

/**
 * Generates codes of a specified length using a given {@link Alphabet} as valid characters.
 */
public class RecoveryCodeGenerator {

    /**
     * Default length of generated codes.
     */
    public final static int DEFAULT_LENGTH = 10;

    /**
     * Default number of retries to use before resulting in error.
     */
    public final static int DEFAULT_RETRIES = 100;

    private final SecureRandom secureRandom;
    private final int retryMaximum;

    /**
     * Generates a new CodeUtils which can be used to generate a plethora of
     * codes suited to fit your needs.
     *
     * @param secureRandom The {@link SecureRandom} instance to use when selecting
     *                     characters from an alphabet set. Must not be null.
     * @param retryMaximum If duplicate codes are not allowed, this is the number of times
     *                     we will attempt to generate another code upon discovering a collision.
     */
    public RecoveryCodeGenerator(SecureRandom secureRandom, int retryMaximum) {
        this.secureRandom = secureRandom;
        this.retryMaximum = retryMaximum;
    }

    /**
     * Generates a new CodeUtils which can be used to generate a plethora of
     * codes suited to fit your needs. Uses the {@link RecoveryCodeGenerator#DEFAULT_RETRIES}
     * as its number of retries before failing.
     *
     * @param secureRandom The {@link SecureRandom} instance to use when selecting
     *                     characters from an alphabet set. Must not be null.
     */
    @Inject
    public RecoveryCodeGenerator(SecureRandom secureRandom) {
        this(secureRandom, DEFAULT_RETRIES);
    }

    /**
     * Generates a code of the supplied length, using the provided alphabet as its
     * source of random characters.
     *
     * @param alphabet The alpha to use from which to pick characters. Must not be null.
     * @param length The size of the produced codes. Must be > 0.
     *
     * @return a randomly generated code.
     */
    public String generateCode(CodeGeneratorSource alphabet, int length) {
        Reject.ifTrue(length < 1);
        Reject.ifNull(alphabet);

        StringBuilder codeBuilder = new StringBuilder(length);
        String chars = alphabet.getChars();

        for (int k = 0; k < length; k++) {
            codeBuilder.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }

        return codeBuilder.toString();
    }

    /**
     * Generates a code of the supplied length, using the provided alphabet as its
     * source of random characters. The generated codes will have random characters for the
     * size of each of the supplied {@code groups}, with a {@code delimiter} between them.
     *
     * @param alphabet The alpha to use from which to pick characters. Must not be null.
     * @param groups The groupings of characters. For example, {@code {4, 4}} will produce two sets
     *               of four characters with a {@code delimiter} between them. Must not be null. Must have at least
     *               one entry. Entries must be > 0.
     * @param delimiter The character to use between each of the groups.
     *
     * @return a randomly generated, delimited code.
     * */
    public String generateDelimitedCode(CodeGeneratorSource alphabet, char delimiter, int... groups) {
        Reject.ifNull(groups);
        Reject.ifTrue(groups.length < 1);
        Reject.ifNull(delimiter);

        StringBuilder codeBuilder = new StringBuilder();

        for (int group : groups) {
            Reject.ifTrue(group < 1);
            if (codeBuilder.length() > 0) {
                codeBuilder.append(delimiter);
            }
            codeBuilder.append(generateCode(alphabet, group));
        }

        return codeBuilder.toString();
    }

    /**
     * Generates a code of the supplied length, using the provided alphabet as its
     * source of random characters. The generated codes will have random characters for the
     * size of each of the supplied {@code groups}, with a {@code delimiter} between them.
     *
     * The provided specifics map can then be used to replace individual character indexes with a
     * specific character.
     *
     * @param alphabet The alpha to use from which to pick characters. Must not be null.
     * @param groups The groupings of characters. For example, {@code {4, 4}} will produce two sets
     *               of four characters with a {@code delimiter} between them. Must not be null. Must have at least
     *               one entry. Entries must be > 0.
     * @param delimiter The character to use between each of the groups. Must not be null.
     * @param specifics A map used to specify characters to use at a given location.
     *
     * @return a randomly generated, delimited code with specific characters at given indexes.
     * */
    public String generateDelimitedCodeWithSpecifics(CodeGeneratorSource alphabet, char delimiter,
                                                       Map<Integer, Character> specifics, int... groups) {

        StringBuilder codeBuilder = new StringBuilder(generateDelimitedCode(alphabet, delimiter, groups));
        int maxLength = codeBuilder.length();

        for(Map.Entry<Integer, Character> specific : specifics.entrySet()) {
            Reject.ifTrue(specific.getKey() < 0 || specific.getKey() > maxLength);
            Reject.ifNull(specific.getValue());
            codeBuilder.replace(specific.getKey(), specific.getKey() + 1, String.valueOf(specific.getValue()));
        }

        return codeBuilder.toString();
    }

    /**
     * Generate a set of codes using the provided alphabet of the default length.
     *
     * @param numCodes The number of codes to generate. Must be > 0.
     * @param alphabet The alphabet to use from which to select characters. Must not be null.
     * @param allowDuplicates Whether to allow duplicates in the result set.
     *
     * @throws CodeException if duplicate codes were produced, disallowed and the number of retries was exceeded.
     * @return A set of recovery codes.
     */
    public String[] generateCodes(int numCodes, CodeGeneratorSource alphabet, boolean allowDuplicates)
            throws CodeException {
        return generateCodes(numCodes, alphabet, DEFAULT_LENGTH, allowDuplicates);
    }

    /**
     * Generate a set of codes using the provided alphabet of the provided length.
     *
     * @param numCodes Number of recovery codes to generate. Must be > 0.
     * @param alphabet The alphabet to use from which to select characters. Must not be null.
     * @param length The length of produced codes. Must be > 0.
     * @param allowDuplicates Whether or not to allow duplicates in the result set.
     *
     * @throws CodeException if duplicate codes were produced, disallowed and the number of retries was exceeded.
     * @return a String array of randomly generated recovery codes, of size numSize.
     */
    public String[] generateCodes(int numCodes, CodeGeneratorSource alphabet, int length, boolean allowDuplicates)
            throws CodeException {
        Reject.ifTrue(numCodes < 1, "numCodes must be greater than or equal to 1.");
        Reject.ifTrue(length < 1, "length must be greater than or equal to 1.");

        String[] recoveryCodes = new String[numCodes];
        String result;

        for (int i = 0; i < numCodes; i++) {

            int counter = 0;

            do {
                result = generateCode(alphabet, length);
            } while (!allowDuplicates
                    && Arrays.asList(recoveryCodes).contains(result)
                    && counter++ <= retryMaximum);

            if (counter >= retryMaximum) {
                throw new CodeException("Unable to generate unique codes with the given constraints.");
            }

            recoveryCodes[i] = result;
        }

        return recoveryCodes;
    }

    /**
     * Generates a code of the supplied length, using the provided alphabet as its
     * source of random characters. The generated codes will have random characters for the
     * size of each of the supplied {@code groups}, with a {@code delimiter} between them.
     *
     * @param numCodes Number of recovery codes to generate. Must be > 0.
     * @param alphabet The alphabet to use from which to select numbers. Must not be null.
     * @param groups The groupings of lengths of characters. For example, {@code {4, 4}} will produce two set
     *               of four characters with a {@code delimiter} between them. Must not be null. Must have at least
     *               one entry. Entries must be > 0.
     * @param delimiter The character to use between each of the groups.
     * @param allowDuplicates Whether or not to allow duplicates in the result set.
     *
     * @throws CodeException if duplicate codes were produced, disallowed and the number of retries was exceeded.
     * @return a String array of randomly generated recovery codes, of size numCodes.
     */
    public String[] generateDelimitedCodes(int numCodes, CodeGeneratorSource alphabet,
                                           char delimiter, boolean allowDuplicates, int... groups)
            throws CodeException {
        Reject.ifTrue(numCodes < 1, "numCodes must be greater than or equal to 1.");

        String[] recoveryCodes = new String[numCodes];
        String result;

        for (int i = 0; i < numCodes; i++) {

            int counter = 0;

            do {
                result = generateDelimitedCode(alphabet, delimiter, groups);
            } while (!allowDuplicates
                    && Arrays.asList(recoveryCodes).contains(result)
                    && counter++ <= retryMaximum);

            if (counter >= retryMaximum) {
                throw new CodeException("Unable to generate unique codes with the given constraints.");
            }

            recoveryCodes[i] = result;
        }

        return recoveryCodes;
    }

    /**
     * Generates a code of the supplied length, using the provided alphabet as its
     * source of random characters. The generated codes will have random characters for the
     * size of each of the supplied {@code groups}, with a {@code delimiter} between them.
     *
     * The provided specifics map can then be used to replace individual character indexes with a
     * specific character.
     *
     * @param numCodes The number of codes to generate. Must be > 0.
     * @param alphabet The alphabet to use from which to pick characters. Must not be null.
     * @param groups The groupings of lengths of characters. For example, {@code {4, 4}} will produce two sets
     *               of four characters with a {@code delimiter} between them. Must not be null. Must have at least
     *               one entry. Entries must be > 0.
     * @param delimiter The character to use between each of the groups.
     * @param specifics A map used to specify characters to use at a given location.
     * @param allowDuplicates Whether to allow duplicate codes.
     *
     * @throws CodeException if duplicate codes were produced, disallowed and the number of retries was exceeded.
     * @return a String array of randomly generated recovery codes, of size numCodes.
     */
    public String[] generateDelimitedCodesWithSpecifics(int numCodes, CodeGeneratorSource alphabet,
                                                        char delimiter, Map<Integer, Character> specifics,
                                                        boolean allowDuplicates, int... groups) throws CodeException {
        Reject.ifTrue(numCodes < 1);

        String[] recoveryCodes = new String[numCodes];
        String result;

        for (int i = 0; i < numCodes; i++) {

            int counter = 0;

            do {
                result = generateDelimitedCodeWithSpecifics(alphabet, delimiter, specifics, groups);
            } while (!allowDuplicates
                    && Arrays.asList(recoveryCodes).contains(result)
                    && counter++ <= retryMaximum);

            if (counter >= retryMaximum) {
                throw new CodeException("Unable to generate unique codes with the given constraints.");
            }

            recoveryCodes[i] = result;
        }

        return recoveryCodes;
    }

}
