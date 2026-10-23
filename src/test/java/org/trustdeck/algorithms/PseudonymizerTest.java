package org.trustdeck.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import org.trustdeck.service.AlgorithmDBService;

class PseudonymizerTest {

    private static final String IDENTIFIER = "10000008912ANY-ID";

    private final AlgorithmDBService algorithmDBService = mock(AlgorithmDBService.class);

    private final PseudonymizationFactory factory = new PseudonymizationFactory(new DefaultProperties(), algorithmDBService);

    @Test
    void producesKnownHashValues() {
        assertEquals("TS-1DB52BB3C33934E21560398C3233F9E9", pseudonymizer("MD5", 32).pseudonymize(IDENTIFIER, "TS-"));
        assertEquals("TS-0DF03E5022F62439D69A1CDA9148A83CF8696C82", pseudonymizer("SHA1", 40).pseudonymize(IDENTIFIER, "TS-"));
        assertEquals("B72334F1F738CB93E19D098EC961674301D303C1CF8802506EF2C1BC367F81A95BF2C2DA870C6AF1CDEE4EEB2268DE31C4D175D9F5AD77E2B723773B8D27CC1A",
                pseudonymizer("SHA2", 128).pseudonymize(IDENTIFIER, ""));
        assertEquals("992C9E3AC9577B030A30FC794E1C070B5D920CA86E76D6FB586AAAF2F0A827D25940B08D6237958BC2C5B0E5E7F90469B8566258525D3D8BF7488D7B265479C3",
                pseudonymizer("SHA3", 128).pseudonymize(IDENTIFIER, ""));
        assertEquals("AF1349B9F5F9A1A6A0404DEA36DCC9499BCB25C9ADC112B7CC9A93CAE41F3262", pseudonymizer("BLAKE3", 64).pseudonymize("", ""));
    }

    @Test
    void hashPseudonymsAreDeterministic() {
        // The xxHash pseudonymizer reads the salt from the database
        when(algorithmDBService.getAlgorithmByID(1)).thenReturn(algorithm("XXHASH", 16));

        for (String name : new String[] {"MD5", "SHA1", "SHA2", "SHA3", "BLAKE3", "XXHASH"}) {
            Pseudonymizer pseudonymizer = pseudonymizer(name, 16);

            assertEquals(pseudonymizer.pseudonymize(IDENTIFIER, "P-"), pseudonymizer.pseudonymize(IDENTIFIER, "P-"));
            assertNotEquals(pseudonymizer.pseudonymize(IDENTIFIER, "P-"), pseudonymizer.pseudonymize(IDENTIFIER + "X", "P-"));
        }
    }

    @Test
    void correctsThePseudonymLength() {
        // Longer hashes are shortened
        assertEquals("TS-1DB52BB3", pseudonymizer("MD5", 8).pseudonymize(IDENTIFIER, "TS-"));

        // Shorter hashes are padded at the front
        assertEquals("TS-00000000" + "1DB52BB3C33934E21560398C3233F9E9", pseudonymizer("MD5", 40).pseudonymize(IDENTIFIER, "TS-"));

        // Without padding, short pseudonyms are kept as they are
        Pseudonymizer withoutPadding = new MD5Pseudonymizer(false, algorithm("MD5", 40), new DefaultProperties(), algorithmDBService);
        assertEquals("1DB52BB3C33934E21560398C3233F9E9", withoutPadding.pseudonymize(IDENTIFIER, ""));
    }

    @Test
    void includesTheCounterWhenMultiplePseudonymsAreAllowed() {
        Algorithm algorithm = algorithm("MD5", 32);
        algorithm.setConsecutiveValueCounter(5L);
        when(algorithmDBService.getAlgorithmByID(algorithm.getId())).thenReturn(algorithm);
        Pseudonymizer pseudonymizer = factory.getPseudonymizer(algorithm);
        pseudonymizer.setMultiplePsnAllowed(true);

        assertEquals("7ECF65F9A38B61C806013313BBFE77AA", pseudonymizer.pseudonymize(IDENTIFIER, ""));
        verify(algorithmDBService).updateCounter(6L, algorithm.getId());
    }

    @Test
    void consecutivePseudonymsUseAndIncreaseTheCounter() {
        Algorithm algorithm = algorithm("CONSECUTIVE", 6);
        algorithm.setAlphabet("0123456789");
        algorithm.setConsecutiveValueCounter(41L);
        when(algorithmDBService.getAlgorithmByID(algorithm.getId())).thenReturn(algorithm);
        when(algorithmDBService.updateCounter(42L, algorithm.getId())).thenReturn(true);

        assertEquals("C-000042", factory.getPseudonymizer(algorithm).pseudonymize(IDENTIFIER, "C-"));
        verify(algorithmDBService).updateCounter(42L, algorithm.getId());
    }

    @Test
    void randomPseudonymsOnlyUseTheAlphabet() {
        Algorithm algorithm = algorithm("RANDOM_LET", 20);
        algorithm.setAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        Pseudonymizer pseudonymizer = factory.getPseudonymizer(algorithm);

        Set<String> pseudonyms = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            String psn = pseudonymizer.pseudonymize(IDENTIFIER, "R-");
            assertEquals(22, psn.length());
            assertTrue(psn.substring(2).matches("[A-Z]{20}"));
            pseudonyms.add(psn);
        }

