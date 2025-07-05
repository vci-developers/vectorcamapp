package com.vci.vectorcamapp.core.presentation.util.error

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val DEFAULT_DEBOUNCE_DURATION = 300.milliseconds

@OptIn(FlowPreview::class)
fun <T> CoroutineScope.collectEmptyStateError(
    flow: Flow<List<T>>,
    debounceTime: Duration = DEFAULT_DEBOUNCE_DURATION,
    emitError: () -> Unit
) {
    launch {
        flow
            .debounce(debounceTime)
            .map { it.isEmpty() }
            .distinctUntilChanged()
            .filter { it }
            .collect { emitError() }
    }
}
