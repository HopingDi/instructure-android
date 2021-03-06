/*
 * Copyright (C) 2018 - present Instructure, Inc.
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
 */
@file:Suppress("unused")

package com.instructure.parentapp.ui.utils

import com.instructure.canvasapi2.models.User
import com.instructure.dataseeding.InProcessServer
import com.instructure.soseedy.CanvasUser
import com.instructure.soseedy.SeedParentDataRequest
import com.instructure.soseedy.SeededParentData

fun ParentTest.seedData(parents: Int = 0, courses: Int = 0, students: Int = 0): SeededParentData {
    val request = SeedParentDataRequest.newBuilder()
        .setParents(parents)
        .setCourses(courses)
        .setStudents(students)
        .build()
    return mockableSeed { InProcessServer.generalClient.seedParentData(request) }
}

fun ParentTest.tokenLogin(user: CanvasUser) {
    activityRule.runOnUiThread {
        activityRule.activity.loginWithToken(
            user.token,
            user.domain,
            User().apply {
                id = user.id
                name = user.name
                shortName = user.shortName
                avatarUrl = user.avatarUrl
            }
        )
    }
}
