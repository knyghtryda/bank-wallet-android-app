package bitcoin.wallet.core

import bitcoin.wallet.entities.Currency
import com.google.gson.GsonBuilder
import io.reactivex.Flowable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

class NetworkManager : INetworkManager {

    override fun getRate(coinCode: String, currency: String, year: Int, month: String, day: String, hour: String, minute: String): Flowable<Double> {
        return ServiceExchangeApi.service
                .getRate(coinCode, currency, year, month, day, hour, minute)
                .onErrorResumeNext(getRateByDay(coinCode, currency, year, month, day))
    }

    override fun getRateByDay(coinCode: String, currency: String, year: Int, month: String, day: String): Flowable<Double> {
        return ServiceExchangeApi.service
                .getRateByDay(coinCode, currency, year, month, day)
                .onErrorReturn {
                    0.0
                }
    }

    override fun getLatestRate(coinCode: String, currency: String): Flowable<Double> {
        return ServiceExchangeApi.service
                .getLatestRate(coinCode, currency)
                .onErrorReturn {
                    0.0
                }
    }

    override fun getCurrencies(): Flowable<List<Currency>> {
        return ServiceExchangeApi.service.getCurrencies()
                .onErrorReturn {
                    listOf()
                }
    }

}

object ServiceExchangeApi {

    val service: IExchangeRate
    private const val apiURL = "http://ipfs.grouvi.org/ipns/QmVefrf2xrWzGzPpERF6fRHeUTh9uVSyfHHh4cWgUBnXpq/io-hs/data/xrates/"

    init {
        val logger = HttpLoggingInterceptor()
        logger.level = HttpLoggingInterceptor.Level.BASIC

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logger)

        val gsonBuilder = GsonBuilder()
        gsonBuilder.setLenient()
        val gson = gsonBuilder.create()

        val retrofit = Retrofit.Builder()
                .baseUrl(apiURL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build()

        service = retrofit.create(IExchangeRate::class.java)
    }

    interface IExchangeRate {

        @GET("{coin}/{fiat}/{year}/{month}/{day}/{hour}/{minute}/index.json")
        fun getRate(
                @Path("coin") coinCode: String,
                @Path("fiat") currency: String,
                @Path("year") year: Int,
                @Path("month") month: String,
                @Path("day") day: String,
                @Path("hour") hour: String,
                @Path("minute") minute: String
        ): Flowable<Double>

        @GET("{coin}/{fiat}/{year}/{month}/{day}/index.json")
        fun getRateByDay(
                @Path("coin") coinCode: String,
                @Path("fiat") currency: String,
                @Path("year") year: Int,
                @Path("month") month: String,
                @Path("day") day: String
        ): Flowable<Double>

        @GET("{coin}/{fiat}/index.json")
        fun getLatestRate(
                @Path("coin") coinCode: String,
                @Path("fiat") currency: String
        ): Flowable<Double>

        @GET("index.json")
        fun getCurrencies(): Flowable<List<Currency>>

    }
}
