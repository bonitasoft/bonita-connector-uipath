/**
 * Copyright (C) 2018 Bonitasoft S.A.
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
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.connector.uipath.model.Release;
import org.bonitasoft.engine.connector.uipath.model.Robot;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import lombok.Data;

@RunWith(MockitoJUnitRunner.class)
public class UIPathStartJobsConnectorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @ClassRule
    public static WireMockRule uiPathService = new WireMockRule(8888);

    @Before
    public void configureStubs() throws Exception {
        uiPathService.stubFor(WireMock.post(WireMock.urlEqualTo("/api/account/authenticate"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("mock.authenticate.response.json")));

        uiPathService.stubFor(WireMock.get(WireMock.urlEqualTo("/odata/Releases"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("mock.releases.response.json")));

        uiPathService.stubFor(WireMock.get(WireMock.urlEqualTo("/odata/Robots"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("mock.robots.response.json")));

        uiPathService
                .stubFor(WireMock.post(WireMock.urlEqualTo("/odata/Jobs/UiPath.Server.Configuration.OData.StartJobs"))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("mock.jobs.response.json")));
    }

    @Test
    public void should_authenticate() throws Exception {
        UIPathConnector uiPathConnector = createConnector();
        uiPathConnector.connect();
        String token = uiPathConnector.authenticate();

        assertThat(token).isEqualTo("1xAaDytyclwDmxlgxFqMM2E5Kdj19JxGPHStcjRLfz8YA5HimP7y2_d5A");
    }

    @Test
    public void should_retrieve_releases() throws Exception {
        UIPathStartJobsConnector uiPathConnector = createConnector();
        uiPathConnector.connect();
        List<Release> releases = uiPathConnector.releases("aToken");

        assertThat(releases).hasSize(1);
        Release release = releases.get(0);
        assertThat(release.getId()).isEqualTo(1);
        assertThat(release.getProcessKey()).isEqualTo("myProcessKey");
        assertThat(release.getCurrentVersion().getId()).isEqualTo(2);
    }

    @Test
    public void should_retrieve_robots() throws Exception {
        UIPathStartJobsConnector uiPathConnector = createConnector();
        uiPathConnector.connect();
        List<Robot> robots = uiPathConnector.robots("aToken");

        assertThat(robots).hasSize(1);
        Robot robot = robots.get(0);
        assertThat(robot.getId()).isEqualTo(5);
    }

    @Test
    public void should_start_jobs() throws Exception {
        UIPathConnector uiPathConnector = createConnector();
        uiPathConnector.connect();
        Map<String, Object> outputs = uiPathConnector.execute();
        List<String> startedJobs = (List<String>) outputs.get("startedJobs");

        assertThat(startedJobs).hasSize(1);
        String job = startedJobs.get(0);

        assertThat(job).contains("54");
    }

    @Test
    public void should_throw_a_ConnectorValidationException_if_input_arguments_has_non_string_keys() throws Exception {
        UIPathStartJobsConnector uiPathConnector = new UIPathStartJobsConnector();
        Map<String, Object> inputs = new HashMap<>();
        Map<Object, Object> inputArgs = new HashMap<>();
        inputArgs.put("key", "value1");
        inputArgs.put(1, "value2");
        inputs.put(UIPathStartJobsConnector.INPUT_ARGS, inputArgs);
        uiPathConnector.setInputParameters(inputs);

        expectedException.expect(ConnectorValidationException.class);
        expectedException.expectMessage("Only String keys are allowed in job input arguments. Found [1].");

        uiPathConnector.checkArgsInput();
    }


    @Test
    public void should_validate_input_arguments()
            throws Exception {
        UIPathStartJobsConnector uiPathConnector = new UIPathStartJobsConnector();
        Map<String, Object> inputs = new HashMap<>();
        Map<Object, Object> inputArgs = new HashMap<>();
        inputArgs.put("key", "value1");
        User user = new User();
        user.setFirstname("John");
        user.setName("Doe");
        User manager = new User();
        manager.setFirstname("Jane");
        manager.setName("Doe");
        user.setManager(manager);
        inputArgs.put("user", user);
        inputs.put(UIPathStartJobsConnector.INPUT_ARGS, inputArgs);
        uiPathConnector.setInputParameters(inputs);

        uiPathConnector.checkArgsInput();
    }

    private UIPathStartJobsConnector createConnector() throws Exception {
        UIPathStartJobsConnector uiPathConnector = spy(new UIPathStartJobsConnector());
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIPathConnector.CLOUD, false);
        parameters.put(UIPathConnector.URL, "http://localhost:8888");
        parameters.put(UIPathConnector.TENANT, "a_tenant");
        parameters.put(UIPathConnector.USER, "admin");
        parameters.put(UIPathConnector.PASSWORD, "somePassowrd");
        parameters.put(UIPathStartJobsConnector.PROCESS_NAME, "myProcessKey");
        parameters.put(UIPathStartJobsConnector.PROCESS_VERSION, "1.0");
        uiPathConnector.setInputParameters(parameters);
        uiPathConnector.validateInputParameters();
        return uiPathConnector;
    }

    @Data
    class User {
        private String name;
        private String firstname;
        private User manager;
    }
}
