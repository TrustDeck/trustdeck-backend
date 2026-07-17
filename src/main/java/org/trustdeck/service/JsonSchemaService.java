/*
 * Trust Deck Services
 * Copyright 2025-2026 Armin Müller
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooq.JSONB;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.trustdeck.linkage.model.EntityLinkageConfig;
import org.trustdeck.linkage.model.LinkageFieldRule;
import org.trustdeck.linkage.model.PPRLConfig;
import org.trustdeck.utils.LRUCache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import lombok.extern.slf4j.Slf4j;

/**
 * Class used to handle the entity type's JSON schemas and their validation.
 * Includes a simple LRU caching for compiled schemas.
 * 
 * @author Armin Müller
 */
@Service
@Slf4j
public class JsonSchemaService {

	/** A mapper for transforming JSONB into JsonNode and back. */
	private ObjectMapper om;

	/** The schema factory used for building the schemas used for validating entity types and instances. */
	private JsonSchemaFactory factory;

	/** Stores (caches) the meta schema used to validate user-provided schemas. */
	private JsonSchema metaSchema;
	
	/** Least recently used cache for the compiled type schemas. */
	private final Map<String, JsonSchema> compiledCache = Collections.synchronizedMap(new LRUCache<>(50));
	
	/** Path of the meta schema that defines how the entity type schema's are allowed to look like. */
	private static final String META_SCHEMA_PATH = "entity-schemas/definition-meta-schema.json";

	/**
	 * Constructor that defines the object mapper and initializes the schema factory
	 * and the meta schema.
	 * 
	 * @param om the object mapper to map from the meta schema file to a JsonNode (will be auto injected by spring boot)
	 */
	public JsonSchemaService(ObjectMapper om) {
		this.om = om;
		this.factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

		// Load the meta schema from file into the application
		try (InputStream metaSchema = new ClassPathResource(META_SCHEMA_PATH).getInputStream()) {
			// Map the input stream to the proper type
			JsonNode meta = om.readTree(metaSchema);
			this.metaSchema = factory.getSchema(meta);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load definition meta-schema.", e);
		}
	}

	/**
	 * Validates the given definition.
	 * 
	 * @param definition the schema definition
	 * @return a list of human-readable errors (an empty list means its valid)
	 */
	public List<String> validateDefinition(JsonNode definition) {
		// Validate the given schema and collect all messages (errors)
		Set<ValidationMessage> msgs = metaSchema.validate(definition);

		// Check if there are any messages in the set
		if (!msgs.isEmpty()) {
			return msgs.stream().map(ValidationMessage::getMessage).collect(Collectors.toList());
		}

		// Check additional constraints that cannot (easily) be defined in the schema
		Set<String> seenPaths = new HashSet<>();

		return validateAttributes(definition.get("attributes"), "", seenPaths);
	}

	/**
	 * Build an instance JSON schema from a validated definition in JsonNode form 
	 * that can be used for validating entity instances.
	 * 
	 * @param definition the validated schema definition
	 * @return an instance JSON schema
	 */
	public JsonNode buildInstanceSchema(JsonNode definition) {
		// Build schema object from the JsonNode type
		ObjectNode schema = buildObjectSchemaFromAttributes(definition.get("attributes"));
		schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
		return schema;
	}
	
	/**
	 * Build an instance JSON schema from a validated definition in JSONB form 
	 * that can be used for validating entity instances.
	 * 
	 * @param definition the validated schema definition
	 * @return an instance JSON schema
	 */
	public JsonNode buildInstanceSchema(JSONB definition) {
		JsonNode schema;
		try {
			schema = om.readTree(definition.toString());
		} catch (JsonProcessingException e) {
			log.debug("Failed to parse JSONB into JsonNode while building the instance schema.", e);
			return null;
		}
		
		return buildInstanceSchema(schema);
	}

