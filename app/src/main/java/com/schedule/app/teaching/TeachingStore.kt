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
        _message.value = "正在获取教学网课程列表..."
        try {
            val homeHtml = session.get(TeachingURLs.home)
            val allCourses = TeachingParser.parseCourses(homeHtml)
            val currentCourses = allCourses.filter { it.isCurrent }
            val chosen = if (currentCourses.isNotEmpty()) currentCourses else allCourses
            val replacement = _snapshot.value.items.toMutableList()
            var successfulCourses = 0
            
            for ((index, course) in chosen.withIndex()) {
                _message.value = "正在同步第 ${index + 1}/${chosen.size} 门课程: ${course.displayTitle}"
                try {
                    val items = fetchCourse(course)
                    replacement.removeAll { it.courseID == course.id }
                    replacement.addAll(items)
                    successfulCourses++
                } catch (e: Exception) {
                    if (e is TeachingError.LoginRequired) throw e
                    e.printStackTrace()
                }
            }
            
            val allowedCourseIds = chosen.map { it.id }.toSet()
            val newSnap = _snapshot.value.copy(
                courses = chosen,
                items = replacement.filter { allowedCourseIds.contains(it.courseID) },
                fetchedAt = if (successfulCourses > 0 || chosen.isEmpty()) System.currentTimeMillis() else _snapshot.value.fetchedAt
            )
            saveSnapshot(newSnap)
            _isSignedIn.value = true
        } catch (e: Exception) {
            _message.value = "同步失败: ${e.message}"
            if (e is TeachingError.LoginRequired) {
                _isSignedIn.value = false
            }
        } finally {
            _isRefreshing.value = false
            _message.value = null
        }
    }

    val unreadCount: Int
        get() = _snapshot.value.items.count {
            it.kind == TeachingKind.GRADE &&
            !_snapshot.value.readKeys.contains(it.id) &&
            !_snapshot.value.readKeys.contains(it.itemReadKey)
        }

    private suspend fun fetchCourse(course: TeachingCourse): List<TeachingItem> {
        val result = mutableListOf<TeachingItem>()

        // 1. Fetch grades for this course (non-fatal try-catch)
        try {
            // A. Try REST API for grades
            val colJson = session.getOrNull(TeachingURLs.gradebookColumnsApi(course.id))
            val userGradesJson = session.getOrNull(TeachingURLs.gradesApi(course.id))
            val apiGrades = if (!colJson.isNullOrBlank() && !userGradesJson.isNullOrBlank()) {
                TeachingParser.parseApiGrades(colJson, userGradesJson, course)
            } else {
                emptyList()
            }

            // B. Fetch grades HTML page
            val courseHtml = session.get(TeachingURLs.course(course.id))
            val gradesUrl = TeachingParser.parseGradesUrl(courseHtml, course.id)
            val gradesHtml = if (gradesUrl == TeachingURLs.course(course.id)) courseHtml else session.get(gradesUrl)
            val htmlGrades = TeachingParser.parseGrades(gradesHtml, course)

            // Merge API and HTML grades
            val mergedGrades = mutableListOf<TeachingItem>()
            val seenGradeTitles = mutableSetOf<String>()
            for (g in htmlGrades) {
                seenGradeTitles.add(g.title.trim())
                mergedGrades.add(g)
            }
            for (g in apiGrades) {
                if (!seenGradeTitles.contains(g.title.trim())) {
                    mergedGrades.add(g)
                }
            }
            result.addAll(mergedGrades)
        } catch (e: Exception) {
            if (e is TeachingError.LoginRequired) throw e
            e.printStackTrace()
        }

        // 2. Fetch Assignments and Materials
        try {
            val courseHtml = session.get(TeachingURLs.course(course.id))
            val roots = TeachingParser.parseRoots(courseHtml)
            val queue = ArrayDeque(roots)
            val visited = mutableSetOf<String>()
            val seenItemIds = result.map { it.id }.toMutableSet()

            while (queue.isNotEmpty()) {
                val id = queue.removeFirst()
                if (!visited.add(id)) continue
                if (visited.size > 150) break

                val contentHtml = session.get(TeachingURLs.content(course.id, id))
                val page = TeachingParser.parseContents(contentHtml, course)
                for (f in page.folders) {
                    if (!visited.contains(f)) {
                        queue.add(f)
                    }
                }
                for (item in page.items) {
                    if (seenItemIds.add(item.id)) {
                        var finalItem = item
                        if (item.kind == TeachingKind.ASSIGNMENT) {
                            try {
                                val assignHtml = session.get(TeachingURLs.assignment(course.id, item.contentID))
                                val (dueDate, raw) = TeachingParser.parseDeadline(assignHtml)
                                if (dueDate != null) {
                                    finalItem = finalItem.copy(dueDate = dueDate, dueDateText = raw)
                                } else if (finalItem.dueDate == null && raw != null) {
                                    finalItem = finalItem.copy(dueDateText = raw)
                                }
                            } catch (e: Exception) {
                                // ignore deadline fetch error
                            }
                        }
                        result.add(finalItem)
                    }
                }
            }
        } catch (e: Exception) {
            if (e is TeachingError.LoginRequired) throw e
            e.printStackTrace()
        }

        return result
    }

    suspend fun refreshIfNeeded() {
        val hasNoGrades = _snapshot.value.courses.isNotEmpty() && _snapshot.value.items.none { it.kind == TeachingKind.GRADE }
        if (_snapshot.value.items.isEmpty() || hasNoGrades || System.currentTimeMillis() - _snapshot.value.fetchedAt > 1000 * 60 * 60) {
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

    fun markAllAnnouncementsRead() {
        val keys = _snapshot.value.readKeys.toMutableSet()
        for (item in _snapshot.value.items) {
            if (item.kind == TeachingKind.GRADE || item.kind == TeachingKind.ANNOUNCEMENT) {
                keys.add(item.id)
                keys.add(item.itemReadKey)
            }
        }
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
