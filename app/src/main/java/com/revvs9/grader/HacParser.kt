package com.revvs9.grader

import android.util.Log // Import Android Log
import com.revvs9.grader.model.Assignment
import com.revvs9.grader.model.CourseGrades
import com.revvs9.grader.model.MarkingPeriod
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

object HacParser {

    private const val TAG = "HacParser" // Define a TAG for logging

    /**
     * Extracts details from the HAC login form.
     *
     * @param html The HTML content of the login page.
     * @return A map of input field names to their values. Username and password values will be empty strings,
     *         to be filled by the user. Hidden fields will have their pre-filled values.
     */
    fun extractLoginFormDetails(html: String): Map<String, String> {
        val document: Document = Jsoup.parse(html)
        val loginFormInputs = mutableMapOf<String, String>()

        // Extract username input field name
        val usernameInput = document.select("input[name=LogOnDetails.UserName]").first()
        if (usernameInput != null) {
            loginFormInputs[usernameInput.attr("name")] = "" // Username will be filled by user
        }

        // Extract password input field name
        val passwordInput = document.select("input[name=LogOnDetails.Password]").first()
        if (passwordInput != null) {
            loginFormInputs[passwordInput.attr("name")] = "" // Password will be filled by user
        }

        // Extract __RequestVerificationToken
        val requestVerificationTokenInput = document.select("input[name=__RequestVerificationToken]").first()
        if (requestVerificationTokenInput != null) {
            loginFormInputs[requestVerificationTokenInput.attr("name")] = requestVerificationTokenInput.`val`()
        }

        // Extract Database hidden field
        val databaseInput = document.select("input[name=Database]").first()
        if (databaseInput != null) {
            loginFormInputs[databaseInput.attr("name")] = databaseInput.`val`()
        }

        // Extract VerificationOption hidden field
        val verificationOptionInput = document.select("input[name=VerificationOption]").first()
        if (verificationOptionInput != null) {
            loginFormInputs[verificationOptionInput.attr("name")] = verificationOptionInput.`val`()
        }
        
        return loginFormInputs
    }

    /**
     * Extracts common ASP.NET hidden field values from HTML content.
     * These fields are typically __VIEWSTATE, __VIEWSTATEGENERATOR, and __EVENTVALIDATION.
     *
     * @param html The HTML content of the page.
     * @return A map of hidden field names to their values.
     */
    fun extractAspNetHiddenFields(html: String): Map<String, String> {
        val document: Document = Jsoup.parse(html)
        val hiddenFields = mutableMapOf<String, String>()

        val viewStateElement = document.selectFirst("input[name=__VIEWSTATE]")
        if (viewStateElement != null) {
            hiddenFields["__VIEWSTATE"] = viewStateElement.`val`()
        }

        val viewStateGeneratorElement = document.selectFirst("input[name=__VIEWSTATEGENERATOR]")
        if (viewStateGeneratorElement != null) {
            hiddenFields["__VIEWSTATEGENERATOR"] = viewStateGeneratorElement.`val`()
        }

        val eventValidationElement = document.selectFirst("input[name=__EVENTVALIDATION]")
        if (eventValidationElement != null) {
            hiddenFields["__EVENTVALIDATION"] = eventValidationElement.`val`()
        }
        
        // Log if any of the common fields are missing, as this might indicate parsing issues or changes in HAC's structure.
        if (!hiddenFields.containsKey("__VIEWSTATE")) {
            Log.w(TAG, "__VIEWSTATE hidden field not found during specific ASP.NET field extraction.")
        }
        if (!hiddenFields.containsKey("__VIEWSTATEGENERATOR")) {
            Log.w(TAG, "__VIEWSTATEGENERATOR hidden field not found during specific ASP.NET field extraction.")
        }
        if (!hiddenFields.containsKey("__EVENTVALIDATION")) {
            Log.w(TAG, "__EVENTVALIDATION hidden field not found during specific ASP.NET field extraction.")
        }

        return hiddenFields
    }

