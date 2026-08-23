package com.iattend.app.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iattend.app.core.data.db.Subject

/** Horizontal single-select subject picker, shared by the slot editor and Extra Classes forms. */
@Composable
fun SubjectChipRow(
    subjects: List<Subject>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(subjects, key = { it.id }) { subject ->
            FilterChip(selected = subject.id == selectedId, onClick = { onSelect(subject.id) }, label = { Text(subject.name) })
        }
    }
}
