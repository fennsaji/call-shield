package com.fenn.callshield.ui.screens.whitelist

import androidx.lifecycle.ViewModel
import com.fenn.callshield.data.local.entity.WhitelistEntry
import com.fenn.callshield.domain.repository.WhitelistRepository
import com.fenn.callshield.util.PhoneNumberHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class WhitelistViewModel @Inject constructor(
    private val repo: WhitelistRepository,
    private val hasher: PhoneNumberHasher,
) : ViewModel() {

    val entries: Flow<List<WhitelistEntry>> = repo.observeAll()

    suspend fun add(rawNumber: String) {
        val hash = hasher.hash(rawNumber) ?: return
        val label = rawNumber.takeLast(4).let { "****$it" }
        repo.add(hash, label)
    }

    suspend fun remove(numberHash: String) = repo.remove(numberHash)
}
