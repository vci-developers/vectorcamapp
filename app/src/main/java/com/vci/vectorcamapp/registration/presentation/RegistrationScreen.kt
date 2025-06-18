
package com.vci.vectorcamapp.registration.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.animation.presentation.LoadingAnimation

@Composable
fun RegistrationScreen(
    state: RegistrationState,
    onAction: (RegistrationAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->

        Box(modifier = Modifier.padding(innerPadding)) {
            if (state.isLoading) {
                LoadingAnimation(text = "Loading programs…")
            } else {
                state.error?.let {
                    Text("Error: $it", modifier = Modifier.padding(16.dp))
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.programs) { program ->
                        ProgramItem(program) {
                            onAction(RegistrationAction.RegisterProgram(program.id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramItem(program: Program, onSelect: () -> Unit) {
    Button(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(program.name)
    }
}