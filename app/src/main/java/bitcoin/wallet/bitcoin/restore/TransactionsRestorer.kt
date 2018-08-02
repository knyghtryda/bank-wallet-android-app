package bitcoin.wallet.bitcoin.restore

import io.reactivex.Completable
import org.bitcoinj.wallet.Wallet

class TransactionsRestorer(private val restoredBlocksProvider: RestoredBlocksProvider, private val filteredBlocksProvider: FilteredBlocksProvider, private val walletRestorer: WalletRestorer) {

    fun restore(wallet: Wallet): Completable = restoredBlocksProvider
            .getBlocksForRestore(wallet)
            .flatMap { restoredBlocks ->
                filteredBlocksProvider.getFilteredBlocks(wallet, restoredBlocks)
            }
            .flatMapCompletable { filteredBlocksWithHeights ->
                walletRestorer.restoreTransactionsToWallet(wallet, filteredBlocksWithHeights)
            }

}
