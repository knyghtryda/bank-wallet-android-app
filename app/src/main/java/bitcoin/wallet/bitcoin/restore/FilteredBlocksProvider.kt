package bitcoin.wallet.bitcoin.restore

import bitcoin.wallet.RestoredBlock
import bitcoin.wallet.log
import io.reactivex.Observable
import org.bitcoinj.core.*
import org.bitcoinj.net.discovery.DnsDiscovery
import org.bitcoinj.store.MemoryBlockStore
import org.bitcoinj.wallet.Wallet
import java.util.*

class FilteredBlocksProvider {

    fun getFilteredBlocks(wallet: Wallet, restoredBlocks: List<RestoredBlock>): Observable<Map<Int, FilteredBlock>> {

        return Observable.create<Map<Int, FilteredBlock>> { emitter ->

            val result = mutableMapOf<Int, FilteredBlock>()

            val blockChain = object : BlockChain(wallet.params, wallet, MemoryBlockStore(wallet.params)) {

                override fun add(block: FilteredBlock): Boolean {
                    block.blockHeader.hashAsString.log("Restoring Transactions got Filtered Block")
                    restoredBlocks.firstOrNull { it.hash == block.hash.toString() }?.let { restoredBlock ->
                        result[restoredBlock.height] = block

                        if (result.size == restoredBlocks.size) {
                            emitter.onNext(result)
                            emitter.onComplete()
                        }
                    }

                    return super.add(block)
                }

            }

            val peerGroup = PeerGroup(wallet.params, blockChain)

            peerGroup.addWallet(wallet)
            peerGroup.addPeerDiscovery(DnsDiscovery(wallet.params))
            peerGroup.fastCatchupTimeSecs = Date().time / 1000
            peerGroup.maxConnections = 4

            var xxx = false

            peerGroup.addConnectedEventListener { peer, peerCount ->

                if (!xxx) {
                    val getDataMessage = GetDataMessage(wallet.params)
                    restoredBlocks.forEach {
                        getDataMessage.addFilteredBlock(Sha256Hash.wrap(it.hash))
                    }

                    peer.sendMessage(getDataMessage)

                    xxx = true
                }

            }

            peerGroup.startAsync()

        }
    }

}
