package com.expensemind.ai.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expensemind.ai.agent.AgentController
import com.expensemind.ai.data.AppDatabase
import com.expensemind.ai.parser.ImportPipeline
import com.expensemind.ai.parser.ImportResult
import com.expensemind.ai.tools.FinanceTools
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val tools = FinanceTools(db)
    private val agent = AgentController(tools)
    private val importPipeline = ImportPipeline(application)

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage(
                "Hi! Import a bank statement PDF using the + button, then ask me things like " +
                        "\"how much did I spend this month\" or \"category breakdown\".",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        _messages.update { it + ChatMessage(text, isUser = true) }
        _isProcessing.value = true

        viewModelScope.launch {
            val reply = agent.handleMessage(text)
            _messages.update { it + ChatMessage(reply, isUser = false) }
            _isProcessing.value = false
        }
    }

    fun importStatement(uri: Uri, password: String?) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = importPipeline.importStatement(uri, password)
            val reply = when (result) {
                is ImportResult.Success ->
                    "Imported ${result.count} transactions from ${result.bank}. Ask me anything about them."
                is ImportResult.UnrecognizedBank -> result.message
                is ImportResult.Failure ->
                    "Import failed: ${result.error.message}. If the PDF is password-protected, " +
                            "make sure the right password was entered."
            }
            _messages.update { it + ChatMessage(reply, isUser = false) }
            _isProcessing.value = false
        }
    }
}
