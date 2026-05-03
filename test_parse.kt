import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class RadioStationResponse(
    @SerializedName("status") val status: String,
    @SerializedName("stations") val stations: List<RadioStationItem>
)

data class RadioStationItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("image") val image: String,
    @SerializedName("streamUrl") val streamUrl: String,
    @SerializedName("homepage") val homepage: String,
    @SerializedName("tags") val tags: List<String>,
    @SerializedName("country") val country: String,
    @SerializedName("language") val language: String,
    @SerializedName("codec") val codec: String,
    @SerializedName("bitrate") val bitrate: Int,
    @SerializedName("votes") val votes: Int
)

fun main() {
    val json = """{"status":"true","stations":[{"id":"d1a54d2e-623e-4970-ab11-35f7b56c5ec3","title":"Classic Vinyl HD","image":"https://icecast.walmradio.com:8443/classic.jpg","streamUrl":"https://icecast.walmradio.com:8443/classic","homepage":"https://walmradio.com/classic","tags":["1930","1940","1950","1960","beautiful music","big band","classic hits","crooners","easy","easy listening","hd","jazz","light orchestral","lounge","oldies","orchestral","otr","relaxation","strings","swing","unwind","walm"],"country":"US","language":"english","codec":"MP3","bitrate":320,"votes":269195}]}"""
    val gson = Gson()
    try {
        val parsed = gson.fromJson(json, RadioStationResponse::class.java)
        println(parsed.stations.size)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
