/*
 * Trust Deck Services
 * Copyright 2022-2025 Armin Müller and Eric Wündisch
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.trustdeck.dto.DomainDTO;
import org.trustdeck.dto.AlgorithmDTO;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.utils.Assertion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * This class offers tests for testing the REST endpoints.
 *
 * @author Armin Müller and Eric Wündisch
 */
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AssertWebRequestService {

    /** Mocks a Model-View-Controller application. */
    private MockMvc mockMvc;

    /** The temporarily stored keycloak access token. */
    private String accessToken;

    /** Represents the environment in which the current application is running. */
    @Autowired
    private Environment env;

    /** Enables access to the permission grants database methods. */
    @Autowired
    private PermissionDBService permissionDBService;
    
    /** The name of the database where TRUSTDECK stores its data. **/
    private static final String TRUSTDECK = "trustdeck";
    
    /**
     * Resets the test environment and sets up the mock objects.
     *
     * @param webApplicationContext the web application context
     * @param restDocumentation the context of the REST documentation
     * @throws Exception forwards any internally thrown exceptions
     */
    @BeforeEach
    public void setUp(WebApplicationContext webApplicationContext,
                      RestDocumentationContextProvider restDocumentation) throws Exception {

    	this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentation))
                .apply(springSecurity())
                .build();
    	
    	log.debug("Obtaining a token...");
        this.obtainNewAccessToken("test", "test");

		if (!resetTestData()) {
			log.error("Resetting the test data failed.");
			throw new TestInstantiationException("Resetting the test data failed.");
		}

        log.debug("Ready to test.");
    }
    
    /**
     * Helper method to reset the test data in TRUSTDECK's database as well as in Keycloak
     * @return {@code true} when resetting the databases was successful, {@code false} otherwise
     */
    private boolean resetTestData() {
    	try {
	        // Reset the database
	        try (Connection conn = getDatabaseConnection(TRUSTDECK)) {
	        	// Remove all data from the database
	        	log.debug("Truncating table domain.");
	        	conn.createStatement().execute("TRUNCATE TABLE domain CASCADE;");
	        	
	        	// Reset the sequence counter
	        	log.debug("Resetting the sequence counter in the database.");
                conn.createStatement().execute("ALTER SEQUENCE domain_id_seq RESTART WITH 1;");
            }
	        
	        // Remove all access rights and roles on domains (incl. orphaned ones, 
	        // i.e. those roles that do not have a domain in the database anymore)
	        permissionDBService.removeDomainPermissions();
	        
	        // Create the test domain DTO
	        DomainDTO domainDTO = new DomainDTO();
	        domainDTO.setName("TestStudie");
	        domainDTO.setPrefix("TS-");
	        domainDTO.setValidFrom(LocalDateTime.of(2022, 2, 26, 19, 15, 20, 885853000));
	        domainDTO.setValidTo(LocalDateTime.of(2052, 2, 19, 19, 15, 20, 885853000));
	        domainDTO.setEnforceStartDateValidity(true);
	        domainDTO.setEnforceEndDateValidity(true);
	        domainDTO.setMultiplePsnAllowed(false);
	        AlgorithmDTO algorithm = new AlgorithmDTO();
	        algorithm.setName("MD5");
	        algorithm.setAlphabet("ABCDEF0123456789");
	        algorithm.setRandomAlgorithmDesiredSize(100000000L);
	        algorithm.setRandomAlgorithmDesiredSuccessProbability(0.99999998d);
	        algorithm.setConsecutiveValueCounter(1L);
	        algorithm.setPseudonymLength(32);
	        algorithm.setPaddingCharacter("0");
	        algorithm.setAddCheckDigit(true);
	        algorithm.setLengthIncludesCheckDigit(false);
	        algorithm.setSalt("azMPTIQXJsept_4nDj5B1BXN83Bj_8VJ");
	        algorithm.setSaltLength(32);
	        domainDTO.setAlgorithm(algorithm);
	    	
	    	// Recreate the test domain
	        log.debug("Recreating the test domain.");
	        assertCreatedRequest("createTestDomain", post("/api/domains/complete"), null, domainDTO, this.getAccessToken());
	        
	        // Create test record DTO
	        PseudonymDTO pseudonymDTO = new PseudonymDTO();
	        pseudonymDTO.setIdentifierItem(IdentifierItem.builder().identifier("10000008912").idType("ANY-ID").build());
	        pseudonymDTO.setPsn("TS-9EEEE39F0D5C03507CB9388609E925F9");
	        pseudonymDTO.setValidFrom(LocalDateTime.of(2022, 2, 26, 19, 15, 20, 885853000));
	        pseudonymDTO.setValidTo(LocalDateTime.of(2052, 2, 19, 19, 15, 20, 885853000));
	        
	        // Recreate the test record
	        log.debug("Recreating the test record.");
	        assertCreatedRequest("createTestRecord", post("/api/domains/"+domainDTO.getName()+"/pseudonyms"), null, pseudonymDTO, this.getAccessToken());
	        
	        return true;
        } catch (Exception e) {
            log.error("Error while resetting the test data: ", e);
            return false;
        }
    }
    
    /**
     * Helper method to obtain a connection to a database.
     *
     * @param databaseName the name of the database you want to connect to
     * @return a connection to the database
     * @throws IllegalStateException when the database URL, username, or password are missing in the application.yml
     * @throws SQLException if establishing the connection fails
     */
    private Connection getDatabaseConnection(String databaseName) throws IllegalStateException, SQLException {
        // Read the TRUSTDECK URL from application.yml
        String trustdeckURL = env.getProperty("app.datasource.trustdeck.url");
        String username = env.getProperty("app.datasource.trustdeck.username");
        String password = env.getProperty("app.datasource.trustdeck.password");

        if (!Assertion.assertNotNullAll(trustdeckURL, username, password)) {
            throw new IllegalStateException("app.datasource.trustdeck.url, app.datasource.trustdeck.username, or app.datasource.trustdeck.password is not configured.");
        }

        // Replace the database name with "postgres"
        String adminUrl = trustdeckURL.replace("/trustdeck", "/" + databaseName);

        return DriverManager.getConnection(adminUrl, username, password);
    }

    /**
     * Returns the mock MVC object.
     *
     * @return the mock MVC
     */
    protected MockMvc getMockMvc() {
        return this.mockMvc;
    }

    /**
     * Creates the request builder from the given parameters.
     *
     * @param method the HTTP method e.g. GET, POST etc.
     * @param params the parameter in the URL
     * @param body the request body as any object
     * @param accessToken the access token string
     * @return the mock HTTP servlet request builder
     * @throws JsonProcessingException the JSON processing exception
     */
    protected MockHttpServletRequestBuilder createRequestBuilder(MockHttpServletRequestBuilder method, Map<String, String> params, Object body, String accessToken) throws JsonProcessingException {
        if (accessToken != null && !accessToken.isEmpty()) {
            method.header("Authorization", "Bearer " + accessToken);
        }

        if (params != null && params.size() > 0) {
            for (Map.Entry<String, String> set : params.entrySet()) {
                method.param(set.getKey(), set.getValue());
            }
        }

        if (body != null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            method.content(mapper.writeValueAsString(body));
        }

        method.contentType("application/json");
        method.characterEncoding("UTF-8");

        return method;
    }

    /**
     * Creates the handler for generating a response or request log.
     *
     * @param identifier the identifier for generating the response and request text
     * @return the REST documentation result handler
     */
    private RestDocumentationResultHandler generateHandler(String identifier) {
        return document(identifier, preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
    }

    /**
     * Assert that the request result is not found.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param method the method for example get, post etc.
     * @param params the parameter in the URL
     * @param body the request body as any object
     * @param accessToken the access token string
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertNotFoundRequest(String identifier, MockHttpServletRequestBuilder method, Map<String, String> params, Object body, String accessToken) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, method, params, body, accessToken, status().isNotFound());
    }

    /**
     * Assert that the request result is not found.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param requestBuilder the request builder object
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertNotFoundRequest(String identifier, RequestBuilder requestBuilder) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, requestBuilder, status().isNotFound());
    }

    /**
     * Assert that the request result is unauthorized.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param method the method for example get, post etc.
     * @param params the parameter in the URL
     * @param body the request body as any object
     * @param accessToken the access token string
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertUnauthorizedRequest(String identifier, MockHttpServletRequestBuilder method, Map<String, String> params, Object body, String accessToken) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, method, params, body, accessToken, status().isUnauthorized());
    }

    /**
     * Assert that the request result is unauthorized.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param requestBuilder the request builder object
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertUnauthorizedRequest(String identifier, RequestBuilder requestBuilder) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, requestBuilder, status().isUnauthorized());
    }

    /**
     * Assert that the request result is forbidden.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param method the method for example get, post etc.
     * @param params the parameter in the URL
     * @param body the request body as any object
     * @param accessToken the access token string
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertForbiddenRequest(String identifier, MockHttpServletRequestBuilder method, Map<String, String> params, Object body, String accessToken) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, method, params, body, accessToken, status().isForbidden());
    }

    /**
     * Assert that the request result is forbidden.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param requestBuilder the request builder object
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertForbiddenRequest(String identifier, RequestBuilder requestBuilder) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, requestBuilder, status().isForbidden());
    }

    /**
     * Assert that the request result is created.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param method the method for example get, post etc.
     * @param params the parameter in the URL
     * @param body the request body as any object
     * @param accessToken the access token string
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertCreatedRequest(String identifier, MockHttpServletRequestBuilder method, Map<String, String> params, Object body, String accessToken) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, method, params, body, accessToken, status().isCreated());
    }

    /**
     * Assert that the request result is created.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param requestBuilder the request builder object
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertCreatedRequest(String identifier, RequestBuilder requestBuilder) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, requestBuilder, status().isCreated());
    }

    /**
     * Assert that the request result is a bad request.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param method the method for example get, post etc.
     * @param params the parameter in the URL
     * @param body the request body as any object
     * @param accessToken the access token string
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertBadRequestRequest(String identifier, MockHttpServletRequestBuilder method, Map<String, String> params, Object body, String accessToken) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, method, params, body, accessToken, status().isBadRequest());
    }

    /**
     * Assert that the request result is a bad request.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param requestBuilder the request builder object
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertBadRequestRequest(String identifier, RequestBuilder requestBuilder) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, requestBuilder, status().isBadRequest());
    }

    /**
     * Assert that the request result is OK.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param method the method for example get, post etc.
     * @param params the parameter in the URL
     * @param body the request body as any object
     * @param accessToken the access token string
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertOkRequest(String identifier, MockHttpServletRequestBuilder method, Map<String, String> params, Object body, String accessToken) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, method, params, body, accessToken, status().isOk());
    }

    /**
     * Assert that the request result is OK.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param requestBuilder the request builder object
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertOkRequest(String identifier, RequestBuilder requestBuilder) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, requestBuilder, status().isOk());
    }

    /**
     * Assert that the request result is not acceptable.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param method the method for example get, post etc.
     * @param params the parameter in the URL
     * @param body the request body as any object
     * @param accessToken the access token string
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertNotAcceptable(String identifier, MockHttpServletRequestBuilder method, Map<String, String> params, Object body, String accessToken) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, method, params, body, accessToken, status().isNotAcceptable());
    }

    /**
     * Assert that the request result is not acceptable.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param requestBuilder the request builder object
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertNotAcceptable(String identifier, RequestBuilder requestBuilder) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, requestBuilder, status().isNotAcceptable());
    }

    /**
     * Assert that the request result is the status code no content.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param method the method for example get, post etc.
     * @param params the parameter in the URL
     * @param body the request body as any object
     * @param accessToken the access token string
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertNoContent(String identifier, MockHttpServletRequestBuilder method, Map<String, String> params, Object body, String accessToken) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, method, params, body, accessToken, status().isNoContent());
    }

    /**
     * Assert that the request result is the status code no content.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param requestBuilder the request builder object
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertNoContent(String identifier, RequestBuilder requestBuilder) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, requestBuilder, status().isNoContent());
    }

    /**
     * Assert that the request result is an unprocessable entity.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param method the method for example get, post etc.
     * @param params the parameter in the URL
     * @param body the request body as any object
     * @param accessToken the access token string
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertUnprocessableEntity(String identifier, MockHttpServletRequestBuilder method, Map<String, String> params, Object body, String accessToken) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, method, params, body, accessToken, status().isUnprocessableEntity());
    }

    /**
     * Assert that the request result is an unprocessable entity.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param requestBuilder the request builder object
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertUnprocessableEntity(String identifier, RequestBuilder requestBuilder) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, requestBuilder, status().isUnprocessableEntity());
    }

    /**
     * Assert that the request result is an internal server error.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param method the method for example get, post etc.
     * @param params the parameter in the URL
     * @param body the request body as any object
     * @param accessToken the access token string
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertInternalServerError(String identifier, MockHttpServletRequestBuilder method, Map<String, String> params, Object body, String accessToken) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, method, params, body, accessToken, status().isInternalServerError());
    }

    /**
     * Assert that the request result is an internal server error.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param requestBuilder the request builder object
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertInternalServerError(String identifier, RequestBuilder requestBuilder) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, requestBuilder, status().isInternalServerError());
    }


    /**
     * Assert request from request builder mock HTTP servlet response.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param method the method for example get, post etc.
     * @param params the parameter in the URL
     * @param body the request body as any object
     * @param accessToken the access token string
     * @param resultMatcher the target result to assert
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertRequestFromRequestBuilder(String identifier, MockHttpServletRequestBuilder method, Map<String, String> params, Object body, String accessToken, ResultMatcher resultMatcher) throws Exception {
        return this.assertRequestFromRequestBuilder(identifier, this.createRequestBuilder(method, params, body, accessToken), resultMatcher);
    }

    /**
     * Assert request from request builder mock HTTP servlet response.
     *
     * @param identifier the identifier for generating the response and request text.
     * @param requestBuilder the request builder object
     * @param resultMatcher the target result to assert
     * @return the response from the request
     * @throws Exception the exception
     */
    protected MockHttpServletResponse assertRequestFromRequestBuilder(String identifier, RequestBuilder requestBuilder, ResultMatcher resultMatcher) throws Exception {
        ResultActions result = this.getMockMvc().perform(requestBuilder);
        result.andDo(this.generateHandler("doc_" + identifier));
        result.andExpect(resultMatcher);
        return result.andReturn().getResponse();
    }

    /**
     * Obtains very simple an access token from the given keycloak instance. This function should split in separate parts
     * in the future if it gets to complex.
     *
     * @param username the user name as String
     * @param password the password as String
     * @return the accessToken as String
     * @throws IOException the IO exception
     * @throws CertificateException the certificate exception
     * @throws NoSuchAlgorithmException the no such algorithm exception
     * @throws KeyStoreException the key store exception
     * @throws KeyManagementException the key management exception
     */
    public String obtainNewAccessToken(String username, String password) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Getting the trust-store file
        //File trustore = ResourceUtils.getFile(Objects.requireNonNull(this.env.getProperty("spring.security.oauth2.resourceserver.jwt.truststore")));

        // Setting up a SSL context in case we want to do HTTPS requests
        SSLContext theContext = SSLContexts.custom()
                .setProtocol("TLS")
                .loadTrustMaterial((chain, authType) -> true) //except any https requests <- its just for test not any prod call
                .build();

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("client_id", this.env.getProperty("spring.security.oauth2.resourceserver.jwt.client-id")));
        params.add(new BasicNameValuePair("username", username));
        params.add(new BasicNameValuePair("password", password));
        params.add(new BasicNameValuePair("grant_type", "password"));
        params.add(new BasicNameValuePair("client_secret", this.env.getProperty("spring.security.oauth2.resourceserver.jwt.client-secret")));

        // Contains the realm
        String targetUrl = this.env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri") + "/protocol/openid-connect/token";
        //System.setProperty("javax.net.ssl.trustStore", trustore.getAbsolutePath());

        HttpClient httpClient = HttpClientBuilder.create().setSSLContext(theContext).build();

        HttpPost httpPost = new HttpPost(targetUrl);
        // Needed to send it as a form
        httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse response = httpClient.execute(httpPost);
            String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
            AccessTokenResponse accessTokenResponse = new ObjectMapper().readValue(responseString, AccessTokenResponse.class);
            this.accessToken = accessTokenResponse.getToken();
        } catch (Exception e) {
            this.accessToken = "";
            throw new RuntimeException(e);
        }

        return this.accessToken;
    }

    /**
     * Returns the keycloak access token if set
     *
     * @return the keycloak access token as String
     */
    public String getAccessToken() {
        return this.accessToken;
    }

    /**
     * Maps a list of JSON objects (encoded in a single String) to a proper list of the provided type.
     *
     * @param content the raw JSON content
     * @param valueType the class to map
     * @param <T> the Template class of the class to map
     * @return the list of mapped new objects
     */
    public <T> List<T> mapJsonObjectsInStringToList(String content, Class<T> valueType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            return mapper.readValue(content, CollectionsTypeFactory.listOf(valueType));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Maps a single JSON object to an instance of the provided type.
     *
     * @param content the raw JSON content
     * @param valueType the class to map
     * @param <T> the Template class of the class to map
     * @return the mapped new object
     */
    public <T> T applySingleJsonContentToClass(String content, Class<T> valueType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            return mapper.readValue(content, valueType);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return null;
        }

    }

    /**
     * A small helper to assert that the length of a requested
     * domain hierarchy list equals the expected length.
     *
     * @param expectedLength the expected length
     * @return the list of domains that were checked and returned by the service
     * @throws Exception forwards any internally thrown exceptions
     */
    protected List<DomainDTO> assertEqualsListDomainHierarchyLength(int expectedLength) throws Exception {

        // Unauthorized tests for getting this list
        this.assertBadRequestRequest("listDomainHierarchyBadRequest", get("/api/domains/hierarchy"), null, null, "");
        this.assertUnauthorizedRequest("listDomainHierarchyUnauth", get("/api/domains/hierarchy"), null, null, "SomeToken");

        MockHttpServletResponse response = this.assertOkRequest("listDomainHierarchy", get("/api/domains/hierarchy"), null, null, this.getAccessToken());
        String content = response.getContentAsString();
        List<DomainDTO> domains = this.mapJsonObjectsInStringToList(content, DomainDTO.class);

        if (expectedLength == 0) {
            assertNull(domains);
        } else {
            assertEquals(expectedLength, domains.size());
        }

        return domains;
    }

    /**
     * A small helper to assert that the number of records is as expected.
     *
     * @param expectedLength the expected length
     * @param goodDomainButNotFound name of a domain that will return a 404 status
     * @param goodDomain a domain where the batch retrieval will work
     * @return the list of records that were checked and returned by the service
     * @throws Exception forwards any internally thrown exceptions
     */
    protected List<PseudonymDTO> assertEqualsListRecordsLength(int expectedLength, String goodDomainButNotFound, String goodDomain) throws Exception {
        this.assertNotFoundRequest("getRecordBatchNotFound", get("/api/domains/" + goodDomainButNotFound + "/pseudonyms"), null, null, this.getAccessToken());
        MockHttpServletResponse response = this.assertOkRequest("getRecordBatch", get("/api/domains/" + goodDomain + "/pseudonyms"), null, null, this.getAccessToken());
        String content = response.getContentAsString();
        List<PseudonymDTO> records = this.mapJsonObjectsInStringToList(content, PseudonymDTO.class);

        if (expectedLength == 0) {
            assertNull(records);
        } else {
            assertEquals(expectedLength, records.size());
        }

        return records;
    }

    /**
     * A helper to completely update a domain and assert that the updated attribute is updated correctly.
     *
     * @param expectedDomain the object of the expected domain
     * @param actualDomainName the domain name as string to request
     * @throws Exception forwards any internally thrown exceptions
     */
    protected void domainUpdateHelperComplete(DomainDTO expectedDomain, String actualDomainName, DomainDTO domainDTO) throws Exception {
        Map<String, String> updateParameterChange = new HashMap<>() {
			private static final long serialVersionUID = -2708461912069760668L;
		{
            put("name", actualDomainName);
            put("recursive", "false"); // is required
        }};

        // Unauthorized tests for updating a domain
        this.assertBadRequestRequest("updateDomainBadRequestComplete", put("/api/domains/complete"), updateParameterChange, null, "");
        this.assertUnauthorizedRequest("updateDomainUnauthComplete", put("/api/domains/complete"), updateParameterChange, null, "SomeToken");

        this.assertOkRequest("commonUpdateDomainComplete", put("/api/domains/complete"), updateParameterChange, domainDTO, this.getAccessToken());

        this.getAndCheckDomain(expectedDomain, actualDomainName);
    }
    
    /**
     * A helper to update a domain and assert that the updated attribute is updated correctly.
     *
     * @param expectedDomain the object of the expected domain
     * @param actualDomainName the domain name as string to request
     * @throws Exception forwards any internally thrown exceptions
     */
    protected void domainUpdateHelperReduced(DomainDTO expectedDomain, String actualDomainName, DomainDTO domainDTO) throws Exception {
        Map<String, String> updateParamterChange = new HashMap<>() {
        	private static final long serialVersionUID = 5655387819735798090L;
		{
            put("name", actualDomainName);
        }};

        // Unauthorized tests for updating a domain
        this.assertBadRequestRequest("updateDomainBadRequest", put("/api/domains"), updateParamterChange, null, "");
        this.assertUnauthorizedRequest("updateDomainUnauth", put("/api/domains"), updateParamterChange, null, "SomeToken");

        this.assertOkRequest("commonUpdateDomain", put("/api/domains"), updateParamterChange, domainDTO, this.getAccessToken());

        this.getAndCheckDomain(expectedDomain, actualDomainName);
    }

    /**
     * A helper to get and assert all values of a domain
     *
     * @param expectedDomain the object of the expected domain
     * @param actualDomainName the domain name as string to request
     * @throws Exception forwards any internally thrown exceptions
     */
    protected void getAndCheckDomain(DomainDTO expectedDomain, String actualDomainName) throws Exception {

        MockHttpServletResponse response = this.assertOkRequest("getDomain", get("/api/domains/" + actualDomainName), null, null, this.getAccessToken());
        String content = response.getContentAsString();

        DomainDTO actualDomain = this.applySingleJsonContentToClass(content, DomainDTO.class);

        assertEquals(expectedDomain.getId(), actualDomain.getId());
        assertEquals(expectedDomain.getName(), actualDomain.getName());
        assertEquals(expectedDomain.getPrefix(), actualDomain.getPrefix());
        assertEquals(expectedDomain.getValidFrom(), actualDomain.getValidFrom());
        assertEquals(expectedDomain.getValidTo(), actualDomain.getValidTo());
        assertEquals(expectedDomain.getEnforceStartDateValidity(), actualDomain.getEnforceStartDateValidity());
        assertEquals(expectedDomain.getEnforceEndDateValidity(), actualDomain.getEnforceEndDateValidity());
        assertEquals(expectedDomain.getAlgorithm(), actualDomain.getAlgorithm());
        assertEquals(expectedDomain.getMultiplePsnAllowed(), actualDomain.getMultiplePsnAllowed());
        assertEquals(expectedDomain.getDescription(), actualDomain.getDescription());
    }

    /**
     * Tests a common delete operation without any recursion
     *
     * @param domainName the name of the domain as string
     * @throws Exception forwards any internally thrown exceptions
     */
    protected void assertIsDeletedDomain(String domainName) throws Exception {
        // Delete the domain
        Map<String, String> deleteParameter = new HashMap<>() {
			private static final long serialVersionUID = 7861959161189599718L;
		{
            put("name", domainName);
        }};

        // Unauthorized tests for delete domain
        this.assertBadRequestRequest("deleteDomainBadRequest", delete("/api/domains"), deleteParameter, null, "");
        this.assertUnauthorizedRequest("deleteDomainUnauth", delete("/api/domains"), deleteParameter, null, "SomeToken");

        this.assertNoContent("deleteDomain", delete("/api/domains"), deleteParameter, null, this.getAccessToken());

        // Getting the domain must lead to a not found
        Map<String, String> getParameter = new HashMap<>() {
			private static final long serialVersionUID = -3594953606458809296L;
		{
            put("name", domainName);
        }};

        this.assertForbiddenRequest("getDomainNotFoundAfterDelete", get("/api/domains"), getParameter, null, this.getAccessToken());
    }

    /**
     * Helper class that works like a generic container to cast a generic object into a defined list container
     * Thanks for help: https://stackoverflow.com/a/61154659
     */
    public static class CollectionsTypeFactory {
        static JavaType listOf(@SuppressWarnings("rawtypes") Class clazz) {
            return TypeFactory.defaultInstance().constructCollectionType(List.class, clazz);
        }
    }
}
