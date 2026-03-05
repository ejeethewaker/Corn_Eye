// CornEye Application
// Custom Application class for app-wide Firebase and SDK initialization.
package com.corneye.app
import coil.ImageLoader
import coil.ImageLoaderFactory
import okhttp3.OkHttpClient

class CornEyeApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header(
                                "User-Agent",
                                "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 " +
                                "CornEye/1.0 (educational; corn disease identification app)"
                            )
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
