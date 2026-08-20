/*
 * Trust Deck Services
 * Copyright 2026 Armin Müller and Eric Wündisch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.trustdeck.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds configurable application defaults from the {@code app.defaults} section.
 *
 * @author Armin Müller
 */
@Component
@ConfigurationProperties(prefix = "app.defaults")
@Getter
@Setter
public class DefaultProperties {
	
    /** Default values applied when creating domains. */
    private Domain domain = new Domain();
    
    /** Default values applied when creating algorithms. */
    private Algorithm algorithm = new Algorithm();
    
    /** Default values applied when creating and searching pseudonyms. */
    private Pseudonym pseudonym = new Pseudonym();
    
    /** Default values applied when creating projects. */
    private Project project = new Project();
    
    /** Default values applied when creating and searching permissions. */
    private Permission permission = new Permission();
    
    /** Default values applied to the local permission cache. */
    private Cache cache = new Cache();
    
    /** Default values applied during record linkage. */
    private Linkage linkage = new Linkage();

    /** Default values for domain creation. */
    @Getter
    @Setter
    public static class Domain {
        /** Whether domains allow multiple pseudonyms for an identifier. */
        private boolean allowMultiplePsn = false;
        
        /** Whether domain end dates are enforced. */
        private boolean enforceEndDateValidity = true;
        
        /** Whether domain start dates are enforced. */
        private boolean enforceStartDateValidity = true;
        
        /** Whether domain changes propagate to child domains. */
        private boolean performRecursiveChanges = true;
        
        /** Validity period for a domain without an explicit end date. */
        private String validityTime = "30 years";
        
        /** Maximum number of domains returned by a search. */
        private int searchResultLimit = 20;
    }

    /** 
     * Default values for algorithm creation.
     */
    @Getter
    @Setter
    public static class Algorithm {
        /** Algorithm name used when none is supplied. */
        private String name = "RANDOM";
        
        /** Desired capacity for a random algorithm. */
        private long randomDesiredSize = 1000000000L;
        
        /** Desired success probability for a random algorithm. */
        private double randomDesiredSuccessProbability = 0.999999998d;
        
        /** Alphabet used for a random algorithm. */
        private String randomAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        
        /** Initial value for a consecutive algorithm. */
        private long consecutiveValueCounter = 1L;
        
        /** Pseudonym length used when none is supplied. */
        private int pseudonymLength = 16;
        
        /** Pseudonym length used for a default random algorithm. */
        private int randomPseudonymLength = 10;
        
        /** Number of generation attempts before reporting a collision. */
        private int numberOfRetries = 3;
        
        /** Character used to pad pseudonyms. */
        private String paddingCharacter = "0";
        
        /** Whether generated pseudonyms include a check digit. */
        private boolean addCheckDigit = true;
        
        /** Whether a check digit counts towards the configured length. */
        private boolean lengthIncludesCheckDigit = true;
        
        /** Length of generated algorithm salts. */
        private int saltLength = 32;
    }

    /**
     * Default values for pseudonym operations.
     */
    @Getter
    @Setter
    public static class Pseudonym {
        /** Maximum number of pseudonyms accepted in a batch. */
        private int batchLength = 50000;
        
        /** Whether affected pseudonyms are regenerated on update. */
        private boolean regenerateOnUpdate = true;
        
        /** Maximum number of pseudonyms returned by a search. */
        private int searchResultLimit = 30;
    }

    /**
     * Default values for project creation.
     */
    @Getter
    @Setter
    public static class Project {
        /** Validity duration, in years, for a project without an end date. */
        private int validityYears = 10;
        
        /** Whether projects store entities by default. */
        private boolean storeEntities = true;
        
        /** Whether projects store pseudonyms by default. */
        private boolean storePseudonyms = true;
    }

    /**
     * Default values for permission operations.
     */
    @Getter
    @Setter
    public static class Permission {
        /** Maximum number of users returned by a user search. */
        private int userSearchResultLimit = 20;
        
        /** Validity duration, in days, for a permission without an end date. */
        private int validityDays = 3650;
    }

    /**
     * Default values for the local permission cache.
     */
    @Getter
    @Setter
    public static class Cache {
        /** Name assigned to the local Hazelcast instance. */
        private String instanceName = "trustdeck-hazelcast";
        
        /** Cache entry lifetime, in minutes. */
        private long ttlMinutes = 15;
    }

    /**
     * Default values for record linkage.
     */
    @Getter
    @Setter
    public static class Linkage {
        /** Weight multiplier for phonetic matches. */
        private double phoneticMatchWeightFactor = 0.75d;
    }
}