	/**
	 * Ensure the project specific type definition (projectDef) is a superset of a
	 * base type (baseDef):
	 * <ul>
	 *   <li>Contains all base attributes (no deletions)</li>
	 *   <li>Same type for shared attributes</li>
	 *   <li>Constraints can only be stricter, never weaker:
	 *     <ul>
	 *       <li>numbers: min &ge; base.min; max &le; base.max</li>
	 *       <li>strings: minLength &ge; base.minLength; maxLength &le; base.maxLength</li>
	 *       <li>pattern: require equality to base type</li>
	 *     </ul>
	 *   </li>
	 * </ul>
	 * 
	 * @param baseDef the type definition of the base type
	 * @param projectDef the type definition for the project-specific type
	 * @return a list of errors encountered during validation
	 */
	public List<String> validateProjectTypeIsSuperset(JsonNode baseDef, JsonNode projectDef) {
		List<String> errors = new ArrayList<>();

		// Map attributes by name
		Map<String, JsonNode> baseAttributes = flattenLeafNodes(baseDef);
		Map<String, JsonNode> projectAttributes = flattenLeafNodes(projectDef);

		// Ensure that there are no attribute deletions: project must contain all base attributes
		for (String path : baseAttributes.keySet()) {
			if (!projectAttributes.containsKey(path)) {
				errors.add("Project specific type definition is missing base attribute \"" + path + "\".");
			}
		}
		
		if (!errors.isEmpty())
			return errors;

		// For shared attributes, ensure type equality, constraints not weaker
		for (String path : baseAttributes.keySet()) {
			JsonNode b = baseAttributes.get(path);
			JsonNode p = projectAttributes.get(path);

			// Check type equality
			String bType = normalizeType(b);
			String pType = normalizeType(p);

			if (!bType.equals(pType)) {
				errors.add("Attribute \"" + path + "\" changes type from \"" + bType + "\" to \"" + pType + "\".");
				continue;
			}
			
			// Check equal repeatability
			boolean bRepeatable = b.path("repeatable").asBoolean(false);
			boolean pRepeatable = p.path("repeatable").asBoolean(false);
			if (bRepeatable != pRepeatable) {
				errors.add("Attribute \"" + path + "\" changes repeatable from \"" + bRepeatable + "\" to \"" + pRepeatable + "\".");
			}

			// Check equal required-status
			boolean bRequired = b.path("required").asBoolean(false);
			boolean pRequired = p.path("required").asBoolean(false);
			if (bRequired && !pRequired) {
				errors.add("Attribute \"" + path + "\" is required in the base type and must remain required.");
			}
			
			// Check equal linkage-status
			boolean bLinkage = b.path("linkage").asBoolean(false);
			boolean pLinkage = p.path("linkage").asBoolean(false);
			if (bLinkage && !pLinkage) {
			    errors.add("Attribute \"" + path + "\" is linkage-enabled in the base type and must remain linkage-enabled.");
			}

			// Check equal linkage tags
			Set<String> bTags = jsonArraytoStringSet(b.get("tags"));
			Set<String> pTags = jsonArraytoStringSet(p.has("tags") ? p.get("tags") : b.get("tags"));
			if (!pTags.containsAll(bTags)) {
			    errors.add("Attribute \"" + path + "\" must preserve all linkage tags from the base type.");
			}

			// Check that constraints are equal or stricter
			if (b.has("minimum")) {
				if (!p.has("minimum") || p.get("minimum").asDouble() < b.get("minimum").asDouble()) {
					errors.add("Attribute \"" + path + "\": project minimum must be >= base minimum.");
				}
			}

			if (b.has("maximum")) {
				if (!p.has("maximum") || p.get("maximum").asDouble() > b.get("maximum").asDouble()) {
					errors.add("Attribute \"" + path + "\": project maximum must be <= base maximum.");
				}
			}

			if (b.has("minLength")) {
				if (!p.has("minLength") || p.get("minLength").asInt() < b.get("minLength").asInt()) {
					errors.add("Attribute \"" + path + "\": project minLength must be >= base minLength.");
				}
			}

			if (b.has("maxLength")) {
				if (!p.has("maxLength") || p.get("maxLength").asInt() > b.get("maxLength").asInt()) {
					errors.add("Attribute \"" + path + "\": project maxLength must be <= base maxLength.");
				}
			}

			// Ensure string constraints are kept
			if (b.has("pattern")) {
				if (!p.has("pattern")) {
					errors.add("Attribute \"" + path + "\": base has a pattern; project must keep the same pattern.");
				} else if (!b.get("pattern").asText().equals(p.get("pattern").asText())) {
					errors.add("Attribute \"" + path + "\": project pattern must equal base pattern (\"" + b.get("pattern").asText() + "\").");
				}
			}
			
			// Check the allowed values (for enums)
			Set<String> baseAllowed = extractAllowedValues(b);
			Set<String> projectAllowed = extractAllowedValues(p);
			if (baseAllowed != null) {
				if (projectAllowed == null) {
					errors.add("Attribute \"" + path + "\": base defines allowed values; project must keep allowed values.");
				} else if (!baseAllowed.containsAll(projectAllowed)) {
					errors.add("Attribute \"" + path + "\": project allowed values must be equal to or a subset of the base allowed values.");
				}
			}
		}

		return errors;
	}

	/**
	 * Validate an instance JSON against an instance schema.
	 * 
	 * @param instance the instance as a JsonNode
	 * @param instanceSchema the schema to evaluate against
	 * @return a list of validation errors (an empty list means its valid)
	 */
	public List<String> validateInstance(JsonNode instance, JsonNode instanceSchema) {
		// Compile schema so it can be used
		JsonSchema compiled = factory.getSchema(instanceSchema);

		// Validate and return possible error messages to the caller
		Set<ValidationMessage> msgs = compiled.validate(instance);
		return msgs.stream().map(ValidationMessage::getMessage).collect(Collectors.toList());
	}

	/**
	 * Validate an instance JSONB against an instance schema.
	 * 
	 * @param instanceData the instance as a JSONB
	 * @param instanceSchema the schema to evaluate against
	 * @return a list of validation errors (an empty list means its valid)
	 */
	public List<String> validateInstance(JSONB instanceData, JsonNode instanceSchema) {
		// Parse instance data
		JsonNode instance;
		try {
			instance = om.readTree(instanceData.toString());
		} catch (JsonProcessingException e) {
			log.debug("Failed to parse JSONB into JsonNode while validating the instance payload.", e);
			return null;
		}
		
		return validateInstance(instance, instanceSchema);
	}

	/**
	 * Validate an instance JSONB against a previously compiled instance schema.
	 * 
	 * @param instanceData the instance as a JSONB
	 * @param compiledInstanceSchema the already compiled schema to evaluate against
	 * @return a list of validation errors (an empty list means its valid)
	 */
	public List<String> validateInstance(JSONB instanceData, JsonSchema compiledInstanceSchema) {
		// Parse instance data
		JsonNode instance;
		try {
			instance = om.readTree(instanceData.toString());
		} catch (JsonProcessingException e) {
			log.debug("Failed to parse JSONB into JsonNode while validating the instance payload.", e);
			return List.of("Malformed JSON payload: " + e.getMessage());
		}
	
		// Validate and return any encountered errors
		Set<ValidationMessage> msgs = compiledInstanceSchema.validate(instance);
        return msgs.stream().map(ValidationMessage::getMessage).collect(Collectors.toList());
	}

