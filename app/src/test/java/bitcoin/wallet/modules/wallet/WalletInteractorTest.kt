package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.BitcoinAdapter
import bitcoin.wallet.core.IExchangeRateManager
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.CurrencyType
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.atLeast
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@SuppressStaticInitializationFor("bitcoin.wallet.core.ExchangeRateManager")
class WalletInteractorTest {

    private val delegate = mock(WalletModule.IInteractorDelegate::class.java)
    private val adapterManager = mock(AdapterManager::class.java)
    private val exchangeRateManager = mock(IExchangeRateManager::class.java)
    private val bitcoinAdapter = mock(BitcoinAdapter::class.java)
    private val storage = mock(ILocalStorage::class.java)
    private lateinit var interactor: WalletInteractor
    private var coin = Bitcoin()
    private var words = listOf("used", "ugly", "meat", "glad", "balance", "divorce", "inner", "artwork", "hire", "invest", "already", "piano")
    private var wordsHash = words.joinToString(" ")
    private var adapterId: String = "${wordsHash.hashCode()}-${coin.code}"

    private val currency1 = Currency().apply {
        code = "USD"
        symbol = "U+0024"
        name = "US Dollar"
        type = CurrencyType.FIAT
        codeNumeric = 840
    }
    private var exchangeRates = mutableMapOf(Bitcoin() as Coin to CurrencyValue(currency1, 10_000.0))

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor = WalletInteractor(adapterManager, exchangeRateManager, storage)
        interactor.delegate = delegate

        adapterManager.adapters = mutableListOf(bitcoinAdapter)

        whenever(exchangeRateManager.getLatestExchangeRateSubject()).thenReturn(PublishSubject.create())
    }

    @Test
    fun fetchWalletBalances() {
        whenever(exchangeRateManager.getExchangeRates()).thenReturn(exchangeRates)
        whenever(adapterManager.subject).thenReturn(PublishSubject.create<Any>())

        interactor.notifyWalletBalances()

        verify(delegate).didInitialFetch(any(), any(), any())
    }

    @Test
    fun fetchWalletBalances_balanceUpdated() {
        val coin = Bitcoin()
        val newBalanceValue = 3.4
        val balanceSub: PublishSubject<Double> = PublishSubject.create()
        val managerSub: PublishSubject<Any> = PublishSubject.create()

        whenever(exchangeRateManager.getExchangeRates()).thenReturn(exchangeRates)

        whenever(adapterManager.subject).thenReturn(managerSub)
        whenever(adapterManager.adapters).thenReturn(mutableListOf(bitcoinAdapter))

        whenever(bitcoinAdapter.id).thenReturn(adapterId)
        whenever(bitcoinAdapter.coin).thenReturn(coin)
        whenever(bitcoinAdapter.balanceSubject).thenReturn(balanceSub)
        whenever(bitcoinAdapter.balance).thenReturn(0.0)

        interactor.notifyWalletBalances()
        balanceSub.onNext(newBalanceValue)
        val expectedCoinValue = CoinValue(Bitcoin(), newBalanceValue)

        verify(delegate).didUpdate(expectedCoinValue, adapterId)
    }

    @Test
    fun notifyWalletBalances_adapterManagerSubjectUpdate() {
        val managerSub: PublishSubject<Any> = PublishSubject.create()

        whenever(adapterManager.subject).thenReturn(managerSub)
        whenever(exchangeRateManager.getExchangeRates()).thenReturn(exchangeRates)

        interactor.notifyWalletBalances()

        managerSub.onNext(Any())

        verify(delegate, atLeast(2)).didInitialFetch(any(), any(), any())
    }

    @Test
    fun checkIfPinSet_set() {

        whenever(storage.getPin()).thenReturn("123456")

        interactor.checkIfPinSet()

        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun checkIfPinSet_notSet() {

        whenever(storage.getPin()).thenReturn(null)

        interactor.checkIfPinSet()

        verify(delegate).onPinNotSet()
    }

}
