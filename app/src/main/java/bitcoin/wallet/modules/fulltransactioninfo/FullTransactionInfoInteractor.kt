package bitcoin.wallet.modules.fulltransactioninfo

import bitcoin.wallet.core.IAdapter
import bitcoin.wallet.core.IClipboardManager
import bitcoin.wallet.core.IExchangeRateManager
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem
import io.reactivex.schedulers.Schedulers
import java.util.*

class FullTransactionInfoInteractor(
        private val adapter: IAdapter?,
        private val exchangeRateManager: IExchangeRateManager,
        private val transactionId: String,
        private var clipboardManager: IClipboardManager,
        private val baseCurrency: Currency) : FullTransactionInfoModule.IInteractor {

    private var transactionRecordViewItem: TransactionRecordViewItem? = null
    var delegate: FullTransactionInfoModule.IInteractorDelegate? = null

    override fun retrieveTransaction() {
        adapter?.let { adapter ->
            adapter.transactionRecordsSubject.subscribe {
                updateTransaction(adapter, transactionId)
            }
            updateTransaction(adapter, transactionId)
        }
    }

    private fun updateTransaction(adapter: IAdapter, transactionId: String) {
        val transaction = adapter.transactionRecords.firstOrNull { it.transactionHash == transactionId }
        transaction?.let {
            val viewItem = getTransactionRecordViewItem(it, adapter)
            transactionRecordViewItem = viewItem
            fetchExchangeRate(transaction)
            delegate?.didGetTransactionInfo(viewItem)
        }
    }

    private fun fetchExchangeRate(transaction: TransactionRecord) {
        transaction.timestamp?.let { timestamp ->
            exchangeRateManager.getRate(coinCode = transaction.coinCode, currency = baseCurrency.code, timestamp = timestamp)
                    .subscribeOn(Schedulers.io())
                    .unsubscribeOn(Schedulers.io())
                    .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                    .subscribe { rate ->
                        if (rate > 0) {
                            val value = (transactionRecordViewItem?.amount?.value ?: 0.0) * rate
                            transactionRecordViewItem?.currencyAmount = CurrencyValue(currency = baseCurrency, value = value)
                            transactionRecordViewItem?.exchangeRate = rate
                            transactionRecordViewItem?.let { delegate?.didGetTransactionInfo(it) }
                        }
                    }
        }
    }

    private fun getTransactionRecordViewItem(record: TransactionRecord, adapter: IAdapter): TransactionRecordViewItem {
        return TransactionRecordViewItem(
                hash = record.transactionHash,
                adapterId = adapter.id,
                amount = CoinValue(adapter.coin, record.amount),
                fee = CoinValue(coin = adapter.coin, value = record.fee),
                from = record.from.first(),
                to = record.to.first(),
                incoming = record.amount > 0,
                blockHeight = record.blockHeight,
                date = record.timestamp?.let { Date(it) },
                confirmations = TransactionRecordViewItem.getConfirmationsCount(record.blockHeight, adapter.latestBlockHeight)
        )
    }

    override fun getTransactionInfo() {
        transactionRecordViewItem?.let { delegate?.didGetTransactionInfo(it) }
    }

    override fun onCopyFromAddress() {
        transactionRecordViewItem?.from?.let {
            clipboardManager.copyText(it)
            delegate?.didCopyToClipboard()
        }
    }

    override fun onCopyToAddress() {
        transactionRecordViewItem?.to?.let {
            clipboardManager.copyText(it)
            delegate?.didCopyToClipboard()
        }
    }

    override fun showBlockInfo() {
        transactionRecordViewItem?.let { delegate?.showBlockInfo(it) }
    }

    override fun openShareDialog() {
        transactionRecordViewItem?.let { delegate?.openShareDialog(it) }
    }

    override fun onCopyTransactionId() {
        transactionRecordViewItem?.hash?.let {
            clipboardManager.copyText(it)
            delegate?.didCopyToClipboard()
        }
    }
}
