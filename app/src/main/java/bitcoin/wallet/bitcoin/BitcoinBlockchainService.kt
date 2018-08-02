package bitcoin.wallet.bitcoin

import bitcoin.wallet.bitcoin.restore.*
import bitcoin.wallet.blockchain.BlockchainStorage
import bitcoin.wallet.blockchain.IBlockchainService
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.entities.Balance
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.log
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.bitcoinj.core.listeners.DownloadProgressTracker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object BitcoinBlockchainService : IBlockchainService {

    private lateinit var storage: BlockchainStorage
    private lateinit var bitcoinJWrapper: BitcoinJWrapper

    private var updateTransactionsSubject = PublishSubject.create<Map<String, TransactionRecord>>()

    private const val BTC = "BTC"

    private var txs = ConcurrentHashMap<String, TransactionRecord>()

    fun init(bitcoinJWrapper: BitcoinJWrapper, storage: BlockchainStorage) {
        this.storage = storage
        this.bitcoinJWrapper = bitcoinJWrapper
        updateTransactionsSubject.sample(2, TimeUnit.SECONDS).subscribe {
            dequeueTransactionUpdate()
        }
    }

    fun initNewWallet(words: List<String>) {
        updateBalance(0)
        updateBlockHeight(0)
        start(words)

    }

    fun restoreWalletAsync(words: List<String>) {
        updateBalance(0)
        updateBlockHeight(0)

        val wallet = bitcoinJWrapper.getWallet(words)

        val restoredBlocksProvider = RestoredBlocksProvider(Factory.networkManager, BitcoinAddressProvider())
        val filteredBlocksProvider = FilteredBlocksProvider()
        val walletRestorer = WalletRestorer()

        TransactionsRestorer(restoredBlocksProvider, filteredBlocksProvider, walletRestorer)
                .restore(wallet)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe {
                    storage.updateBlockchainSyncing(BTC, true)
                }
                .doFinally {
                    storage.updateBlockchainSyncing(BTC, false)
                }
                .subscribe {
                    updateBalance(wallet.balance.value)
                    updateBlockHeight(wallet.lastBlockSeenHeight.toLong())

                    wallet.getTransactions(true).forEach {
                        enqueueTransactionUpdate(bitcoinJWrapper.newTransactionRecord(wallet, it))
                    }
                    start(words)
                }
    }

    fun start(words: List<String>) {
        bitcoinJWrapper.prepareEnvForWallet(words, object : BitcoinChangeListener {
            override fun onBalanceChange(value: Long) {
                updateBalance(value)
            }

            override fun onNewTransaction(tx: TransactionRecord) {
                enqueueTransactionUpdate(tx)
            }

            override fun onTransactionConfidenceChange(tx: TransactionRecord) {
                enqueueTransactionUpdate(tx)
            }

            override fun onBestChainHeightChange(value: Int) {
                updateBlockHeight(value.toLong())
            }
        })

        bitcoinJWrapper.startAsync(object : DownloadProgressTracker() {
            override fun startDownload(blocks: Int) {
                super.startDownload(blocks)
                storage.updateBlockchainSyncing(BTC, true)
            }

            override fun doneDownload() {
                super.doneDownload()
                storage.updateBlockchainSyncing(BTC, false)
            }
        })
    }

    override fun getReceiveAddress(): String = bitcoinJWrapper.getReceiveAddress()

    override fun sendCoins(address: String, value: Long) = bitcoinJWrapper.sendCoins(address, value)

    private fun updateBalance(balance: Long) {
        storage.updateBalance(Balance().apply {
            code = BTC
            value = balance
        })
    }

    private fun updateBlockHeight(height: Long) {
        storage.updateBlockchainHeight(BTC, height)
    }

    private fun enqueueTransactionUpdate(tx: TransactionRecord) {
        synchronized(txs) {
            txs[tx.transactionHash] = tx
            txs.size.log("Transactions count: ")

            updateTransactionsSubject.onNext(txs)
        }
    }

    private fun dequeueTransactionUpdate() {
        synchronized(txs) {
            val transactionRecords = mutableListOf<TransactionRecord>()
            txs.forEach {
                val tx = it.value

                // collect items for bulk write/update
                transactionRecords.add(tx)

                // remove item from queue
                txs.remove(tx.transactionHash)
            }

            storage.insertOrUpdateTransactions(transactionRecords)
        }
    }

}
