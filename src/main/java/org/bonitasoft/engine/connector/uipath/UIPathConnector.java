package org.bonitasoft.engine.connector.uipath;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.connector.uipath.converters.WrappedAttributeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.Retrofit.Builder;
import retrofit2.converter.jackson.JacksonConverterFactory;

public abstract class UIPathConnector extends AbstractConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(UIPathConnector.class.getName());

    static final String URL = "url";
    static final String USER = "user";
    static final String PASSWORD = "password";
    static final String TENANT = "tenant";

    protected UIPathService service;
    protected ObjectMapper mapper = new ObjectMapper();

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        checkMandatoryStringInput(URL);
        checkMandatoryStringInput(TENANT);
        checkMandatoryStringInput(USER);
        checkMandatoryStringInput(PASSWORD);
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
            response = service.authenticate(getTenant(), getUser(), getPassword())
                    .execute();
        } catch (IOException e) {
            throw new ConnectorException(
                    String.format("Failed to authenticate to '%s' on tenant '%s' with user '%s'", getUrl(), getTenant(),
                            getUser()),
                    e);
        }
        if (!response.isSuccessful()) {
            try {
                throw new ConnectorException(response.errorBody().string());
            } catch (IOException e) {
                throw new ConnectorException("Failed to read response body.", e);
            }
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Map<Object, Object> toMap(Object inputParameter) {
        Map<Object, Object> result = new HashMap<>();
        for (Object row : (Iterable) inputParameter) {
            if (row instanceof List && ((List) row).size() == 2) {
                result.put(((List<Object>) row).get(0), ((List<Object>) row).get(1));
            }
        }
        return result;
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

}
