package com.iattend.app.core

/** Small constants object for the About screen (Settings) - mirrors Cashiro's Constants.Links
 * pattern. REPO URL IS A KNOWN PLACEHOLDER: points at the current repo for now; update once the
 * new public repo exists (current history has sensitive info and won't be pushed as-is). */
object AppLinks {
    const val GITHUB_PROFILE_URL = "https://github.com/shuaib-07"
    const val GITHUB_REPO_URL = "https://github.com/shuaib-07/iAttend"
    const val REPORT_BUG_URL = "$GITHUB_REPO_URL/issues/new"
    const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/shuaib-07/iAttend/releases/latest"
    const val RELEASES_PAGE_URL = "$GITHUB_REPO_URL/releases"
}