	/**
	 * Validate an instance JsonNode against a previously compiled instance schema.
	 * 
	 * @param data the JSON data as a JsonNode
	 * @param compiledInstanceSchema the already compiled schema to evaluate against
	 * @return a list of validation errors (an empty list means its valid)
	 */
	public List<String> validateInstance(JsonNode data, JsonSchema compiledInstanceSchema) {
		// Validate and return any encountered errors
		Set<ValidationMessage> msgs = compiledInstanceSchema.validate(data);
        return msgs.stream().map(ValidationMessage::getMessage).collect(Collectors.toList());
	}

	/**
	 * Build or reuse a compiled schema for an entity type (build
	 * from its definition or get from LRU-cached).
	 * Convenience method that allows using the JSONB directly.
	 * 
	 * @param typeDefinition the type definition
	 * @return the compiled schema
	 */
	public JsonSchema getCompiledSchemaFromDefinition(JSONB typeDefinition) {
		// Parse the JSONB
		JsonNode def;
		try {
			def = om.readTree(typeDefinition.toString());
		} catch (JsonProcessingException e) {
			log.debug("Failed to parse JSONB into JsonNode while retrieving the compiled schema.", e);
			return null;
		}
		
		return getCompiledSchemaFromDefinition(def);
	}

	/**
	 * Build or reuse a compiled schema for an entity type (build
	 * from its definition or get from LRU-cached).
	 * 
	 * @param typeDefinition the type definition
	 * @return the compiled schema
	 */
	public JsonSchema getCompiledSchemaFromDefinition(JsonNode typeDefinition) {
		boolean useCache = true;
		
		// Build the schema from the definition
		JsonNode instanceSchemaNode = buildInstanceSchema(typeDefinition);
		
		// Canonicalize the schema and hash it
		byte[] canonical = canonicalize(instanceSchemaNode);
		String key = null;
		try {
			key = sha256Base64(canonical);
		} catch (IllegalStateException e) {
			log.debug("Failed to compute hash, so caching is unavailable. Compile instead.");
			useCache = false;
		}

		if (useCache) {
			log.trace("Checking cache for compiled schema.");
		
			// Check the (locked) cache for an already compiled schema
			JsonSchema cached;
			synchronized (compiledCache) {
				cached = compiledCache.get(key);
			}
			
			if (cached != null) {
				log.trace("Cache hit: using compiled schema from cache.");
				return cached;
			} else {
				log.trace("Cache miss: compile schema and cache it.");
			}
		}

		// No cached schema found or no cache used: compile the schema
		log.trace("Compiling schema ...");
		JsonSchema compiled = factory.getSchema(instanceSchemaNode);

		if (useCache) {
			// Cache the newly compiled schema (use double-checked locking)
			synchronized (compiledCache) {
				JsonSchema existing = compiledCache.get(key);
				if (existing != null) {
					// The schema was cached in the meantime; return it
					return existing;
				}
				
				// Add to cache
				compiledCache.put(key, compiled);
				log.trace("Added compiled schema to cache.");
			}
		}
		
		return compiled;
	}
	
	/**
	 * Method to create an entity linkage configuration from the given type definition.
	 * 
	 * @param typeDefinition the project-specific entity type's definition
	 * @param baseDefinition the entity type definition of the base type that this project-specific entity is based on
	 * @return the record linkage configuration object for this entity
	 */
	public EntityLinkageConfig resolveEntityLinkageConfig(JsonNode typeDefinition, JsonNode baseDefinition) {
		// Retrieve the linkage config from base and project-specific type
		JsonNode projectConfig = objectProperty(typeDefinition, "recordLinkage");
		JsonNode baseConfig = objectProperty(baseDefinition, "recordLinkage");

		// Build linkage config for this entity
		EntityLinkageConfig config = new EntityLinkageConfig();
		config.setEnabled(booleanValue(projectConfig, baseConfig, "enabled", config.isEnabled()));
		config.setPrivacyMode(textValue(projectConfig, baseConfig, "privacyMode", config.getPrivacyMode()));
		config.setMinScore(doubleValue(projectConfig, baseConfig, "minScore", config.getMinScore()));
		config.setMinNormalizedScore(doubleValue(projectConfig, baseConfig, "minNormalizedScore", config.getMinNormalizedScore()));
		config.setBloomMinSimilarity(doubleValue(projectConfig, baseConfig, "bloomMinSimilarity", config.getBloomMinSimilarity()));
		config.setCandidateLimit(intValue(projectConfig, baseConfig, "candidateLimit", config.getCandidateLimit()));
		config.setAutoLinkOnCreate(booleanValue(projectConfig, baseConfig, "autoLinkOnCreate", config.isAutoLinkOnCreate()));
		config.setOnMatch(textValue(projectConfig, baseConfig, "onMatch", config.getOnMatch()));

		// Decide whether or not to use PPRL
		JsonNode projectPprl = objectProperty(projectConfig, "pprl");
		JsonNode basePprl = objectProperty(baseConfig, "pprl");
		config.setPprlConfig(resolvePprlConfig(projectPprl, basePprl));
		
		return config;
	}
	
