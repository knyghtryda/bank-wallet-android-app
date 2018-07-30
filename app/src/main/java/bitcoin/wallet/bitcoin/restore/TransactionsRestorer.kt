package bitcoin.wallet.bitcoin.restore

import org.bitcoinj.wallet.Wallet

class TransactionsRestorer(private val restoredBlocksProvider: RestoredBlocksProvider, private val filteredBlocksProvider: FilteredBlocksProvider, private val walletRestorer: WalletRestorer) {

    fun restore(wallet: Wallet) {
        restoredBlocksProvider
                .getBlocksForRestore(wallet)
                .flatMap { restoredBlocks ->
                    filteredBlocksProvider.getFilteredBlocks(wallet, restoredBlocks)
                }
                .subscribe { filteredBlocksWithHeights ->
                    walletRestorer.restoreTransactionsToWallet(wallet, filteredBlocksWithHeights)
                }
    }

}
