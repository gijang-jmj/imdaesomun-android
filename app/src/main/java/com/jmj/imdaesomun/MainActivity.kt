package com.jmj.imdaesomun

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.jmj.imdaesomun.presentation.theme.ImdaesomunTheme
import com.jmj.imdaesomun.presentation.screen.home.HomeScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // push notification 권한 요청
        askNotificationPermission()

        // FCM 토큰을 가져오기
        lifecycleScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                val msg = "FCM 토큰: $token"
                Log.d("getToken", msg)
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.w("getToken", "Fetching FCM registration token failed", e)
            }
        }

        // Edge-to-edge 모드 활성화
        enableEdgeToEdge()

        // 상태바 아이콘을 어두운 색으로 설정
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        // 홈 화면을 설정
        setContent {
            ImdaesomunTheme {
                HomeScreen()
            }
        }
    }
}
