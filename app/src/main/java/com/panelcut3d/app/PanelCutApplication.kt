package com.panelcut3d.app

import android.app.Application
import com.panelcut3d.app.ads.AdsManager
import com.panelcut3d.app.billing.BillingRepository
import com.panelcut3d.app.data.PanelCutDatabase
import com.panelcut3d.app.data.ProjectRepository
import com.panelcut3d.app.data.SettingsRepository

class PanelCutApplication : Application() {

    val database: PanelCutDatabase by lazy { PanelCutDatabase.build(this) }
    val projects: ProjectRepository by lazy { ProjectRepository(database.projectDao()) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val ads: AdsManager by lazy { AdsManager(this) }
    val billing: BillingRepository by lazy { BillingRepository(this, settings) }

    override fun onCreate() {
        super.onCreate()
        // Restore a previous Remove Ads entitlement as soon as the app starts.
        billing
    }
}
