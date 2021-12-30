package nick.template.di

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import nick.template.ui.AndroidScreenDimensions
import nick.template.ui.ScreenDimensions

@Module
@InstallIn(SingletonComponent::class)
interface AppModule {

    companion object {
        @Provides
        @IoContext
        fun ioContext(): CoroutineContext = Dispatchers.IO

        @Provides
        fun contentResolver(@ApplicationContext context: Context): ContentResolver = context.contentResolver

        @Provides
        fun resources(@ApplicationContext context: Context): Resources = context.resources
    }

    @Binds
    fun screenDimensions(screenDimensions: AndroidScreenDimensions): ScreenDimensions
}
