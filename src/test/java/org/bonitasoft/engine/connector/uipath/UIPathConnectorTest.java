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
import org.bonitasoft.engine.connector.uipath.model.CloudAuthentication;
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
    void should_authenticate_in_the_cloud() throws Exception {
        UIPathConnector connector = newConnector();
        UIPathService sevrice = Mockito.mock(UIPathService.class);
        Call<Map<String, String>> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(new HashMap<String, String>()));
        when(sevrice.authenticateInCloud(Mockito.anyMap(), Mockito.notNull())).thenReturn(call);
        doReturn(sevrice).when(connector).createService();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIPathConnector.CLOUD, true);
        parameters.put(UIPathConnector.ACCOUNT_LOGICAL_NAME, "bonitasoft");
        parameters.put(UIPathConnector.TENANT_LOGICAL_NAME, "a_tenant");
        parameters.put(UIPathConnector.ORGANIZATION_UNIT_ID, "myUnitId");
        parameters.put(UIPathConnector.USER_KEY, "someToken");
        parameters.put(UIPathConnector.CLIENT_ID, "1234");
        parameters.put(UIPathGetJobConnector.JOB_ID, "268348846");
        connector.setInputParameters(parameters);
        connector.validateInputParameters();
        connector.connect();
        connector.authenticate();

        Map<String, String> headers = new HashMap<>();
        headers.put("X-UIPATH-TenantName", "a_tenant");
        headers.put("Content-Type", "application/json");
        CloudAuthentication cloudAuthentication = new CloudAuthentication("refresh_token", "1234", "someToken");

        verify(sevrice).authenticateInCloud(headers, cloudAuthentication);
    }

    private UIPathConnector newConnector() {
        return spy(new UIPathConnector() {

            @Override
            protected void executeBusinessLogic() throws ConnectorException {
            }
        });
    }

}
