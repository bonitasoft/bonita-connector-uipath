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
package org.bonitasoft.connector.uipath;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.connector.uipath.model.Job;
import org.bonitasoft.connector.uipath.model.Release;
import org.bonitasoft.connector.uipath.model.Robot;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class UIPathConnectorTest {

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
        UIPathConnector uiPathConnector = createConnector();
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
        UIPathConnector uiPathConnector = createConnector();
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
        List<Job> startedJobs = uiPathConnector.startJobs("aToken", new Release(), new ArrayList<>());

        assertThat(startedJobs).hasSize(1);
        Job job = startedJobs.get(0);
        assertThat(job.getId()).isEqualTo("54");
    }

    private UIPathConnector createConnector() throws ConnectorValidationException {
        UIPathConnector uiPathConnector = new UIPathConnector();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIPathConnector.URL, "http://localhost:8888");
        parameters.put(UIPathConnector.TENANT, "bonita_heroes");
        parameters.put(UIPathConnector.USER, "admin");
        parameters.put(UIPathConnector.PASSWORD, "Bonitasoft2018");
        parameters.put(UIPathConnector.PROCESS_NAME, "TestProcess");
        parameters.put(UIPathConnector.PROCESS_VERSION, "TestVersion");
        uiPathConnector.setInputParameters(parameters);
        uiPathConnector.validateInputParameters();
        return uiPathConnector;
    }


}
