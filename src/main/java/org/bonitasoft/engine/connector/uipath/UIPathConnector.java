package org.bonitasoft.engine.connector.uipath;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.connector.uipath.converters.WrappedAttributeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.Retrofit.Builder;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class UIPathConnector extends AbstractConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(UIPathConnector.class.getName());

    static final String CLOUD = "cloud";
    static final String CLOUD_AUTH_TYPE = "cloudAuthType";
    // classic parameters
    static final String URL = "url";
    static final String USER = "user";
    static final String PASSWORD = "password";
    static final String TENANT = "tenant";
    // cloud parameters
    static final String ACCOUNT_LOGICAL_NAME = "accountLogicalName";
    static final String TENANT_LOGICAL_NAME = "tenantLogicalName";
    static final String CLIENT_ID = "clientId";
    static final String CLIENT_SECRET = "clientSecret";
    static final String SCOPE = "scope";
    static final String TOKEN = "token";
    static final String ORGANIZATION_UNIT_ID = "organizationUnitId";

    static final String TOKEN_AUTH_TYPE = "Token (Bearer)";
    static final String CLIENT_CREDENTIALS_AUTH_TYPE = "Client credentials (Oauth)";

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private static final String CLOUD_ORCHESTRATOR_BASE_URL = "https://cloud.uipath.com";
    private static final String TENANT_NAME_HEADER = "X-UIPATH-TenantName";
    private static final String X_UIPATH_ORGANIZATION_UNIT_ID_HEADER = "X-UIPATH-OrganizationUnitId";
    private static final String AUTHORIZATION_NAME_HEADER = "Authorization";

    protected UIPathService service;
    protected ObjectMapper mapper = new ObjectMapper();

    private static String appendTraillingSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        checkCloudInput();
        if (isCloud()) {
            checkMandatoryStringInput(CLOUD_AUTH_TYPE);
            if (CLIENT_CREDENTIALS_AUTH_TYPE.equalsIgnoreCase(getCloudAuthType())) {
                checkMandatoryStringInput(ACCOUNT_LOGICAL_NAME);
                checkMandatoryStringInput(TENANT_LOGICAL_NAME);
                checkMandatoryStringInput(CLIENT_SECRET);
                checkMandatoryStringInput(CLIENT_ID);
                checkMandatoryStringInput(ORGANIZATION_UNIT_ID);
            } else if (TOKEN_AUTH_TYPE.equalsIgnoreCase(getCloudAuthType())) {
                checkMandatoryStringInput(ACCOUNT_LOGICAL_NAME);
                checkMandatoryStringInput(TENANT_LOGICAL_NAME);
                checkMandatoryStringInput(TOKEN);
                checkMandatoryStringInput(ORGANIZATION_UNIT_ID);
            } else {
                throw new ConnectorValidationException(this,
                        String.format("Cloud authentication type '%s' is not supported. Supported types are: '%s' and '%s'",
                                getCloudAuthType(), TOKEN_AUTH_TYPE, CLIENT_CREDENTIALS_AUTH_TYPE));
            }
        } else {
            checkMandatoryStringInput(URL);
            checkMandatoryStringInput(TENANT);
            checkMandatoryStringInput(USER);
            checkMandatoryStringInput(PASSWORD);
        }
    }

    protected void checkCloudInput() throws ConnectorValidationException {
        Boolean value = null;
        try {
            value = (Boolean) getInputParameter(CLOUD);
        } catch (ClassCastException e) {
            throw new ConnectorValidationException(this, String.format("'%s' parameter must be a Boolean", CLOUD));
        }
        if (value == null) {
            throw new ConnectorValidationException(this, String.format("Mandatory parameter '%s' is missing.", CLOUD));
        }
    }

    protected void checkMandatoryStringInput(String input) throws ConnectorValidationException {
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

    @Override
    public void connect() throws ConnectorException {
        service = createService();
    }

    protected UIPathService getService() {
        return service;
    }

    protected String buildTokenHeader(String token) {
        return "Bearer " + token;
    }

    String authenticate() throws ConnectorException {
        Response<Map<String, String>> response;
        try {
            if (isCloud()) {
                if (CLIENT_CREDENTIALS_AUTH_TYPE.equalsIgnoreCase(getCloudAuthType())) {
                    LOGGER.debug("Authenticate in cloud with client credentials");
                    response = service.authenticateInCloudWithClientCredentials(
                            getAccountLogicalName(),
                            "client_credentials",
                            getClientId(),
                            getClientSecret(),
                            getScope()).execute();
                } else {
                    LOGGER.debug("Authenticate in cloud with token");
                    return getToken();
                }
            } else {
                LOGGER.debug("Authenticate on premise");
                response = service.authenticate(getTenant(), getUser(), getPassword()).execute();
            }
        } catch (IOException e) {
            throw new ConnectorException(
                    String.format("Failed to authenticate to '%s' on tenant '%s' with user '%s'", getUrl(), getTenant(),
                            getUser()),
                    e);
        }
        if (!response.isSuccessful()) {
            throw new ConnectorException(String.format("Authentication failed: %s - %s",
                    response.code(),
                    getErrorMessage(response)));
        }
        return isCloud()
                ? response.body().get("access_token")
                : response.body().get("result");
    }

    protected String getErrorMessage(Response<?> response) {
        try {
            return response.errorBody().string();
        } catch (IOException e) {
            return null;
        }
    }

    protected Map<String, String> createAuthenticationHeaders(String token) {
        Map<String, String> headers = new HashMap<>();
        headers.put(AUTHORIZATION_NAME_HEADER, buildTokenHeader(token));
        if (isCloud()) {
            headers.put(TENANT_NAME_HEADER, getTenantLogicalName());
        }
        return headers;
    }

    protected UIPathService createService() {
        if (service == null) {
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
            Interceptor jsonHeaderInterceptor = chain -> {
                Request.Builder requestBuilder = chain.request().newBuilder();
                requestBuilder.header(CONTENT_TYPE, APPLICATION_JSON);
                if (isCloud()) {
                    requestBuilder.header(X_UIPATH_ORGANIZATION_UNIT_ID_HEADER, getOrganizationUnitId());
                }
                return chain.proceed(requestBuilder.build());
            };
            clientBuilder.addInterceptor(jsonHeaderInterceptor);
            if (LOGGER.isDebugEnabled()) {
                HttpLoggingInterceptor loggerInterceptor = new HttpLoggingInterceptor();
                loggerInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                clientBuilder.addInterceptor(loggerInterceptor);
            }
            OkHttpClient client = clientBuilder.build();
            Builder retrofitBuilder = new Retrofit.Builder()
                    .client(client)
                    .addConverterFactory(new WrappedAttributeConverter(mapper))
                    .addConverterFactory(JacksonConverterFactory.create())
                    .baseUrl(getUrl());
            if (client != null) {
                retrofitBuilder.client(client);
            }
            service = retrofitBuilder.build().create(UIPathService.class);
        }
        return service;
    }

    protected Map<Object, Object> toMap(Object inputParameter) {
        Map<Object, Object> result = new HashMap<>();
        for (Object row : (Iterable) inputParameter) {
            if (row instanceof List && ((List) row).size() == 2) {
                result.put(((List<Object>) row).get(0), ((List<Object>) row).get(1));
            }
        }
        return result;
    }

    String getOrganizationUnitId() {
        return (String) getInputParameter(ORGANIZATION_UNIT_ID);
    }

    String getTenant() {
        return (String) getInputParameter(TENANT);
    }

    String getTenantLogicalName() {
        return (String) getInputParameter(TENANT_LOGICAL_NAME);
    }

    String getUser() {
        return (String) getInputParameter(USER);
    }

    String getPassword() {
        return (String) getInputParameter(PASSWORD);
    }

    String getUrl() {
        return appendTraillingSlash(isCloud()
                ? String.format("%s/%s/%s", CLOUD_ORCHESTRATOR_BASE_URL, getAccountLogicalName(),
                getTenantLogicalName())
                : (String) getInputParameter(URL));
    }

    boolean isCloud() {
        return (boolean) getInputParameter(CLOUD);
    }

    String getCloudAuthType() {
        return (String) getInputParameter(CLOUD_AUTH_TYPE);
    }

    String getAccountLogicalName() {
        return (String) getInputParameter(ACCOUNT_LOGICAL_NAME);
    }

    String getClientId() {
        return (String) getInputParameter(CLIENT_ID);
    }

    String getClientSecret() {
        return (String) getInputParameter(CLIENT_SECRET);
    }

    String getToken() {
        return (String) getInputParameter(TOKEN);
    }

    String getScope() {
        return (String) getInputParameter(SCOPE);
    }

}
