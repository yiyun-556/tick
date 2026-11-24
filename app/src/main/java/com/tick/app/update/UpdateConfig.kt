package com.tick.app.update

object UpdateConfig {
    // Filled in before the public release build.
    const val OWNER = "yiyun-556"
    const val REPO = "tick"
    const val ASSET = "update.json"

    val isConfigured: Boolean get() = OWNER.isNotBlank() && REPO.isNotBlank()
    val updateJsonUrl: String
        get() = "https://github.com/$OWNER/$REPO/releases/latest/download/$ASSET"
}