	/**
	 * Resolves the effective record-linkage rules for all linkage-enabled leaf 
	 * attributes of a project-specific type definition.
	 * 
	 * Linkage flags, tags, and linkage settings may be inherited from the 
	 * corresponding base-type attribute. Project-specific values take precedence. 
	 * Attributes without an effective {@code linkage} flag or default rule are skipped. 
	 * 
	 * @param typeDefinition the project-specific type definition
	 * @param baseDefinition the optional base-type definition
	 * @return the resolved linkage rules for all eligible leaf attributes
	 */
	public List<LinkageFieldRule> resolveLinkageFieldRules(JsonNode typeDefinition, JsonNode baseDefinition) {
		// Get a flat map of the base type's leaf nodes, if available 
		Map<String, JsonNode> baseLeafNodes = (baseDefinition == null) ? Map.of() : flattenLeafNodes(baseDefinition);
		
		// Get a flat map of the project-specific type's leaf nodes
		Map<String, JsonNode> projectLeafNodes = flattenLeafNodes(typeDefinition);

		// Create the rules
		List<LinkageFieldRule> resolvedRules = new ArrayList<>();
		for (Map.Entry<String, JsonNode> entry : projectLeafNodes.entrySet()) {
			String path = entry.getKey();
			JsonNode projectSpecificNode = entry.getValue();
			JsonNode baseNode = baseLeafNodes.get(path);

			// Check if this attribute should be used for record linkage purposes
			if (!projectSpecificNode.path("linkage")
					.asBoolean(baseNode != null && baseNode.path("linkage").asBoolean(false))) {
				continue;
			}

			// Use either the base type's default-linkage-configuration or the sproject-spefic type's one if there is no base type
			LinkageFieldRule rule = applyDefaultLinkageConfig(path, (baseNode != null) ? baseNode : projectSpecificNode);
			if (rule == null) {
				continue;
			}

			// Set the tag for the rule by either using the project-sp. type's tag or the base type's instead
			JsonNode tagSourceNode = projectSpecificNode.has("tags") ? projectSpecificNode : baseNode;
			if (tagSourceNode != null && tagSourceNode.has("tags") && tagSourceNode.get("tags").isArray()
					&& !tagSourceNode.get("tags").isEmpty()) {
				rule.setTag(tagSourceNode.get("tags").get(0).asText());
			}

			// Decide which linkage config to use
			JsonNode linkageConfigNode = projectSpecificNode.has("linkageConfig")
					? projectSpecificNode.get("linkageConfig")
					: (baseNode != null ? baseNode.get("linkageConfig") : null);

			// Add normalizer, encoder, blocking, comparator, and weight settings to the rule
			if (linkageConfigNode != null) {
				if (linkageConfigNode.has("normalizers")) {
					rule.setNormalizers(jsonArrayToStringList(linkageConfigNode.get("normalizers")));
				}
				
				if (linkageConfigNode.has("encoders")) {
					rule.setEncoders(jsonArrayToStringList(linkageConfigNode.get("encoders")));
				}
				
				if (linkageConfigNode.has("blocking")) {
					rule.setBlocking(jsonArrayToStringList(linkageConfigNode.get("blocking")));
				}
				
				if (linkageConfigNode.has("weight")) {
					rule.setWeight(linkageConfigNode.get("weight").asDouble());
				}
			}

			// Done building the rule for this attribute
			resolvedRules.add(rule);
		}

		return resolvedRules;
	}
	
	/**
	 * Validate attribute nodes in an attribute array. 
	 * 
	 * @param attributes the JsonNode representation of the attribute-array
	 * @param currentPath the path of the attributes in the JSON object
	 * @param seenPaths the list of paths that were already validated
	 * @return a list of encountered errors
	 */
	private List<String> validateAttributes(JsonNode attributes, String currentPath, Set<String> seenPaths) {
		List<String> errors = new ArrayList<>();
		
		// Check if it's actually an array attribute
		if (attributes == null || !attributes.isArray()) {
			return errors;
		}

		for (JsonNode attr : attributes) {
			errors.addAll(validateAttributeNode(attr, currentPath, seenPaths));
		}
		
		return errors;
	}

