package com.example.androidapp.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.androidapp.MainActivity
import com.example.domain.entity.ListElementEntity
import com.example.domain.usecase.ElementByIdUseCase
import com.example.domain.usecase.ListUseCase
import org.koin.core.context.GlobalContext.get

class Widget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state = currentState<Preferences>()
            val listUseCase = remember { get().get<ListUseCase>() }
            var elements by remember { mutableStateOf<List<ListElementEntity>>(emptyList()) }
            var elementIdState by remember { mutableStateOf<Long?>(null) }
            val elementId = state[elementIdKey]
            val title = state[titleKey]
            val date = state[subtitleKey]
            val imageUrl = state[imageUrlKey]
            var image by remember(imageUrl) { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(imageUrl) {
                if (imageUrl != null) {
                    image = context.getImage(imageUrl)
                }
            }
            LaunchedEffect(elementIdState) {
                updateAppWidgetState(context, id) { prefs ->
                    val elId = elementIdState
                    if (elId != null) {
                        prefs[elementIdKey] = elId
                    }
                }
                update(context, id)
            }
            LaunchedEffect(state) {
                if (title == null && elementId != null) {
                    val useCase: ElementByIdUseCase = get().get()
                    runCatching { useCase.execute(elementId) }
                        .onSuccess { result ->
                            updateAppWidgetState(context, id) { prefs ->
                                prefs[titleKey] = result.title
                                prefs[subtitleKey] = result.subtitle.orEmpty()
                                prefs[imageUrlKey] = result.image.orEmpty()
                            }
                            update(context, id)
                        }
                }
            }
            LaunchedEffect(elements) {
                elements = listUseCase.execute(Unit)
            }
            if (elementId == null) {
                LazyColumn(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.fillMaxSize()
                        .background(ColorProvider(color = Color(0xffffffff)))
                        .clickable(actionStartService(Intent()))
                ) {
                    elements.forEach {
                        item {
                            Text(
                                modifier = GlanceModifier.fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable {
                                        elementIdState = it.id
                                    },
                                text = it.title
                            )
                        }
                    }
                }
            } else if (title == null) {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                        .background(ColorProvider(color = Color(0xffffffff)))
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                        .background(ColorProvider(color = Color(0xffffffff)))
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (image != null) {
                        Image(
                            modifier = GlanceModifier.fillMaxWidth(),
                            provider = ImageProvider(image!!),
                            contentScale = ContentScale.Fit,
                            contentDescription = ""
                        )
                    }
                    Text(
                        title
                    )
                    Text(date.orEmpty())
                }
            }
        }
    }
    private suspend fun Context.getImage(url: String, force: Boolean = false): Bitmap? {
        val request = ImageRequest.Builder(this).data(url).apply {
            if (force) {
                memoryCachePolicy(CachePolicy.DISABLED)
                diskCachePolicy(CachePolicy.DISABLED)
            }
        }.build()
        return when (val result = imageLoader.execute(request)) {
            is ErrorResult -> throw result.throwable
            is SuccessResult -> result.drawable.toBitmapOrNull()
        }
    }
    companion object {
        val elementIdKey = longPreferencesKey("element_id")
        val titleKey = stringPreferencesKey("title_key")
        val subtitleKey = stringPreferencesKey("subtitle_key")
        val imageUrlKey = stringPreferencesKey("image_url_key")
    }
}