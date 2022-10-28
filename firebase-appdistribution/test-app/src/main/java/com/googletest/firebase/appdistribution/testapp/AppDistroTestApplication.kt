package com.googletest.firebase.appdistribution.testapp

import android.app.Application

class AppDistroTestApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Perform any required trigger initialization here
        CustomNotificationFeedbackTrigger.initialize(this);

        // Default feedback triggers can optionally be enabled application-wide here
//        ShakeDetectionFeedbackTrigger.enable(this)
//        ScreenshotDetectionFeedbackTrigger.enable()
//        NotificationFeedbackTrigger.enable()
    }
}
