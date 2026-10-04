package com.guesswhere.app

import android.app.Application
import android.content.Context
import com.guesswhere.app.data.CommonsApi
import com.guesswhere.app.data.CommonsRepository
import com.guesswhere.app.data.LocalStore
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import coil3.request.crossfade
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Manual DI: one container per process, plain constructors. Swap for Hilt if
 * the app grows past a handful of collaborators.
 */
class GuessWhereApp : Application() {

    lateinit var container: Container
        private set

    override fun onCreate() {
        super.onCreate()
        container = Container(this)
        configureOsmdroid(this)
        configureImageLoader(container.okHttp)
    }
}

/**
 * Coil owns no network by default; without this it builds its own OkHttpClient
 * with a generic/empty User-Agent, which the Wikimedia CDN blocks with 403.
 */
private fun configureImageLoader(client: OkHttpClient) {
    coil3.SingletonImageLoader.setSafe(
        coil3.SingletonImageLoader.Factory { context ->
            coil3.ImageLoader.Builder(context)
                .components {
                    add(
                        coil3.network.okhttp.OkHttpNetworkFetcherFactory(
                            callFactory = { client },
                        ),
                    )
                }
                .crossfade(true)
                .build()
        },
    )
}

class Container(context: Context) {

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val localStore = LocalStore(context, json)

    val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            // Wikimedia etiquette/policy: identify the app and a contact.
            // REQUIRED: upload.wikimedia.org / thumb.wikimedia.org 403-block
            // generic library User-Agents like "okhttp/5.5.0".
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", NETWORK_UA)
                    .build(),
            )
        }
        .build()

    private val commonsApi: CommonsApi = Retrofit.Builder()
        .baseUrl("https://commons.wikimedia.org/")
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(CommonsApi::class.java)

    val repository = CommonsRepository(commonsApi, localStore)
}

/** Tile cache stays app-private; the UA identifies us to tile servers too. */
private fun configureOsmdroid(context: Context) {
    val config = org.osmdroid.config.Configuration.getInstance()
    val base = File(context.cacheDir, "osmdroid")
    config.osmdroidBasePath = base
    config.osmdroidTileCache = File(base, "tiles")
    config.userAgentValue = NETWORK_UA
}

/**
 * Set a REAL contact before shipping to the Play Store; Wikimedia and OSM
 * both require an identifiable User-Agent and may throttle generic ones.
 */
private const val NETWORK_UA =
    "GuessWhere! Android/1.0 (personal project; contact: your-email@example.invalid)"