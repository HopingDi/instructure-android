/*
 * Copyright (C) 2016 - present Instructure, Inc.
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

package com.instructure.candroid.fragment

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.instructure.candroid.R
import com.instructure.candroid.router.RouteMatcher
import com.instructure.candroid.util.LockInfoHTMLHelper
import com.instructure.canvasapi2.managers.OAuthManager
import com.instructure.canvasapi2.managers.QuizManager
import com.instructure.canvasapi2.models.AuthenticatedSession
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.Quiz
import com.instructure.canvasapi2.utils.*
import com.instructure.canvasapi2.utils.weave.*
import com.instructure.interactions.router.Route
import com.instructure.interactions.router.RouterParams
import com.instructure.pandautils.utils.*

class BasicQuizViewFragment : InternalWebviewFragment() {

    private var quizDetailsJob: WeaveJob? = null

    private var baseURL: String? by NullableStringArg()
    private var apiURL: String? by NullableStringArg()
    private var quiz: Quiz? by NullableParcelableArg()
    private var quizId: Long by LongArg()

    override fun title(): String = getString(R.string.quizzes)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isUnsupportedFeature = true
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // anything that relies on intent data belongs here
        if (apiURL != null) {
            getQuizDetails(apiURL!!)
        } else if (quiz != null && quiz?.lockInfo != null && CanvasContext.Type.isCourse(canvasContext) && !(canvasContext as Course).isTeacher) {
            //if the quiz is locked we don't care if they're a teacher
            populateWebView(LockInfoHTMLHelper.getLockedInfoHTML(quiz?.lockInfo, activity, R.string.lockedQuizDesc, R.string.lockedAssignmentDescLine2))
        } else if (quizId != 0L) {
            getQuizDetails(quizId)
        } else {
            quizDetailsJob = weave {
                val targetUrl = quiz?.url ?: baseURL
                val authenticatedUrl = tryOrNull {
                    awaitApi<AuthenticatedSession> { OAuthManager.getAuthenticatedSession(targetUrl, it) }.sessionUrl
                }
                loadUrl(authenticatedUrl ?: targetUrl)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //we need to set the webviewclient before we get the quiz so it doesn't try to open the
        //quiz in a different browser
        if (baseURL == null) {
            //if the baseURL is null something went wrong, nothing will show here
            //but at least it won't crash
            return
        }
        val uri = Uri.parse(baseURL)
        val host = uri.host
        getCanvasWebView()?.settings?.javaScriptCanOpenWindowsAutomatically = true
        getCanvasWebView()?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return handleOverrideURlLoading(view, request?.url?.toString())
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleOverrideURlLoading(view, url)
            }

            private fun handleOverrideURlLoading(view: WebView?, url: String?): Boolean {
                if(view == null || url == null) return false
                val currentUri = Uri.parse(url)

                if (url.contains(host)) { //we need to handle it.
                    return if (currentUri != null && currentUri.pathSegments.size >= 3 && currentUri.pathSegments[2] == "quizzes") {  //if it's a quiz, stay here.
                        view.loadUrl(url, APIHelper.getReferrer())
                        true
                    } else if (currentUri != null && currentUri.pathSegments.size >= 1 && currentUri.pathSegments[0].equals("login", ignoreCase = true)) {
                        view.loadUrl(url, APIHelper.getReferrer())
                        true
                    } else { //It's content but not a quiz. Could link to a discussion (or whatever) in a quiz. Route
                        RouteMatcher.canRouteInternally(activity, url, ApiPrefs.domain, true)
                    }//might need to log in to take the quiz -- the url would say domain/login. If we just use the AppRouter it will take the user
                    //back to the dashboard. This check will keep them here and let them log in and take the quiz
                }
                return false
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                getCanvasLoading()?.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                getCanvasLoading()?.visibility = View.GONE
            }
        }
    }

    private fun getQuizDetails(quizUrl: String) {
        quizDetailsJob = tryWeave {
            quiz = awaitApi<Quiz> { QuizManager.getDetailedQuizByUrl(quizUrl, true, it) }

            if (shouldShowNatively(quiz)) return@tryWeave

            if (quiz?.lockInfo != null) {
                populateWebView(LockInfoHTMLHelper.getLockedInfoHTML(quiz?.lockInfo, activity, R.string.lockedQuizDesc, R.string.lockedAssignmentDescLine2))
            } else {
                val sourceUrl = quiz?.url.validOrNull() ?: baseURL
                val authenticatedUrl = tryOrNull {
                    awaitApi<AuthenticatedSession> { OAuthManager.getAuthenticatedSession(sourceUrl, it) }.sessionUrl
                }
                getCanvasWebView()?.loadUrl(authenticatedUrl ?: sourceUrl, APIHelper.getReferrer())
            }
        } catch { Logger.e("Error loading quiz information: ${it.message}") }
    }

    private fun getQuizDetails(quizId: Long) {
        quizDetailsJob = tryWeave {
            quiz = awaitApi<Quiz> { QuizManager.getDetailedQuiz(canvasContext, quizId, true, it) }
            baseURL = quiz?.url ?: baseURL

            if (shouldShowNatively(quiz)) return@tryWeave

            if (quiz?.lockInfo != null) {
                populateWebView(LockInfoHTMLHelper.getLockedInfoHTML(quiz!!.lockInfo, activity, R.string.lockedQuizDesc, R.string.lockedAssignmentDescLine2))
            } else {
                val authenticatedUrl = tryOrNull {
                    awaitApi<AuthenticatedSession> { OAuthManager.getAuthenticatedSession(quiz?.url, it) }.sessionUrl
                }
                getCanvasWebView()?.loadUrl(authenticatedUrl ?: quiz?.url, APIHelper.getReferrer())
            }
        } catch { Logger.e("Error loading quiz information: ${it.message}") }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        quizDetailsJob?.cancel()
    }

    override fun handleBackPressed() = getCanvasWebView()?.handleGoBack() ?: false

    /**
     * When we route we always route quizzes here, so this checks to see if we support
     * native quizzes and if we do then we'll show it natively
     * @param quiz
     * @return
     */
    private fun shouldShowNatively(quiz: Quiz?): Boolean {
        if (QuizListFragment.isNativeQuiz(canvasContext, quiz!!)) {
            //take them to the quiz start fragment instead, let them take it natively
            navigation?.popCurrentFragment()
            quiz.let { RouteMatcher.route(context, QuizStartFragment.makeRoute(canvasContext, it).apply { ignoreDebounce = true }) }
            return true
        }
        return false
    }

    companion object {

        @JvmStatic
        fun newInstance(route: Route) : BasicQuizViewFragment? {
            return if(validRoute(route)) BasicQuizViewFragment().apply {
                arguments = route.arguments

                with(arguments) {
                    if (containsKey(Const.URL)) baseURL = getString(Const.URL)
                    if (containsKey(Const.API_URL)) apiURL = getString(Const.API_URL)
                    if (containsKey(Const.QUIZ)) quiz = getParcelable(Const.QUIZ)
                    if (route.paramsHash.containsKey(RouterParams.QUIZ_ID)) quizId = route.paramsHash[RouterParams.QUIZ_ID]?.toLong() ?: 0L
                }

                this.canvasContext = route.canvasContext!!
            } else null
        }

        @JvmStatic
        private fun validRoute(route: Route): Boolean {
            return route.canvasContext != null &&
                    (route.arguments.containsKey(Const.URL) ||
                    (route.arguments.containsKey(Const.URL) && route.arguments.containsKey(Const.API_URL)) ||
                    (route.arguments.containsKey(Const.QUIZ) && route.arguments.containsKey(Const.URL)) ||
                     route.paramsHash.containsKey(RouterParams.QUIZ_ID))
        }

        @JvmStatic
        fun makeRoute(canvasContext: CanvasContext, url: String): Route {
            return Route(null, BasicQuizViewFragment::class.java, canvasContext, canvasContext.makeBundle(Bundle().apply { putString(Const.URL, url) }))
        }

        @JvmStatic
        fun makeRoute(canvasContext: CanvasContext, url: String, apiUrl: String): Route {
            return Route(null, BasicQuizViewFragment::class.java, canvasContext, canvasContext.makeBundle(
                    Bundle().apply {
                        putString(Const.URL, url)
                        putString(Const.API_URL, apiUrl)
                    }))
        }

        @JvmStatic
        fun makeRoute(canvasContext: CanvasContext, quiz: Quiz, url: String): Route {
            return Route(null, BasicQuizViewFragment::class.java, canvasContext, canvasContext.makeBundle(
                    Bundle().apply {
                        putString(Const.URL, url)
                        putParcelable(Const.QUIZ, quiz)
                    }))
        }
    }
}
