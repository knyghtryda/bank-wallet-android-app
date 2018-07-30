package bitcoin.wallet.bitcoin.restore

import bitcoin.wallet.RestoredBlock
import bitcoin.wallet.core.INetworkManager
import io.reactivex.Observable
import org.bitcoinj.wallet.Wallet

class RestoredBlocksProvider(private val networkManager: INetworkManager, private val addressProvider: BitcoinAddressProvider) {

    fun getBlocksForRestore(wallet: Wallet, page: Int = 0, list: List<RestoredBlock> = listOf()): Observable<List<RestoredBlock>> {
        return networkManager.getTransactionBlocks(addressProvider.getAddresses(wallet, page, 100))
                .flatMap {
                    if (it.isEmpty()) {
                        Observable.just(list)
                    } else {
                        getBlocksForRestore(wallet, page + 1, list + it)
                    }
                }
    }

}