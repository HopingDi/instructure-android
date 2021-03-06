/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.instructure.teacher.presenters

import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.AnnouncementManager
import com.instructure.canvasapi2.managers.DiscussionManager
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.DiscussionTopicHeader
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.canvasapi2.utils.weave.StatusCallbackError
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.weave
import com.instructure.teacher.viewinterface.DiscussionListView
import instructure.androidblueprint.SyncExpandablePresenter
import kotlinx.coroutines.experimental.Job
import retrofit2.Response
import java.util.*

class DiscussionListPresenter(private val mCanvasContext: CanvasContext, private val mIsAnnouncements: Boolean) : SyncExpandablePresenter<
        String,
        DiscussionTopicHeader,
        DiscussionListView>(String::class.java, DiscussionTopicHeader::class.java) {

    private var discussionsListJob: Job? = null

    override fun loadData(forceNetwork: Boolean) {
        discussionsListJob = weave {
            onRefreshStarted()
            try {
                val response = awaitApi<List<DiscussionTopicHeader>> {
                    if (mIsAnnouncements) AnnouncementManager.getAllAnnouncements(mCanvasContext, forceNetwork, it)
                    else DiscussionManager.getAllDiscussionTopicHeaders(mCanvasContext, forceNetwork, it)
                }

                if(mIsAnnouncements) {
                    data.addOrUpdateAllItems(ANNOUNCEMENTS, response)
                } else {
                    data.addOrUpdateAllItems(PINNED, response.filter { getHeaderType(it) == PINNED })
                    data.addOrUpdateAllItems(CLOSED_FOR_COMMENTS, response.filter { getHeaderType(it) == CLOSED_FOR_COMMENTS })
                    data.addOrUpdateAllItems(UNPINNED, response.filter { getHeaderType(it) == UNPINNED })
                }
            } catch (e: StatusCallbackError) {
            }
            viewCallback?.onRefreshFinished()
            viewCallback?.checkIfEmpty()
        }
    }

    override fun refresh(forceNetwork: Boolean) {
        discussionsListJob?.cancel()
        onRefreshStarted()
        clearData()
        loadData(true)
    }

    override fun onDestroyed() {
        super.onDestroyed()
        discussionsListJob?.cancel()
    }

    private val mDiscussionTopicHeaderPinnedCallback = object : StatusCallback<DiscussionTopicHeader>() {
        override fun onResponse(response: Response<DiscussionTopicHeader>, linkHeaders: LinkHeaders, type: ApiType) {
            response.body()?.let { viewCallback?.moveToGroup(PINNED, it) }
        }
    }

    private val mDiscussionTopicHeaderUnpinnedCallback = object : StatusCallback<DiscussionTopicHeader>() {
        override fun onResponse(response: Response<DiscussionTopicHeader>, linkHeaders: LinkHeaders, type: ApiType) {
            response.body()?.let { viewCallback?.moveToGroup(UNPINNED, it) }
        }
    }

    private val mDiscussionTopicHeaderClosedForCommentsCallback = object : StatusCallback<DiscussionTopicHeader>() {
        override fun onResponse(response: Response<DiscussionTopicHeader>, linkHeaders: LinkHeaders, type: ApiType) {
            // if the discussion is pinned we need to keep it pinned.
            response.body()?.let { viewCallback?.moveToGroup(if(it.isPinned) PINNED else CLOSED_FOR_COMMENTS, it) }
        }
    }

    private val mDiscussionTopicHeaderOpenedForCommentsCallback = object : StatusCallback<DiscussionTopicHeader>() {
        override fun onResponse(response: Response<DiscussionTopicHeader>, linkHeaders: LinkHeaders, type: ApiType) {
            //Check if pinned or unpinned...
            response.body()?.let { viewCallback?.moveToGroup(if(it.isPinned) PINNED else UNPINNED, it) }
        }
    }

    private fun getHeaderType(discussionTopicHeader: DiscussionTopicHeader): String {
        if(discussionTopicHeader.isPinned) return PINNED
        if(discussionTopicHeader.isLocked) return CLOSED_FOR_COMMENTS
        return UNPINNED
    }

    fun requestMoveDiscussionTopicToGroup(groupTo: String, groupFrom: String, discussionTopicHeader: DiscussionTopicHeader) {
        //Move from this group into another
        when(groupFrom) {
            PINNED -> {
                when(groupTo) {
                    UNPINNED -> DiscussionManager.unpinDiscussionTopicHeader(mCanvasContext, discussionTopicHeader.id, mDiscussionTopicHeaderUnpinnedCallback)
                    CLOSED_FOR_COMMENTS -> {
                        // When we're pinned we stay in the same spot, but we need to update the locked status
                        if(discussionTopicHeader.isLocked) {
                            DiscussionManager.unlockDiscussionTopicHeader(mCanvasContext, discussionTopicHeader.id, mDiscussionTopicHeaderOpenedForCommentsCallback)
                        } else {
                            DiscussionManager.lockDiscussionTopicHeader(mCanvasContext, discussionTopicHeader.id, mDiscussionTopicHeaderClosedForCommentsCallback)
                        }
                    }
                    DELETE -> viewCallback?.askToDeleteDiscussionTopicHeader(discussionTopicHeader)
                }
            }
            UNPINNED -> {
                when(groupTo) {
                    PINNED -> DiscussionManager.pinDiscussionTopicHeader(mCanvasContext, discussionTopicHeader.id, mDiscussionTopicHeaderPinnedCallback)
                    CLOSED_FOR_COMMENTS -> DiscussionManager.lockDiscussionTopicHeader(mCanvasContext, discussionTopicHeader.id, mDiscussionTopicHeaderClosedForCommentsCallback)
                    DELETE -> viewCallback?.askToDeleteDiscussionTopicHeader(discussionTopicHeader)
                }
            }
            CLOSED_FOR_COMMENTS -> {
                when(groupTo) {
                    PINNED -> DiscussionManager.pinDiscussionTopicHeader(mCanvasContext, discussionTopicHeader.id, mDiscussionTopicHeaderPinnedCallback)
                    CLOSED_FOR_COMMENTS -> DiscussionManager.unlockDiscussionTopicHeader(mCanvasContext, discussionTopicHeader.id, mDiscussionTopicHeaderOpenedForCommentsCallback)
                    DELETE -> viewCallback?.askToDeleteDiscussionTopicHeader(discussionTopicHeader)
                }
            }
            DELETE -> viewCallback?.askToDeleteDiscussionTopicHeader(discussionTopicHeader)
        }
    }

    fun deleteDiscussionTopicHeader(discussionTopicHeader: DiscussionTopicHeader) {
        DiscussionManager.deleteDiscussionTopicHeader(mCanvasContext, discussionTopicHeader.id, object : StatusCallback<Void>() {
            override fun onResponse(response: Response<Void>, linkHeaders: LinkHeaders, type: ApiType) {
                viewCallback?.discussionDeletedSuccessfully(discussionTopicHeader)
            }
        })
    }

    companion object {
        //Named funny to preserve the order.
        const val PINNED = "1_PINNED"
        const val UNPINNED = "2_UNPINNED"
        const val CLOSED_FOR_COMMENTS = "3_CLOSED_FOR_COMMENTS"
        const val ANNOUNCEMENTS = "ANNOUNCEMENTS"
        const val DELETE = "delete"
    }

    override fun compare(group: String?, item1: DiscussionTopicHeader, item2: DiscussionTopicHeader): Int {
        return when {
            PINNED == group -> item1.position.compareTo(item2.position)
            mIsAnnouncements -> item1.position.compareTo(item2.position) // Keep API order for announcements
            else -> item2.lastReplyAt?.compareTo(item1.lastReplyAt ?: Date(0)) ?: -1
        }
    }

    override fun compare(group1: String?, group2: String?): Int {
        if(group1 == null || group2 == null) return super.compare(group1, group2)
        return group1.compareTo(group2)
    }

    override fun areItemsTheSame(group1: String?, group2: String?): Boolean {
        if(group1 == null || group2 == null) return super.areItemsTheSame(group1, group2)
        return group1 == group2
    }

    override fun areItemsTheSame(item1: DiscussionTopicHeader?, item2: DiscussionTopicHeader?): Boolean {
        return item1?.id == item2?.id
    }

    override fun getUniqueItemId(item: DiscussionTopicHeader?): Long {
        return item?.id ?: super.getUniqueItemId(item)
    }
}
