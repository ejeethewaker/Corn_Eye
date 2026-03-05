// Main Activity
// Entry point of the Android app; sets up Jetpack Compose and the navigation host.
package com.corneye.app
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.corneye.app.navigation.CornEyeNavGraph
import com.corneye.app.ui.theme.CornEyeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = 0x80000000.toInt()

        setContent {
            CornEyeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    CornEyeNavGraph(navController = navController)
                }
            }
        }
    }
}
