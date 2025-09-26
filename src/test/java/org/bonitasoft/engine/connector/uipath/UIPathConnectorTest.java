package org.bonitasoft.engine.connector.uipath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import retrofit2.Call;
import retrofit2.Response;

class UIPathConnectorTest {

    @Test
    void should_validate_cloud_input_parameter() throws Exception {
        UIPathConnector connector = newConnector();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIPathConnector.CLOUD, null);
        connector.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> connector.validateInputParameters());
        
        parameters.put(UIPathConnector.CLOUD, "true");
        connector.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> connector.validateInputParameters());
    }
    
    @Test
    void should_validate_mandatory_url_input_parameter() throws Exception {
        UIPathConnector connector = newConnector();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIPathConnector.CLOUD, false);
        parameters.put(UIPathConnector.URL, null);
        connector.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> connector.validateInputParameters());
        
        parameters.put(UIPathConnector.URL, 123);
        connector.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> connector.validateInputParameters());
        
        parameters.put(UIPathConnector.URL, "");
        connector.setInputParameters(parameters);
        assertThrows(ConnectorValidationException.class, () -> connector.validateInputParameters());
    }
    
    @Test
    void should_build_on_premise_url() throws Exception {
        UIPathConnector connector = newConnector();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIPathConnector.CLOUD, false);
        
        parameters.put(UIPathConnector.URL, "https://cloud.uipath.com");
        connector.setInputParameters(parameters);
        assertThat(connector.getUrl()).isEqualTo("https://cloud.uipath.com/");
        
        parameters.put(UIPathConnector.URL, "https://cloud.uipath.com/");
        connector.setInputParameters(parameters);
        assertThat(connector.getUrl()).isEqualTo("https://cloud.uipath.com/");
    }
    
    @Test
    void should_build_cloud_url() throws Exception {
        UIPathConnector connector = newConnector();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIPathConnector.CLOUD, true);
        
        parameters.put(UIPathConnector.ACCOUNT_LOGICAL_NAME, "bonita");
        parameters.put(UIPathConnector.TENANT_LOGICAL_NAME, "bonita");
        connector.setInputParameters(parameters);
        assertThat(connector.getUrl()).isEqualTo("https://cloud.uipath.com/bonita/bonita/");
    }

    @Test
    void should_authenticate_on_premise() throws Exception {
        UIPathConnector connector = newConnector();
        UIPathService sevrice = Mockito.mock(UIPathService.class);
        Call<Map<String, String>> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(new HashMap<String, String>()));
        when(sevrice.authenticate(Mockito.notNull(), Mockito.notNull(), Mockito.notNull())).thenReturn(call);
        doReturn(sevrice).when(connector).createService();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIPathConnector.CLOUD, false);
        parameters.put(UIPathConnector.URL, "http://localhost:9090");
        parameters.put(UIPathConnector.USER, "bonitasoft");
        parameters.put(UIPathConnector.TENANT, "a_tenant");
        parameters.put(UIPathConnector.PASSWORD, "somePassword");
        connector.setInputParameters(parameters);
        connector.validateInputParameters();
        connector.connect();
        connector.authenticate();

        verify(sevrice).authenticate("a_tenant", "bonitasoft", "somePassword");
    }

    @Test
    void should_authenticate_in_the_cloud_with_client_credentials() throws Exception {
        UIPathConnector connector = newConnector();
        UIPathService service = Mockito.mock(UIPathService.class);
        Call<Map<String, String>> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(new HashMap<String, String>()));
        when(service.authenticateInCloudWithClientCredentials(Mockito.notNull(), Mockito.notNull(), Mockito.notNull(), Mockito.notNull(), Mockito.notNull())).thenReturn(call);
        doReturn(service).when(connector).createService();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIPathConnector.CLOUD, true);
        parameters.put(UIPathConnector.CLOUD_AUTH_TYPE, UIPathConnector.CLIENT_CREDENTIALS_AUTH_TYPE);
        parameters.put(UIPathConnector.ACCOUNT_LOGICAL_NAME, "bonitasoft");
        parameters.put(UIPathConnector.TENANT_LOGICAL_NAME, "a_tenant");
        parameters.put(UIPathConnector.ORGANIZATION_UNIT_ID, "myUnitId");
        parameters.put(UIPathConnector.CLIENT_ID, "1234");
        parameters.put(UIPathConnector.CLIENT_SECRET, "someSecret");
        parameters.put(UIPathConnector.SCOPE, "someScope");
        connector.setInputParameters(parameters);
        connector.validateInputParameters();
        connector.connect();
        connector.authenticate();

        verify(service).authenticateInCloudWithClientCredentials("bonitasoft", "client_credentials", "1234", "someSecret", "someScope");
    }

    @Test
    void should_authenticate_in_the_cloud_with_token() throws Exception {
        UIPathConnector connector = newConnector();
        UIPathService service = Mockito.mock(UIPathService.class);
        doReturn(service).when(connector).createService();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIPathConnector.CLOUD, true);
        parameters.put(UIPathConnector.CLOUD_AUTH_TYPE, UIPathConnector.TOKEN_AUTH_TYPE);
        parameters.put(UIPathConnector.ACCOUNT_LOGICAL_NAME, "bonitasoft");
        parameters.put(UIPathConnector.TENANT_LOGICAL_NAME, "a_tenant");
        parameters.put(UIPathConnector.ORGANIZATION_UNIT_ID, "myUnitId");
        parameters.put(UIPathConnector.TOKEN, "someToken");
        connector.setInputParameters(parameters);
        connector.validateInputParameters();
        connector.connect();

        assertThat(connector.authenticate()).isEqualTo("someToken");

    }

    private UIPathConnector newConnector() {
        return spy(new UIPathConnector() {

            @Override
            protected void executeBusinessLogic() throws ConnectorException {
            }
        });
    }

}
