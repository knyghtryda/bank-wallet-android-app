package bitcoin.wallet

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class RestoredBlock : RealmObject() {

    @PrimaryKey
    var hash = ""

    var height = 0
    var timestamp = 0L

    override fun toString(): String {
        return hash
    }
}
