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

import java.io.IOException;

import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.bonitasoft.engine.connector.uipath.model.Job;

import retrofit2.Response;

public class UIPathGetJobConnector extends UIPathConnector {

    static final String JOB_ID = "jobId";
    static final String JOB_OUTPUT_ARGS = "jobOutputsArgs";
    static final String JOB_STATE = "jobState";

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        super.validateInputParameters();
        checkMandatoryStringInput(JOB_ID);
        try {
            Long.valueOf(getJobId());
        } catch (NumberFormatException e) {
            throw new ConnectorValidationException(this,
                    String.format("Job id input must have a valid number format be is '%s'.", getJobId()));
        }
    }

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        String token = authenticate();
        try {
            Job job = job(Long.valueOf(getJobId()), token);
            String state = job.getState();
            setOutputParameter(JOB_STATE, state);
            setOutputParameter(JOB_OUTPUT_ARGS, job.getOutputArgs());
        } catch (NumberFormatException | IOException e) {
            throw new ConnectorException(
                    String.format("Failed to get job with id: %s", getJobId()), e);
        }
    }

    String getJobId() {
        return (String) getInputParameter(JOB_ID);
    }

    Job job(long id, String token) throws IOException, ConnectorException {
        Response<Job> response = getService().job(buildTokenHeader(token), id).execute();
        if (!response.isSuccessful()) {
            throw new ConnectorException(response.errorBody().string());
        }
        setOutputParameter(STATUS_CODE_OUTPUT, response.code());
        setOutputParameter(STATUS_MESSAGE_OUTPUT, response.message());
        return response.body();
    }

}
