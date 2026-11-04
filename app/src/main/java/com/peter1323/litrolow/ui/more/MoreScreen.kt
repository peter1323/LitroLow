package com.peter1323.litrolow.ui.more

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.peter1323.litrolow.R
import com.peter1323.litrolow.ui.components.BrandHeader

private const val REPOSITORY_URL = "https://github.com/peter1323/LitroLow"
private const val UPDATES_URL = "$REPOSITORY_URL/releases/latest"

/** Pestaña «Más»: información de la app, su creador y los enlaces a GitHub. No tiene estado, así que no necesita ViewModel. */
@Composable
fun MoreScreen() {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BrandHeader {
            Text(stringResource(R.string.about_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.about_version, appVersion(context)))
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AboutCard(R.string.about_creator, stringResource(R.string.about_creator_name))
            AboutCard(R.string.about_updates, stringResource(R.string.about_updates_hint)) { openLink(context, UPDATES_URL) }
            AboutCard(R.string.about_github, REPOSITORY_URL) { openLink(context, REPOSITORY_URL) }
            AboutCard(R.string.about_data, stringResource(R.string.about_data_sources))
            Text(
                stringResource(R.string.about_copyright),
                Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Tarjeta con un título y su texto; si recibe [onClick], se puede pulsar. */
@Composable
private fun AboutCard(@StringRes title: Int, text: String, onClick: (() -> Unit)? = null) {
    val content = @Composable {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (onClick != null) Card(onClick = onClick) { content() } else Card { content() }
}

/** Lee la versión instalada, para no tener que actualizar este texto en cada versión. */
private fun appVersion(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

/** Abre un enlace en el navegador; avisa en vez de cerrarse si el móvil no tiene ninguno. */
private fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_browser, Toast.LENGTH_LONG).show()
    }
}
