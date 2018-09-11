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
package org.bonitasoft.connector.uipath;

import java.util.List;
import java.util.Map;

import org.bonitasoft.connector.uipath.model.Job;
import org.bonitasoft.connector.uipath.model.JobRequest;
import org.bonitasoft.connector.uipath.model.Release;
import org.bonitasoft.connector.uipath.model.Robot;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface UIPathService {

    @FormUrlEncoded
    @POST("api/account/authenticate")
    Call<Map<String, String>> authenticate(@Field("tenancyName") String tenant,
            @Field("usernameOrEmailAddress") String user,
            @Field("password") String password);

    @GET("/odata/Releases")
    Call<List<Release>> releases(@Header("Authorization") String token);

    @GET("/odata/Robots")
    Call<List<Robot>> robots(@Header("Authorization") String token);

    @POST("/odata/Jobs/UiPath.Server.Configuration.OData.StartJobs")
    Call<List<Job>> startJob(@Header("Authorization") String token, @Body JobRequest jobRequest);
}
