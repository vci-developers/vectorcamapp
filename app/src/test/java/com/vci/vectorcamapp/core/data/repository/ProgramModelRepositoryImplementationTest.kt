package com.vci.vectorcamapp.core.data.repository

import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.dto.program_model.GetProgramModelsResponseDto
import com.vci.vectorcamapp.core.data.program_model.LocalProgramModelStore
import com.vci.vectorcamapp.core.domain.ProgramModelChangeNotifier
import com.vci.vectorcamapp.core.domain.model.ProgramModel
import com.vci.vectorcamapp.core.domain.network.api.ProgramDataSource
import com.vci.vectorcamapp.core.domain.network.api.ProgramModelDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ProgramModelRepositoryImplementationTest {

    @Test
    fun sync_emptyModelList_clearsDownloadedModels() = runTest {
        val store = mockk<LocalProgramModelStore>(relaxed = true)
        val notifier = mockk<ProgramModelChangeNotifier>(relaxUnitFun = true)
        val repository = repository(
            models = Result.Success(GetProgramModelsResponseDto(emptyList())),
            store = store,
            notifier = notifier,
        )

        val result = repository.syncConfiguredModels(PROGRAM_ID)

        assertThat(result).isEqualTo(Result.Success(emptyList<ProgramModel>()))
        coVerify { store.clear(PROGRAM_ID) }
        verify { notifier.notifyChanged() }
    }

    @Test
    fun sync_modelsListNotFound_clearsDownloadedModels() = runTest {
        val store = mockk<LocalProgramModelStore>(relaxed = true)
        val repository = repository(
            models = Result.Error(NetworkError.NOT_FOUND),
            store = store,
            notifier = mockk(relaxUnitFun = true),
        )

        val result = repository.syncConfiguredModels(PROGRAM_ID)

        assertThat(result).isEqualTo(Result.Success(emptyList<ProgramModel>()))
        coVerify { store.clear(PROGRAM_ID) }
    }

    @Test
    fun sync_listFailure_keepsDownloadedModels() = runTest {
        val store = mockk<LocalProgramModelStore>(relaxed = true)
        val notifier = mockk<ProgramModelChangeNotifier>(relaxUnitFun = true)
        val repository = repository(
            models = Result.Error(NetworkError.NO_INTERNET),
            store = store,
            notifier = notifier,
        )

        val result = repository.syncConfiguredModels(PROGRAM_ID)

        assertThat(result).isEqualTo(Result.Error(NetworkError.NO_INTERNET))
        coVerify(exactly = 0) { store.clear(any()) }
        verify { notifier.notifyChanged() }
    }

    private fun repository(
        models: Result<GetProgramModelsResponseDto, NetworkError>,
        store: LocalProgramModelStore,
        notifier: ProgramModelChangeNotifier,
    ): ProgramModelRepositoryImplementation {
        val programDataSource = mockk<ProgramDataSource>()
        val modelDataSource = mockk<ProgramModelDataSource>()
        coEvery { programDataSource.getProgramById(PROGRAM_ID) } returns Result.Error(NetworkError.NOT_FOUND)
        coEvery { modelDataSource.getModels(PROGRAM_ID) } returns models
        return ProgramModelRepositoryImplementation(
            programDataSource = programDataSource,
            programModelDataSource = modelDataSource,
            localProgramModelStore = store,
            programModelChangeNotifier = notifier,
        )
    }

    private companion object {
        const val PROGRAM_ID = 7
    }
}
