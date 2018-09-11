package org.bonitasoft.connector.uipath;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bonitasoft.connector.uipath.converters.WrappedAttributeConverter;
import org.bonitasoft.connector.uipath.model.Job;
import org.bonitasoft.connector.uipath.model.JobRequest;
import org.bonitasoft.connector.uipath.model.Release;
import org.bonitasoft.connector.uipath.model.Robot;
import org.bonitasoft.connector.uipath.model.StartInfo;
import org.bonitasoft.connector.uipath.model.Strategy;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class UIPathConnector extends AbstractConnector {

    private static final Logger LOGGER = Logger.getLogger(UIPathConnector.class.getName());

    static final String URL = "url";
    static final String USER = "user";
    static final String PASSWORD = "password";
    static final String TENANT = "tenant";
    static final String PROCESS_NAME = "processName";
    static final String PROCESS_VERSION = "processVersion";
    static final String ROBOTS_NAMES = "robotNames";
    static final String NB_OF_ROBOTS = "nbOfRobots";

    private UIPathService service;

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        checkMandatoryStringInput(URL);
        checkMandatoryStringInput(TENANT);
        checkMandatoryStringInput(USER);
        checkMandatoryStringInput(PASSWORD);
        checkMandatoryStringInput(PROCESS_NAME);
        checkMandatoryStringInput(PROCESS_VERSION);
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
            Release release = releases.stream()
                    .filter(r -> Objects.equals(r.getProcessKey(), getProcessName()))
                    .filter(r -> Objects.equals(r.getProcessVersion(), getProcessVersion()))
                    .findFirst()
                    .orElseThrow(() -> new ConnectorException(
                            String.format("No release found for process %s in version %s", getProcessName(),
                                    getProcessVersion())));
            List<Robot> robots = robots(token);
            List<Integer> robotIds = robots.stream()
                    .filter(r -> getRobots().contains(r.getName()))
                    .map(Robot::getId)
                    .collect(Collectors.toList());

            List<Job> jobs = startJobs(token, release, robotIds);
        } catch (IOException e) {
            throw new ConnectorException(
                    String.format("Failed to authenticate to '%s' on tenant '%s' with user '%s'", getUrl(), getTenant(),
                            getUser()),
                    e);
        }
    }


    List<Job> startJobs(String token, Release release, List<Integer> robotIds)
            throws IOException, ConnectorException {
        StartInfo startInfo = new StartInfo()
                                .setReleaseKey(release.getKey())
                                .setRobotIds(robotIds)
                                .setNoOfRobots(getNumberOfRobots())
                        .setStrategy(Strategy.SPECIFIC.toString());
        Response<List<Job>> response = service.startJob(buildTokenHeader(token), new JobRequest().setStartInfo(startInfo))
                .execute();
        if (!response.isSuccessful()) {
            LOGGER.log(Level.SEVERE, response.toString());
            throw new ConnectorException(response.errorBody().string());
        }
        setOutputParameter("status_code", response.code());
        setOutputParameter("status_message", response.message());
        return response.body();
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
            //            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            //            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            //            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

            Retrofit retrofit = new Retrofit.Builder()
                    .addConverterFactory(new WrappedAttributeConverter())
                    .addConverterFactory(JacksonConverterFactory.create())
                    .baseUrl(appendTraillingSlash(getUrl()))
                    //  .client(client)
                    .build();
            service = retrofit.create(UIPathService.class);
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

    String getProcessVersion() {
        return (String) getInputParameter(PROCESS_VERSION);
    }

    List<String> getRobots() {
        return (List<String>) getInputParameter(ROBOTS_NAMES);
    }

    Integer getNumberOfRobots() {
        Integer nbRobots = (Integer) getInputParameter(NB_OF_ROBOTS);
        return nbRobots != null ? nbRobots : 0;
    }

}