        // Random pseudonyms must differ for the same identifier
        assertTrue(pseudonyms.size() > 1);
    }

    @Test
    void randomPseudonymsFailWithoutAlphabet() {
        Algorithm algorithm = algorithm("RANDOM", 20);
        algorithm.setAlphabet("");

        assertNull(factory.getPseudonymizer(algorithm).pseudonymize(IDENTIFIER, "R-"));
    }

    @Test
    void randomNumberPseudonymsOnlyContainDigits() {
        Pseudonymizer pseudonymizer = factory.getPseudonymizer(algorithm("RANDOM_NUM", 25));

        for (int i = 0; i < 20; i++) {
            assertTrue(pseudonymizer.pseudonymize(IDENTIFIER, "").matches("\\d{25}"));
        }
    }

    @Test
    void unknownAlgorithmsFallBackToRandomLetters() {
        Pseudonymizer pseudonymizer = factory.getPseudonymizer(algorithm("UNKNOWN", 10));

        assertEquals(RandomAlphabetPseudonymizer.class, pseudonymizer.getClass());
        assertTrue(pseudonymizer.pseudonymize(IDENTIFIER, "").matches("[A-Z]{10}"));
    }

    @Test
    void addsValidCheckDigits() {
        Pseudonymizer pseudonymizer = pseudonymizer("MD5", 32);
        String psn = pseudonymizer.pseudonymize(IDENTIFIER, "TS-");

        // Appended check digit
        String withCheckDigit = pseudonymizer.addCheckDigit(psn, false, "TestStudie", "TS-");
        assertEquals(psn.length() + 1, withCheckDigit.length());
        assertTrue(withCheckDigit.startsWith(psn));
        assertTrue(new LuhnMod16CheckDigit().validateCheckDigit(withCheckDigit, "TS-"));

        // Check digit replacing the last character
        String replacedLastChar = pseudonymizer.addCheckDigit(psn, true, "TestStudie", "TS-");
        assertEquals(psn.length(), replacedLastChar.length());
        assertTrue(new LuhnMod16CheckDigit().validateCheckDigit(replacedLastChar, "TS-"));
    }

    @Test
    void consecutiveCheckDigitsNeverCutThePseudonym() {
        Algorithm algorithm = algorithm("CONSECUTIVE", 6);
        algorithm.setAlphabet("0123456789");
        Pseudonymizer pseudonymizer = factory.getPseudonymizer(algorithm);

        assertEquals("C-0000422", pseudonymizer.addCheckDigit("C-000042", true, "Consecutive", "C-"));
    }

    @Test
    void rejectsCheckDigitsForUnknownAlgorithms() {
        Pseudonymizer pseudonymizer = pseudonymizer("MD5", 32);
        pseudonymizer.setAlgorithmName("UNKNOWN");

        assertThrows(IllegalArgumentException.class, () -> pseudonymizer.addCheckDigit("ABC", false, "TestStudie", ""));
    }

    @Test
    void computesAndValidatesLuhnCheckDigits() {
        // Classic Luhn example
        assertEquals('3', new LuhnMod10CheckDigit().computeCheckDigit("7992739871", ""));
        assertTrue(new LuhnMod10CheckDigit().validateCheckDigit("79927398713", ""));
        assertFalse(new LuhnMod10CheckDigit().validateCheckDigit("79927398714", ""));
        assertFalse(new LuhnMod10CheckDigit().validateCheckDigit("79927398731", ""));

        // The prefix is ignored, case and trailing white spaces don't matter
        assertEquals('3', new LuhnMod10CheckDigit().computeCheckDigit("P-7992739871", "P-"));
        assertTrue(new LuhnMod10CheckDigit().validateCheckDigit("P-79927398713 ", "P-"));
        Character hexCheckDigit = new LuhnMod16CheckDigit().computeCheckDigit("1db52bb3", "");
        assertTrue(new LuhnMod16CheckDigit().validateCheckDigit("1DB52BB3" + hexCheckDigit, ""));

        // Characters outside of the alphabet can't get a check digit
        assertNull(new LuhnMod10CheckDigit().computeCheckDigit("12A4", ""));
    }

    @Test
    void checkDigitsDetectSingleCharacterErrors() {
        LuhnCheckDigit luhn = new LuhnMod36CheckDigit();
        String psn = "TRUSTDECK2026";
        String withCheckDigit = psn + luhn.computeCheckDigit(psn, "");

        for (int i = 0; i < psn.length(); i++) {
            char replacement = withCheckDigit.charAt(i) == 'A' ? 'B' : 'A';
            String changed = withCheckDigit.substring(0, i) + replacement + withCheckDigit.substring(i + 1);
            assertFalse(luhn.validateCheckDigit(changed, ""), "Changing position " + i + " was not detected.");
        }
    }

    private Pseudonymizer pseudonymizer(String name, int pseudonymLength) {
        return factory.getPseudonymizer(algorithm(name, pseudonymLength));
    }

    private Algorithm algorithm(String name, int pseudonymLength) {
        Algorithm algorithm = new Algorithm();
        algorithm.setId(1);
        algorithm.setName(name);
        algorithm.setAlphabet("ABCDEF0123456789");
        algorithm.setConsecutiveValueCounter(1L);
        algorithm.setPseudonymLength(pseudonymLength);
        algorithm.setPaddingCharacter("0");
        algorithm.setAddCheckDigit(false);
        algorithm.setLengthIncludesCheckDigit(false);
        algorithm.setSalt(null);
        return algorithm;
    }
}
