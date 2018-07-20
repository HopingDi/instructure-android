//
// Copyright (C) 2018-present Instructure, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//


package com.instructure.dataseeding.api

import com.instructure.dataseeding.model.ConversationModel
import com.instructure.dataseeding.model.CreateConversation
import com.instructure.dataseeding.util.CanvasRestAdapter
import com.instructure.dataseeding.util.Randomizer
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

object ConversationsApi {
    interface ConversationsService {
        @POST("conversations")
        fun createConversation(@Body createConversation: CreateConversation): Call<List<ConversationModel>>
    }

    private fun conversationsService(token: String): ConversationsService
            = CanvasRestAdapter.retrofitWithToken(token).create(ConversationsApi.ConversationsService::class.java)

    fun createConversation(token: String, recipients: List<String>): List<ConversationModel> {
        val conversation = CreateConversation(recipients, Randomizer.randomConversationSubject(), Randomizer.randomConversationBody())
        return conversationsService(token)
                .createConversation(conversation)
                .execute()
                .body()!!
    }
}