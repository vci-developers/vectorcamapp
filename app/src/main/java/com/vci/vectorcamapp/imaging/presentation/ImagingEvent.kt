package com.vci.vectorcamapp.imaging.presentation

import com.vci.vectorcamapp.core.domain.util.imaging.ImagingError

sealed interface ImagingEvent {
    data class DisplayImagingError(val error: ImagingError): ImagingEvent
}
