package com.bssm.reunionmanager

import android.app.Application

class ReunionManagerApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
