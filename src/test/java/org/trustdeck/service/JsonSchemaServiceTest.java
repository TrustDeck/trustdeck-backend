package org.trustdeck.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;

class JsonSchemaServiceTest {

    private final ObjectMapper om = new ObjectMapper();

    private final JsonSchemaService service = new JsonSchemaService(om);

    private static final String BASE_DEFINITION = """
            {
              "attributes": [
                { "name": "firstName", "type": "string", "required": true, "linkage": false, "minLength": 1, "maxLength": 50 },
                { "name": "age", "type": "integer", "required": false, "linkage": false, "minimum": 0, "maximum": 150 },
                { "name": "sex", "type": "enum", "required": false, "linkage": false, "values": ["female", "male", "diverse"] }
              ]
            }
            """;

    @Test
    void acceptsValidDefinition() throws Exception {
        assertEquals(List.of(), service.validateDefinition(json(BASE_DEFINITION)));
    }

    @Test
    void acceptsDefinitionWithContainersAndRepeatableGroups() throws Exception {
        JsonNode definition = json("""
                {
                  "attributes": [
                    { "layout": "row", "attributes": [
                      { "name": "firstName", "type": "string", "required": true, "linkage": false },
                      { "name": "lastName", "type": "string", "required": true, "linkage": false }
                    ] },
                    { "name": "addresses", "layout": "group", "repeatable": true, "attributes": [
                      { "name": "city", "type": "string", "required": true, "linkage": false }
                    ] }
                  ]
                }
                """);

        assertEquals(List.of(), service.validateDefinition(definition));
    }

    @Test
    void rejectsDefinitionsViolatingTheMetaSchema() throws Exception {
        assertFalse(service.validateDefinition(json("{ \"attributes\": [] }")).isEmpty());
        assertFalse(service.validateDefinition(json("{ \"typeName\": \"person\" }")).isEmpty());
        assertFalse(service.validateDefinition(json("""
                { "attributes": [ { "name": "firstName", "type": "text", "required": true, "linkage": false } ] }
                """)).isEmpty());
        assertFalse(service.validateDefinition(json("""
                { "attributes": [ { "name": "firstName", "type": "string", "linkage": false } ] }
                """)).isEmpty());
    }

    @Test
    void rejectsDuplicatedAttributeNames() throws Exception {
        JsonNode definition = json("""
                {
                  "attributes": [
                    { "name": "firstName", "type": "string", "required": true, "linkage": false },
                    { "layout": "col", "attributes": [
                      { "name": "firstName", "type": "string", "required": false, "linkage": false }
                    ] }
                  ]
                }
                """);

        assertTrue(service.validateDefinition(definition).stream().anyMatch(error -> error.contains("Duplicated")));
    }

    @Test
    void rejectsConstraintsThatDoNotMatchTheAttributeType() throws Exception {
        assertFalse(service.validateDefinition(json("""
                { "attributes": [ { "name": "sex", "type": "enum", "required": true, "linkage": false } ] }
                """)).isEmpty());
        assertFalse(service.validateDefinition(json("""
                { "attributes": [ { "name": "age", "type": "integer", "required": true, "linkage": false, "minLength": 1 } ] }
                """)).isEmpty());
        assertFalse(service.validateDefinition(json("""
                { "attributes": [ { "name": "name", "type": "string", "required": true, "linkage": false, "minimum": 1 } ] }
                """)).isEmpty());
        assertFalse(service.validateDefinition(json("""
                { "attributes": [ { "layout": "row", "repeatable": true, "attributes": [
                  { "name": "name", "type": "string", "required": true, "linkage": false } ] } ] }
                """)).isEmpty());
    }

