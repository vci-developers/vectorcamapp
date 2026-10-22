package com.vci.vectorcamapp.intake.domain.strategy.collection_method
 
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions.CollectionMethodOption
import com.vci.vectorcamapp.navigation.Destination
import org.junit.Test
import java.util.UUID
 
class CollectionMethodWorkflowFactoryTest {
    private val factory = CollectionMethodWorkflowFactory()
    private val sessionId = UUID.randomUUID()
 
    @Test
    fun prokopackUsesCollectionBatches() {
        val workflow = factory.create(sessionId, CollectionMethodOption.PROKOPACK_ASPIRATOR.label)
        assertThat(workflow.postIntakeDestination)
            .isEqualTo(Destination.CollectionBatchList(sessionId.toString()))
    }
 
    @Test
    fun otherUsesDirectImaging() {
        val workflow = factory.create(sessionId, "${CollectionMethodOption.OTHER.label} hand collection")
        assertThat(workflow.postIntakeDestination).isEqualTo(Destination.Imaging(null))
    }
 
    @Test
    fun onlyKenyaCollectionMethodsAreAvailable() {
        assertThat(CollectionMethodOption.entries).containsExactly(
            CollectionMethodOption.PROKOPACK_ASPIRATOR,
            CollectionMethodOption.OTHER
        ).inOrder()
    }
}