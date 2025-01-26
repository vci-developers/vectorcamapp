package com.vci.vectorcamapp.core.data.network

/* TODO: After getting a base URL from the API, do the following:
*   1. Add BASE_URL to the Gradle BuildConfig for the release and debug build types. Make sure BASE_URL has a trailing /.
*   2. Under buildFeatures, set buildConfig to true
*   3. Replace BASE_URL with BuildConfig.BASE_URL
*/
const val BASE_URL = "" // Placeholder Variable

fun constructUrl(url: String): String {
    return when {
        url.contains(BASE_URL) -> url
        url.startsWith("/") -> BASE_URL + url.drop(1)
        else -> BASE_URL + url
    }
}
