package com.cammy.locationupdates.activities

import android.os.Bundle
import android.support.v4.app.Fragment
import com.cammy.cammyui.activities.BaseActivity
import com.cammy.locationupdates.fragments.RootFragment


class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var rootFragment: Fragment? = supportFragmentManager.findFragmentByTag(BaseActivity.FRAGMENT_ROOT)
        if (rootFragment == null) {
            rootFragment = RootFragment.newInstance()
        }
        setRootFragment(rootFragment)
    }
}
