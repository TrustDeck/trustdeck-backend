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

package org.trustdeck.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.trustdeck.dto.EntityInstanceDTO;
import org.trustdeck.dto.EntityTypeDTO;
import org.trustdeck.dto.RecordLinkageCandidateDTO;
import org.trustdeck.linkage.LinkageTokenService;
import org.trustdeck.linkage.PPRLEncodingService;
import org.trustdeck.linkage.model.CandidateStatus;
import org.trustdeck.linkage.model.EntityLinkageConfig;
import org.trustdeck.linkage.model.LinkageFieldRule;
import org.trustdeck.linkage.model.LinkageToken;
import org.trustdeck.linkage.model.LinkageTokenType;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Performs entity-level record linkage. Candidate generation still uses
 * field-specific blocking tokens, while the final result is one score for the
 * complete entity. Each attribute can contribute at most its configured weight.
 * 
 * @author Armin Müller
 */
@Service
@Slf4j
public class RecordLinkageService {

	/** Service used to resolve effective linkage field rules from type definitions. */
    @Autowired
    private JsonSchemaService jsonSchemaService;

    /** Used to retrieve base types for given entity types. */
    @Autowired
    private EntityTypeDBService entityTypeService;

    /** The service used to generate linkage tokens from input payloads. */
    @Autowired
    private LinkageTokenService linkageTokenService;
    
    /** The service used to compare PPRL Bloom filter tokens. */
    @Autowired
    private PPRLEncodingService pprlService;

    /** Used to retrieve candidate entity instances from the database. */
    @Autowired
    private EntityInstanceDBService entityInstanceService;
    
    /** A factor to adjust the weight of a phonetic match. As it is less accurate than an exact match on a normalized string, the factor is usually < 1.0. */
    private static final double PHONETIC_MATCH_WEIGHT_FACTOR = 0.75;

    /**
     * Finds record-linkage candidates using the effective entity-level linkage
     * configuration of the given entity type.
     *
     * @param projectId the project containing the entity instances
     * @param entityType the entity type for which candidates should be found
     * @param payload the entity payload to compare
     * @param limit the maximum number of candidates to return
     * @param includeDeleted whether deleted entity instances should be included
     * @return the matching candidates ordered by score, an empty list if no
     *         candidates exist, or {@code null} if linkage cannot be performed
     */
    public List<RecordLinkageCandidateDTO> findCandidates(int projectId, EntityTypeDTO entityType, JsonNode payload, int limit, boolean includeDeleted) {
    	// Get the corresponding base type, if available
    	EntityTypeDTO baseType = entityType.getBaseTypeName() == null ? null : entityTypeService.getEntityTypeByName(entityType.getBaseTypeName(), null);
    	JsonNode baseDefinition = baseType == null ? null : baseType.getTypeDefinition();

    	// Resolve the effective entity-level linkage configuration
    	EntityLinkageConfig entityConfig = jsonSchemaService.resolveEntityLinkageConfig(entityType.getTypeDefinition(), baseDefinition);
    	if (!entityConfig.isEnabled()) {
    		return List.of();
    	}

    	// Resolve the field-level linkage rules and generate tokens for the payload
    	List<LinkageFieldRule> rules = jsonSchemaService.resolveLinkageFieldRules(entityType.getTypeDefinition(), baseDefinition);
    	List<LinkageToken> payloadTokens = linkageTokenService.buildTokens(entityConfig, rules, payload, projectId, entityType.getId());

    	if (payloadTokens.isEmpty()) {
    		log.trace("No linkage tokens could be generated for the payload.");
    		return null;
    	}

    	// Calculate the maximum score available for the payload
    	double maxPossibleScore = maxPossibleScore(payloadTokens);
    	if (maxPossibleScore <= 0.0) {
    		return null;
    	}

    	// Find candidate instances using the generated blocking tokens
    	List<Long> candidateIds = entityInstanceService.findCandidateIdsByBlockingTokens(projectId, entityType.getId(), payloadTokens, entityConfig.getCandidateLimit(), includeDeleted);
    	if (candidateIds.isEmpty()) {
    		return List.of();
    	}

    	// Load the matching entity instances
    	List<EntityInstanceDTO> instances = entityInstanceService.getEntityInstancesByIDs(candidateIds, entityType.getId(), includeDeleted);
    	if (instances == null) {
    		return null;
    	}
    	
    	if (instances.isEmpty()) {
    		return List.of();
    	}

    	// Load the stored linkage tokens for all candidate instances
    	Map<Long, List<LinkageToken>> tokensByInstance = entityInstanceService.getLinkageTokensForInstances(candidateIds, entityType.getId());

    	// Score and filter the candidate instances
    	List<RecordLinkageCandidateDTO> candidates = new ArrayList<>();
    	for (EntityInstanceDTO instance : instances) {
    		LinkageScoreResult score = score(payloadTokens, tokensByInstance.getOrDefault(instance.getId(), List.of()), entityConfig.getBloomMinSimilarity());
    		double normalizedScore = score.score() / maxPossibleScore;

    		// Only include candidates that satisfy both score thresholds
    		if (score.score() >= entityConfig.getMinScore() && normalizedScore >= entityConfig.getMinNormalizedScore()) {
    			boolean deleted = Boolean.TRUE.equals(instance.getIsDeleted());

    			candidates.add(RecordLinkageCandidateDTO.builder()
    					.entityInstance(instance)
    					.score(score.score())
    					.normalizedScore(normalizedScore)
    					.matchedOn(score.matchedOn())
    					.candidateStatus(deleted ? CandidateStatus.DELETED : CandidateStatus.ACTIVE)
    					.build());
    		}
    	}

    	// Return the highest-scoring candidates up to the requested limit
    	return candidates.stream()
    			.sorted(Comparator.comparingDouble(RecordLinkageCandidateDTO::getNormalizedScore)
    					.reversed()
    					.thenComparing(Comparator.comparingDouble(RecordLinkageCandidateDTO::getScore).reversed()))
    			.limit(limit)
    			.toList();
    }

