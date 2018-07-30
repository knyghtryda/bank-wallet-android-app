package bitcoin.wallet.bitcoin.restore

import bitcoin.wallet.RestoredBlock
import bitcoin.wallet.core.INetworkManager
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.wallet.Wallet
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock

class RestoredBlocksProviderTest {

    private val networkManager = mock(INetworkManager::class.java)
    private val addressProvider = mock(BitcoinAddressProvider::class.java)
    private val wallet = Mockito.mock(Wallet::class.java)
    private val params = Mockito.mock(NetworkParameters::class.java)

    private val restoredBlocksProvider = RestoredBlocksProvider(networkManager, addressProvider)

    @Test
    fun getBlocksForRestore() {
        val addresses = listOf("raddr1", "raddr2", "caddr1", "caddr2")
        val restoredBlocks = listOf(mock(RestoredBlock::class.java))

        val addresses2 = listOf("raddr21", "raddr22", "caddr21", "caddr22")

        whenever(addressProvider.getAddresses(wallet, 0, 100)).thenReturn(addresses)
        whenever(networkManager.getTransactionBlocks(addresses)).thenReturn(Observable.just(restoredBlocks))

        whenever(addressProvider.getAddresses(wallet, 1, 100)).thenReturn(addresses2)
        whenever(networkManager.getTransactionBlocks(addresses2)).thenReturn(Observable.just(listOf()))

        restoredBlocksProvider
                .getBlocksForRestore(wallet)
                .test()
                .assertResult(restoredBlocks)
    }

    @Test
    fun getBlocksForRestore_requestUntilEmptyNetwork() {
        val addresses1 = listOf("raddr11", "raddr12", "caddr11", "caddr12")
        val restoredBlocks1 = listOf(RestoredBlock().apply { hash = "hash1" })

        val addresses2 = listOf("raddr21", "raddr22", "caddr21", "caddr22")
        val restoredBlocks2 = listOf(RestoredBlock().apply { hash = "hash2" })

        val addresses3 = listOf("raddr31", "raddr32", "caddr31", "caddr32")
        val restoredBlocks3 = listOf<RestoredBlock>()

        whenever(addressProvider.getAddresses(wallet, 0, 100)).thenReturn(addresses1)
        whenever(networkManager.getTransactionBlocks(addresses1)).thenReturn(Observable.just(restoredBlocks1))

        whenever(addressProvider.getAddresses(wallet, 1, 100)).thenReturn(addresses2)
        whenever(networkManager.getTransactionBlocks(addresses2)).thenReturn(Observable.just(restoredBlocks2))

        whenever(addressProvider.getAddresses(wallet, 2, 100)).thenReturn(addresses3)
        whenever(networkManager.getTransactionBlocks(addresses3)).thenReturn(Observable.just(restoredBlocks3))

        restoredBlocksProvider
                .getBlocksForRestore(wallet)
                .test()
                .assertResult(restoredBlocks1 + restoredBlocks2)
    }

}
