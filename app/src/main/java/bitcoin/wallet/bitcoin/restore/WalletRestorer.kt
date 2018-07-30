package bitcoin.wallet.bitcoin.restore

import org.bitcoinj.core.AbstractBlockChain
import org.bitcoinj.core.FilteredBlock
import org.bitcoinj.core.StoredBlock
import org.bitcoinj.wallet.Wallet

class WalletRestorer {

    fun restoreTransactionsToWallet(wallet: Wallet, filteredBlocksWithHeights: Map<Int, FilteredBlock>) {
        filteredBlocksWithHeights.forEach {
            val blockHeight = it.key
            val filteredBlock = it.value
            var relativityOffset = 0

            filteredBlock.associatedTransactions.forEach {
                wallet.receiveFromBlock(it.value, StoredBlock(filteredBlock.blockHeader, filteredBlock.blockHeader.work, blockHeight), AbstractBlockChain.NewBlockType.BEST_CHAIN, relativityOffset++)
            }
        }
    }

}
