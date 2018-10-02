package org.bonitasoft.engine.connector.uipath;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.connector.uipath.converters.WrappedAttributeConverter;
import org.bonitasoft.engine.connector.uipath.model.Job;
import org.bonitasoft.engine.connector.uipath.model.JobRequest;
import org.bonitasoft.engine.connector.uipath.model.Release;
import org.bonitasoft.engine.connector.uipath.model.Robot;
import org.bonitasoft.engine.connector.uipath.model.StartInfo;
import org.bonitasoft.engine.connector.uipath.model.Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.Retrofit.Builder;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class UIPathConnector extends AbstractConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(UIPathConnector.class.getName());

    static final String URL = "url";
    static final String USER = "user";
    static final String PASSWORD = "password";
    static final String TENANT = "tenant";
    static final String PROCESS_NAME = "processName";
    static final String PROCESS_VERSION = "processVersion";
    static final String ROBOTS_NAMES = "robotNames";
    static final String JOBS_COUNT = "jobsCount";
    static final String SOURCE = "source";
    static final String STRATEGY = "strategy";
    static final String INPUT_ARGS = "inputArguments";

    static final String PROCESS_INSTANCE_ID = "bonitaProcessInstanceId";
    static final String BONITA_PROCESS_NAME = "bonitaProcessName";
    static final String BONITA_MESSAGE_NAME = "bonitaMessageName";

    private UIPathService service;
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        checkMandatoryStringInput(URL);
        checkMandatoryStringInput(TENANT);
        checkMandatoryStringInput(USER);
        checkMandatoryStringInput(PASSWORD);
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    void checkArgsInput() throws ConnectorValidationException {
        Optional<Map> inputArguments = getInputArguments();
        if (inputArguments.isPresent()) {
            Map map = inputArguments.get();
            Set nonStringKeys = (Set) map.keySet().stream().filter(key -> !(key instanceof String))
                    .collect(Collectors.toSet());
            if (!nonStringKeys.isEmpty()) {
                throw new ConnectorValidationException(
                        String.format("Only String keys are allowed in job input arguments. Found %s.", nonStringKeys));
            }
            Set nonSerializableValue = (Set) map.values().stream().filter(value -> {
                try {
                    mapper.writeValueAsString(value);
                    return false;
                } catch (Throwable t) {
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

    private void checkMandatoryStringInput(String input) throws ConnectorValidationException {
        String value = null;
        try {
            value = (String) getInputParameter(input);
        } catch (ClassCastException e) {
            throw new ConnectorValidationException(this, String.format("'%s' parameter must be a String", input));
        }

        if (value == null || value.isEmpty()) {
            throw new ConnectorValidationException(this,
                    String.format("Mandatory parameter '%s' is missing.", input));
        }
    }

    /*
     * (non-Javadoc)
     * @see org.bonitasoft.engine.connector.AbstractConnector#connect()
     */
    @Override
    public void connect() throws ConnectorException {
        service = createService();
    }

    /*
     * (non-Javadoc)
     * @see org.bonitasoft.engine.connector.AbstractConnector#executeBusinessLogic()
     */
    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        try {
            String token = authenticate();
            List<Release> releases = releases(token);
            List<Release> processReleases = releases.stream()
                    .filter(r -> Objects.equals(r.getProcessKey(), getProcessName()))
                    .collect(Collectors.toList());
             if(processReleases.isEmpty()) {
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
            setOutputParameter("startedJobs", output);
        } catch (IOException e) {
            throw new ConnectorException(
                    String.format("Failed to authenticate to '%s' on tenant '%s' with user '%s'", getUrl(), getTenant(),
                            getUser()),
                    e);
        }
    }

    private String toJSON(Job job) {
        try {
            return mapper.writeValueAsString(job);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to parse output.", e);
            return null;
        }
    }

    List<Job> startJobs(String token, Release release, List<Integer> robotIds)
            throws IOException, ConnectorException {
        StartInfo startInfo = new StartInfo()
                .setReleaseKey(release.getKey())
                .setRobotIds(robotIds);
        getJobsCount().ifPresent(startInfo::setJobsCount);
        getSource().ifPresent(startInfo::setSource);
        getStrategy().ifPresent(startInfo::setStrategy);
        try {
            startInfo.setArgs(mapper.writeValueAsString(handleInputArgs()));
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to convert InputArguments into a JSON String.", e);
        }
        Response<List<Job>> response = service.startJob(buildTokenHeader(token), new JobRequest().setStartInfo(startInfo))
                .execute();
        if (!response.isSuccessful()) {
            LOGGER.error(response.toString());
            throw new ConnectorException(response.errorBody().string());
        }
        setOutputParameter("statusCode", response.code());
        setOutputParameter("statusMessage", response.message());
        return response.body();
    }

    private Map<String, Object> handleInputArgs() {
        Map<String, Object> inputArgs = getInputArguments().orElse(new HashMap<String, Object>());
        inputArgs.put(PROCESS_INSTANCE_ID, getExecutionContext().getProcessInstanceId());
        inputArgs.put(BONITA_PROCESS_NAME, getBonitaProcessName().orElse(retrieveCurrentProcessName()));
        getBonitaMessageName().ifPresent(message -> inputArgs.put(BONITA_MESSAGE_NAME, message));
        return inputArgs;
    }

    private String retrieveCurrentProcessName() {
        long pDef = getExecutionContext().getProcessDefinitionId();
        try {
            ProcessDefinition processDefinition = getAPIAccessor().getProcessAPI().getProcessDefinition(pDef);
            return processDefinition.getName();
        } catch (ProcessDefinitionNotFoundException e) {
            LOGGER.error("Cannot retrieve bonita process name", e);
            return null;
        }
    }

    List<Release> releases(String token) throws IOException, ConnectorException {
        Response<List<Release>> response = service.releases(buildTokenHeader(token)).execute();
        if (!response.isSuccessful()) {
            throw new ConnectorException(response.errorBody().string());
        }
        return response.body();
    }

    List<Robot> robots(String token) throws IOException, ConnectorException {
        Response<List<Robot>> response = service.robots(buildTokenHeader(token)).execute();
        if (!response.isSuccessful()) {
            throw new ConnectorException(response.errorBody().string());
        }
        return response.body();
    }

    private String buildTokenHeader(String token) {
        return "Bearer " + token;
    }

    String authenticate() throws IOException, ConnectorException {
        Response<Map<String, String>> response = service.authenticate(getTenant(), getUser(), getPassword())
                .execute();
        if (!response.isSuccessful()) {
            throw new ConnectorException(response.errorBody().string());
        }
        return response.body().get("result");
    }

    protected UIPathService createService() {
        if (service == null) {
            OkHttpClient client = null;
            if (LOGGER.isDebugEnabled()) {
                HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
                interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
            }

            Builder retrofitBuilder = new Retrofit.Builder()
                    .addConverterFactory(new WrappedAttributeConverter(mapper))
                    .addConverterFactory(JacksonConverterFactory.create())
                    .baseUrl(appendTraillingSlash(getUrl()));

            if (client != null) {
                retrofitBuilder.client(client);
            }

            service = retrofitBuilder.build().create(UIPathService.class);
        }
        return service;
    }

    private static String appendTraillingSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    String getTenant() {
        return (String) getInputParameter(TENANT);
    }

    String getUser() {
        return (String) getInputParameter(USER);
    }

    String getPassword() {
        return (String) getInputParameter(PASSWORD);
    }

    String getUrl() {
        return (String) getInputParameter(URL);
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

    Optional<String> getSource() {
        return Optional.ofNullable((String) getInputParameter(SOURCE));
    }

    Optional<String> getStrategy() {
        return Optional.ofNullable((String) getInputParameter(STRATEGY));
    }

    Optional<Map> getInputArguments() {
        Object inputParameter = getInputParameter(INPUT_ARGS);
        if (inputParameter instanceof List) {
            return Optional.of(toMap(inputParameter));
        }
        return Optional.ofNullable((Map) getInputParameter(INPUT_ARGS));
    }

    Optional<String> getBonitaProcessName() {
        return Optional.ofNullable((String) getInputParameter(BONITA_PROCESS_NAME));
    }

    Optional<String> getBonitaMessageName() {
        return Optional.ofNullable((String) getInputParameter(BONITA_MESSAGE_NAME));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map<Object, Object> toMap(Object inputParameter) {
        Map<Object, Object> result = new HashMap<>();
        for (Object row : (Iterable) inputParameter) {
            if (row instanceof List && ((List) row).size() == 2) {
                result.put(((List<Object>) row).get(0), ((List<Object>) row).get(1));
            }
        }
        return result;
    }

}
