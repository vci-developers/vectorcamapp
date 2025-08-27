package com.vci.vectorcamapp.core.data.upload.image.util

import android.util.Log
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.tus.java.client.TusClient
import io.tus.java.client.TusUpload
import io.tus.java.client.ProtocolException as TusProtocolException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import com.vci.vectorcamapp.core.domain.util.Result as DomainResult

/**
 * Utility functions for verifying upload completion and handling edge cases.
 */
object UploadVerificationUtils {
    
    private const val TAG = "UploadVerificationUtils"
    
    /**
     * Verifies if an upload is actually complete by checking with the server.
     * This is crucial for handling HTTP 409 conflicts properly.
     * 
     * @param tusClient The TUS client to use for verification
     * @param uploadUrl The URL of the upload to verify
     * @param expectedSize The expected size of the complete upload
     * @return DomainResult indicating if upload is verified complete
     */
    suspend fun verifyUploadCompletion(
        tusClient: TusClient,
        uploadUrl: URL,
        expectedSize: Long
    ): DomainResult<Boolean, NetworkError> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Verifying upload completion for URL: $uploadUrl")
            
            // Use TUS HEAD request to check upload status
            val uploader = tusClient.resumeUpload(uploadUrl)
            val currentOffset = uploader.offset
            
            Log.d(TAG, "Upload verification - Current offset: $currentOffset, Expected size: $expectedSize")
            
            when {
                currentOffset == expectedSize -> {
                    Log.d(TAG, "Upload verified as complete")
                    DomainResult.Success(true)
                }
                currentOffset < expectedSize -> {
                    Log.w(TAG, "Upload incomplete - offset: $currentOffset < expected: $expectedSize")
                    DomainResult.Success(false)
                }
                else -> {
                    Log.w(TAG, "Upload size mismatch - offset: $currentOffset > expected: $expectedSize")
                    DomainResult.Error(NetworkError.TUS_PERMANENT_ERROR)
                }
            }
        } catch (e: TusProtocolException) {
            Log.w(TAG, "TUS protocol error during verification", e)
            if (e.shouldRetry()) {
                DomainResult.Error(NetworkError.TUS_TRANSIENT_ERROR)
            } else {
                DomainResult.Error(NetworkError.TUS_PERMANENT_ERROR)
            }
        } catch (e: IOException) {
            Log.w(TAG, "IO error during verification", e)
            DomainResult.Error(NetworkError.TUS_TRANSIENT_ERROR)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during verification", e)
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }
    }
    
    /**
     * Handles upload conflicts by verifying if the upload is actually complete.
     * This prevents premature marking of uploads as complete.
     * 
     * @param tusClient The TUS client to use for verification
     * @param upload The TusUpload object containing upload metadata
     * @param locationUrl The location URL from the conflict response
     * @return DomainResult indicating the conflict resolution result
     */
    suspend fun handleUploadConflict(
        tusClient: TusClient,
        upload: TusUpload,
        locationUrl: String
    ): DomainResult<ConflictResolution, NetworkError> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.i(TAG, "Handling upload conflict for location: $locationUrl")
            
            val uploadUrl = URL(locationUrl)
            val verificationResult = verifyUploadCompletion(tusClient, uploadUrl, upload.size)
            
            when (verificationResult) {
                is DomainResult.Success -> {
                    if (verificationResult.data) {
                        Log.i(TAG, "Conflict resolved: Upload is actually complete")
                        DomainResult.Success(ConflictResolution.UPLOAD_COMPLETE)
                    } else {
                        Log.i(TAG, "Conflict resolved: Upload is incomplete, can resume")
                        DomainResult.Success(ConflictResolution.CAN_RESUME)
                    }
                }
                is DomainResult.Error -> {
                    Log.w(TAG, "Error during conflict verification: ${verificationResult.error}")
                    verificationResult
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during conflict handling", e)
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }
    }
    
    /**
     * Represents the result of resolving an upload conflict.
     */
    enum class ConflictResolution {
        UPLOAD_COMPLETE,    // Upload is verified complete
        CAN_RESUME          // Upload is incomplete but can be resumed
    }
}