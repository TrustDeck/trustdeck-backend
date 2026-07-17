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

package org.trustdeck.linkage;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.trustdeck.linkage.model.EntityLinkageConfig;
import org.trustdeck.linkage.model.LinkageFieldRule;
import org.trustdeck.linkage.model.LinkageToken;
import org.trustdeck.linkage.model.LinkageTokenType;
import org.trustdeck.linkage.model.PPRLConfig;
import org.trustdeck.utils.Assertion;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Generates record-linkage tokens from complete entity payloads.
 * Entity-level configuration selects the privacy mode and PPRL parameters;
 * field rules control participation, pre-processing, blocking, and weight.
 *
 * @author Armin Müller
 */
@Service
@Slf4j
public class LinkageTokenService {

	/** The normalization service used for value normalization and phonetic encoding. */
    @Autowired
    private LinkageNormalizationService normalizationService;
    
    /** The service used to generate privacy-preserving linkage tokens. */
    @Autowired
    private PPRLEncodingService pprlEncodingService;

    /**
     * Generates record linkage tokens for a payload according to the provided linkage field rules.
     * For each linkage-enabled field, the raw input value is normalized first and then transformed
     * into one or more tokens for exact matching, phonetic matching, and blocking.
     *
     * @param entityConfig the entity-level linkage configuration
     * @param rules the linkage field rules that determine how tokens should be generated
     * @param payload the JSON payload from which the linkage tokens should be derived
     * @param projectId the (internal) database ID of the project in which the record linkage takes place
     * @param entityTypeId the (internal) database ID of the entity type of the record for which RL is done
     * @return the list of generated linkage tokens
     */
    public List<LinkageToken> buildTokens(EntityLinkageConfig entityConfig, List<LinkageFieldRule> rules, JsonNode payload, int projectId, int entityTypeId) {
        // Ensure non-null linkage configuration and rules-list
    	EntityLinkageConfig effectiveConfig = entityConfig == null ? new EntityLinkageConfig() : entityConfig;
        if (!effectiveConfig.isEnabled() || rules == null || rules.isEmpty()) {
            return List.of();
        }

        // Handle each rule for each attribute in the payload
        List<LinkageToken> tokens = new ArrayList<>();
        for (LinkageFieldRule rule : rules) {
            for (JsonNode valueNode : getNodesByPath(payload, rule.getPath())) {
                String rawValue = valueNode.asText(null);
                if (Assertion.isNullOrEmpty(rawValue)) {
                    continue;
                }
                
                // Normalize the raw values
                String normalized = switch (rule.getType()) {
                    case "date" -> normalizationService.normalizeDate(rawValue);
                    default -> normalizationService.normalize(rawValue, rule.getNormalizers());
                };

                if (Assertion.isNullOrEmpty(normalized)) {
                    continue;
                }

                // Build PPRL or plain-text tokens
                if (effectiveConfig.usesPprl()) {
                    tokens.addAll(buildPprlTokens(rule, normalized, rule.getTag(), false, effectiveConfig.getPprlConfig(), projectId, entityTypeId));

                    // Encoders remain attribute-level: in PPRL mode, their output is protected with a separate  
                    // context tag so it can only match the same encoder output of the same semantic attribute.
                    if (rule.getEncoders() != null) {
                        for (String encoder : rule.getEncoders()) {
                            String encoded = normalizationService.phoneticEncode(normalized, encoder);
                            if (Assertion.isNotNullOrEmpty(encoded)) {
                                String encodedTag = rule.getTag() + ":encoder:" + encoder.toLowerCase();
                                tokens.addAll(buildPprlTokens(rule, encoded, encodedTag, true, effectiveConfig.getPprlConfig(), projectId, entityTypeId));
                            }
                        }
                    }
                } else {
                    tokens.addAll(buildPlainTokens(rule, normalized));
                }
            }
        }

        return tokens;
    }
    
