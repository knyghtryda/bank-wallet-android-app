package bitcoin.wallet.entities.coins.bitcoinCash

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class BitcoinCashUnspentOutput : RealmObject() {

    @PrimaryKey
    var transactionHash: String = ""

    var value: Long = 0

    var index: Long = 0

    var confirmations: Long = 0

    var script: String = ""

}
