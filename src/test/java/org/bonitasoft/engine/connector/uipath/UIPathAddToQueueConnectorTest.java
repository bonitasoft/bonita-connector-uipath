/**
 * Copyright (C) 2020 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.engine.connector.uipath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

class UIPathAddToQueueConnectorTest {
    
    public static WireMockRule uiPathService;

    @BeforeAll
    public static void startMockServer() {
        uiPathService = new WireMockRule(8888);
        uiPathService.start();
    }

    @AfterAll
    public static void stopMockServer() {
        uiPathService.stop();
    }

    @BeforeEach
    public void configureStubs() throws Exception {
        uiPathService.stubFor(WireMock.post(WireMock.urlEqualTo("/api/account/authenticate"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("mock.authenticate.response.json")));

        uiPathService
                .stubFor(WireMock.post(WireMock.urlEqualTo("/odata/Queues/UiPathODataSvc.AddQueueItem"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("mock.addToQueue.response.json")));
    }
    
    private UIPathAddToQueueConnector createConnector() throws Exception {
        UIPathAddToQueueConnector connector = spy(new UIPathAddToQueueConnector());
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIPathConnector.CLOUD, false);
        parameters.put(UIPathConnector.URL, "http://localhost:8888");
        parameters.put(UIPathConnector.TENANT, "a_tenant");
        parameters.put(UIPathConnector.USER, "admin");
        parameters.put(UIPathConnector.PASSWORD, "somePassowrd");
        parameters.put(UIPathAddToQueueConnector.QUEUE_NAME, "myQueue");
        Map<String, Object> content = new HashMap<>();
        content.put("hello", "world");
        parameters.put(UIPathAddToQueueConnector.QUEUE_CONTENT, content);
        connector.setInputParameters(parameters);
        connector.validateInputParameters();
        return connector;
    }
    
    @Test
    void should_add_item_to_queue() throws Exception {
        UIPathAddToQueueConnector connector = createConnector();
        
        connector.connect();
        Map<String, Object> outputs = connector.execute();
        
       assertThat(outputs).containsEntry(UIPathAddToQueueConnector.ITEM_ID_OUTPUT, 39578029L)
           .containsEntry(UIPathAddToQueueConnector.ITEM_KEY_OUTPUT,"ef306441-f7a6-4fad-ba8a-d09ec1237e2c");
    }

}
