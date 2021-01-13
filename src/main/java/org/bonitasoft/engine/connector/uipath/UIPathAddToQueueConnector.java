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
import org.bonitasoft.engine.connector.uipath.model.AddToQueueRequest;
import org.bonitasoft.engine.connector.uipath.model.Priority;
import org.bonitasoft.engine.connector.uipath.model.QueueItem;
import org.bonitasoft.engine.connector.uipath.model.QueueItemRequest;
import retrofit2.Response;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

public class UIPathAddToQueueConnector extends UIPathConnector {

    static final String QUEUE_NAME = "queueName";
    static final String REFERENCE_INPUT = "reference";
    static final String QUEUE_CONTENT = "queueContent";
    static final String PRIORITY_INPUT = "priority";
    static final String DUE_DATE_INPUT = "dueDate";
    static final String DEFER_DATE_INPUT = "deferDate";
    static final String ITEM_ID_OUTPUT = "itemId";
    static final String ITEM_KEY_OUTPUT = "itemKey";
    private static final int MAX_REF_LENGTH = 128;

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        super.validateInputParameters();
        checkMandatoryStringInput(QUEUE_NAME);
        Optional<String> reference = getReference();
        if (reference.isPresent()) {
            String refValue = reference.get();
            if (refValue.length() > MAX_REF_LENGTH) {
                throw new ConnectorValidationException(
                        String.format(
                                "The maximum length of a queue item reference is %s characters. '%s' is too long (%s chars)",
                                MAX_REF_LENGTH, refValue, refValue.length()));
            }
        }
        Optional<String> dueDate = getDueDate();
        if (dueDate.isPresent()) {
            validateDateFormat(dueDate.get(), DUE_DATE_INPUT);
        }
        Optional<String> deferDate = getDeferDate();
        if (deferDate.isPresent()) {
            validateDateFormat(deferDate.get(), DEFER_DATE_INPUT);
        }
    }

    private void validateDateFormat(String dateValue, String input) throws ConnectorValidationException {
        try {
            OffsetDateTime.parse(dateValue);
        } catch (DateTimeParseException e) {
            throw new ConnectorValidationException(String
                    .format("Invalid date format for input '%s'. ISO-8601 date format is expected but found '%s'",
                            input, dateValue));
        }
    }

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        String token = authenticate();
        QueueItemRequest itemData = new QueueItemRequest()
                .setName(getQueueName())
                .setPriority(getPriority());
        getReference().ifPresent(itemData::setReference);
        Optional<Map<Object, Object>> content = getContent();
        if (content.isPresent()) {
            Map<String, Object> contentMap = content.get().entrySet().stream().collect(Collectors.toMap(
                    entry -> entry.getKey().toString(),
                    Entry<Object, Object>::getValue));
            itemData.setContent(contentMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            e -> {
                                try {
                                    return isPrimitive(e.getValue()) ? e.getValue()
                                            : mapper.writeValueAsString(e.getValue());
                                } catch (JsonProcessingException e1) {
                                    return null;
                                }
                            })));
        }
        getDueDate().ifPresent(itemData::setDueDate);
        getDeferDate().ifPresent(itemData::setDeferDate);
        try {
            QueueItem item = addToQueue(token, new AddToQueueRequest(itemData));
            setOutputParameter(ITEM_ID_OUTPUT, item.getId());
            setOutputParameter(ITEM_KEY_OUTPUT, item.getKey());
        } catch (IOException e) {
            throw new ConnectorException("Failed to add queue item.", e);
        }
    }

    private boolean isPrimitive(Object value) {
        return value != null && (value instanceof String || value.getClass().isPrimitive());
    }

    String getQueueName() {
        return (String) getInputParameter(QUEUE_NAME);
    }

    Optional<String> getReference() {
        return Optional.ofNullable((String) getInputParameter(REFERENCE_INPUT));
    }

    Optional<Map<Object, Object>> getContent() {
        Object inputParameter = getInputParameter(QUEUE_CONTENT);
        if (inputParameter instanceof List) {
            return Optional.of(toMap(inputParameter));
        }
        return Optional.ofNullable((Map<Object, Object>) getInputParameter(QUEUE_CONTENT));
    }

    String getPriority() {
        return Optional.ofNullable((String) getInputParameter(PRIORITY_INPUT)).orElse(Priority.NORMAL.toString());
    }

    Optional<String> getDueDate() {
        return Optional.ofNullable((String) getInputParameter(DUE_DATE_INPUT));
    }

    Optional<String> getDeferDate() {
        return Optional.ofNullable((String) getInputParameter(DEFER_DATE_INPUT));
    }

    QueueItem addToQueue(String token, AddToQueueRequest request) throws IOException, ConnectorException {
        Response<QueueItem> response = getService().addQueueItem(createAuthenticationHeaders(token), request).execute();
        if (!response.isSuccessful()) {
            throw new ConnectorException(response.errorBody().string());
        }
        return response.body();
    }

}
