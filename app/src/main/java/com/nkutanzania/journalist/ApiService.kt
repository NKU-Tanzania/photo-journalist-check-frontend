import com.nkutanzania.journalist.ImageModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("users/{userId}/images")
    fun getUserImages(@Path("userId") userId: Int): Call<List<ImageModel>>
}