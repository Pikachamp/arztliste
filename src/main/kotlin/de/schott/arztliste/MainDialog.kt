package de.schott.arztliste

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import java.io.File
import java.io.PrintWriter
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.Period
import kotlin.io.path.*

const val OUTPUT_FILE_NAME = "data.csv"

fun main() = application {
    Window(title = "JSON-CSV-Konvertierung", onCloseRequest = ::exitApplication) {
        App()
    }
}

@Preview
@Composable
fun preview() {
    App(
        CsvConverterViewModel(
            inputFile = mutableStateOf(Path("123")),
            outputDirectory = mutableStateOf(Path("123")),
            consultationTypes = mutableStateOf(setOf(ConsultationType.PHONE)),
            phoneNumbersToExclude = mutableStateOf(setOf("123456789"))
        )
    )
}

@Composable
fun App(viewModel: CsvConverterViewModel = CsvConverterViewModel()) {
    var isFileChooserOpen by remember { mutableStateOf(viewModel.fileToChoose != FileToChoose.NONE) }
    var chosenFile by remember { viewModel.inputFile }
    var namesToExclude by remember { viewModel.namesToExclude }
    var phoneNumbersToExclude by remember { viewModel.phoneNumbersToExclude }
    var selectedConsultationTypes by remember { viewModel.consultationTypes }
    var dateRestriction by remember { viewModel.dateRestriction }
    var isDirectoryChooserOpen by remember { mutableStateOf(false) }
    var chosenDirectory by remember { viewModel.outputDirectory }
    var completionStatus by remember { mutableStateOf<String?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    MaterialTheme {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
            Row {
                Button(modifier = Modifier.padding(end = 16.dp), onClick = {
                    viewModel.fileToChoose = FileToChoose.INPUT
                    isFileChooserOpen = true
                }) {
                    Text("Datei auswählen")
                }
                Button(enabled = namesToExclude.isEmpty(), modifier = Modifier.padding(end = 16.dp), onClick = {
                    viewModel.fileToChoose = FileToChoose.NAME_EXCLUSIONS
                    isFileChooserOpen = true
                }) {
                    Text("Namensfilter auswählen")
                }
                Button(enabled = phoneNumbersToExclude.isEmpty(), onClick = {
                    viewModel.fileToChoose = FileToChoose.PHONE_EXCLUSIONS
                    isFileChooserOpen = true
                }) {
                    Text("Telefonnummernfilter auswählen")
                }
            }
            AnimatedVisibility(visible = chosenFile != null) {
                Column {
                    Row {
                        Text(text = "Ausgewählte Datei: ")
                        Text(text = chosenFile?.toAbsoluteStringOrError().orEmpty())
                    }
                    Text(text = "Sprechstunden-Arten:", modifier = Modifier.padding(top = 16.dp))
                    ConsultationType.entries.forEach { type ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = type in selectedConsultationTypes, onCheckedChange = {
                                when (it) {
                                    true -> selectedConsultationTypes += type
                                    false -> selectedConsultationTypes -= type
                                }
                            })
                            Text(text = type.toString())
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { isDropdownExpanded = true }) {
                            Text("Zeitraum beschränken")
                            DropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false }) {
                                TextButton(onClick = {
                                    isDropdownExpanded = false
                                    dateRestriction = null
                                }) { Text("Keine Beschränkung") }
                                TextButton(onClick = {
                                    isDropdownExpanded = false
                                    dateRestriction = Period.ofDays(1)
                                }) { Text("1 Tag") }
                                for (i in 2..7) {
                                    TextButton(onClick = {
                                        isDropdownExpanded = false
                                        dateRestriction = Period.ofDays(i)
                                    }) { Text("$i Tage") }
                                }
                            }
                        }
                        Text(text = when (val days = dateRestriction?.days) {
                            null -> "Keine Beschränkung"
                            1 -> "1 Tag"
                            else -> "$days Tage"
                        }, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
            Button(onClick = {
                isDirectoryChooserOpen = true
            }) {
                Text("Ausgabeordner auswählen")
            }
            AnimatedVisibility(visible = chosenDirectory != null) {
                Row {
                    Text(text = "Ausgewählter Ausgabeordner: ")
                    Text(text = chosenDirectory?.isValidOutputDirectory.orEmpty())
                }
            }
            AnimatedVisibility(visible = chosenFile?.canBeAccessed ?: false) {
                Button(onClick = {
                    try {
                        convert(
                            json = requireNotNull(viewModel.inputFile.value) { "An input file must be selected before starting the conversion process!" },
                            outputPath = viewModel.outputFile,
                            doctorsToExclude = viewModel.namesToExclude.value,
                            phoneNumbersToExclude = viewModel.phoneNumbersToExclude.value,
                            consultationTypeFilter = viewModel.consultationTypes.value,
                            period = viewModel.dateRestriction.value
                        )
                        completionStatus = "Die Konvertierung wurde erfolgreich durchgeführt!"
                    } catch (e: Exception) {
                        completionStatus = "Bei der Konvertierung ist ein Fehler aufgetreten: ${e.message}"
                        PrintWriter(Path("error.log").bufferedWriter()).use {
                            it.println("Time of the error: ${LocalDateTime.now()}")
                            it.println("Exception: $e")
                            it.println("Path to JSON file: ${viewModel.inputFile.value}")
                            it.println("Path to output file: ${viewModel.outputFile}")
                            it.println("Selected consultation types: ${viewModel.consultationTypes.value}")
                            it.println("Exclusions by name: ${viewModel.namesToExclude.value.joinToString()}")
                            it.println("Exclusions by phone number: ${viewModel.phoneNumbersToExclude.value.joinToString()}")
                            it.println("Date restriction: ${viewModel.dateRestriction.value}")
                        }
                    }
                }) {
                    Text(text = "CSV erzeugen")
                }
            }
            AnimatedVisibility(visible = completionStatus != null) {
                Text(text = completionStatus.orEmpty())
            }
        }
        FilePicker(show = isFileChooserOpen) {
            when (viewModel.fileToChoose) {
                FileToChoose.INPUT -> when (val choice = it?.platformFile) {
                    is File -> chosenFile = choice.toPath()
                    null -> it?.let { chosenFile = Path(it.path) }
                    else -> {}
                }

                FileToChoose.NAME_EXCLUSIONS -> when (val choice = it?.platformFile) {
                    is File -> {
                        if (choice.toPath().canBeAccessed) {
                            namesToExclude = choice.readLines().toSet()
                        }
                    }

                    else -> {}
                }

                FileToChoose.PHONE_EXCLUSIONS -> when (val choice = it?.platformFile) {
                    is File -> {
                        if (choice.toPath().canBeAccessed) {
                            phoneNumbersToExclude = choice.readLines().toSet()
                        }
                    }

                    else -> {}
                }

                FileToChoose.NONE -> {}
            }
            isFileChooserOpen = false
            viewModel.fileToChoose = FileToChoose.NONE
        }
        DirectoryPicker(show = isDirectoryChooserOpen) {
            val selection = it?.let { Path(it) }
            chosenDirectory = if (selection != null && selection.exists() && selection.isDirectory()) {
                selection
            } else {
                null
            }
            isDirectoryChooserOpen = false
        }
    }
}