    @Test
    void validatesInstancesAgainstTheDefinition() throws Exception {
        JsonSchema schema = service.getCompiledSchemaFromDefinition(json(BASE_DEFINITION));

        assertEquals(List.of(), service.validateInstance(json("{ \"firstName\": \"Erika\", \"age\": 42, \"sex\": \"female\" }"), schema));
        assertEquals(List.of(), service.validateInstance(json("{ \"firstName\": \"Erika\" }"), schema));

        // Missing required attribute
        assertFalse(service.validateInstance(json("{ \"age\": 42 }"), schema).isEmpty());
        // Wrong type
        assertFalse(service.validateInstance(json("{ \"firstName\": \"Erika\", \"age\": \"42\" }"), schema).isEmpty());
        // Constraint violations
        assertFalse(service.validateInstance(json("{ \"firstName\": \"\" }"), schema).isEmpty());
        assertFalse(service.validateInstance(json("{ \"firstName\": \"Erika\", \"age\": 151 }"), schema).isEmpty());
        assertFalse(service.validateInstance(json("{ \"firstName\": \"Erika\", \"sex\": \"unknown\" }"), schema).isEmpty());
        // Unknown attribute
        assertFalse(service.validateInstance(json("{ \"firstName\": \"Erika\", \"nickname\": \"Eri\" }"), schema).isEmpty());
    }

    @Test
    void validatesRepeatableGroupsInInstances() throws Exception {
        JsonSchema schema = service.getCompiledSchemaFromDefinition(json("""
                {
                  "attributes": [
                    { "name": "addresses", "layout": "group", "repeatable": true, "attributes": [
                      { "name": "city", "type": "string", "required": true, "linkage": false }
                    ] }
                  ]
                }
                """));

        assertEquals(List.of(), service.validateInstance(json("{ \"addresses\": [ { \"city\": \"Berlin\" }, { \"city\": \"Hamburg\" } ] }"), schema));
        assertFalse(service.validateInstance(json("{ \"addresses\": { \"city\": \"Berlin\" } }"), schema).isEmpty());
        assertFalse(service.validateInstance(json("{ \"addresses\": [ { } ] }"), schema).isEmpty());
    }

    @Test
    void acceptsProjectTypesThatExtendTheBaseType() throws Exception {
        JsonNode projectDefinition = json("""
                {
                  "attributes": [
                    { "name": "firstName", "type": "string", "required": true, "linkage": false, "minLength": 2, "maxLength": 40 },
                    { "name": "age", "type": "integer", "required": true, "linkage": false, "minimum": 18, "maximum": 150 },
                    { "name": "sex", "type": "enum", "required": false, "linkage": false, "values": ["female", "male", "diverse"] },
                    { "name": "studyId", "type": "string", "required": false, "linkage": false }
                  ]
                }
                """);

        assertEquals(List.of(), service.validateProjectTypeIsSuperset(json(BASE_DEFINITION), projectDefinition));
    }

    @Test
    void rejectsProjectTypesThatWeakenTheBaseType() throws Exception {
        JsonNode baseDefinition = json(BASE_DEFINITION);

        // Missing base attribute
        assertFalse(service.validateProjectTypeIsSuperset(baseDefinition, json("""
                { "attributes": [
                  { "name": "firstName", "type": "string", "required": true, "linkage": false, "minLength": 1, "maxLength": 50 },
                  { "name": "age", "type": "integer", "required": false, "linkage": false, "minimum": 0, "maximum": 150 } ] }
                """)).isEmpty());

        // Changed type
        assertFalse(service.validateProjectTypeIsSuperset(baseDefinition, json(BASE_DEFINITION.replace(
                "\"name\": \"age\", \"type\": \"integer\"", "\"name\": \"age\", \"type\": \"number\""))).isEmpty());

        // Required attribute becomes optional
        assertFalse(service.validateProjectTypeIsSuperset(baseDefinition, json(BASE_DEFINITION.replace(
                "\"name\": \"firstName\", \"type\": \"string\", \"required\": true", "\"name\": \"firstName\", \"type\": \"string\", \"required\": false"))).isEmpty());

        // Weaker constraint
        assertFalse(service.validateProjectTypeIsSuperset(baseDefinition, json(BASE_DEFINITION.replace(
                "\"maximum\": 150", "\"maximum\": 200"))).isEmpty());
        assertFalse(service.validateProjectTypeIsSuperset(baseDefinition, json(BASE_DEFINITION.replace(
                "\"maxLength\": 50", "\"maxLength\": 100"))).isEmpty());
    }

    private JsonNode json(String content) throws Exception {
        return om.readTree(content);
    }
}
