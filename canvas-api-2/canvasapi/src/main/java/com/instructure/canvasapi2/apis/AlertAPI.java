/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.instructure.canvasapi2.apis;

import android.support.annotation.NonNull;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.Alert;
import com.instructure.canvasapi2.models.ObserverAlert;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Url;


public class AlertAPI {

    public static final String ALERT_DISMISSED = "dismissed";
    public static final String ALERT_READ = "read";

    public static final String AIRWOLF_DOMAIN_AMERICA = "https://airwolf-iad-prod.instructure.com";
    public static final String AIRWOLF_DOMAIN_DUBLIN = "https://airwolf-dub-prod.instructure.com";
    public static final String AIRWOLF_DOMAIN_SYDNEY = "https://airwolf-syd-prod.instructure.com";
    public static final String AIRWOLF_DOMAIN_SINGAPORE = "https://airwolf-sin-prod.instructure.com";
    public static final String AIRWOLF_DOMAIN_FRANKFURT = "https://airwolf-fra-prod.instructure.com";
    public static final String AIRWOLF_DOMAIN_CANADA = "https://airwolf-yul-prod.instructure.com";

    public static final String[] AIRWOLF_DOMAIN_LIST = {
            AIRWOLF_DOMAIN_AMERICA, AIRWOLF_DOMAIN_DUBLIN,
            AIRWOLF_DOMAIN_SYDNEY, AIRWOLF_DOMAIN_SINGAPORE,
            AIRWOLF_DOMAIN_FRANKFURT, AIRWOLF_DOMAIN_CANADA };

    interface AlertInterface {

        @GET("alerts/student/{parentId}/{studentId}")
        Call<List<Alert>> getAlertsForStudent(@Path("parentId") String parentId, @Path("studentId") String studentId);

        @GET
        Call<List<Alert>> next(@Url String nextUrl);

        @FormUrlEncoded
        @POST("alert/{parentId}/{alertId}")
        Call<ResponseBody> markAlertAsRead(@Path("parentId") String parentId, @Path("alertId") String alertId, @Field("read") String isRead);

        @FormUrlEncoded
        @POST("alert/{parentId}/{alertId}")
        Call<ResponseBody> markAlertAsDismissed(@Path("parentId") String parentId, @Path("alertId") String alertId, @Field("dismissed") String isDismissed);

        @GET("users/self/observer_alerts/{studentId}")
        Call<List<ObserverAlert>> getObserverAlertsForStudent(@Path("studentId") long studentId);

        @GET
        Call<List<ObserverAlert>> nextObservers(@Url String nextUrl);

        @PUT("users/self/observer_alerts/{alertId}/{workflowState}")
        Call<ObserverAlert> updateObserverAlert(@Path("alertId") long alertId, @Path("workflowState") String workflowState);
    }

    //region Alert API

    public static void getAlertsAirwolf(
            @NonNull String parentId,
            @NonNull String studentId,
            @NonNull RestBuilder adapter,
            @NonNull StatusCallback<List<Alert>> callback,
            @NonNull RestParams params) {

        if(StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(AlertInterface.class, params)
                    .getAlertsForStudent(parentId, studentId)).enqueue(callback);
        } else if(StatusCallback.moreCallsExist(callback.getLinkHeaders()) && callback.getLinkHeaders() != null) {
            callback.addCall(adapter.build(AlertInterface.class, params)
                    .next(callback.getLinkHeaders().nextUrl)).enqueue(callback);
        }
    }

    public static void markAlertAsRead(
            @NonNull String parentId,
            @NonNull String alertId,
            @NonNull RestBuilder adapter,
            @NonNull StatusCallback<ResponseBody> callback,
            @NonNull RestParams params) {

        callback.addCall(adapter.build(AlertInterface.class, params)
                .markAlertAsRead(parentId, alertId, "true")).enqueue(callback);
    }

    public static void markAlertAsDismissed(
            @NonNull String parentId,
            @NonNull String alertId,
            @NonNull RestBuilder adapter,
            @NonNull StatusCallback<ResponseBody> callback,
            @NonNull RestParams params) {

        callback.addCall(adapter.build(AlertInterface.class, params)
                .markAlertAsDismissed(parentId, alertId, "true")).enqueue(callback);
    }

    public static void getObserverAlerts(
            long studentId,
            @NonNull RestBuilder adapter,
            @NonNull StatusCallback<List<ObserverAlert>> callback,
            @NonNull RestParams params) {

        if(StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(AlertInterface.class, params)
                    .getObserverAlertsForStudent(studentId)).enqueue(callback);
        } else if(StatusCallback.moreCallsExist(callback.getLinkHeaders()) && callback.getLinkHeaders() != null) {
            callback.addCall(adapter.build(AlertInterface.class, params)
                    .nextObservers(callback.getLinkHeaders().nextUrl)).enqueue(callback);
        }
    }

    public static void updateObserverAlert(
            @NonNull long alertId,
            @NonNull String workflowState, // currently either read or dismissed
            @NonNull RestBuilder adapter,
            @NonNull StatusCallback<ObserverAlert> callback,
            @NonNull RestParams params) {

        callback.addCall(adapter.build(AlertInterface.class, params)
                .updateObserverAlert(alertId, workflowState)).enqueue(callback);
    }

    //endregion
}