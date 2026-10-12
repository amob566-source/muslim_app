package com.universalazan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.universalazan.app.ui.navigation.AppRoot
import com.universalazan.app.ui.theme.UniversalAzanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniversalAzanTheme {
                AppRoot()
            }
        }
    }
}
