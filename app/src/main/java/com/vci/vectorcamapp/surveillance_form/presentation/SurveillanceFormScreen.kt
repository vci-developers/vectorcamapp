package com.vci.vectorcamapp.surveillance_form.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun SurveillanceFormScreen(
    state: SurveillanceFormState, onAction: (SurveillanceFormAction) -> Unit, modifier: Modifier = Modifier
) {
    Button(
        onClick = { onAction(SurveillanceFormAction.StartImaging) },
        modifier = modifier
    ) {
        Text("Start Imaging")
    }
}

@PreviewLightDark
@Composable
fun SurveillanceFormScreenPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            SurveillanceFormScreen(
                state = SurveillanceFormState(), onAction = { }, modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