	/**
	 * Method to validate individual attributes.
	 * 
	 * @param attr the attribute to validate
	 * @param currentPath the path of the attribute in the JSON object
	 * @param errors the list of encountered errors
	 * @param seenPaths the list of paths that were already validated
	 */
	private List<String> validateAttributeNode(JsonNode attr, String currentPath, Set<String> seenPaths) {
		List<String> errors = new ArrayList<>();
		
		// Check if the node is a container for nested nodes
		if (isContainerNode(attr)) {
			// The current node is a container and therefore defines a layout; extract the layout, name, and repeatability
			String layout = attr.path("layout").asText("");
			String name = getOptionalName(attr);
			boolean repeatable = attr.path("repeatable").asBoolean(false);

			// Check if we have a name
			if (("row".equalsIgnoreCase(layout) || "col".equalsIgnoreCase(layout)) && name != null) {
				errors.add("layout \"" + layout + "\" must not define a name.");
			}

			// Ensure that repeatability is not set to 'true' for rows or columns
			if (("row".equalsIgnoreCase(layout) || "col".equalsIgnoreCase(layout)) && repeatable) {
				errors.add("repeatable is not allowed for layout \"" + layout + "\".");
			}

			if (repeatable && name == null) {
				errors.add("repeatable is only allowed on named groups or leaf attributes.");
			}

			// If the current attribute is a group, append the group name to the path that is used for the next validation step
			String nextPath = currentPath;
			if (isNamedGroup(attr)) {
				String groupPath = appendPath(currentPath, attr.get("name").asText());
				if (!seenPaths.add(groupPath)) {
					errors.add("Duplicated attribute name/path: \"" + groupPath + "\".");
				}
				
				nextPath = groupPath;
			}

			// Recursively check nested attributes
			errors.addAll(validateAttributes(attr.get("attributes"), nextPath, seenPaths));
			return errors;
		}

		String name = attr.get("name").asText();
		String type = attr.get("type").asText();
		String fullPath = appendPath(currentPath, name);

		if (!seenPaths.add(fullPath)) {
			log.trace("Found a duplicated attribute path in the JSON Schema: " + fullPath);
			errors.add("Duplicated attribute name/path: \"" + fullPath + "\".");
		}

		// Check the allowed values and constraints when the attribute is an enum
		if ("enum".equalsIgnoreCase(type)) {
			if (!hasAllowedValues(attr)) {
				errors.add("values (or enum) must be provided for enum attribute \"" + fullPath + "\".");
			}

			if (attr.has("minimum")) {
				errors.add("minimum only valid for number/integer (attribute \"" + fullPath + "\").");
			}
			
			if (attr.has("maximum")) {
				errors.add("maximum only valid for number/integer (attribute \"" + fullPath + "\").");
			}
			
			if (attr.has("minLength")) {
				errors.add("minLength only valid for string (attribute \"" + fullPath + "\").");
			}
			
			if (attr.has("maxLength")) {
				errors.add("maxLength only valid for string (attribute \"" + fullPath + "\").");
			}
			
			if (attr.has("pattern")) {
				errors.add("pattern only valid for string (attribute \"" + fullPath + "\").");
			}
			
			return errors;
		}

		// Ensure that we do not have an orphaned values-field
		if (attr.has("values")) {
			errors.add("values only valid for enum (attribute \"" + fullPath + "\").");
		}

		// Ensure proper constraints for non-string types
		if (!"string".equalsIgnoreCase(type)) {
			if (attr.has("minLength")) {
				errors.add("minLength only valid for string (attribute \"" + fullPath + "\").");
			}
			if (attr.has("maxLength")) {
				errors.add("maxLength only valid for string (attribute \"" + fullPath + "\").");
			}
			if (attr.has("pattern")) {
				errors.add("pattern only valid for string (attribute \"" + fullPath + "\").");
			}
		}

		// Ensure proper constraints for non-number types
		if (!"integer".equalsIgnoreCase(type) && !"number".equalsIgnoreCase(type)) {
			if (attr.has("minimum")) {
				errors.add("minimum only valid for number/integer (attribute \"" + fullPath + "\").");
			}
			if (attr.has("maximum")) {
				errors.add("maximum only valid for number/integer (attribute \"" + fullPath + "\").");
			}
		}

		// Ensure semantic correctness of constraints
		if (attr.has("minLength") && attr.has("maxLength")
				&& attr.get("minLength").asInt() > attr.get("maxLength").asInt()) {
			errors.add("minLength must be <= maxLength (attribute \"" + fullPath + "\").");
		}

		if (attr.has("minimum") && attr.has("maximum")
				&& attr.get("minimum").asDouble() > attr.get("maximum").asDouble()) {
			errors.add("minimum must be <= maximum (attribute \"" + fullPath + "\").");
		}
		
		return errors;
	}

	/**
	 * Build a schema object from the attributes of a node.
	 * 
	 * @param attributes the attributes of a given object/node
	 * @return the schema for the given object/node
	 */
	private ObjectNode buildObjectSchemaFromAttributes(JsonNode attributes) {
		ObjectNode schema = om.createObjectNode();
		schema.put("type", "object");
		schema.put("additionalProperties", false);

		ObjectNode properties = om.createObjectNode();
		ArrayNode required = om.createArrayNode();

		// Traverse all child attribute definitions and convert them into JSON Schema properties of the current object schema
		if (attributes != null && attributes.isArray()) {
			for (JsonNode defAttribute : attributes) {
				// Container nodes may either become a nested group property or be flattened into the current 
				// object, depending on their layout/name
				if (isContainerNode(defAttribute)) {
					ObjectNode childObjectSchema = buildObjectSchemaFromAttributes(defAttribute.get("attributes"));

					// Named groups become real nested properties in the instance schema: 
					// { "name": "address", "layout": "group", ... }  becomes "address": { "type": "object", ... }
					if (isNamedGroup(defAttribute)) {
						String groupName = defAttribute.get("name").asText();
						JsonNode groupSchema = childObjectSchema;

						// Repeatable groups are represented as arrays of objects
						if (defAttribute.path("repeatable").asBoolean(false)) {
							groupSchema = wrapArraySchema(childObjectSchema);
						}

						properties.set(groupName, groupSchema);
						continue;
					}

					// All other container nodes (row, col, unnamed group) are layout-only wrappers, they should not  
					// create another nesting level in the stored instance data. Instead, their child properties are  
					// merged directly into the surrounding object schema.
					JsonNode childPropertiesNode = childObjectSchema.get("properties");
					if (childPropertiesNode != null && childPropertiesNode.isObject()) {
						Iterator<Map.Entry<String, JsonNode>> fields = childPropertiesNode.fields();
						
						while (fields.hasNext()) {
							Map.Entry<String, JsonNode> entry = fields.next();
							properties.set(entry.getKey(), entry.getValue());
						}
					}

					// If the child contains required fields, they also need to be propagated to the parent 
					// because the container itself was flattened into the current object
					JsonNode childRequiredNode = childObjectSchema.get("required");
					if (childRequiredNode != null && childRequiredNode.isArray()) {
						for (JsonNode req : childRequiredNode) {
							required.add(req.asText());
						}
					}
					
					continue;
				}
				
				// Leaf attributes become direct properties of the current object schema
				String name = defAttribute.get("name").asText();
				properties.set(name, buildLeafSchema(defAttribute));
				
				if (defAttribute.path("required").asBoolean(false)) {
					required.add(name);
				}
			}
		}

		schema.set("properties", properties);
		if (required.size() > 0) {
			schema.set("required", required);
		}

		return schema;
	}

