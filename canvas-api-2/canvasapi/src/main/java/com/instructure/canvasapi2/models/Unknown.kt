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
 *
 */

package com.instructure.canvasapi2.models

import paperparcel.PaperParcel
import paperparcel.PaperParcelable

/**
 * The default Canvas Context type when we don't have one
 */
@PaperParcel
class Unknown : CanvasContext(), PaperParcelable {
    override fun getName(): String = "UNKNOWN"

    override fun getType(): Type = Type.UNKNOWN

    override fun getId(): Long = -1

    override fun describeContents() = 0

    companion object {
        @Suppress("unresolved_reference")
        @JvmField val CREATOR = PaperParcelUnknown.CREATOR
    }
}
