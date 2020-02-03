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

import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.connector.uipath.model.AddToQueueRequest;
import org.bonitasoft.engine.connector.uipath.model.CloudAuthentication;
import org.bonitasoft.engine.connector.uipath.model.Job;
import org.bonitasoft.engine.connector.uipath.model.JobRequest;
import org.bonitasoft.engine.connector.uipath.model.QueueItem;
import org.bonitasoft.engine.connector.uipath.model.Release;
import org.bonitasoft.engine.connector.uipath.model.Robot;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface UIPathService {

    @FormUrlEncoded
    @POST("api/account/authenticate")
    Call<Map<String, String>> authenticate(@Field("tenancyName") String tenant,
            @Field("usernameOrEmailAddress") String user,
            @Field("password") String password);

    @POST("https://account.uipath.com/oauth/token")
    Call<Map<String, String>> authenticateInCloud(@HeaderMap Map<String, String> headers,
            @Body CloudAuthentication cloudAuthentication);

    @GET("odata/Releases")
    Call<List<Release>> releases(@HeaderMap Map<String, String> headers);

    @GET("odata/Robots")
    Call<List<Robot>> robots(@HeaderMap Map<String, String> headers);

    @POST("odata/Jobs/UiPath.Server.Configuration.OData.StartJobs")
    Call<List<Job>> startJob(@HeaderMap Map<String, String> headers, @Body JobRequest jobRequest);

    @GET("odata/Jobs({Id})")
    Call<Job> job(@HeaderMap Map<String, String> headers, @Path("Id") long id);

    @POST("odata/Queues/UiPathODataSvc.AddQueueItem")
    Call<QueueItem> addQueueItem(@HeaderMap Map<String, String> headers, @Body AddToQueueRequest request);
}