    /**
     * Generates the plaintext linkage tokens for a normalized value.
     * This includes normalized tokens, optional phonetic tokens, and blocking tokens.
     * 
     * @param rule the linkage field rule
     * @param normalized the normalized field value
     * @return the generated plaintext linkage tokens
     */
	private List<LinkageToken> buildPlainTokens(LinkageFieldRule rule, String normalized) {
		List<LinkageToken> tokens = new ArrayList<>();
	
		// Create and add normalized-linkage token
		tokens.add(new LinkageToken(rule.getPath(), rule.getTag(), LinkageTokenType.NORM, normalized, rule.getWeight()));
	
		// Phonetically encode the normalized string if encoders are configured
		if (rule.getEncoders() != null) {
			for (String encoder : rule.getEncoders()) {
				String encoded = normalizationService.phoneticEncode(normalized, encoder);
	
				if (Assertion.isNotNullOrEmpty(encoded)) {
                    // Add phonetically encoded-linkage token
					tokens.add(new LinkageToken(rule.getPath(), rule.getTag(), LinkageTokenType.PHONETIC, encoded, rule.getWeight()));
				}
			}
		}
	
		// Generate blocking tokens if blocking strategies are configured
		if (rule.getBlocking() != null) {
			for (String block : rule.getBlocking()) {
				tokens.addAll(buildBlockingTokens(rule, normalized, block));
			}
		}
	
		return tokens;
	}
	
	/**
     * Generates protected score and blocking tokens. The PPRL algorithm and all Bloom-filter parameters 
     * are entity-level. Blocking remains field-level: "exact" creates a protected exact block, "bloomBands" 
     * creates protected Bloom-band blocks, and "phonetic" creates a protected exact block for an encoded value.
     * 
	 * @param rule the linkage field rule defining blocking and weight settings
	 * @param preparedValue the normalized or encoded field value
	 * @param tokenTag the tag used to separate tokens belonging to different value variants
	 * @param encodedValue whether the prepared value was produced by a field encoder
	 * @param configuredPprl the entity-level PPRL configuration; may be {@code null}
	 * @param projectId the project ID used for PPRL context separation
	 * @param entityTypeId the entity type ID used for PPRL context separation
	 * @return the generated PPRL score and blocking tokens
	 */
    private List<LinkageToken> buildPprlTokens(LinkageFieldRule rule, String preparedValue, String tokenTag, boolean encodedValue, PPRLConfig configuredPprl, int projectId, int entityTypeId) {
    	// Check if the project and type context are given
    	if (projectId <= 0 || entityTypeId <= 0) {
            throw new IllegalArgumentException("PPRL token generation requires projectId and entityTypeId.");
        }

    	// Get or create the entity-level PPRL config
        PPRLConfig config = configuredPprl == null ? new PPRLConfig() : configuredPprl;
        List<LinkageToken> tokens = new ArrayList<>();
        String method = config.getMethod() == null ? PPRLConfig.METHOD_NGRAM_BLOOM_FILTER : config.getMethod();

        // Encoded values use phonetic blocking, while normalized values use exact blocking
        boolean exactBlocking = encodedValue ? hasBlocking(rule, "phonetic") : hasBlocking(rule, "exact");

        // Generate score and blocking tokens based on the entity-level PPRL method
        switch (method.trim().toLowerCase()) {
            case "hmacexact" -> {
            	// Generate the protected exact token used for scoring
                String exact = pprlEncodingService.hmacExact(projectId, entityTypeId, tokenTag, preparedValue);
                tokens.add(new LinkageToken(rule.getPath(), tokenTag, LinkageTokenType.PPRL_EXACT, exact, rule.getWeight()));
                
                // Reuse the exact token as a blocking key if configured for the field
                if (exactBlocking) {
                    tokens.add(new LinkageToken(rule.getPath(), tokenTag, LinkageTokenType.PPRL_BLOCK, exact, rule.getWeight()));
                }
            }
            case "ngrambloomfilter" -> {
            	// Encode the prepared value into a protected Bloom filter for fuzzy scoring
            	BitSet bloomFilter = pprlEncodingService.buildBloomFilter(projectId, entityTypeId, tokenTag, preparedValue, config);
                String encodedBloom = pprlEncodingService.encodeBloomFilter(bloomFilter, config.getLength());
                
                tokens.add(new LinkageToken(rule.getPath(), tokenTag, LinkageTokenType.PPRL_BLOOM, encodedBloom, rule.getWeight()));

                // Generate Bloom-band blocking tokens only for non-encoded values
                if (!encodedValue && hasBlocking(rule, "bloomBands")) {
                    for (String bandToken : pprlEncodingService.buildBloomBandTokens(projectId, entityTypeId, tokenTag, bloomFilter, config)) {
                        tokens.add(new LinkageToken(rule.getPath(), tokenTag, LinkageTokenType.PPRL_BLOCK, bandToken, rule.getWeight()));
                    }
                }

                // Generate the exact HMAC once if needed for scoring or blocking
                String exact = null;
                if (config.isExact() || exactBlocking) {
                    exact = pprlEncodingService.hmacExact(projectId, entityTypeId, tokenTag, preparedValue);
                }
                
                // Add optional exact-agreement scoring
                if (config.isExact()) {
                    tokens.add(new LinkageToken(rule.getPath(), tokenTag, LinkageTokenType.PPRL_EXACT, exact, rule.getWeight()));
                }
                
                // Add exact blocking for the prepared value
                if (exactBlocking) {
                    tokens.add(new LinkageToken(rule.getPath(), tokenTag, LinkageTokenType.PPRL_BLOCK, exact, rule.getWeight()));
                }
            }
            default -> log.debug("Unknown entity-level PPRL method \"" + method + "\".");
        }

        return tokens;
    }
    
