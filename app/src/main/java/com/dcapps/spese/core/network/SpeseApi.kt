package com.dcapps.spese.core.network

import com.dcapps.spese.core.model.AddParticipantRequest
import com.dcapps.spese.core.model.BalanceResponse
import com.dcapps.spese.core.model.BatchSaveRequest
import com.dcapps.spese.core.model.BulkItemsRequest
import com.dcapps.spese.core.model.CheckAllRequest
import com.dcapps.spese.core.model.CheckRequest
import com.dcapps.spese.core.model.CheckResponse
import com.dcapps.spese.core.model.CreateExpenseRequest
import com.dcapps.spese.core.model.CreateExpensesListRequest
import com.dcapps.spese.core.model.CreateProductRequest
import com.dcapps.spese.core.model.CreatePurchaseRequest
import com.dcapps.spese.core.model.CreateShoppingCategoryRequest
import com.dcapps.spese.core.model.CreateShoppingItemRequest
import com.dcapps.spese.core.model.CreateShoppingListRequest
import com.dcapps.spese.core.model.CreateUserRequest
import com.dcapps.spese.core.model.CreatedResponse
import com.dcapps.spese.core.model.Expense
import com.dcapps.spese.core.model.ExpenseType
import com.dcapps.spese.core.model.ExpenseTypesResponse
import com.dcapps.spese.core.model.ExpensesList
import com.dcapps.spese.core.model.ExpensesListsResponse
import com.dcapps.spese.core.model.ExpensesResponse
import com.dcapps.spese.core.model.InviteTokenResponse
import com.dcapps.spese.core.model.JoinRequest
import com.dcapps.spese.core.model.MeasurementUnit
import com.dcapps.spese.core.model.MeasurementUnitRequest
import com.dcapps.spese.core.model.MeasurementUnitsResponse
import com.dcapps.spese.core.model.MessageResponse
import com.dcapps.spese.core.model.NameRequest
import com.dcapps.spese.core.model.ParticipantsResponse
import com.dcapps.spese.core.model.PriceHistoryResponse
import com.dcapps.spese.core.model.Product
import com.dcapps.spese.core.model.ProductsResponse
import com.dcapps.spese.core.model.ProfileImageResponse
import com.dcapps.spese.core.model.Purchase
import com.dcapps.spese.core.model.PurchasesResponse
import com.dcapps.spese.core.model.ReorderRequest
import com.dcapps.spese.core.model.SelectImageRequest
import com.dcapps.spese.core.model.ShoppingList
import com.dcapps.spese.core.model.ShoppingListsResponse
import com.dcapps.spese.core.model.TransferOwnerRequest
import com.dcapps.spese.core.model.TransferShoppingOwnershipRequest
import com.dcapps.spese.core.model.UpdateExpenseRequest
import com.dcapps.spese.core.model.UpdateExpensesListRequest
import com.dcapps.spese.core.model.UpdateProductRequest
import com.dcapps.spese.core.model.UpdatePurchaseRequest
import com.dcapps.spese.core.model.UpdateShoppingCategoryRequest
import com.dcapps.spese.core.model.UpdateShoppingItemRequest
import com.dcapps.spese.core.model.UpdateShoppingListRequest
import com.dcapps.spese.core.model.UpdateUserRequest
import com.dcapps.spese.core.model.UpsertUserRequest
import com.dcapps.spese.core.model.User
import com.dcapps.spese.core.model.UserRolesResponse
import com.dcapps.spese.core.model.UserWithExpensesListsResponse
import com.dcapps.spese.core.model.UsersResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API REST del backend Spese. Nessun header di autenticazione: gli endpoint sono pubblici
 * e identificano l'utente per id/email nei path/body.
 *
 * Header `X-Silent: 1` usato su alcune chiamate per coerenza con la webapp (il backend lo
 * ignora; serve solo come marcatore lato client per sopprimere eventuali toast di successo).
 */
interface SpeseApi {

