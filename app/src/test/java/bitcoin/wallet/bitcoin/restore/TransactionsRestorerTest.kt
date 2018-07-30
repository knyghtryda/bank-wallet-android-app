package bitcoin.wallet.bitcoin.restore

import bitcoin.wallet.RestoredBlock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.bitcoinj.core.FilteredBlock
import org.bitcoinj.wallet.Wallet
import org.junit.Test
import org.mockito.Mockito.mock

class TransactionsRestorerTest {

    private val restoredBlocksProvider = mock(RestoredBlocksProvider::class.java)
    private val filteredBlocksProvider = mock(FilteredBlocksProvider::class.java)
    private val walletRestorer = mock(WalletRestorer::class.java)

    private val restorer = TransactionsRestorer(restoredBlocksProvider, filteredBlocksProvider, walletRestorer)

    @Test
    fun restore() {
        val wallet = mock(Wallet::class.java)

        val restoredBlocks = listOf(mock(RestoredBlock::class.java))
        val filteredBlock = mock(FilteredBlock::class.java)
        val filteredBlocksWithHeights = mapOf(100 to filteredBlock)

        whenever(restoredBlocksProvider.getBlocksForRestore(wallet)).thenReturn(Observable.just(restoredBlocks))
        whenever(filteredBlocksProvider.getFilteredBlocks(wallet, restoredBlocks)).thenReturn(Observable.just(filteredBlocksWithHeights))

        restorer.restore(wallet)

        verify(walletRestorer).restoreTransactionsToWallet(wallet, filteredBlocksWithHeights)
    }

}