	/**
	 * Builds the JSON Schema fragment for a non-container attribute / leaf attribute definition.
	 * 
	 * @param defAttribute the leaf attribute definition from the type definition
	 * @return a JSON Schema node representing the given leaf attribute
	 * @throws IllegalArgumentException if the attribute uses a type that is not supported by the schema generator
	 */
	private JsonNode buildLeafSchema(JsonNode defAttribute) {
		ObjectNode attribute = om.createObjectNode();
		String type = defAttribute.get("type").asText();

		// Handle the type
		switch (type) {
			case "string":
				attribute.put("type", "string");
				break;
			case "integer":
				attribute.put("type", "integer");
				break;
			case "number":
				attribute.put("type", "number");
				break;
			case "boolean":
				attribute.put("type", "boolean");
				break;
			case "date":
				attribute.put("type", "string");
				attribute.put("format", "date");
				break;
			case "datetime":
				attribute.put("type", "string");
				attribute.put("format", "date-time");
				break;
			case "enum":
				attribute.put("type", "string");
				
				ArrayNode allowedValues = extractAllowedValuesArray(defAttribute);
				if (allowedValues != null) {
					attribute.set("enum", allowedValues);
				}
				
				break;
			default:
				throw new IllegalArgumentException("Unsupported type: " + type);
		}

		// Add the constraints if there are any
		copyIfPresent(defAttribute, attribute, "minimum");
		copyIfPresent(defAttribute, attribute, "maximum");
		copyIfPresent(defAttribute, attribute, "minLength");
		copyIfPresent(defAttribute, attribute, "maxLength");
		copyIfPresent(defAttribute, attribute, "pattern");

		if (!attribute.has("enum") && defAttribute.has("enum")) {
			attribute.set("enum", defAttribute.get("enum"));
		}

		// Handle list representation
		if (defAttribute.path("repeatable").asBoolean(false)) {
			return wrapArraySchema(attribute);
		}

		return attribute;
	}

	/**
	 * Wraps a given schema node into a JSON Schema array definition.
	 * 
	 * @param itemSchema the schema describing the individual array elements
	 * @return a JSON Schema object with {@code type: "array"} and the given {@code items} schema
	 */
	private ObjectNode wrapArraySchema(JsonNode itemSchema) {
		ObjectNode arraySchema = om.createObjectNode();
		arraySchema.put("type", "array");
		arraySchema.set("items", itemSchema);
		
		return arraySchema;
	}

	/**
	 * Flattens all leaf attributes of a type definition into a map, the key being by their attribute path.
	 * 
	 * @param definition the full type definition whose leaf attributes should be flattened
	 * @return a map from logical attribute paths to their corresponding leaf definition nodes
	 */
	private Map<String, JsonNode> flattenLeafNodes(JsonNode definition) {
		if (definition == null) {
			return new HashMap<>();
		}
		
		return collectLeafNodes(definition.get("attributes"), "");
	}

	/**
	 * Recursively collects all leaf attributes from a definition subtree and stores them 
	 * in a flat map with the keys being their logical path.
	 * 
	 * @param attributes the array of attribute definition nodes to inspect
	 * @param currentPath the current path prefix created from parent named groups
	 * @return the target map that receives the collected leaf nodes, keyed by their full logical path
	 */
	private Map<String, JsonNode> collectLeafNodes(JsonNode attributes, String currentPath) {
		Map<String, JsonNode> flat = new HashMap<>();
		
		if (attributes == null || !attributes.isArray()) {
			return flat;
		}

		for (JsonNode node : attributes) {
			if (isContainerNode(node)) {
				String nextPath = currentPath;
				if (isNamedGroup(node)) {
					nextPath = appendPath(currentPath, node.get("name").asText());
				}
				
				flat.putAll(collectLeafNodes(node.get("attributes"), nextPath));
			} else {
				flat.put(appendPath(currentPath, node.get("name").asText()), node);
			}
		}
		
		return flat;
	}

	/**
	 * Checks if the given node is a container, i.e. if it has attributes.
	 * 
	 * @param node the node to check
	 * @return {@code true} if the node is a container, {@code false} otherwise
	 */
	private boolean isContainerNode(JsonNode node) {
		return node != null && node.has("attributes");
	}

	/**
	 * Checks if a given node is a group-layout-descriptor with a name.
	 * 
	 * @param node the node to check
	 * @return {@code true} if the node is a group-layout-descriptor with a name, {@code false} otherwise
	 */
	private boolean isNamedGroup(JsonNode node) {
		return node != null && "group".equalsIgnoreCase(node.path("layout").asText(""))
				&& node.hasNonNull("name") && !node.get("name").asText().isBlank();
	}

	/**
	 * If a node has a name, this method extracts it.
	 * 
	 * @param node the node to check
	 * @return the name if available
	 */
	private String getOptionalName(JsonNode node) {
		if (node != null && node.hasNonNull("name")) {
			String value = node.get("name").asText();
			
			return value == null || value.isBlank() ? null : value;
		}
		
		return null;
	}
	
