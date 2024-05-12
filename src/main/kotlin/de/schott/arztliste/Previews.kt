package de.schott.arztliste

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import kotlin.io.path.Path

@Preview
@Composable
fun previewFullDialog() {
    App(
        CsvConverterViewModel(
            inputFile = mutableStateOf(Path("idea.sh")),
            outputDirectory = mutableStateOf(Path("")),
            consultationTypes = mutableStateOf(setOf(ConsultationType.PHONE)),
            phoneNumbersToExclude = mutableStateOf(setOf("123456789"))
        )
    )
}

@Preview
@Composable
fun previewDialogWithErrors() {
    App(
        CsvConverterViewModel(
            inputFile = mutableStateOf(Path("123")),
            outputDirectory = mutableStateOf(Path("123")),
            consultationTypes = mutableStateOf(setOf(ConsultationType.PHONE)),
            phoneNumbersToExclude = mutableStateOf(setOf("123456789"))
        )
    )
}

@Preview
@Composable
fun previewDialogBeforeSelection() {
    App(
        CsvConverterViewModel(
            inputFile = mutableStateOf(null),
            outputDirectory = mutableStateOf(null)
        )
    )
}

@Preview
@Composable
fun previewDateRestrictionDropdown() {
    DateRestrictionDropdown(viewModel = CsvConverterViewModel(dateRestriction = mutableStateOf(null)))
}

@Preview
@Composable
fun previewDateRestrictionDropdownItem() {
    Column {
        DateRestrictionDropdownItem(numberOfDays = null, changeDropdownState = {}) {}
        DateRestrictionDropdownItem(numberOfDays = 0, changeDropdownState = {}) {}
        DateRestrictionDropdownItem(numberOfDays = 1, changeDropdownState = {}) {}
        DateRestrictionDropdownItem(numberOfDays = 2, changeDropdownState = {}) {}
    }
}

@Preview
@Composable
fun previewDirectoryChooserNoChoice() {
    DirectoryChooser(viewModel = CsvConverterViewModel(outputDirectory = mutableStateOf(null)))
}

@Preview
@Composable
fun previewDirectoryChooserChoice() {
    DirectoryChooser(viewModel = CsvConverterViewModel(outputDirectory = mutableStateOf(Path("123"))))
}
