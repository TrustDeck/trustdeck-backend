package org.trustdeck.security.authentication.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class CustomAccessDeniedHandlerTest {

    @Test
    void doesNotLogBearerToken() {
        String token = "secret-token-that-must-not-be-logged";
        List<String> messages = new CopyOnWriteArrayList<>();
        Logger logger = (Logger) LogManager.getLogger(CustomAccessDeniedHandler.class);
        Level previousLevel = logger.getLevel();
        Appender appender = new AbstractAppender("access-denied-test", null, PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY) {
            @Override
            public void append(org.apache.logging.log4j.core.LogEvent event) {
                messages.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/domains");
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();

            new CustomAccessDeniedHandler().handle(request, response, new AccessDeniedException("denied"));

            assertEquals(403, response.getStatus());
            assertFalse(messages.stream().anyMatch(message -> message.contains(token)));
        } finally {
            logger.removeAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }
}