	/**
	 * Creates the default record linkage rule for a single leaf attribute definition.
	 * The returned rule contains the baseline linkage configuration for the given field
	 * and can later be refined or overridden by an explicit linkage configuration from a
	 * project-specific entity type or its corresponding base type.
	 * 
	 * @param path the logical path of the field inside the type definition
	 * @param node the leaf definition node from which the default linkage rule should be derived
	 * @return the default linkage rule for the given field
	 */
	private LinkageFieldRule applyDefaultLinkageConfig(String path, JsonNode node) {
		if (node == null) {
			return null;
		}
		
		// Extract the name
		String name = node.path("name").asText();
		
		// Extract the data type of the attribute which this rule is for
		String type = node.path("type").asText("string");
		
		// Extract the semantic tag for this attribute
		String tag = null;
		if (node.has("tags") && node.get("tags").isArray() && !node.get("tags").isEmpty()) {
			tag = node.get("tags").get(0).asText();
		}
		
		// Build the rule
		LinkageFieldRule rule = LinkageFieldRule.builder()
			.path(path)
			.name(name)
			.tag((tag != null && !tag.isBlank()) ? tag : "generic." + name)
			.type(type)
			.linkage(node.path("linkage").asBoolean(false))
			.build();
		
		// If the attribute is a date, use date-related defaults
		switch (type) {
			case "date" -> {
				rule.setNormalizers(List.of("trim"));
				rule.setEncoders(List.of());
				rule.setBlocking(List.of("exact", "year", "yearMonth", "bloomBands"));
				rule.setWeight(3.0);
			}
			default -> {
				rule.setNormalizers(List.of("trim", "lower", "collapseWhitespace"));
				rule.setEncoders(List.of());
				rule.setBlocking(List.of("exact", "prefix4", "bloomBands"));
				rule.setWeight(1.0);
			}
		}
		
		return rule;
	}

	/**
	 * Converts a JSON array node into a list of strings.
	 * Non-array nodes result in an empty list.
	 * 
	 * @param arrayNode the JSON array node whose values should be converted to strings
	 * @return a list containing the string representation of all array elements, or an empty list if the node is not an array
	 */
	private List<String> jsonArrayToStringList(JsonNode arrayNode) {
		if (arrayNode == null || !arrayNode.isArray()) {
			return List.of();
		}

		List<String> values = new ArrayList<>();
		for (JsonNode node : arrayNode) {
			values.add(node.asText());
		}
		
		return values;
	}
	
	/**
	 * Converts a JSON array node into a set of strings.
	 * Non-array or {@code null} nodes result in an empty set.
	 * 
	 * @param arrayNode the JSON array node whose values should be converted to strings
	 * @return a set containing the string representation of all array elements, or an empty set if the node is not an array
	 */
	private Set<String> jsonArraytoStringSet(JsonNode arrayNode) {
	    Set<String> out = new HashSet<>();
	    
	    if (arrayNode != null && arrayNode.isArray()) {
	        for (JsonNode n : arrayNode) {
	            out.add(n.asText());
	        }
	    }
	    
	    return out;
	}

	/**
	 * Method to append the attribute name to the current path.
	 * 
	 * @param currentPath the path up until the currently processed node
	 * @param name the attribute name to add
	 * @return the path including the attribute name, separated by a dot ('.')
	 */
	private String appendPath(String currentPath, String name) {
		return currentPath == null || currentPath.isBlank() ? name : currentPath + "." + name;
	}

	/**
	 * Normalize this nodes type. Turns "enum" into a "string".
	 * 
	 * @param node the node to check
	 * @return the node's type or "string", whenever the type was "enum" 
	 */
	private String normalizeType(JsonNode node) {
		String type = node.path("type").asText();
		
		return "enum".equalsIgnoreCase(type) ? "string" : type;
	}

	/**
	 * Checks if a given node has a list of allowed values (e.g. for an enum definition).
	 * 
	 * @param node the node to check
	 * @return {@code true} when the node has a non-empty values attribute or is an enum, {@code false} otherwise
	 */
	private boolean hasAllowedValues(JsonNode node) {
		return (node.has("values") && node.get("values").isArray() && node.get("values").size() > 0)
				|| (node.has("enum") && node.get("enum").isArray() && node.get("enum").size() > 0);
	}

	/**
	 * Retrieves the list of allowed values from a node as an {@link ArrayNode}.
	 * 
	 * @param node the node to check
	 * @return the list of allowed values or enum values if available
	 */
	private ArrayNode extractAllowedValuesArray(JsonNode node) {
		if (node.has("values") && node.get("values").isArray()) {
			return (ArrayNode) node.get("values").deepCopy();
		}
		
		if (node.has("enum") && node.get("enum").isArray()) {
			return (ArrayNode) node.get("enum").deepCopy();
		}
		
		return null;
	}

	/**
	 * Retrieves the list of allowed values from a node as a {@link Set} of strings.
	 * 
	 * @param node the node to check
	 * @return the list of allowed values or enum values if available
	 */
	private Set<String> extractAllowedValues(JsonNode node) {
		ArrayNode values = extractAllowedValuesArray(node);
		if (values == null) {
			return null;
		}

		Set<String> result = new HashSet<>();
		for (JsonNode value : values) {
			result.add(value.asText());
		}
		
		return result;
	}