    /**
     * Finds active record-linkage candidates using the effective entity-level
     * linkage configuration of the given entity type.
     *
     * @param projectId the project containing the entity instances
     * @param entityType the entity type for which candidates should be found
     * @param payload the entity payload to compare
     * @param limit the maximum number of candidates to return
     * @return the matching active candidates ordered by score
     */
    public List<RecordLinkageCandidateDTO> findCandidates(int projectId, EntityTypeDTO entityType, JsonNode payload, int limit) {
    	return findCandidates(projectId, entityType, payload, limit, false);
    }

    /**
     * Calculates the linkage score for a complete entity. Multiple tokens belonging
     * to the same attribute compete with each other, so only the strongest
     * contribution of each attribute is added to the entity score.
     *
     * @param payloadTokens the linkage tokens generated for the query payload
     * @param candidateTokens the stored linkage tokens of the candidate instance
     * @param bloomMinSimilarity the minimum accepted Bloom-filter similarity
     * @return the calculated score and descriptions of the contributing matches
     */
    private LinkageScoreResult score(List<LinkageToken> payloadTokens, List<LinkageToken> candidateTokens, double bloomMinSimilarity) {
    	// Group the candidate's non-blocking tokens by comparable field, tag, and type
    	Map<String, List<LinkageToken>> candidateTokensByKey = new HashMap<>();
    	for (LinkageToken token : candidateTokens) {
    		if (isBlockingToken(token)) {
    			continue;
    		}

    		candidateTokensByKey.computeIfAbsent(comparisonKey(token), ignored -> new ArrayList<>()).add(token);
    	}

    	// Remove duplicate scoring tokens from the payload
    	Map<String, LinkageToken> uniquePayloadTokens = new LinkedHashMap<>();
    	for (LinkageToken token : payloadTokens) {
    		if (!isBlockingToken(token)) {
    			uniquePayloadTokens.putIfAbsent(scoreTokenKey(token), token);
    		}
    	}

    	// Keep only the strongest contribution for each field
    	Map<String, Double> bestContributionByField = new HashMap<>();
    	Map<String, String> bestExplanationByField = new HashMap<>();

    	for (LinkageToken payloadToken : uniquePayloadTokens.values()) {
    		List<LinkageToken> matches = candidateTokensByKey.getOrDefault(comparisonKey(payloadToken), List.of());
    		MatchContribution contribution = calculateContribution(payloadToken, matches, bloomMinSimilarity);

    		if (contribution.score() <= 0.0) {
    			continue;
    		}

    		String field = payloadToken.getFieldPath();
    		if (contribution.score() > bestContributionByField.getOrDefault(field, 0.0)) {
    			bestContributionByField.put(field, contribution.score());
    			bestExplanationByField.put(field, contribution.explanation());
    		}
    	}

    	// Sum the strongest contribution of every matched field
    	double score = bestContributionByField.values().stream().mapToDouble(Double::doubleValue).sum();

    	return new LinkageScoreResult(score, List.copyOf(bestExplanationByField.values()));
    }