    /**
     * Extracts ALL hidden input field names and values from HTML content.
     *
     * @param html The HTML content of the page.
     * @return A map of all hidden input field names to their values.
     */
    fun extractAllHiddenFields(html: String): Map<String, String> {
        val document: Document = Jsoup.parse(html)
        val hiddenFieldsMap = mutableMapOf<String, String>()
        val hiddenInputElements: Elements = document.select("input[type=hidden]")

        for (inputElement in hiddenInputElements) {
            val name = inputElement.attr("name")
            val value = inputElement.`val`()
            if (name.isNotEmpty()) { // Ensure the hidden input has a name
                hiddenFieldsMap[name] = value
            }
        }
        Log.d(TAG, "Extracted ${hiddenFieldsMap.size} hidden fields. Keys: ${hiddenFieldsMap.keys.joinToString()}")
        // For brevity, not logging all values if __VIEWSTATE is huge.
        // hiddenFieldsMap.forEach { (key, value) =>
        //     Log.d(TAG, "Hidden Field: $key = ${value.take(100)}${if (value.length > 100) "..." else ""}")
        // }
        return hiddenFieldsMap
    }

    /**
     * Extracts the selected value from a radio button group.
     *
     * @param html The HTML content of the page.
     * @param radioButtonName The name attribute of the radio button group.
     * @return The value of the selected radio button, or null if not found or not selected.
     */
    fun extractSelectedRadioButtonValue(html: String, radioButtonName: String): String? {
        val document: Document = Jsoup.parse(html)
        val selectedRadio: Element? = document.select("input[name='$radioButtonName'][checked]").first()
        if (selectedRadio != null) {
            val value = selectedRadio.`val`()
            Log.d(TAG, "Found selected radio button for '$radioButtonName': value='$value'")
            return value
        }
        Log.w(TAG, "No selected radio button found for name '$radioButtonName'")
        return null
    }

    /**
     * Parses the assignments page HTML to extract available marking periods.
     *
     * @param html The HTML content of the assignments page.
     * @return A list of MarkingPeriod objects.
     */
    fun parseMarkingPeriods(html: String): List<MarkingPeriod> {
        val document: Document = Jsoup.parse(html)
        val markingPeriods = mutableListOf<MarkingPeriod>()
        val selectElement: Element? = document.selectFirst("select[name*=\'ddlReportCardRuns\'], select[id*=\'ddlReportCardRuns\'], select[name*=\'ddlMarkingPeriod\'], select[id*=\'ddlMarkingPeriod\']")

        if (selectElement != null) {
            val options: Elements = selectElement.select("option")
            for (option in options) {
                val name = option.text().trim()
                val value = option.attr("value")
                val isSelected = option.hasAttr("selected") // Check for the \'selected\' attribute
                
                if (name.isNotBlank() && value.isNotBlank() && name.any { it.isDigit() }) {
                    markingPeriods.add(MarkingPeriod(name, value, isSelected))
                }
            }
        } else {
            Log.w(TAG, "Marking period select element not found. Please verify selector.")
        }
        // If no period was marked as selected in HTML, and we have periods, select the one with the highest numeric value in its \'value\' attribute.
        // This assumes \'value\' is something like "1", "2", "3", "4".
        if (markingPeriods.none { it.isSelected } && markingPeriods.isNotEmpty()) {
            val latestPeriod = markingPeriods.maxByOrNull { it.value.toIntOrNull() ?: -1 } 
            latestPeriod?.let {
                val index = markingPeriods.indexOf(it)
                if (index != -1) {
                    markingPeriods[index] = it.copy(isSelected = true)
                }
            }
        }
        return markingPeriods
    }