	/**
	 * Ensures the JSON is always written in a consistent way by sorting
	 * object keys alphabetically before converting it to bytes.
	 * 
	 * @param node the JSON to canonicalize
	 * @return a byte array representing the canonicalized form of the given JSON
	 */
	private byte[] canonicalize(JsonNode node) {
		try {
			ObjectMapper omCopy = om.copy();
			omCopy.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
			
			return omCopy.writeValueAsBytes(node);
		} catch (IOException e) {
			// Fallback to String
			return node.toString().getBytes(StandardCharsets.UTF_8);
		}
	}
	
	/**
	 * Returns the specified child property if it exists and is a JSON object.
	 * 
	 * @param parent the parent node
	 * @param property the property name
	 * @return the object property, or {@code null} if unavailable or not an object
	 */
	private JsonNode objectProperty(JsonNode parent, String property) {
		if (parent == null || !parent.isObject() || !parent.has(property) || !parent.get(property).isObject()) {
			return null;
		}
		
		return parent.get(property);
	}

	/**
	 * Resolves a text property from the primary node, then the fallback node.
	 * 
	 * @param primary the preferred source node
	 * @param fallback the fallback source node
	 * @param property the property name
	 * @param defaultValue the value returned if the property is unavailable
	 * @return the resolved text value
	 */
	private String textValue(JsonNode primary, JsonNode fallback, String property, String defaultValue) {
		if (primary != null && primary.hasNonNull(property)) {
			return primary.get(property).asText(defaultValue);
		}
		
		if (fallback != null && fallback.hasNonNull(property)) {
			return fallback.get(property).asText(defaultValue);
		}
		
		return defaultValue;
	}

	/**
	 * Resolves a boolean property from the primary node, then the fallback node.
	 * 
	 * @param primary the preferred source node
	 * @param fallback the fallback source node
	 * @param property the property name
	 * @param defaultValue the value returned if the property is unavailable
	 * @return the resolved boolean value
	 */
	private boolean booleanValue(JsonNode primary, JsonNode fallback, String property, boolean defaultValue) {
		if (primary != null && primary.hasNonNull(property)) {
			return primary.get(property).asBoolean(defaultValue);
		}
		
		if (fallback != null && fallback.hasNonNull(property)) {
			return fallback.get(property).asBoolean(defaultValue);
		}
		
		return defaultValue;
	}

	/**
	 * Resolves a double property from the primary node, then the fallback node.
	 * 
	 * @param primary the preferred source node
	 * @param fallback the fallback source node
	 * @param property the property name
	 * @param defaultValue the value returned if the property is unavailable
	 * @return the resolved double value
	 */
	private double doubleValue(JsonNode primary, JsonNode fallback, String property, double defaultValue) {
		if (primary != null && primary.hasNonNull(property)) {
			return primary.get(property).asDouble(defaultValue);
		}
		
		if (fallback != null && fallback.hasNonNull(property)) {
			return fallback.get(property).asDouble(defaultValue);
		}
		
		return defaultValue;
	}

	/**
	 * Resolves an integer property from the primary node, then the fallback node.
	 * 
	 * @param primary the preferred source node
	 * @param fallback the fallback source node
	 * @param property the property name
	 * @param defaultValue the value returned if the property is unavailable
	 * @return the resolved integer value
	 */
	private int intValue(JsonNode primary, JsonNode fallback, String property, int defaultValue) {
		if (primary != null && primary.hasNonNull(property)) {
			return primary.get(property).asInt(defaultValue);
		}
		
		if (fallback != null && fallback.hasNonNull(property)) {
			return fallback.get(property).asInt(defaultValue);
		}
		
		return defaultValue;
	}

	/**
	 * Resolves the effective PPRL configuration using project-specific values,
	 * inherited base values, and the defaults defined by {@link PPRLConfig}.
	 * 
	 * @param projectPprl the project-specific PPRL configuration
	 * @param basePprl the inherited base-type PPRL configuration
	 * @return the resolved PPRL configuration
	 */
	private PPRLConfig resolvePprlConfig(JsonNode projectPprl, JsonNode basePprl) {
		PPRLConfig config = new PPRLConfig();
		config.setMethod(textValue(projectPprl, basePprl, "method", config.getMethod()));
		config.setN(intValue(projectPprl, basePprl, "n", config.getN()));
		config.setLength(intValue(projectPprl, basePprl, "length", config.getLength()));
		config.setHashPositions(intValue(projectPprl, basePprl, "hashPositions", config.getHashPositions()));
		config.setBandSize(intValue(projectPprl, basePprl, "bandSize", config.getBandSize()));
		config.setExact(booleanValue(projectPprl, basePprl, "exact", config.isExact()));
		
		return config;
	}

	/**
	 * Computes the SHA-256 hash of the given byte 
	 * array and returns it as a Base64 string.
	 * 
	 * @param data the input data to hash
	 * @return the Base64 encoded SHA-256 hash of the input
	 * @throws IllegalStateException if the SHA-256 algorithm is not available on the system
	 */
	private static String sha256Base64(byte[] data) throws IllegalStateException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest(data));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available.", e);
		}
	}

	/**
	 * Helper method that copies optional constraint keywords from an attribute
	 * definition node into the generated instance-schema property node, but only if
	 * the keyword exists. Copying is done by call-by-reference.
	 * 
	 * @param from the attribute definition node
	 * @param to the generated instance-schema property node
	 * @param field the optional constraint keyword (e.g., minimum, maximum, enum, ...)
	 */
	private static void copyIfPresent(JsonNode from, ObjectNode to, String field) {
		if (from.has(field)) {
			to.set(field, from.get(field));
		}
	}
}
