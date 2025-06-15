package com.revvs9.grader

import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface HacApiService {

    @GET
    suspend fun getLoginPage(@Url url: String): Response<String> // To fetch the initial login page HTML

    @FormUrlEncoded
    @POST
    suspend fun login(
        @Url url: String, // Full login URL, e.g., "https://hac.friscoisd.org/HomeAccess/Account/LogOn?ReturnUrl=%2fHomeAccess"
        @FieldMap fields: Map<String, String> // Map of form fields including username, password, and hidden tokens
    ): Response<String> // The response from HAC login is typically HTML content as a String

    @GET
    suspend fun getAssignmentsPage(@Url url: String): Response<String> // New function to fetch the grades/assignments page HTML

    @FormUrlEncoded
    @POST
    suspend fun postToAssignmentsPage( // Correctly renamed from getAssignmentsForMarkingPeriod
        @Url url: String, // This will be the Assignments.aspx URL
        @FieldMap fields: Map<String, String> // Fields to select the marking period
    ): Response<String>
}
