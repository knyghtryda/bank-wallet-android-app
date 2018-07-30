package bitcoin.wallet.bitcoin.restore

import org.bitcoinj.wallet.KeyChain
import org.bitcoinj.wallet.Wallet

class BitcoinAddressProvider {

    fun getAddresses(wallet: Wallet, page: Int, itemsPerPage: Int): List<String> =
            wallet
                    .freshKeys(KeyChain.KeyPurpose.RECEIVE_FUNDS, itemsPerPage)
                    .plus(wallet.freshKeys(KeyChain.KeyPurpose.CHANGE, itemsPerPage))
                    .map { it.toAddress(wallet.params).toBase58() }

}
