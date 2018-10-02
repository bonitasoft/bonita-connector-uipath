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
package org.bonitasoft.engine.connector.uipath.converters;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Converter.Factory;
import retrofit2.Retrofit;


public class WrappedAttributeConverter extends Factory {

    private final ObjectMapper objectMapper;

    public WrappedAttributeConverter(ObjectMapper mapper) {
        objectMapper = mapper;
    }
    /*
     * (non-Javadoc)
     * @see retrofit2.Converter.Factory#responseBodyConverter(java.lang.reflect.Type, java.lang.annotation.Annotation[], retrofit2.Retrofit)
     */
    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        if (isParameterizedList(type)) {
            return responseBody -> listFromJson(responseBody.string(), "value",
                    getClassArgumentFromParameterizedList(type));
        }
        return null;
    }

    private boolean isParameterizedList(Type type) {
        if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            return rawType.equals(List.class);
        }
        return false;
    }

    private static Class<?> getClassArgumentFromParameterizedList(Type type) {
        Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
        return (Class<?>) actualTypeArguments[0];
    }

    public <T> List<T> listFromJson(String json, String listAttribute, Class<T> elementClass) throws IOException {
        return objectMapper.readValue(objectMapper.readTree(json).get(listAttribute).toString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass));
    }
}
