package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.blocks.MerkleBlock
import bitcoin.wallet.kit.message.MerkleBlockMessage
import bitcoin.wallet.kit.message.TransactionMessage
import bitcoin.walllet.kit.network.PeerListener
import bitcoin.walllet.kit.network.message.*
import bitcoin.walllet.kit.struct.InvVect
import bitcoin.walllet.kit.struct.Transaction
import java.lang.Exception

class Peer(val host: String, private val listener: PeerListener) : PeerInteraction, PeerConnection.Listener {

    var isFree = true

    private val peerConnection = PeerConnection(host, this)
    private var requestedMerkleBlocks: MutableMap<ByteArray, MerkleBlock?> = mutableMapOf()

    fun start() {
        peerConnection.start()
    }

    fun close() {
        peerConnection.close()
    }

    override fun requestHeaders(headerHashes: Array<ByteArray>, switchPeer: Boolean) {
        peerConnection.sendMessage(GetHeadersMessage(headerHashes))
    }


    override fun requestMerkleBlocks(headerHashes: Array<ByteArray>) {
        requestedMerkleBlocks.plusAssign(headerHashes.map { it to null }.toMap())

        peerConnection.sendMessage(GetDataMessage(InvVect.MSG_FILTERED_BLOCK, headerHashes))
        isFree = false
    }

    override fun relay(transaction: Transaction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onMessage(message: Message) {
        when (message) {
            is PingMessage -> peerConnection.sendMessage(PongMessage(message.nonce))
            is VersionMessage -> peerConnection.sendMessage(VerAckMessage())
            is VerAckMessage -> listener.connected(this)
            is MerkleBlockMessage -> {
                val merkleBlock = message.merkleBlock
                requestedMerkleBlocks[merkleBlock.blockHash] = merkleBlock

                if (merkleBlock.associatedTransactionHashes.isEmpty()) {
                    merkleBlockCompleted(merkleBlock)
                }
            }
            is TransactionMessage -> {
                val transaction = message.transaction

                val merkleBlock = requestedMerkleBlocks.values.filterNotNull().firstOrNull { it.associatedTransactionHashes.contains(transaction.txHash) }
                if (merkleBlock != null) {
                    merkleBlock.addTransaction(transaction)
                    if (merkleBlock.associatedTransactionHashes.size == merkleBlock.associatedTransactions.size) {
                        merkleBlockCompleted(merkleBlock)
                    }
                }
            }
        }
    }

    private fun merkleBlockCompleted(merkleBlock: MerkleBlock) {
        listener.onReceiveMerkleBlock(merkleBlock)
        requestedMerkleBlocks.minusAssign(merkleBlock.blockHash)
        if (requestedMerkleBlocks.isEmpty()) {
            isFree = true
        }
    }

    override fun disconnected(e: Exception?) {
        listener.disconnected(this, e, requestedMerkleBlocks.keys.toTypedArray())
    }

}