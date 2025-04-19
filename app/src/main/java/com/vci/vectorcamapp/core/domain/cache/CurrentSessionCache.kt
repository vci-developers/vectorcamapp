package com.vci.vectorcamapp.core.domain.cache

import com.vci.vectorcamapp.core.domain.model.Session

interface CurrentSessionCache {
    suspend fun saveSession(session: Session)
    suspend fun getSession() : Session?
    suspend fun clearSession()
}
