package com.vci.vectorcamapp.core.presentation.components.camera
/** Prefer a valid barcode from the frame; use recognized text only if none is valid. */
internal fun selectScanValue(
    barcodes: List<String>,
    recognizedText: String?,
    transformBarcode: (String) -> String?,
    transformText: ((String) -> String?)?,
): String? = barcodes.firstNotNullOfOrNull(transformBarcode)
    ?: recognizedText?.let { transformText?.invoke(it) }