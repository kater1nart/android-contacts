package ru.yandex.practicum.contacts

import android.Manifest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.practicum.contacts.data.models.Contact
import ru.yandex.practicum.contacts.data.models.CountryCode
import ru.yandex.practicum.contacts.data.models.MessagingApp
import ru.yandex.practicum.contacts.data.models.SortOrder
import ru.yandex.practicum.contacts.domain.repository.ContactsRepository
import ru.yandex.practicum.contacts.domain.usecase.GetContactsUseCase
import ru.yandex.practicum.contacts.presentation.main.ContactsScreen
import ru.yandex.practicum.contacts.presentation.main.ContactsViewModel
import ru.yandex.practicum.contacts.presentation.ui.theme.ContactsTheme

@OptIn(ExperimentalPermissionsApi::class)
@RunWith(AndroidJUnit4::class)
class ContactsFiltersTest {
    @get:Rule val compose = createComposeRule()

    private fun showContacts(): ContactsViewModel {
        val repository = object : ContactsRepository {
            override fun getContacts() = flowOf(listOf(
                Contact(1, "Alice", "Zulu", "+49111", listOf(MessagingApp.TELEGRAM)),
                Contact(2, "Bob", "Alpha", "+44222", listOf(MessagingApp.SIGNAL)),
                Contact(3, "Carol", "Bravo", "+33333", listOf(MessagingApp.TELEGRAM)),
                Contact(4, "Dave", "Delta", "+44444", listOf(MessagingApp.WHATS_APP))
            ))
        }
        val viewModel = ContactsViewModel(GetContactsUseCase(repository))
        val permission = object : PermissionState {
            override val permission = Manifest.permission.READ_CONTACTS
            override val status: PermissionStatus = PermissionStatus.Granted
            override fun launchPermissionRequest() = Unit
        }
        compose.setContent { ContactsTheme { ContactsScreen(permission, viewModel) } }
        compose.waitUntil(5_000) { viewModel.contacts.size == 4 }
        return viewModel
    }

    private fun select(menu: String, option: String) {
        compose.onNodeWithContentDescription(menu).performClick()
        compose.onNodeWithText(option).performClick()
    }

    @Test fun countrySelectionPersistsAndCanBeRemovedAfterReopening() {
        val viewModel = showContacts()
        select("Код страны", "United Kingdom")
        select("Код страны", "Germany")
        compose.onNodeWithContentDescription("Код страны").performClick()
        compose.onAllNodes(isOn()).assertCountEquals(2)
        compose.onNodeWithContentDescription("Закрыть").performClick()
        compose.runOnIdle {
            assertEquals(setOf("+44", "+49"), viewModel.selectedCountryCodes.map { it.code }.toSet())
        }
        select("Код страны", "United Kingdom")
        compose.runOnIdle {
            assertEquals(setOf(CountryCode("+49", "Germany")), viewModel.selectedCountryCodes)
        }
        compose.onNodeWithText("Alice Zulu").assertIsDisplayed()
        compose.onNodeWithText("Bob Alpha").assertDoesNotExist()
    }

    @Test fun combinedFiltersAndSortingRetainBothMessengerSelections() {
        val viewModel = showContacts()
        select("Код страны", "United Kingdom")
        select("Код страны", "Germany")
        select("Фильтр", "TELEGRAM")
        select("Фильтр", "SIGNAL")
        compose.onNodeWithContentDescription("Фильтр").performClick()
        compose.onAllNodes(isOn()).assertCountEquals(2)
        compose.onNodeWithContentDescription("Закрыть").performClick()
        compose.runOnIdle {
            assertEquals(listOf("Alice", "Bob"), viewModel.getFilteredAndSortedContacts().map { it.firstName })
        }
        compose.onNodeWithContentDescription("Сортировка").performClick()
        compose.onAllNodes(isToggleable())[1].performClick()
        compose.runOnIdle {
            assertEquals(SortOrder.NAME_DESC, viewModel.sortOrder)
            assertEquals(listOf("Bob", "Alice"), viewModel.getFilteredAndSortedContacts().map { it.firstName })
            assertEquals(setOf(MessagingApp.TELEGRAM, MessagingApp.SIGNAL), viewModel.selectedMessagingApps)
            assertEquals(2, viewModel.selectedCountryCodes.size)
        }
        compose.onNodeWithText("Carol Bravo").assertDoesNotExist()
        compose.onNodeWithText("Dave Delta").assertDoesNotExist()
        select("Фильтр", "SIGNAL")
        compose.runOnIdle {
            assertEquals(listOf("Alice"), viewModel.getFilteredAndSortedContacts().map { it.firstName })
        }
    }
}
