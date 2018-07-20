package com.instructure.teacher.ui.pages

import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.withText
import com.instructure.espresso.OnViewWithId
import com.instructure.espresso.page.BasePage
import com.instructure.soseedy.CanvasUser
import com.instructure.teacher.R

class NavDrawerPage: BasePage() {

    private val logo by OnViewWithId(R.id.navigationDrawerInstitutionImage)
    private val settings by OnViewWithId(R.id.navigationDrawerSettings)
    private val userName by OnViewWithId(R.id.navigationDrawerUserName)
    private val userEmail by OnViewWithId(R.id.navigationDrawerUserEmail)
    private val changeUser by OnViewWithId(R.id.navigationDrawerItem_changeUser)
    private val logout by OnViewWithId(R.id.navigationDrawerItem_logout)
    private val version by OnViewWithId(R.id.navigationDrawerVersion)

    fun assertProfileDetails(teacher: CanvasUser) {
        userName.check(matches(withText(teacher.shortName)))
    }
}