    /**
     * Parses the assignments page HTML to extract course and assignment details.
     *
     * @param html The HTML content of the assignments page.
     * @return A list of CourseGrades objects, each containing course and assignment details.
     */
    fun parseAssignmentsPageHtml(html: String): List<CourseGrades> {
        val document: Document = Jsoup.parse(html)
        val coursesList = mutableListOf<CourseGrades>()

        // Select each course block. Courses are contained in divs with class "AssignmentClass".
        val courseElements: Elements = document.select("div.AssignmentClass")

        if (courseElements.isEmpty()) {
            Log.w(TAG, "No course elements found with selector 'div.AssignmentClass'.")
            // Check for indicators of the "Competency Groups" view or other alternative views
            val competencyGroupTitle = document.selectFirst("span#plnMain_lblSubTitle:contains(Competency Group)")
            if (competencyGroupTitle != null) {
                Log.w(TAG, "Detected 'Competency Group' title. The assignments page might be in the wrong view mode (e.g., Competency Groups instead of Courses).")
            } else {
                Log.w(TAG, "The HTML structure might be different or the page content is not as expected (e.g. no courses for this marking period, or an error page).")
            }
            return coursesList
        }
        Log.d(TAG, "Found ${courseElements.size} 'div.AssignmentClass' elements. Processing each...")

        for ((index, courseElement) in courseElements.withIndex()) {
            Log.d(TAG, "Processing courseElement ${index + 1}/${courseElements.size}. HTML (first 300 chars): ${courseElement.html().take(300)}")
            // Extract course name from the link within the header of the course block.
            // Example: <a class="sg-header-heading" ...>COURSE NAME</a>
            val courseNameElement = courseElement.selectFirst("div.sg-header a.sg-header-heading")
            val rawCourseNameText = courseNameElement?.text()
            Log.d(TAG, "Course Name Element: ${if (courseNameElement != null) "Found" else "Not Found"}, Raw Text: '$rawCourseNameText'")
            var courseName = rawCourseNameText?.trim() ?: "Unknown Course"
            
            if (courseName != "Unknown Course") { // Only apply substringBefore if name was found and not default
                val originalNameBeforeSubstring = courseName
                courseName = courseName.substringBefore("@").trim()
                if (courseName.isEmpty() && originalNameBeforeSubstring.isNotEmpty()) { 
                    Log.w(TAG, "Course name became empty after 'substringBefore(\"@\")'. Original: '$originalNameBeforeSubstring'. Reverting to 'Unknown Course'.")
                    courseName = "Unknown Course" 
                }
            }

            // Extract overall score. Example: <span class="sg-header-heading sg-right">Student Grades 98.60%</span>
            val overallScoreElement = courseElement.selectFirst("div.sg-header span.sg-header-heading.sg-right")
            val rawOverallScoreText = overallScoreElement?.text()
            Log.d(TAG, "Overall Score Element: ${if (overallScoreElement != null) "Found" else "Not Found"}, Raw Text: '$rawOverallScoreText'")
            var overallScoreString = rawOverallScoreText?.trim() ?: "N/A"
            
            if (overallScoreString != "N/A") { // Only process if not already default "N/A"
                val originalScoreBeforeProcessing = overallScoreString
                if (overallScoreString.startsWith("Student Grades ")) {
                    overallScoreString = overallScoreString.substringAfter("Student Grades ").removeSuffix("%").trim()
                } else {
                    // Fallback: try to extract a number, possibly followed by %
                    val scoreMatch = Regex("([0-9.]+).*").find(overallScoreString)
                    overallScoreString = scoreMatch?.groupValues?.get(1) ?: run {
                        Log.w(TAG, "Overall score text '$originalScoreBeforeProcessing' did not match 'Student Grades ' prefix or numeric pattern. Setting to N/A.")
                        "N/A"
                    }
                }
                if (overallScoreString.isEmpty() && originalScoreBeforeProcessing.isNotEmpty()) {
                    Log.w(TAG, "Overall score became empty after parsing. Original raw: '$originalScoreBeforeProcessing'. Setting to N/A.")
                    overallScoreString = "N/A"
                }
            }


            Log.d(TAG, "Intermediate parsed name for element ${index + 1}: '$courseName', Intermediate parsed score: '$overallScoreString'")

            val assignmentsList = mutableListOf<Assignment>()
            // Assignments are in a table with class "sg-asp-table" within the course block.
            val assignmentsTable = courseElement.selectFirst("table.sg-asp-table")

            if (assignmentsTable == null) {
                Log.w(TAG, "No assignments table (table.sg-asp-table) found for course: '$courseName' (element ${index + 1})")
            } else {
                // Assignment details are in rows with class "sg-asp-table-data-row".
                val assignmentRows: Elements = assignmentsTable.select("tr.sg-asp-table-data-row")

                if (assignmentRows.isEmpty()) {
                    Log.w(TAG, "No assignment rows found in table for course: '$courseName' (element ${index + 1}) with selector 'tr.sg-asp-table-data-row'")
                }

                for (row in assignmentRows) {
                    val cells: Elements = row.select("td") // Get all cells in the row

                    // Expected columns: Date Due, Date Assigned, Assignment, Category, Score, Total Points
                    if (cells.size < 6) {
                        Log.w(TAG, "Skipping assignment row with ${cells.size} cells (expected at least 6) for course '$courseName' (element ${index + 1}): ${row.text()}")
                        continue
                    }

                    try {
                        val dateDue = cells.getOrNull(0)?.text()?.trim()?.takeIf { it.isNotBlank() && it != "&nbsp;" } ?: "N/A"
                        val dateAssigned = cells.getOrNull(1)?.text()?.trim()?.takeIf { it.isNotBlank() && it != "&nbsp;" } ?: "N/A"
                        
                        val assignmentNameElement = cells.getOrNull(2)?.selectFirst("a")
                        val assignmentName = assignmentNameElement?.text()?.trim() ?: cells.getOrNull(2)?.text()?.trim() ?: "Unknown Assignment"
                        
                        val category = cells.getOrNull(3)?.text()?.trim() ?: "N/A"
                        
                        val scoreCell = cells.getOrNull(4)
                        var scoreToParse: String? = scoreCell?.ownText()?.trim()

                        if (scoreToParse.isNullOrBlank()) {
                            scoreToParse = scoreCell?.text()?.trim()
                        }
                        
                        var finalScoreString: String? = null // Explicitly String?
                        if (!scoreToParse.isNullOrBlank()) {
                            // Check for special conditions like "Exempt", "MSG", "Late"
                            if (scoreToParse.equals("X - Exempt", ignoreCase = true) ||
                                scoreToParse.equals("Exempt", ignoreCase = true) ||
                                scoreToParse.equals("MSG", ignoreCase = true) ||
                                scoreToParse.equals("&nbsp;", ignoreCase = true) || // Added missing || operator here
                                scoreToParse.equals("L - Late Work", ignoreCase = true) ||
                                scoreToParse.equals("Late", ignoreCase = true)) {
                                finalScoreString = scoreToParse // Keep the original string for these cases
                            } else {
                                // For actual scores, just use the parsed string.
                                // If it needs to be purely numeric, additional cleaning might be needed here.
                                finalScoreString = scoreToParse 
                            }
                        }

                        // Ensure totalPoints is also treated as String?
                        val finalTotalPointsString = cells.getOrNull(5)?.text()?.trim() // This is already String?

                        assignmentsList.add(
                            Assignment(
                                name = assignmentName, // Assuming local var 'assignmentName' is String
                                category = category,   // Assuming local var 'category' is String
                                dateDue = dateDue,     // Assuming local var 'dateDue' is String?
                                dateAssigned = dateAssigned, // Parameter name from data class
                                score = finalScoreString,    // Pass the String?
                                totalPoints = finalTotalPointsString // Pass the String?
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing assignment row for course '$courseName' (element ${index + 1}): ${row.text()}. Error: ${e.message}", e)
                    }
                }
            }
            
            // Add course to the list, even if it has no assignments (but name/score was found)
            // or if assignmentsTable was null.
            if (courseName != "Unknown Course" || overallScoreString != "N/A") {
                 coursesList.add(
                    CourseGrades(
                        courseName = courseName,
                        overallScore = overallScoreString,
                        assignments = assignmentsList
                    )
                )
                Log.d(TAG, "Added course (element ${index + 1}): '$courseName' with score '$overallScoreString' and ${assignmentsList.size} assignments.")
            } else { // if (courseName == "Unknown Course" && overallScoreString == "N/A")
                 Log.w(TAG, "Skipping course element ${index + 1} because both name and score are default/unknown. Name: '$courseName', Score: '$overallScoreString'. Assignments found: ${assignmentsList.size}. HTML (first 300 chars): ${courseElement.html().take(300)}")
                 if (assignmentsList.isNotEmpty()) {
                     Log.w(TAG, "Skipped course (element ${index + 1}) had ${assignmentsList.size} assignments despite name/score issues.")
                 }
            }
        }

        if (coursesList.isEmpty() && courseElements.isNotEmpty()) {
            Log.w(TAG, "Processed ${courseElements.size} 'div.AssignmentClass' elements but extracted no course data into the final list. Check previous D/W logs for details on each element.")
        } else if (courseElements.isNotEmpty()) {
            Log.i(TAG, "Successfully parsed ${coursesList.size} courses from ${courseElements.size} 'div.AssignmentClass' elements.")
        }
        return coursesList
    }
}

// Helper extension function to safely get elements from a list
fun Elements.getOrNull(index: Int): Element? {
    return if (index >= 0 && index < this.size) this[index] else null
}
