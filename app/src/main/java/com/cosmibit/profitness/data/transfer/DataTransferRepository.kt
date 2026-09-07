package com.cosmibit.profitness.data.transfer

import android.net.Uri

interface DataTransferRepository {
    suspend fun exportTo(userId: String, destination: Uri): Result<DataTransferSummary>
    suspend fun importFrom(userId: String, source: Uri): Result<DataTransferSummary>
}

data class DataTransferSummary(
    val programs: Int,
    val workouts: Int,
    val setRecords: Int,
    val weightRecords: Int
) {
    val totalRecords: Int get() = programs + workouts + setRecords + weightRecords
}