    /**
     * Calculates the score contribution of one payload token against all comparable
     * tokens of a candidate.
     *
     * @param payloadToken the token generated for the query payload
     * @param candidateTokens the comparable tokens of the candidate instance
     * @param bloomMinSimilarity the minimum accepted Bloom-filter similarity
     * @return the resulting score contribution and its explanation
     */
    private MatchContribution calculateContribution(LinkageToken payloadToken, List<LinkageToken> candidateTokens, double bloomMinSimilarity) {
    	// Calculate fuzzy similarity for Bloom-filter tokens
    	if (payloadToken.getTokenType() == LinkageTokenType.PPRL_BLOOM) {
    		double similarity = highestBloomSimilarity(payloadToken, candidateTokens);
    		if (similarity < bloomMinSimilarity) {
    			return MatchContribution.none();
    		}

    		return new MatchContribution(payloadToken.getWeight() * similarity, formatMatchedOn(payloadToken) + " similarity = " + "%.3f".formatted(similarity));
    	}

    	// Check whether an identical candidate token exists
    	boolean exactMatch = candidateTokens.stream().anyMatch(candidate -> payloadToken.getTokenValue().equals(candidate.getTokenValue()));
    	if (!exactMatch) {
    		return MatchContribution.none();
    	}

    	// Apply the score factor associated with the token type
    	double contribution = switch (payloadToken.getTokenType()) {
    		case NORM, PPRL_EXACT -> payloadToken.getWeight();
    		case PHONETIC -> payloadToken.getWeight() * PHONETIC_MATCH_WEIGHT_FACTOR;
    		case BLOCK, PPRL_BLOCK, PPRL_BLOOM -> 0.0;
    	};

    	return new MatchContribution(contribution, formatMatchedOn(payloadToken));
    }

    /**
     * Calculates the maximum possible score for the given payload tokens. The
     * maximum score is the sum of the highest available weight for each attribute,
     * rather than the sum of all generated tokens.
     *
     * @param payloadTokens the linkage tokens generated for the query payload
     * @return the maximum possible linkage score
     */
    private double maxPossibleScore(List<LinkageToken> payloadTokens) {
    	Map<String, Double> maxWeightByField = new HashMap<>();

    	// Determine the highest available scoring weight for every field
    	for (LinkageToken token : payloadTokens) {
    		if (isBlockingToken(token)) {
    			continue;
    		}

    		maxWeightByField.merge(token.getFieldPath(), token.getWeight(), Math::max);
    	}

    	// Sum the highest weights of all fields
    	return maxWeightByField.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * Determines the highest Bloom-filter similarity between a payload token and
     * the comparable candidate tokens.
     *
     * @param payloadToken the payload Bloom-filter token
     * @param candidateTokens the comparable candidate tokens
     * @return the highest Dice similarity, or {@code 0.0} if no Bloom token exists
     */
    private double highestBloomSimilarity(LinkageToken payloadToken, List<LinkageToken> candidateTokens) {
    	double highest = 0.0;

    	// Compare the payload Bloom filter with every candidate Bloom filter
    	for (LinkageToken candidateToken : candidateTokens) {
    		if (candidateToken.getTokenType() == LinkageTokenType.PPRL_BLOOM) {
    			highest = Math.max(highest, pprlService.diceSimilarity(payloadToken.getTokenValue(), candidateToken.getTokenValue()));
    		}
    	}

    	return highest;
    }

    /**
     * Checks whether the given token is used exclusively for candidate blocking.
     *
     * @param token the linkage token to check
     * @return {@code true} if the token is a plain-text or protected blocking token
     */
    private boolean isBlockingToken(LinkageToken token) {
    	return token.getTokenType() == LinkageTokenType.BLOCK || token.getTokenType() == LinkageTokenType.PPRL_BLOCK;
    }

    /**
     * Creates the key used to group mutually comparable linkage tokens.
     *
     * @param token the linkage token
     * @return the comparison key containing the field path, tag, and token type
     */
    private String comparisonKey(LinkageToken token) {
    	return token.getFieldPath() + "|" + token.getTag() + "|" + token.getTokenType();
    }

    /**
     * Creates a unique key for a scoring token, including its token value.
     *
     * @param token the linkage token
     * @return the unique scoring-token key
     */
    private String scoreTokenKey(LinkageToken token) {
    	return comparisonKey(token) + "|" + token.getTokenValue();
    }

    /**
     * Formats a human-readable description of the field and token variant that
     * contributed to a linkage score.
     *
     * @param token the contributing linkage token
     * @return the formatted match description
     */
    private String formatMatchedOn(LinkageToken token) {
    	return token.getFieldPath() + " [" + token.getTag() + ", " + token.getTokenType() + "]";
    }

    /**
     * Represents the score contribution of a single token comparison.
     *
     * @param score the calculated score contribution
     * @param explanation the human-readable explanation of the match
     */
    private record MatchContribution(double score, String explanation) {
    	
    	/**
    	 * Creates an empty contribution for a token that did not match.
    	 *
    	 * @return a contribution with a score of {@code 0.0}
    	 */
    	private static MatchContribution none() {
    		return new MatchContribution(0.0, null);
    	}
    }
    
    /**
     * Internal result object for record linkage scoring.
     * It contains the calculated score and the fields or tags that contributed to it.
     * 
     * @param score the calculated linkage score
     * @param matchedOn the list of fields or tags on which the candidate matched
     */
    private record LinkageScoreResult(double score, List<String> matchedOn) {}
}
