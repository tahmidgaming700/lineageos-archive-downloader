package com.tahmidgaming.lineagearchive

import android.content.Context
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import org.conscrypt.Conscrypt
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * HTTPS client for Android 6+.
 *
 * Android versions below 7.1.1 do not have ISRG Root X1 in their system trust
 * store. Modern LineageOS endpoints use Let's Encrypt, so the app must supply
 * the current ISRG roots itself. A bundled Conscrypt provider is also used so
 * old Android TLS implementations can negotiate current HTTPS safely.
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
        installConscrypt()

        val platform = platformTrustManager()
        val bundled = bundledTrustManager(context)
        val trustManager = CompositeTrustManager(platform, bundled)

        val sslContext = SSLContext.getInstance("TLS", "Conscrypt")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())

        // TLS 1.2 is available on Android 6 and is required by current HTTPS
        // servers. Conscrypt supplies a modern implementation underneath it.
        val tls12 = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2)
            .build()

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .connectionSpecs(listOf(tls12))
            .build()
    }

    private fun installConscrypt() {
        if (Security.getProvider("Conscrypt") == null) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }
    }

    private fun platformTrustManager(): X509TrustManager {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    private fun bundledTrustManager(context: Context): X509TrustManager {
        val store = KeyStore.getInstance(KeyStore.getDefaultType())
        store.load(null, null)
        store.setCertificateEntry("isrg-root-x1", readCertificate(context, R.raw.isrg_root_x1))
        store.setCertificateEntry("isrg-root-x2", readCertificate(context, R.raw.isrg_root_x2))

        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(store)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    private fun readCertificate(context: Context, resourceId: Int): X509Certificate {
        val pem = context.resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
        val base64 = pem
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replace("\\s".toRegex(), "")
        val der = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
        return CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(der)) as X509Certificate
    }

    private class CompositeTrustManager(
        private val platform: X509TrustManager,
        private val bundled: X509TrustManager
    ) : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            platform.checkClientTrusted(chain, authType)
        }

        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            try {
                platform.checkServerTrusted(chain, authType)
            } catch (platformFailure: java.security.cert.CertificateException) {
                bundled.checkServerTrusted(chain, authType)
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> =
            (platform.acceptedIssuers.asList() + bundled.acceptedIssuers.asList()).toTypedArray()
    }
}
