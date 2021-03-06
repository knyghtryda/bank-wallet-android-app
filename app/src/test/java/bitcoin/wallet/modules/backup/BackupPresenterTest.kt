package bitcoin.wallet.modules.backup

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test
import org.mockito.Mockito

class BackupPresenterTest {

    private val interactor = Mockito.mock(BackupModule.IInteractor::class.java)
    private val router = Mockito.mock(BackupModule.IRouter::class.java)
    private val view = Mockito.mock(BackupModule.IView::class.java)
    private val toMain = BackupPresenter.DismissMode.TO_MAIN
    private val selfDismiss = BackupPresenter.DismissMode.DISMISS_SELF
    private val presenter = BackupPresenter(interactor, router, toMain)


    @Test
    fun laterDidClick() {
        presenter.laterDidClick()
        verify(router).navigateToMain()
    }

    @Test
    fun showWordsDidClick() {
        presenter.showWordsDidClick()
        verify(interactor).fetchWords()
    }

    @Test
    fun hideWordsDidClick() {
        presenter.view = view
        presenter.hideWordsDidClick()
        verify(view).hideWords()
    }

    @Test
    fun showConfirmationDidClick() {
        presenter.showConfirmationDidClick()
        verify(interactor).fetchConfirmationIndexes()
    }

    @Test
    fun hideConfirmationDidClick() {
        presenter.view = view
        presenter.hideConfirmationDidClick()
        verify(view).hideConfirmation()
    }

    @Test
    fun validateDidClick() {
        presenter.validateDidClick(hashMapOf())
        verify(interactor).validate(any())
    }

    @Test
    fun didFetchWords() {
        val words = listOf("tree", "2", "lemon")
        presenter.view = view

        presenter.didFetchWords(words)
        verify(view).showWords(words)
    }

    @Test
    fun didFetchConfirmationIndexes() {
        val indexes = listOf(1, 3)
        presenter.view = view

        presenter.didFetchConfirmationIndexes(indexes)
        verify(view).showConfirmationWithIndexes(indexes)
    }

    @Test
    fun didValidateSuccess() {
        presenter.didValidateSuccess()
        verify(router).navigateToMain()
    }

    @Test
    fun didValidateSuccess_withSelfDismiss() {
        val presenterWithSelfDismiss = BackupPresenter(interactor, router, selfDismiss)
        presenterWithSelfDismiss.didValidateSuccess()
        verify(router).close()
    }

    @Test
    fun didValidateFailure() {
        presenter.view = view

        presenter.didValidateFailure()
        verify(view).showConfirmationError()
    }
}