val Path.canBeAccessed
    get() = exists() && isReadable() && isRegularFile()

fun Path.toAbsoluteStringOrError(): String = when {
    !exists() -> "Die ausgewählte Datei existiert nicht, bitte wählen Sie eine andere Datei aus!"
    !isReadable() -> "Auf die ausgewählte Datei kann nicht zugegriffen werden, bitte überprüfen Sie die Dateiberechtigungen!"
    !isRegularFile() -> "Die Auswahl ist keine Datei, bitte wählen Sie eine Datei aus!"
    else -> absolutePathString()
}

val Path.isValidOutputDirectory
    get() = when {
        !isReadable() -> "Auf den ausgewählten Ordner kann nicht zugegriffen werden, bitte überprüfen Sie die Dateiberechtigungen!"
        !isDirectory() -> "Die Auswahl ist keine Datei, bitte wählen Sie eine Datei aus!"
        !isWritable() -> "Die CSV-Datei kann nicht im angegebenen Ordner angelegt werden, bitte überprüfen Sie die Dateiberechtigungen!"
        else -> absolutePathString()
    }

data class CsvConverterViewModel(
    val inputFile: MutableState<Path?> = mutableStateOf(null),
    val outputDirectory: MutableState<Path?> = mutableStateOf(null),
    val consultationTypes: MutableState<Set<ConsultationType>> = mutableStateOf(setOf(ConsultationType.PHONE)),
    val namesToExclude: MutableState<Set<String>> = mutableStateOf(emptySet()),
    val phoneNumbersToExclude: MutableState<Set<String>> = mutableStateOf(emptySet()),
    val dateRestriction: MutableState<Period?> = mutableStateOf(null),
    var fileToChoose: FileToChoose = FileToChoose.NONE
) {
    val outputFile: Path
        get() = outputDirectory.value?.resolve(OUTPUT_FILE_NAME) ?: Path(OUTPUT_FILE_NAME)
}

enum class FileToChoose {
    INPUT, NAME_EXCLUSIONS, PHONE_EXCLUSIONS, NONE
}
