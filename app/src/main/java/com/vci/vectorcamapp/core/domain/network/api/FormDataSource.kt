package com.vci.vectorcamapp.core.domain.network.api

import com.vci.vectorcamapp.core.data.dto.form.FormDto
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError

interface FormDataSource {
    suspend fun getCurrentFormByProgramId(programId: Int) : Result<FormDto, NetworkError>
}
