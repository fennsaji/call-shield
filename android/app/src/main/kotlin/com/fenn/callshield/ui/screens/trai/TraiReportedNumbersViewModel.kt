package com.fenn.callshield.ui.screens.trai

import androidx.lifecycle.ViewModel
import com.fenn.callshield.data.local.dao.TraiReportDao
import com.fenn.callshield.data.local.entity.TraiReportEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class TraiReportedNumbersViewModel @Inject constructor(
    traiReportDao: TraiReportDao,
) : ViewModel() {

    val reports: Flow<List<TraiReportEntry>> = traiReportDao.observeAll()
}
