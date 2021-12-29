package nick.template.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import dagger.hilt.android.AndroidEntryPoint
import nick.template.R
import nick.template.di.MainEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.main_activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        val entryPoint = entryPoint<MainEntryPoint>()
        supportFragmentManager.fragmentFactory = entryPoint.fragmentFactory
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace<MainFragment>(R.id.fragment_container)
            }
        }
    }
}
