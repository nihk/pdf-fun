package nick.template.di

import android.content.ContentResolver
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    companion object {
        @Provides
        @IoContext
        fun ioContext(): CoroutineContext = Dispatchers.IO

        @Provides
        fun contentResolver(@ApplicationContext context: Context): ContentResolver = context.contentResolver
    }
}
