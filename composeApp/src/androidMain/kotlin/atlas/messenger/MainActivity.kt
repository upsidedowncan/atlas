package atlas.messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import atlas.messenger.crypto.initEncryptionService
import atlas.messenger.session.initSessionStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initSessionStore(this)
        initEncryptionService(this)
        setContent {
            App()
        }
    }
}