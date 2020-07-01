package org.bonitasoft.engine.connector.uipath;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.Test;

public class UIPathConnectorTest {

    @Test
    public void should_check_inputs_depending_on_cloud_boolean() throws ConnectorValidationException {
        UIPathConnector connector = spy(new UIPathConnector() {

            @Override
            protected void checkcloudInput() throws ConnectorValidationException {
            }

            @Override
            protected void checkMandatoryStringInput(String input) throws ConnectorValidationException {
            }

            @Override
            protected void executeBusinessLogic() throws ConnectorException {
            }
        });

        when(connector.isCloud()).thenReturn(true);
        connector.validateInputParameters();
        verify(connector, times(1)).checkMandatoryStringInput(UIPathConnector.ACCOUNT_LOGICAL_NAME);
        verify(connector, times(1)).checkMandatoryStringInput(UIPathConnector.TENANT_LOGICAL_NAME);
        verify(connector, times(1)).checkMandatoryStringInput(UIPathConnector.CLIENT_ID);
        verify(connector, times(1)).checkMandatoryStringInput(UIPathConnector.USER_KEY);
        verify(connector, times(0)).checkMandatoryStringInput(UIPathConnector.URL);
        verify(connector, times(0)).checkMandatoryStringInput(UIPathConnector.PASSWORD);
        verify(connector, times(0)).checkMandatoryStringInput(UIPathConnector.USER);
        verify(connector, times(0)).checkMandatoryStringInput(UIPathConnector.TENANT);

        when(connector.isCloud()).thenReturn(false);
        connector.validateInputParameters();
        verify(connector, times(1)).checkMandatoryStringInput(UIPathConnector.ACCOUNT_LOGICAL_NAME);
        verify(connector, times(1)).checkMandatoryStringInput(UIPathConnector.TENANT_LOGICAL_NAME);
        verify(connector, times(1)).checkMandatoryStringInput(UIPathConnector.CLIENT_ID);
        verify(connector, times(1)).checkMandatoryStringInput(UIPathConnector.USER_KEY);
        verify(connector, times(1)).checkMandatoryStringInput(UIPathConnector.URL);
        verify(connector, times(1)).checkMandatoryStringInput(UIPathConnector.PASSWORD);
        verify(connector, times(1)).checkMandatoryStringInput(UIPathConnector.USER);
        verify(connector, times(1)).checkMandatoryStringInput(UIPathConnector.TENANT);
    }

}
