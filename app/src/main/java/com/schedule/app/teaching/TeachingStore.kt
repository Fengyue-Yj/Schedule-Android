package com.schedule.app.teaching

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class TeachingStore private constructor(private val context: Context) {
    private val session = TeachingSession()
    private val gson = Gson()
    private val snapshotFile = File(context.filesDir, "teaching_snapshot.json")
    
    private val _snapshot = MutableStateFlow(TeachingSnapshot())
    val snapshot: StateFlow<TeachingSnapshot> = _snapshot.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _isSignedIn = MutableStateFlow(session.isLoggedIn())
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    init {
        loadSnapshot()
        _isSignedIn.value = session.isLoggedIn()
    }

    private fun loadSnapshot() {
        if (snapshotFile.exists()) {
            try {
                val json = snapshotFile.readText()
                _snapshot.value = gson.fromJson(json, TeachingSnapshot::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveSnapshot(snap: TeachingSnapshot) {
        _snapshot.value = snap
        try {
            snapshotFile.writeText(gson.toJson(snap))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        if (!session.isLoggedIn()) {
            _isSignedIn.value = false
            return@withContext
        }
        _isRefreshing.value = true
        _message.value = "Fetching courses..."
        try {
            val homeHtml = session.get(TeachingURLs.home)
            val courses = TeachingParser.parseCourses(homeHtml)
            val items = mutableListOf<TeachingItem>()
            
            for (course in courses) {
                _message.value = "Fetching ${course.displayTitle}..."
                val courseHtml = session.get(TeachingURLs.course(course.id))
                items.addAll(TeachingParser.parseAnnouncements(courseHtml, course))
                items.addAll(TeachingParser.parseAssignments(courseHtml, course))
                items.addAll(TeachingParser.parseMaterials(courseHtml, course))
            }
            
            val newSnap = _snapshot.value.copy(
                courses = courses,
                items = items,
                fetchedAt = System.currentTimeMillis()
            )
            saveSnapshot(newSnap)
            _isSignedIn.value = true
        } catch (e: Exception) {
            _message.value = "Error: ${e.message}"
            if (e is TeachingError.LoginRequired) {
                _isSignedIn.value = false
            }
        } finally {
            _isRefreshing.value = false
            _message.value = null
        }
    }

    suspend fun refreshIfNeeded() {
        if (_snapshot.value.items.isEmpty() || System.currentTimeMillis() - _snapshot.value.fetchedAt > 1000 * 60 * 60) {
            refresh()
        } else {
            _isSignedIn.value = session.isLoggedIn()
        }
    }

    fun markRead(id: String) {
        val keys = _snapshot.value.readKeys.toMutableSet()
        keys.add(id)
        saveSnapshot(_snapshot.value.copy(readKeys = keys))
    }
    
    fun signOut() {
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        _isSignedIn.value = false
        saveSnapshot(TeachingSnapshot())
    }

    companion object {
        @Volatile
        private var instance: TeachingStore? = null

        fun getInstance(context: Context): TeachingStore {
            return instance ?: synchronized(this) {
                instance ?: TeachingStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