    /**
     * Checks whether the field rule contains the specified blocking strategy.
     * @param rule the linkage field rule
     * @param expected the blocking strategy to check for
     * @return {@code true} if the strategy is configured, otherwise {@code false}
     */
    private boolean hasBlocking(LinkageFieldRule rule, String expected) {
        if (rule.getBlocking() == null) {
            return false;
        }
        
        return rule.getBlocking().stream().anyMatch(expected::equalsIgnoreCase);
    }

    /**
     * Generates blocking tokens for a normalized field value according to the 
     * requested blocking strategy. Blocking tokens are used to reduce the number 
     * of candidate records for which a detailed score needs to be calculated.
     * 
     * @param rule the linkage field rule that defines the source field and linkage token properties
     * @param normalized the normalized field value from which the blocking tokens should be derived
     * @param block the blocking strategy that should be applied
     * @return the list of generated blocking tokens
     */
    private List<LinkageToken> buildBlockingTokens(LinkageFieldRule rule, String normalized, String block) {
    	List<LinkageToken> tokens = new ArrayList<>();
    	
    	switch (block.trim().toLowerCase()) {
    		case "exact" -> 
    			// Uses the full normalized value itself as the blocking key
    			tokens.add(new LinkageToken(rule.getPath(), rule.getTag(), LinkageTokenType.BLOCK, 
    					normalized, rule.getWeight()));
    		case "prefix3" -> {
    			// Uses the first three characters of the normalized value as the blocking key
    			addPrefixBlock(tokens, rule, normalized, 3);
    		}
    		case "prefix4" -> {
    			// Uses the first four characters of the normalized value as the blocking key
    			addPrefixBlock(tokens, rule, normalized, 4);
    		}
    		case "prefix6" -> {
    			// Uses the first six characters of the normalized value as the blocking key
    			addPrefixBlock(tokens, rule, normalized, 6);
    		}
    		// Uses one or more phonetic encodings of the normalized value so 
    		// that similarly sounding values can fall into the same candidate group
    		case "phonetic" -> {
    			if (rule.getEncoders() == null || rule.getEncoders().isEmpty()) {
    				log.trace("Skipping phonetic blocking for field \"" + rule.getPath() + "\" because no phonetic encoder is configured.");
    				break;
    			}

    			for (String encoder : rule.getEncoders()) {
    				String phonetic = normalizationService.phoneticEncode(normalized, encoder);

    				if (Assertion.isNotNullOrEmpty(phonetic)) {
    					tokens.add(new LinkageToken(rule.getPath(), rule.getTag(), LinkageTokenType.BLOCK, 
    							phonetic, rule.getWeight()));
    				}
    			}
    		}
    		case "year" -> {
    			// Uses only the year component of a normalized ISO date as the blocking key
    			if ("date".equals(rule.getType())) {
    				tokens.add(new LinkageToken(rule.getPath(), rule.getTag(), LinkageTokenType.BLOCK,
    						normalizationService.yearFromDate(normalized), rule.getWeight()));
    			}
    		}
    		case "yearmonth" -> {
    			// Uses the year and month component of a normalized ISO date as the blocking key
    			if ("date".equals(rule.getType())) {
    				tokens.add(new LinkageToken(rule.getPath(), rule.getTag(), LinkageTokenType.BLOCK,
    						normalizationService.yearMonthFromDate(normalized), rule.getWeight()));
    			}
    		}
    	}
    	
    	return tokens;
    }
    
