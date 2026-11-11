package com.aramalr.messages.ui.conversations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aramalr.messages.sms.Conversation
import com.aramalr.messages.sms.ConversationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ConversationListViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ConversationRepository(app)
    private val query = MutableStateFlow("")

    val conversations: StateFlow<List<Conversation>> =
        combine(repo.observeConversations(), query) { list, q ->
            if (q.isBlank()) {
                list
            } else {
                val needle = q.trim().lowercase()
                list.filter {
                    it.address.lowercase().contains(needle) ||
                        (it.displayName?.lowercase()?.contains(needle) == true) ||
                        it.snippet.lowercase().contains(needle)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hasPermission: Boolean get() = repo.hasReadPermission()

    fun setQuery(value: String) {
        query.value = value
    }

    fun queryValue(): String = query.value
}
