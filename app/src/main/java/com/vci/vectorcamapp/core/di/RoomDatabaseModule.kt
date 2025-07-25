package com.vci.vectorcamapp.core.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.vci.vectorcamapp.core.data.room.DbSeedStatus
import com.vci.vectorcamapp.core.data.room.SeedDataContainer
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.data.room.VectorCamDatabase
import com.vci.vectorcamapp.core.data.room.dao.InferenceResultDao
import com.vci.vectorcamapp.core.data.room.dao.ProgramDao
import com.vci.vectorcamapp.core.data.room.dao.SessionDao
import com.vci.vectorcamapp.core.data.room.dao.SiteDao
import com.vci.vectorcamapp.core.data.room.dao.SpecimenDao
import com.vci.vectorcamapp.core.data.room.dao.SurveillanceFormDao
import com.vci.vectorcamapp.core.data.room.migrations.ALL_MIGRATIONS
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

private const val DB_NAME = "vectorcam.db"
private const val SEED_DATA_FILENAME = "seed_data.json"

@Module
@InstallIn(SingletonComponent::class)
object RoomDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        programDaoProvider: Provider<ProgramDao>,
        siteDaoProvider: Provider<SiteDao>,
    ): VectorCamDatabase {
        return Room.databaseBuilder(
            context,
            VectorCamDatabase::class.java,
            DB_NAME,
        ).addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val json = context.assets.open(SEED_DATA_FILENAME)
                            .bufferedReader()
                            .use { it.readText() }
                        val seed = Gson().fromJson(json, SeedDataContainer::class.java)

                        programDaoProvider.get().insertAll(seed.programs)
                        siteDaoProvider.get().insertAll(seed.sites)
                        Log.i("RoomCallback", "Seeded ${seed.programs.size} programs, ${seed.sites.size} sites")
                    } catch (e: Exception) {
                        Log.e("RoomCallback", "Error seeding DB", e)
                    } finally {
                        if (!DbSeedStatus.seeded.isCompleted) {
                            DbSeedStatus.seeded.complete(Unit)
                        }
                    }
                }
            }
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Log.d("RoomCallback", "onOpen called")
                if (!DbSeedStatus.seeded.isCompleted) {
                    DbSeedStatus.seeded.complete(Unit)
                }
            }
        }).addMigrations(*ALL_MIGRATIONS).build()
    }

    @Provides
    fun provideTransactionHelper(db: VectorCamDatabase): TransactionHelper = TransactionHelper(db)

    @Provides
    fun provideSessionDao(db: VectorCamDatabase): SessionDao = db.sessionDao

    @Provides
    fun provideSpecimenDao(db: VectorCamDatabase): SpecimenDao = db.specimenDao

    @Provides
    fun provideInferenceResultDao(db: VectorCamDatabase): InferenceResultDao = db.inferenceResultDao

    @Provides
    fun provideSurveillanceFormDao(db: VectorCamDatabase): SurveillanceFormDao = db.surveillanceFormDao

    @Provides
    fun provideProgramDao(db: VectorCamDatabase): ProgramDao = db.programDao

    @Provides
    fun provideSiteDao(db: VectorCamDatabase): SiteDao = db.siteDao
}
