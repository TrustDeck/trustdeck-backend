package org.trustdeck.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import org.trustdeck.service.AlgorithmDBService;

class XxHashPseudonymizerTest {

    @Test
    void producesPlainXxHash64ValuesWithoutSalt() {
        Pseudonymizer pseudonymizer = pseudonymizer(null);

        assertEquals("0ff9ec2d200c293e", pseudonymizer.pseudonymize("fad0fcfe01f8e15592af59626aebadd17a0f3340ad1f9339fd586cde9ef43dbdea750336056381cbe68a21bedb9c312fc7a39e872beacde0cae8f7162a73d0dd", ""));
        assertEquals("0b242d361fda71bc", pseudonymizer.pseudonymize("The quick brown fox jumps over the lazy dog", ""));
        assertEquals("44ad33705751ad73", pseudonymizer.pseudonymize("The quick brown fox jumps over the lazy dog.", ""));
        assertEquals("b7b41276360564d4", pseudonymizer.pseudonymize("1", ""));
        assertEquals("0ed74b125c8ae7f1", pseudonymizer.pseudonymize("fewfwewwedwdwwwwwxsdafvdsssss", ""));
    }

    @Test
    void usesSaltAsSeedAndAddsPrefix() {
        String unsalted = pseudonymizer(null).pseudonymize("1", "");
        String salted = pseudonymizer("azMPTIQXJsept_4nDj5B1BXN83Bj_8VJ").pseudonymize("1", "TS-");

        assertEquals(19, salted.length());
        assertEquals("TS-", salted.substring(0, 3));
        assertNotEquals(unsalted, salted.substring(3));
    }

    @Test
    void isSelectedByTheFactory() {
        PseudonymizationFactory factory = new PseudonymizationFactory(new DefaultProperties(), mock(AlgorithmDBService.class));

        assertEquals(XxHashPseudonymizer.class, factory.getPseudonymizer(algorithm(null)).getClass());
    }

    private Pseudonymizer pseudonymizer(String salt) {
        Algorithm algorithm = algorithm(salt);
        AlgorithmDBService algorithmDBService = mock(AlgorithmDBService.class);
        when(algorithmDBService.getAlgorithmByID(algorithm.getId())).thenReturn(algorithm);

        return new XxHashPseudonymizer(true, algorithm, new DefaultProperties(), algorithmDBService);
    }

    private Algorithm algorithm(String salt) {
        Algorithm algorithm = new Algorithm();
        algorithm.setId(1);
        algorithm.setName("XXHASH");
        algorithm.setAlphabet("0123456789abcdef");
        algorithm.setConsecutiveValueCounter(1L);
        algorithm.setPseudonymLength(16);
        algorithm.setPaddingCharacter("0");
        algorithm.setAddCheckDigit(false);
        algorithm.setLengthIncludesCheckDigit(false);
        algorithm.setSalt(salt);
        return algorithm;
    }
}
