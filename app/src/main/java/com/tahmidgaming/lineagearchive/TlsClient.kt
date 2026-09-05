package com.tahmidgaming.lineagearchive

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.decodeCertificatePem

/**
 * HTTPS client with the normal Android trust store plus current Let's Encrypt roots.
 * Android 6 devices often lack ISRG Root X1/X2, which causes the modern LineageOS
 * endpoints to fail with "Trust anchor for certification path not found".
 *
 * We deliberately keep the platform roots as well, so this does not become an
 * all-trusting client and non-Let's-Encrypt HTTPS endpoints continue to work normally.
 */
object TlsClient {
    @Volatile
    private var cached: OkHttpClient? = null

    fun get(context: Context): OkHttpClient {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: build(context.applicationContext).also { cached = it }
        }
    }

    private fun build(context: Context): OkHttpClient {
        val x1 = context.resources.openRawResource(R.raw.isrg_root_x1)
            .bufferedReader()
            .use { it.readText() }
            .decodeCertificatePem()
        val x2 = context.resources.openRawResource(R.raw.isrg_root_x2)
            .bufferedReader()
            .use { it.readText() }
            .decodeCertificatePem()

        val certificates = HandshakeCertificates.Builder()
            .addPlatformTrustedCertificates()
            .addTrustedCertificate(x1)
            .addTrustedCertificate(x2)
            .build()

        return OkHttpClient.Builder()
            .sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
            .build()
    }
}
