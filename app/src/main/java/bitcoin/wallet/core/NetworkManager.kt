package bitcoin.wallet.core

import bitcoin.wallet.RestoredBlock
import io.reactivex.Observable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

class NetworkManager : INetworkManager {
    override fun getJwtToken(identity: String, pubKeys: Map<Int, String>): Observable<String> {
        return BackendApi.service
                .getJwtToken(mapOf("identity" to identity))
                .map {
                    it["token"]
                }
    }

    override fun getExchangeRates(): Observable<Map<String, Double>> {
        return ServiceExchangeApi.service
                .getRates("USD", "BTC,BCH,ETH")
                .onErrorReturn {
                    mapOf()
                }
    }

    override fun getTransactionBlocks(addresses: List<String>): Observable<List<RestoredBlock>> {
        val blocks = mutableListOf<RestoredBlock>()

        if (addresses.first() == "moyrWfrks5EsHLb2hqUr8nUnC9q9DYXMtK") {
            mapOf(
                    "0000000000000913da941eb0f9133bfd351e01a13d7fac655722e4d325e041b8" to 1316822,
                    "0000000000019e52d567613b43c1f64a47c8c96cbd0b8200158c5e5956248ed3" to 1348909,
                    "0000000000000030c74713df81d88622698c3bb2c89e78296a6ee91ef1b98124" to 1356468
            ).forEach {
                blocks.add(RestoredBlock().apply {
                    hash = it.key
                    height = it.value
                })
            }
        }

        return Observable.just(blocks)
    }
}


object BackendApi {

    var service: IGrouviService

    private const val apiURL = "http://bitnode-db.grouvi.org:3000/api/BTC/testnet/"

    init {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BASIC

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logging)  // <-- this is the important line!

        val retrofit = Retrofit.Builder()
                .baseUrl(apiURL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build()

        service = retrofit.create(IGrouviService::class.java)
    }

    interface IGrouviService {

        @POST("wallet")
        fun getJwtToken(
                @Body params: Map<String, @JvmSuppressWildcards Any>
        ): Observable<HashMap<String, String>>

    }
}

object ServiceExchangeApi {

    val service: IExchangeRate

    init {
        val logger = HttpLoggingInterceptor()
        logger.level = HttpLoggingInterceptor.Level.BASIC

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logger)

        val retrofit = Retrofit.Builder()
                .baseUrl("https://min-api.cryptocompare.com")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build()

        service = retrofit.create(IExchangeRate::class.java)
    }

    interface IExchangeRate {

        @GET("data/price")
        fun getRates(
                @Query("fsym") baseCurrency: String,
                @Query("tsyms") coinTypes: String
        ): Observable<Map<String, Double>>

    }
}
