package com.moorthy.statussaver.data

import com.moorthy.statussaver.data.model.StatusSourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusFoldersTest {

    @Test
    fun whatsAppFolderOnAndroid11AndLater() {
        assertEquals(
            StatusSourceKind.WHATSAPP,
            StatusFolders.classify("primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
        )
    }

    @Test
    fun whatsAppFolderOnAndroid10AndEarlier() {
        assertEquals(
            StatusSourceKind.WHATSAPP,
            StatusFolders.classify("primary:WhatsApp/Media/.Statuses"),
        )
    }

    @Test
    fun businessFolderOnAndroid11AndLater() {
        assertEquals(
            StatusSourceKind.BUSINESS,
            StatusFolders.classify(
                "primary:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
            ),
        )
    }

    @Test
    fun businessFolderOnAndroid10AndEarlier() {
        assertEquals(
            StatusSourceKind.BUSINESS,
            StatusFolders.classify("primary:WhatsApp Business/Media/.Statuses"),
        )
    }

    @Test
    fun statusesFolderOnAnotherStorageVolumeIsAccepted() {
        assertEquals(
            StatusSourceKind.WHATSAPP,
            StatusFolders.classify("1A2B-3C4D:WhatsApp/Media/.Statuses"),
        )
    }

    @Test
    fun parentOfTheStatusesFolderIsAWrongFolder() {
        assertNull(StatusFolders.classify("primary:Android/media/com.whatsapp/WhatsApp/Media"))
    }

    @Test
    fun folderInsideTheStatusesFolderIsAWrongFolder() {
        assertNull(StatusFolders.classify("primary:WhatsApp/Media/.Statuses/old"))
    }

    @Test
    fun statusesFolderOfAnotherAppIsAWrongFolder() {
        assertNull(StatusFolders.classify("primary:Android/media/com.example/Media/.Statuses"))
    }

    @Test
    fun similarlyNamedFolderIsAWrongFolder() {
        assertNull(StatusFolders.classify("primary:WhatsApp/Media/.Statuses backup"))
    }

    @Test
    fun storageRootIsAWrongFolder() {
        assertNull(StatusFolders.classify("primary:"))
    }

    @Test
    fun pickerOpensAtWhatsAppMediaFolderOnAndroid11() {
        assertEquals(
            "primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
            StatusFolders.initialDocumentId(StatusSourceKind.WHATSAPP, sdkInt = 30),
        )
    }

    @Test
    fun pickerOpensAtWhatsAppRootFolderOnAndroid10() {
        assertEquals(
            "primary:WhatsApp/Media/.Statuses",
            StatusFolders.initialDocumentId(StatusSourceKind.WHATSAPP, sdkInt = 29),
        )
    }

    @Test
    fun pickerOpensAtBusinessMediaFolderOnAndroid11() {
        assertEquals(
            "primary:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
            StatusFolders.initialDocumentId(StatusSourceKind.BUSINESS, sdkInt = 30),
        )
    }

    @Test
    fun pickerOpensAtBusinessRootFolderOnAndroid10() {
        assertEquals(
            "primary:WhatsApp Business/Media/.Statuses",
            StatusFolders.initialDocumentId(StatusSourceKind.BUSINESS, sdkInt = 29),
        )
    }

    @Test
    fun folderThePickerOpensAtIsAcceptedForTheSameApp() {
        for (kind in StatusSourceKind.entries) {
            for (sdkInt in listOf(26, 29, 30, 37)) {
                assertEquals(
                    "kind=$kind sdkInt=$sdkInt",
                    kind,
                    StatusFolders.classify(StatusFolders.initialDocumentId(kind, sdkInt)),
                )
            }
        }
    }
}
