package com.fenn.callshield.ui.screens.blocklist

import androidx.lifecycle.ViewModel
import com.fenn.callshield.data.local.entity.BlocklistEntry
import com.fenn.callshield.domain.repository.BlocklistRepository
import com.fenn.callshield.util.PhoneNumberHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class BlocklistViewModel @Inject constructor(
    private val repo: BlocklistRepository,
    private val hasher: PhoneNumberHasher,
) : ViewModel() {

    val entries: Flow<List<BlocklistEntry>> = repo.observeAll()

    suspend fun add(rawNumber: String) {
        val hash = hasher.hash(rawNumber) ?: return
        val label = rawNumber.takeLast(4).let { "****$it" }
        repo.add(hash, label)
    }

    suspend fun remove(numberHash: String) = repo.remove(numberHash)
}
