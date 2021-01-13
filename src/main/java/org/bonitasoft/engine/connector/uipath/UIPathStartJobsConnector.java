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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.connector.uipath.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class UIPathStartJobsConnector extends UIPathConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(UIPathStartJobsConnector.class.getName());

    static final String PROCESS_NAME = "processName";
    static final String PROCESS_VERSION = "processVersion";
    static final String ROBOTS_NAMES = "robotNames";
    static final String JOBS_COUNT = "jobsCount";
    static final String STRATEGY = "strategy";
    static final String INPUT_ARGS = "inputArguments";
    static final String RUNTIME_TYPE = "runtimeType";
    static final String SOURCE = "source";

    static final String STARTED_JOBS_OUTPUT = "startedJobs";

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        super.validateInputParameters();
        checkMandatoryStringInput(PROCESS_NAME);
        checkRobotsIfSpecific();
        checkJobsCountIfJobsCountStrategy();
        checkArgsInput();
    }

    void checkJobsCountIfJobsCountStrategy() throws ConnectorValidationException {
        Optional<String> strategy = getStrategy();
        if (strategy.filter(Strategy.SPECIFIC.toString()::equals).isPresent()) {
            Optional<List<String>> robots = getRobots();
            if (!robots.isPresent() || robots.get().isEmpty()) {
                throw new ConnectorValidationException("Robots should be provided when using Specific strategy.");
            }
        }
    }

    void checkRobotsIfSpecific() throws ConnectorValidationException {
        Optional<String> strategy = getStrategy();
        if (strategy.filter(Strategy.JOBS_COUNT.toString()::equals).isPresent()) {
            Optional<Integer> jobsCount = getJobsCount();
            if (!jobsCount.isPresent() || jobsCount.get() == 0) {
                throw new ConnectorValidationException(
                        "A job count greater than 0 should be provided when using JobsCount strategy.");
            }
        }
    }

    String getProcessName() {
        return (String) getInputParameter(PROCESS_NAME);
    }

    Optional<String> getProcessVersion() {
        return Optional.ofNullable((String) getInputParameter(PROCESS_VERSION));
    }

    Optional<List<String>> getRobots() {
        return Optional.ofNullable((List<String>) getInputParameter(ROBOTS_NAMES));
    }

    Optional<Integer> getJobsCount() {
        return Optional.ofNullable((Integer) getInputParameter(JOBS_COUNT));
    }

    Optional<String> getStrategy() {
        return Optional.ofNullable((String) getInputParameter(STRATEGY));
    }

    Optional<String> getRuntimeType() {
        return Optional.ofNullable((String) getInputParameter(RUNTIME_TYPE));
    }

    Optional<String> getSource() {
        return Optional.ofNullable((String) getInputParameter(SOURCE));
    }

    Optional<Map<Object, Object>> getInputArguments() {
        Object inputParameter = getInputParameter(INPUT_ARGS);
        if (inputParameter instanceof List) {
            return Optional.of(toMap(inputParameter));
        }
        return Optional.ofNullable((Map<Object, Object>) getInputParameter(INPUT_ARGS));
    }

    void checkArgsInput() throws ConnectorValidationException {
        Optional<Map<Object, Object>> inputArguments = getInputArguments();
        if (inputArguments.isPresent()) {
            Map<Object, Object> map = inputArguments.get();
            Set<?> nonStringKeys = map.keySet().stream().filter(key -> !(key instanceof String))
                    .collect(Collectors.toSet());
            if (!nonStringKeys.isEmpty()) {
                throw new ConnectorValidationException(
                        String.format("Only String keys are allowed in job input arguments. Found %s.", nonStringKeys));
            }
            Set<?> nonSerializableValue = map.values().stream().filter(value -> {
                try {
                    mapper.writeValueAsString(value);
                    return false;
                } catch (Exception e) {
                    return true;
                }
            }).collect(Collectors.toSet());
            if (!nonSerializableValue.isEmpty()) {
                throw new ConnectorValidationException(
                        String.format("Only Serializable values are allowed in job input arguments. Found %s.",
                                nonSerializableValue));
            }
        }
    }

    private Map<Object, Object> handleInputArgs() {
        return getInputArguments().orElse(new HashMap<>());
    }

    List<Job> startJobs(String token, Release release, List<Integer> robotIds)
            throws ConnectorException {
        StartInfo startInfo = new StartInfo()
                .setSource(Source.MANUAL.toString())
                .setReleaseKey(release.getKey());
        getRuntimeType().ifPresent(startInfo::setRuntimeType);
        getStrategy().ifPresent(startInfo::setStrategy);

        if (Objects.equals(startInfo.getStrategy(), Strategy.SPECIFIC.toString())) {
            startInfo.setRobotIds(robotIds);
        }
        if (Objects.equals(startInfo.getStrategy(), Strategy.JOBS_COUNT.toString())) {
            getJobsCount().ifPresent(startInfo::setJobsCount);
        }
        if (Objects.equals(startInfo.getStrategy(), Strategy.MODERN_JOBS_COUNT.toString())) {
            getJobsCount().ifPresent(startInfo::setJobsCount);
        }
        try {
            startInfo.setArgs(mapper.writeValueAsString(handleInputArgs()));
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to convert InputArguments into a JSON String.", e);
        }
        Response<List<Job>> response;
        try {
            response = getService()
                    .startJob(createAuthenticationHeaders(token), new JobRequest().setStartInfo(startInfo))
                    .execute();
        } catch (IOException e) {
            throw new ConnectorException("Failed to start job.", e);
        }
        if (!response.isSuccessful()) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(response.toString());
            }
            throw new ConnectorException("Failed to start job: " + response.message());
        }
        return response.body();
    }

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        String token = authenticate();
        List<Release> releases = releases(token);
        List<Release> processReleases = releases.stream()
                .filter(r -> Objects.equals(r.getProcessKey(), getProcessName()))
                .collect(Collectors.toList());
        if (processReleases.isEmpty()) {
            throw new ConnectorException(
                    String.format("No release found for process %s.", getProcessName()));
        }
        getProcessVersion()
                .ifPresent(version -> processReleases.removeIf(r -> !Objects.equals(version, r.getProcessVersion())));
        if (processReleases.isEmpty()) {
            throw new ConnectorException(
                    String.format("No release found for process %s and version %s.", getProcessName(),
                            getProcessVersion().orElse("Unknown")));
        }
        Release release = processReleases.get(0);
        if (!getProcessVersion().isPresent() && release.getCurrentVersion() != null) {
            long currentRelease = release.getCurrentVersion().getReleaseId();
            release = processReleases.stream().filter(r -> r.getId() == currentRelease).findFirst()
                    .orElseThrow(() -> new ConnectorException(
                            String.format("No release found with id %s for process %s", currentRelease,
                                    getProcessName())));
        }
        List<Robot> robots = robots(token);
        List<Integer> robotIds = robots.stream()
                .filter(r -> getRobots().orElse(Collections.emptyList()).contains(r.getName()))
                .map(Robot::getId)
                .collect(Collectors.toList());

        List<String> output = startJobs(token, release, robotIds).stream()
                .map(this::toJSON)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        setOutputParameter(STARTED_JOBS_OUTPUT, output);
    }

    List<Release> releases(String token) throws ConnectorException {
        Response<List<Release>> response;
        try {
            response = getService().releases(createAuthenticationHeaders(token)).execute();
        } catch (IOException e) {
            throw new ConnectorException("Failed to retrieve releases.", e);
        }
        if (!response.isSuccessful()) {
            throw new ConnectorException("Failed to retrieve releases: " + response.message());
        }
        return response.body();
    }

    List<Robot> robots(String token) throws ConnectorException {
        Response<List<Robot>> response;
        try {
            response = getService().robots(createAuthenticationHeaders(token)).execute();
        } catch (IOException e) {
            throw new ConnectorException("Failed to retrieve robots.", e);
        }
        if (!response.isSuccessful()) {
            throw new ConnectorException("Failed to retrieve robots: " + response.message());
        }
        return response.body();
    }

    private String toJSON(Job job) {
        try {
            return mapper.writeValueAsString(job);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to parse output.", e);
            return null;
        }
    }

}
