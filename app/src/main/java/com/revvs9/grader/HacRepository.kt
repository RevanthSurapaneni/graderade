package com.revvs9.grader

import android.util.Log // Keep android.util.Log
import com.revvs9.grader.model.CourseGrades
import com.revvs9.grader.model.MarkingPeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.UUID
import org.jsoup.Jsoup

class HacRepository private constructor(
    private val apiService: HacApiService,
    private val parser: HacParser
) {
    private val instanceId = UUID.randomUUID().toString()

    init {
        Log.d("HacRepository", "HacRepository instance INITIALIZED: $instanceId")
    }

    fun logInstanceId(viewModelName: String) {
        Log.d("HacRepository", "$viewModelName is using HacRepository instance: $instanceId")
    }

    private var cachedCourseGrades: List<CourseGrades>? = null
    private val gradesMutex = Mutex()

    private val _gradesDataFlow = MutableStateFlow<Result<List<CourseGrades>>?>(null)
    val gradesDataFlow: StateFlow<Result<List<CourseGrades>>?> = _gradesDataFlow.asStateFlow()

    private var cachedMarkingPeriods: List<MarkingPeriod>? = null
    private val markingPeriodsMutex = Mutex()

    private val _markingPeriodsFlow = MutableStateFlow<Result<List<MarkingPeriod>>?>(null)
    val markingPeriodsFlow: StateFlow<Result<List<MarkingPeriod>>?> = _markingPeriodsFlow.asStateFlow()

    private var lastAssignmentsPageHtml: String? = null // Still useful for ASP.NET fields if any other POSTs were needed, or for debugging
    private val aspNetFieldsMutex = Mutex()

    private val loginPageUrl = "https://hac.friscoisd.org/HomeAccess/Account/LogOn?ReturnUrl=%2fHomeAccess"
    private val assignmentsPageUrl = "https://hac.friscoisd.org/HomeAccess/Content/Student/Assignments.aspx"
    // markingPeriodFieldName and eventTargetRefreshViewValue are no longer used for selection POSTs

    suspend fun login(username: String, password: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("HacRepository", "[$instanceId] Fetching login page...")
                val loginPageResponse = apiService.getLoginPage(loginPageUrl)

                if (!loginPageResponse.isSuccessful || loginPageResponse.body() == null) {
                    val errorMsg = "[$instanceId] Failed to fetch login page: ${loginPageResponse.code()}"
                    Log.e("HacRepository", errorMsg)
                    return@withContext "Failed to fetch login page: ${loginPageResponse.code()}"
                }
                Log.d("HacRepository", "[$instanceId] Login page fetched successfully.")
                val loginPageHtml = loginPageResponse.body()!!
                aspNetFieldsMutex.withLock { // Store initial HTML from login if needed, though assignments page is primary
                    if (loginPageHtml.contains("__VIEWSTATE")) lastAssignmentsPageHtml = loginPageHtml
                }


                val formDetails = parser.extractLoginFormDetails(loginPageHtml)
                if (formDetails.isEmpty() || !formDetails.containsKey("__RequestVerificationToken")) {
                    val errorMsg = "[$instanceId] Failed to parse login form details or token missing."
                    Log.e("HacRepository", errorMsg)
                    return@withContext "Failed to parse login form details."
                }
                Log.d("HacRepository", "[$instanceId] Login form details parsed.")

                val loginPayload = formDetails.toMutableMap()
                loginPayload["LogOnDetails.UserName"] = username
                loginPayload["LogOnDetails.Password"] = password

                Log.d("HacRepository", "[$instanceId] Attempting login...")
                val loginResponse = apiService.login(loginPageUrl, loginPayload)

                Log.d("HacRepository", "[$instanceId] Login response code: ${loginResponse.code()}")

                if (loginResponse.isSuccessful) {
                    val responseBody = loginResponse.body()
                    val finalUrl = loginResponse.raw().request.url.toString()
                    Log.d("HacRepository", "[$instanceId] Login response final URL: $finalUrl")
                    Log.d("HacRepository", "[$instanceId] Login response body (first 300 chars): ${responseBody?.take(300)}")

                    // Update lastAssignmentsPageHtml if login lands on a page with ASP.NET state
                    if (responseBody != null && responseBody.contains("__VIEWSTATE")) {
                        aspNetFieldsMutex.withLock {
                            lastAssignmentsPageHtml = responseBody
                        }
                    }

                    val invalidCredentials = responseBody?.contains("Invalid username or password", ignoreCase = true) == true
                    val isRedirectToWeekViewInBody = responseBody?.contains("/HomeAccess/Home/WeekView", ignoreCase = true) == true
                    val containsLogOff = responseBody?.contains("Log Off", ignoreCase = true) == true
                    val isLikelyHomePageUrl = finalUrl.contains("/HomeAccess/Home/WeekView", ignoreCase = true) ||
                                              finalUrl.endsWith("Assignments.aspx") ||
                                              (finalUrl.startsWith("https://hac.friscoisd.org/HomeAccess/") &&
                                               !finalUrl.contains("/Account/LogOn", ignoreCase = true) &&
                                               !finalUrl.contains("/Account/Login", ignoreCase = true) &&
                                               responseBody?.contains("<title>Home Access Center</title>", ignoreCase = true) == true)


                    if (!invalidCredentials && (isRedirectToWeekViewInBody || containsLogOff || isLikelyHomePageUrl)) {
                        Log.i("HacRepository", "[$instanceId] Login successful. Triggering initial data load with forceRefresh=true.")
                        loadMarkingPeriodsAndDefaultGrades(forceRefresh = true)
                        return@withContext "Success"
                    } else {
                        val errorDetail = if (invalidCredentials) "Invalid credentials."
                                          else "Unexpected response after login. Final URL: $finalUrl."
                        val errorMsg = "[$instanceId] Login failed. $errorDetail Body: ${responseBody?.take(300)}..."
                        Log.e("HacRepository", errorMsg)
                        return@withContext "Login failed: $errorDetail"
                    }
                } else {
                    val errorMsg = "[$instanceId] Login request failed: ${loginResponse.code()}. Response: ${loginResponse.errorBody()?.string()?.take(200)}..."
                    Log.e("HacRepository", errorMsg)
                    return@withContext "Login request failed: ${loginResponse.code()}"
                }
            } catch (e: Exception) {
                val errorMsg = "[$instanceId] Exception during login: ${e.message}"
                Log.e("HacRepository", errorMsg, e)
                return@withContext "Exception during login: ${e.message}"
            }
        }
    }

    private suspend fun fetchAssignmentsPageHtml(): Response<String> {
        val response = apiService.getAssignmentsPage(assignmentsPageUrl)
        if (response.isSuccessful && response.body() != null) {
            aspNetFieldsMutex.withLock {
                lastAssignmentsPageHtml = response.body()!!
            }
        }
        return response
    }

    // Parameter explicitMarkingPeriodValue removed
    suspend fun loadMarkingPeriodsAndDefaultGrades(forceRefresh: Boolean = false) {
        Log.d("HacRepository", "[$instanceId] loadMarkingPeriodsAndDefaultGrades called. forceRefresh: $forceRefresh (No explicit MP selection)")

        if (!forceRefresh) {
            val currentMarkingPeriods = markingPeriodsMutex.withLock { cachedMarkingPeriods }
            val currentGrades = gradesMutex.withLock { cachedCourseGrades }

            val isMarkingPeriodsCacheValid = currentMarkingPeriods != null && currentMarkingPeriods.isNotEmpty() &&
                                           _markingPeriodsFlow.value?.isSuccess == true && _markingPeriodsFlow.value?.getOrNull()?.isNotEmpty() == true
            val isGradesCacheValid = currentGrades != null && currentGrades.isNotEmpty() &&
                                   _gradesDataFlow.value?.isSuccess == true && _gradesDataFlow.value?.getOrNull()?.isNotEmpty() == true

            if (isMarkingPeriodsCacheValid && isGradesCacheValid) {
                 Log.d("HacRepository", "[$instanceId] CACHE HIT: Default marking periods and grades data is valid. Skipping network.")
                 if (_markingPeriodsFlow.value?.getOrNull() != currentMarkingPeriods) {
                    currentMarkingPeriods?.let { _markingPeriodsFlow.value = Result.success(it) }
                 }
                 if (_gradesDataFlow.value?.getOrNull() != currentGrades) {
                    currentGrades?.let { _gradesDataFlow.value = Result.success(it) }
                 }
                return
            }
            Log.d("HacRepository", "[$instanceId] CACHE MISS or stale data for default load. Proceeding to fetch. forceRefresh=$forceRefresh, isMarkingPeriodsCacheValid=$isMarkingPeriodsCacheValid, isGradesCacheValid=$isGradesCacheValid")
        }

        _markingPeriodsFlow.value = null // Indicate loading
        _gradesDataFlow.value = null     // Indicate loading

        withContext(Dispatchers.IO) {
            // Simplified: Always perform initial GET and parse grades/MPs directly.
            Log.i("HacRepository", "[$instanceId] Performing initial GET for default marking period and grades.")
            try {
                val htmlResponse = fetchAssignmentsPageHtml()
                if (htmlResponse.isSuccessful && htmlResponse.body() != null) {
                    val htmlBody = htmlResponse.body()!!
                    // lastAssignmentsPageHtml is updated by fetchAssignmentsPageHtml

                    val parsedMarkingPeriods = withContext(Dispatchers.Default) { parser.parseMarkingPeriods(htmlBody) }
                    markingPeriodsMutex.withLock { cachedMarkingPeriods = parsedMarkingPeriods }
                    _markingPeriodsFlow.value = Result.success(parsedMarkingPeriods)
                    Log.d("HacRepository", "[$instanceId] Default Load: Fetched and parsed ${parsedMarkingPeriods.size} marking periods. HTML selected: ${parsedMarkingPeriods.find {it.isSelected}?.name}")

                    val parsedGrades = withContext(Dispatchers.Default) { parser.parseAssignmentsPageHtml(htmlBody) }
                    gradesMutex.withLock { cachedCourseGrades = parsedGrades }
                    _gradesDataFlow.value = Result.success(parsedGrades)
                    Log.d("HacRepository", "[$instanceId] Default Load: Parsed ${parsedGrades.size} courses from initial GET response.")

                    if (parsedGrades.isEmpty()) {
                         val selectedInHtml = parsedMarkingPeriods.find { it.isSelected }?.value
                         Log.w("HacRepository", "[$instanceId] Default Load: Parsed 0 grades. Default MP in HTML was: $selectedInHtml.")
                    }

                } else {
                    val errorMsg = "[$instanceId] Error fetching assignments page for default load: ${htmlResponse.code()} - ${htmlResponse.message()}"
                    Log.e("HacRepository", errorMsg)
                    val exception = Exception("Error fetching initial data: ${htmlResponse.code()}")
                    _markingPeriodsFlow.value = Result.failure(exception)
                    _gradesDataFlow.value = Result.failure(exception)
                }
            } catch (e: Exception) {
                val errorMsg = "[$instanceId] Network error or parsing failed during default load sequence"
                Log.e("HacRepository", errorMsg, e)
                val exception = Exception("Network error or parsing failed for default view: ${e.message}", e)
                _markingPeriodsFlow.value = Result.failure(exception)
                _gradesDataFlow.value = Result.failure(exception)
            }
        }
    }

    companion object {
        @Volatile private var instance: HacRepository? = null

        fun getInstance(apiService: HacApiService, parser: HacParser): HacRepository {
            return instance ?: synchronized(this) {
                instance ?: HacRepository(apiService, parser).also {
                    Log.d("HacRepository", "HacRepository Singleton INSTANCE CREATED: ${it.instanceId}")
                    instance = it
                }
            }
        }
    }
}
