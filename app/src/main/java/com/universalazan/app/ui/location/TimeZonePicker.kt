package com.universalazan.app.ui.location

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.universalazan.app.R
import com.universalazan.app.domain.location.timeZoneChoices
import com.universalazan.app.domain.location.utcOffsetLabel
import com.universalazan.app.ui.common.ScreenHeading
import java.time.ZoneId

/** Full-screen, searchable list of time zones (works offline). */
@Composable
fun TimeZonePicker(
    selectedId: String?,
    onPick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var query by rememberSaveable { mutableStateOf("") }
    val allZones = remember { timeZoneChoices() }
    val filtered = remember(query) {
        val q = query.trim().replace(' ', '_')
        if (q.isEmpty()) allZones else allZones.filter { it.contains(q, ignoreCase = true) }
    }
    val phoneZone = remember { ZoneId.systemDefault().id }

    LazyColumn(modifier = modifier.fillMaxSize().testTag(TIME_ZONE_LIST_TAG)) {
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .padding(16.dp),
            ) {
                ScreenHeading(
                    title = stringResource(R.string.time_zone_picker_title),
                    onBack = onBack,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.time_zone_search_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag(TIME_ZONE_SEARCH_TAG),
                )
            }
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.time_zone_phone_option, phoneZone)) },
                modifier = Modifier.clickable { onPick(phoneZone) },
            )
            HorizontalDivider()
        }
        items(filtered, key = { it }) { id ->
            val zone = remember(id) { ZoneId.of(id) }
            ListItem(
                headlineContent = { Text(id.replace('_', ' ')) },
                supportingContent = { Text(utcOffsetLabel(zone)) },
                trailingContent = if (id == selectedId) {
                    { Text(stringResource(R.string.time_zone_selected), color = MaterialTheme.colorScheme.primary) }
                } else {
                    null
                },
                modifier = Modifier.clickable { onPick(id) },
            )
        }
    }
}

const val TIME_ZONE_LIST_TAG = "time_zone_list"
const val TIME_ZONE_SEARCH_TAG = "time_zone_search"
