package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.example.firebase.configureAppCheck
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.OdiaSikshyaSathiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent BEFORE any Compose UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            OdiaSikshyaSathiTheme {
                Surface {
                    AppNavigation()
                }
            }
        }
    }
}
