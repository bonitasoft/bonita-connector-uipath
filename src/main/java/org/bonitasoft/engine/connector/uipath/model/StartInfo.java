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
package org.bonitasoft.engine.connector.uipath.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StartInfo {

    @JsonProperty("ReleaseKey")
    private String releaseKey;
    @JsonProperty("RobotIds")
    @JsonInclude(Include.NON_EMPTY)
    private List<Integer> robotIds;
    @JsonProperty("JobsCount")
    @JsonInclude(Include.NON_NULL)
    private Integer jobsCount;
    @JsonProperty("Strategy")
    @JsonInclude(Include.NON_NULL)
    private String strategy;
    @JsonProperty("Source")
    @JsonInclude(Include.NON_NULL)
    private String source;
    @JsonProperty("InputArguments")
    @JsonInclude(Include.NON_EMPTY)
    private String args;
    @JsonProperty("RuntimeType")
    @JsonInclude(Include.NON_NULL)
    private String runtimeType;
    

}
