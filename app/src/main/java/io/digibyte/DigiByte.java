package io.digibyte;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import io.digibyte.presenter.activities.DisabledActivity;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.security.BRKeyStore;
import io.fabric.sdk.android.Fabric;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class DigiByte extends Application implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = DigiByte.class.getName();

    public static final String HOST = "digibyte.io";
    public static final String FEE_URL = "https://go.digibyte.co/bws/api/v2/feelevels";

    private static DigiByte application;

    public static DigiByte getContext() {
        return application;
    }

    public boolean isSuspended() {
        return activeActivity == null;
    }

    // TODO: Unfortunately there's some part of the app that use Activity context and need
    // TODO: to be reworked accordingly; see getContext usages. This will not leak activity context
    // TODO: as the reference is removed in the lifecycle callback onPause invokation
    // activities
    private Activity activeActivity;

    public Activity getActivity() {
        return activeActivity;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        application = this;
        activeActivity = null;
        registerActivityLifecycleCallbacks(this);
    }


    //////////////////////////////////////////////////////////////////////////////////
    //////////// Implementation of ActivityLifecycleCallbacks interface //////////////
    //////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onActivityCreated(Activity anActivity, Bundle aBundle) {
    }

    @Override
    public void onActivityStarted(Activity anActivity) {
    }

    @Override
    public void onActivityResumed(Activity anActivity) {
        activeActivity = anActivity;
        // TODO: I don't fully understand the usefulness of the DisabledActivity
        // TODO: maybe it's checking if the app has been modified?? But if it's been modified
        // TODO: the modifier could easily remove this check also, thus what's the usefulness
        // TODO: See ActivityUTILS.isAppSafe()
        if (!(activeActivity instanceof DisabledActivity)) {
            // lock wallet if 3 minutes passed
            long suspendedTime = BRSharedPrefs.getSuspendTime(anActivity);
            if (suspendedTime != 0 && (System.currentTimeMillis() - suspendedTime >= 180 * 1000)) {
                if (!BRKeyStore.getPinCode(activeActivity).isEmpty()) {
                    BRAnimator.startBreadActivity(activeActivity, true);
                }
            }
        }
        BRSharedPrefs.putSuspendTime(anActivity, 0);
    }

    @Override
    public void onActivityStopped(Activity anActivity) {
    }

    @Override
    public void onActivityDestroyed(Activity anActivity) {
    }

    @Override
    public void onActivityPaused(Activity anActivity) {
        activeActivity = null;
        BRSharedPrefs.putSuspendTime(anActivity, System.currentTimeMillis());
    }

    @Override
    public void onActivitySaveInstanceState(Activity anActivity, Bundle aBundle) {
    }
}