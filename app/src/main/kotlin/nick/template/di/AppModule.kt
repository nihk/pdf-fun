package nick.template.di

import android.content.Context
import android.content.res.AssetManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    companion object {
        @Provides
        fun assetManager(@ApplicationContext context: Context): AssetManager {
            return context.assets
        }

        @Provides
        @CacheDir
        fun cacheDir(@ApplicationContext context: Context): File {
            return context.cacheDir
        }
    }
}