    /** 
     * Adds a prefix-based blocking token if the normalized value is long enough.
     * 
     * @param tokens the list to which the blocking token is added
     * @param rule the linkage field rule
     * @param normalized the normalized field value
     * @param length the required prefix length
     */
    private void addPrefixBlock(List<LinkageToken> tokens, LinkageFieldRule rule, String normalized, int length) {
        if (normalized.length() >= length) {
            tokens.add(new LinkageToken(rule.getPath(), rule.getTag(), LinkageTokenType.BLOCK, normalized.substring(0, length), rule.getWeight()));
        }
    }

    /**
     * Resolves all JSON nodes that match a logical dot-separated path.
     * If an array is encountered while traversing the path, the remaining path
     * is applied to each array element and all matching result nodes are collected.
     * 
     * @param root the root JSON node from which the values should be retrieved
     * @param path the logical dot-separated path of the desired field
     * @return the list of all JSON nodes that match the given path
     */
    private List<JsonNode> getNodesByPath(JsonNode root, String path) {
    	if (root == null || Assertion.isNullOrEmpty(path)) {
    		return List.of();
    	}
    	
    	return collectNodesByPath(root, path.split("\\."), 0);
    }

    /**
     * Recursively traverses a JSON structure and collects all nodes that match
     * the given path parts starting at the provided index.
     * 
     * When an array node is encountered, the same path index is used for each array
     * element so that the remaining path is applied to all entries of the array.
     * When the full path has been consumed, the current node is added to the result.
     * If the final node is an array, all of its elements are added individually.
     * 
     * @param current the JSON node currently inspected during traversal
     * @param pathParts the split logical path that should be resolved
     * @param pathIndex the current index inside the path parts array
     * @return the target list that receives all matching result nodes
     */
    private List<JsonNode> collectNodesByPath(JsonNode current, String[] pathParts, int pathIndex) {
    	List<JsonNode> out = new ArrayList<>();
    	
    	if (current == null) {
    		return out;
    	}
    	
    	// Check the leaf-element 
    	if (pathIndex >= pathParts.length) {
    		if (current.isArray()) {
    			for (JsonNode element : current) {
    				out.add(element);
    			}
    		} else {
    			out.add(current);
    		}
    		
    		return out;
    	}
    	
    	// Check the current node (which is not a leaf-node)
    	if (current.isArray()) {
    		for (JsonNode element : current) {
    			out.addAll(collectNodesByPath(element, pathParts, pathIndex));
    		}
    		
    		return out;
    	}

    	if (current.isObject()) {
    		out.addAll(collectNodesByPath(current.get(pathParts[pathIndex]), pathParts, pathIndex + 1));
    	}
    	
    	return out;
    }
}