    // ---------- Users ----------
    @GET("api/users")
    suspend fun getUsers(): UsersResponse

    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: Long): User

    @GET("api/users/by-email/{email}")
    suspend fun getUserByEmail(@Path("email") email: String): User

    @POST("api/users/upsert-by-email")
    suspend fun upsertUserByEmail(@Body body: UpsertUserRequest): User

    @POST("api/users")
    suspend fun createUser(@Body body: CreateUserRequest): CreatedResponse

    @GET("api/users/by-email/{email}/expenses-lists")
    suspend fun getUserExpensesLists(@Path("email") email: String): UserWithExpensesListsResponse

    @PUT("api/users/{id}")
    suspend fun updateUser(@Path("id") id: Long, @Body body: UpdateUserRequest): MessageResponse

    @DELETE("api/users/{id}")
    suspend fun deleteUser(@Path("id") id: Long): MessageResponse

    @Multipart
    @POST("api/users/{id}/profile-image")
    suspend fun uploadProfileImage(
        @Path("id") id: Long,
        @Part file: MultipartBody.Part,
    ): ProfileImageResponse

    @PUT("api/users/{id}/profile-image/select")
    suspend fun selectProfileImage(
        @Path("id") id: Long,
        @Body body: SelectImageRequest,
    ): ProfileImageResponse

    // ---------- Expenses lists ----------
    @GET("api/expenses-lists")
    suspend fun getExpensesLists(@Query("paid") paid: Boolean? = null): ExpensesListsResponse

    @GET("api/expenses-lists/{id}")
    suspend fun getExpensesList(@Path("id") id: Long): ExpensesList

    @POST("api/expenses-lists")
    suspend fun createExpensesList(@Body body: CreateExpensesListRequest): CreatedResponse

    @PUT("api/expenses-lists/{id}")
    suspend fun updateExpensesList(
        @Path("id") id: Long,
        @Body body: UpdateExpensesListRequest,
    ): MessageResponse

    @DELETE("api/expenses-lists/{id}")
    suspend fun deleteExpensesList(@Path("id") id: Long): MessageResponse

    @POST("api/expenses-lists/{id}/transfer-owner")
    suspend fun transferExpensesListOwner(
        @Path("id") id: Long,
        @Body body: TransferOwnerRequest,
    ): MessageResponse

    @GET("api/expenses-lists/{id}/balance")
    suspend fun getBalance(@Path("id") id: Long): BalanceResponse

    // ---------- Expenses list participants ----------
    @GET("api/expenses-lists/{listId}/participants")
    suspend fun getParticipants(@Path("listId") listId: Long): ParticipantsResponse

    @POST("api/expenses-lists/{listId}/participants")
    suspend fun addParticipant(
        @Path("listId") listId: Long,
        @Body body: AddParticipantRequest,
    ): MessageResponse

    @DELETE("api/expenses-lists/{listId}/participants/{userId}")
    suspend fun removeParticipant(
        @Path("listId") listId: Long,
        @Path("userId") userId: Long,
    ): MessageResponse

    @DELETE("api/expenses-lists/{listId}/participants/{userId}/guest")
    suspend fun removeGuestParticipant(
        @Path("listId") listId: Long,
        @Path("userId") userId: Long,
    ): MessageResponse

    // ---------- Expenses ----------
    @GET("api/expenses/by-list/{listId}")
    suspend fun getExpensesByList(@Path("listId") listId: Long): ExpensesResponse

    @GET("api/expenses/{id}")
    suspend fun getExpense(@Path("id") id: Long): Expense

    @POST("api/expenses")
    suspend fun createExpense(@Body body: CreateExpenseRequest): CreatedResponse

    @PUT("api/expenses/{id}")
    suspend fun updateExpense(
        @Path("id") id: Long,
        @Body body: UpdateExpenseRequest,
    ): MessageResponse

    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: Long): MessageResponse

    // ---------- Shopping lists ----------
    @GET("api/shopping-lists/by-user/{userId}")
    suspend fun getShoppingListsByUser(@Path("userId") userId: Long): ShoppingListsResponse

    @GET("api/shopping-lists/{id}")
    suspend fun getShoppingList(@Path("id") id: Long): ShoppingList

    @POST("api/shopping-lists")
    suspend fun createShoppingList(@Body body: CreateShoppingListRequest): CreatedResponse

    @PUT("api/shopping-lists/{id}")
    suspend fun updateShoppingList(
        @Path("id") id: Long,
        @Body body: UpdateShoppingListRequest,
    ): MessageResponse

    @DELETE("api/shopping-lists/{id}")
    suspend fun deleteShoppingList(@Path("id") id: Long): MessageResponse

    @POST("api/shopping-lists/{id}/invite-token")
    suspend fun createInviteToken(@Path("id") id: Long): InviteTokenResponse

    @DELETE("api/shopping-lists/{id}/invite-token")
    suspend fun revokeInviteToken(@Path("id") id: Long): MessageResponse

    @POST("api/shopping-lists/join/{token}")
    suspend fun joinShoppingList(
        @Path("token") token: String,
        @Body body: JoinRequest,
    ): CreatedResponse

    @POST("api/shopping-lists/{listId}/transfer-ownership")
    suspend fun transferShoppingOwnership(
        @Path("listId") listId: Long,
        @Body body: TransferShoppingOwnershipRequest,
    ): MessageResponse

    @POST("api/shopping-lists/{listId}/participants")
    suspend fun addShoppingParticipant(
        @Path("listId") listId: Long,
        @Body body: JoinRequest,
    ): MessageResponse

    @DELETE("api/shopping-lists/{listId}/participants/{userId}")
    suspend fun removeShoppingParticipant(
        @Path("listId") listId: Long,
        @Path("userId") userId: Long,
    ): MessageResponse

    @POST("api/shopping-lists/{id}/batch-save")
    suspend fun batchSave(@Path("id") id: Long, @Body body: BatchSaveRequest): MessageResponse

    // ---------- Shopping items ----------
    @POST("api/shopping-items")
    suspend fun createShoppingItem(@Body body: CreateShoppingItemRequest): CreatedResponse

    @PUT("api/shopping-items/{id}")
    suspend fun updateShoppingItem(
        @Path("id") id: Long,
        @Body body: UpdateShoppingItemRequest,
    ): MessageResponse

    /**
     * Variante con body JSON costruito a mano: serve per inviare `category_id: null`
     * esplicito (lo spostamento in "nessuna categoria"), che con explicitNulls=false
     * un data class ometterebbe.
     */
    @PUT("api/shopping-items/{id}")
    suspend fun updateShoppingItemRaw(
        @Path("id") id: Long,
        @Body body: kotlinx.serialization.json.JsonObject,
    ): MessageResponse

    @DELETE("api/shopping-items/{id}")
    suspend fun deleteShoppingItem(@Path("id") id: Long): MessageResponse

    @PATCH("api/shopping-items/{id}/check")
    suspend fun checkShoppingItem(
        @Path("id") id: Long,
        @Body body: CheckRequest,
    ): CheckResponse

    @PUT("api/shopping-items/bulk")
    suspend fun bulkShoppingItems(@Body body: BulkItemsRequest): MessageResponse

    @PATCH("api/shopping-items/check-all")
    suspend fun checkAllShoppingItems(@Body body: CheckAllRequest): MessageResponse

    @PUT("api/shopping-items/reorder")
    suspend fun reorderShoppingItems(@Body body: ReorderRequest): MessageResponse

    // ---------- Shopping categories ----------
    @POST("api/shopping-categories")
    suspend fun createShoppingCategory(@Body body: CreateShoppingCategoryRequest): CreatedResponse

    @PUT("api/shopping-categories/{id}")
    suspend fun updateShoppingCategory(
        @Path("id") id: Long,
        @Body body: UpdateShoppingCategoryRequest,
    ): MessageResponse

    /** Variante raw per inviare `parent_id: null` esplicito (categoria → radice). */
    @PUT("api/shopping-categories/{id}")
    suspend fun updateShoppingCategoryRaw(
        @Path("id") id: Long,
        @Body body: kotlinx.serialization.json.JsonObject,
    ): MessageResponse

    @DELETE("api/shopping-categories/{id}")
    suspend fun deleteShoppingCategory(@Path("id") id: Long): MessageResponse

    @PATCH("api/shopping-categories/{id}/check")
    suspend fun checkShoppingCategory(
        @Path("id") id: Long,
        @Body body: CheckRequest,
    ): CheckResponse

    // ---------- Catalog: roles / expense types / measurement units ----------
    @GET("api/user-roles")
    suspend fun getUserRoles(): UserRolesResponse

    @POST("api/user-roles")
    suspend fun createUserRole(@Body body: NameRequest): CreatedResponse

    @PUT("api/user-roles/{id}")
    suspend fun updateUserRole(@Path("id") id: Long, @Body body: NameRequest): MessageResponse

    @DELETE("api/user-roles/{id}")
    suspend fun deleteUserRole(@Path("id") id: Long): MessageResponse

    @GET("api/expense-types")
    suspend fun getExpenseTypes(): ExpenseTypesResponse

    @POST("api/expense-types")
    suspend fun createExpenseType(@Body body: NameRequest): CreatedResponse

    @PUT("api/expense-types/{id}")
    suspend fun updateExpenseType(@Path("id") id: Long, @Body body: NameRequest): MessageResponse

    @DELETE("api/expense-types/{id}")
    suspend fun deleteExpenseType(@Path("id") id: Long): MessageResponse

    @GET("api/measurement-units")
    suspend fun getMeasurementUnits(): MeasurementUnitsResponse

    @POST("api/measurement-units")
    suspend fun createMeasurementUnit(
        @Body body: MeasurementUnitRequest,
    ): CreatedResponse

    @PUT("api/measurement-units/{id}")
    suspend fun updateMeasurementUnit(
        @Path("id") id: Long,
        @Body body: MeasurementUnitRequest,
    ): MessageResponse

    @DELETE("api/measurement-units/{id}")
    suspend fun deleteMeasurementUnit(@Path("id") id: Long): MessageResponse

    // ---------- Price history ----------
    @GET("api/products")
    suspend fun getProducts(): ProductsResponse

    @GET("api/products/{id}")
    suspend fun getProduct(@Path("id") id: Long): Product

    @POST("api/products")
    suspend fun createProduct(@Body body: CreateProductRequest): CreatedResponse

    @PUT("api/products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Long,
        @Body body: UpdateProductRequest,
    ): MessageResponse

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Long): MessageResponse

    @GET("api/products/{productId}/purchases")
    suspend fun getPurchases(@Path("productId") productId: Long): PurchasesResponse

    @POST("api/purchases")
    suspend fun createPurchase(@Body body: CreatePurchaseRequest): CreatedResponse

    @PUT("api/purchases/{id}")
    suspend fun updatePurchase(
        @Path("id") id: Long,
        @Body body: UpdatePurchaseRequest,
    ): MessageResponse

    @DELETE("api/purchases/{id}")
    suspend fun deletePurchase(@Path("id") id: Long): MessageResponse

    @GET("api/products/{productId}/price-history")
    suspend fun getPriceHistory(
        @Path("productId") productId: Long,
        @Query("granularity") granularity: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): PriceHistoryResponse
}
