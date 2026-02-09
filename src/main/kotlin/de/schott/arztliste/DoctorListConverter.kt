package de.schott.arztliste

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.FileNotFoundException
import java.io.Writer
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.*

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("E dd.MM", Locale.GERMAN)
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("kk:mm", Locale.GERMAN)

private val PRIMARY_SORT_CRITERIA: ConsultationType = ConsultationType.PHONE

@OptIn(ExperimentalSerializationApi::class)
private val serializer = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    allowSpecialFloatingPointValues = true
    allowStructuredMapKeys = true
    allowTrailingComma = true
}

@OptIn(ExperimentalSerializationApi::class)
fun convert(json: Path, outputPath: Path, doctorsToExclude: Set<String>, phoneNumbersToExclude: Set<String>, consultationTypeFilter: Set<ConsultationType>, period: Period?) {
    if (!json.exists() || !json.isReadable() || !json.isRegularFile()) {
        throw FileNotFoundException("Could not find or read the input file at path ${json.absolutePathString()}")
    }
    val doctorDtos = json.inputStream().use {
        serializer.decodeFromStream(DoctorListDto.serializer(), it)
    }
    val doctors = doctorDtos.arztPraxisDatas.map { doctorDto ->
        Doctor(
            name = doctorDto.name.orEmpty(),
            contactData = ContactData(
                phone = doctorDto.tel,
                email = doctorDto.email,
                mobile = doctorDto.handy
            ),
            address = Address(
                street = doctorDto.strasse,
                streetNumber = doctorDto.hausnummer,
                zipCode = doctorDto.plz,
                city = doctorDto.ort
            ),
            consultationHours = doctorDto.tsz.filter { it.tszDesTyps.isNotEmpty() }.map { (day, _, consultationHoursDtos) ->
                day.toLocalDate() to consultationHoursDtos.map { (type, timeframes) ->
                    ConsultationHours(
                        type = ConsultationType.parse(from = type),
                        times = timeframes.map { (timeFrame) ->
                            timeFrame.split('-').let { it[0].toLocalTime() to it[1].toLocalTime() }
                        }
                    )
                }
            }
        )
    }
    val filteredDoctors = doctors.filter { doctor ->
        doctor.name !in doctorsToExclude && doctor.contactData.phone !in phoneNumbersToExclude
                && doctor.contactData.mobile !in phoneNumbersToExclude
                && doctor.hasConsultationHoursWithin(period)
    }.map { doctor ->
        doctor.withConsultationHoursWithin(period)
    }.filter { doctor ->
        doctor.consultationHours.any { (_, hours) -> hours.any { it.type in consultationTypeFilter } }
    }.map {  doctor ->
        doctor.copy(consultationHours = doctor.consultationHours.filter { (_, consultations) ->
            consultations.any { it.type in consultationTypeFilter } // remove days without consultations of the desired types
        }.map { hours ->
            hours.copy(second = hours.second.filter { it.type in consultationTypeFilter })
        })
    }
    outputPath.absolute().parent.createDirectories()
    outputPath.bufferedWriter().use { writer ->
        writeToCsv(
            output = writer,
            doctors = filteredDoctors.sortedBy { doctor ->
                doctor.consultationHours.minWithOrNull(
                    compareBy { consultationHours ->
                        consultationHours.second.filter { it.type == PRIMARY_SORT_CRITERIA }.minOrNull()
                    }
                )?.second?.filter { it.type == PRIMARY_SORT_CRITERIA }?.minOrNull() },
            consultationTypeFilter = consultationTypeFilter
        )
    }
}

fun writeToCsv(output: Writer, doctors: Iterable<Doctor>, consultationTypeFilter: Set<ConsultationType>) {
    output.write("Name;Sprechzeiten;Telefonnummer;E-Mail;Mobilfunknummer;Adresse")
    output.write(System.lineSeparator())
    doctors.forEach { doctor ->
        output.write(doctor.name)
        output.write(";")
        doctor.consultationHours.firstOrNull()?.let { (day, consultationHours) ->
            output.write(DATE_FORMATTER.format(day))
            output.write(": ")
            output.write(consultationHours.toCsvCell(filter = consultationTypeFilter))
        }
        output.write(";")
        output.write(doctor.contactData.phone.orEmpty())
        output.write(";")
        output.write(doctor.contactData.email.orEmpty())
        output.write(";")
        output.write(doctor.contactData.mobile.orEmpty())
        output.write(";")
        output.write("${doctor.address.street} ${doctor.address.streetNumber}")
        output.write(System.lineSeparator())
        output.write(";")
        doctor.consultationHours.getOrNull(1)?.let { (day, consultationHours) ->
            output.write(DATE_FORMATTER.format(day))
            output.write(": ")
            output.write(consultationHours.toCsvCell(filter = consultationTypeFilter))
        }
        output.write(";;;;")
        output.write("${doctor.address.zipCode} ${doctor.address.city}")
        output.write(System.lineSeparator())
        doctor.consultationHours.drop(2).forEach { (day, consultationHours) ->
            output.write(";")
            output.write(DATE_FORMATTER.format(day))
            output.write(": ")
            output.write(consultationHours.toCsvCell(filter = consultationTypeFilter))
            output.write(";;;;")
            output.write(System.lineSeparator())
        }
    }
}

fun List<ConsultationHours>.toCsvCell(filter: Set<ConsultationType>): String = joinToString(separator = " ") {
    when(filter.size) {
        1 -> it.times.joinToString(separator = " ") { (start, end) ->
            "${TIME_FORMATTER.format(start)} - ${TIME_FORMATTER.format(end)}"
        }

        else -> "${it.type}: ${
            it.times.joinToString(separator = " ") { (start, end) ->
                "${TIME_FORMATTER.format(start)} - ${TIME_FORMATTER.format(end)}"
            }
        }"
    }
}

fun String.toLocalDate(): LocalDate = split('.').let { LocalDate.of(LocalDate.now().year, it[1].toInt(), it[0].toInt()) }

fun String.toLocalTime(): LocalTime = split(":").let { LocalTime.of(it[0].toInt(), it[1].toInt()) }

@JvmRecord
data class Doctor(
    val name: String,
    val contactData: ContactData = ContactData(),
    val address: Address = Address(),
    val consultationHours: List<Pair<LocalDate, List<ConsultationHours>>> = emptyList()
) {
    fun hasConsultationHoursWithin(period: Period?): Boolean = when (period) {
        null -> consultationHours.any { (date, _) -> date >= LocalDate.now() }
        else -> consultationHours.any { (date, _) -> date >= LocalDate.now() && date <= LocalDate.now() + period  }
    }

    fun withConsultationHoursWithin(period: Period?): Doctor = copy(consultationHours = when (period) {
        null -> consultationHours.filter { (date, _) -> date >= LocalDate.now() }
        else -> consultationHours.filter { (date, _) -> date >= LocalDate.now() && date <= LocalDate.now() + period }
    })
}

@JvmRecord
data class ContactData(
    val phone: String? = null,
    val email: String? = null,
    val mobile: String? = null
)

@JvmRecord
data class Address(
    val street: String? = null,
    val streetNumber: String? = null,
    val zipCode: String? = null,
    val city: String? = null
)

@JvmRecord
data class ConsultationHours(
    val type: ConsultationType,
    val times: List<Pair<LocalTime, LocalTime>>
) : Comparable<ConsultationHours> {
    override fun compareTo(other: ConsultationHours): Int {
        val earliestAvailability = times.minWithOrNull(compareBy<Pair<LocalTime, LocalTime>> { it.first }.thenBy { it.second })
        val earliestAvailabilityOfOther = other.times.minWithOrNull(compareBy<Pair<LocalTime, LocalTime>> { it.first }.thenBy { it.second })
        return when {
            earliestAvailabilityOfOther == null -> -1
            earliestAvailability == null -> 1
            earliestAvailability.first.compareTo(earliestAvailabilityOfOther.first) != 0 -> earliestAvailability.first.compareTo(earliestAvailabilityOfOther.first)
            else -> earliestAvailability.second.compareTo(earliestAvailabilityOfOther.second)
        }
    }
}

enum class ConsultationType(private val text: String) {
    PHONE("Telefonische Erreichbarkeit"),
    REGULAR("Sprechstunde"),
    APPOINTMENT("Sprechstunde mit Termin"),
    WITHOUT_APPOINTMENT("Sprechstunde ohne Termin"),
    OPEN("Offene Sprechstunde");

    override fun toString(): String = text

    companion object {
        fun parse(from: String): ConsultationType = when (from) {
            PHONE.text -> PHONE
            REGULAR.text -> REGULAR
            APPOINTMENT.text -> APPOINTMENT
            WITHOUT_APPOINTMENT.text -> WITHOUT_APPOINTMENT
            OPEN.text -> OPEN
            else -> throw IllegalArgumentException("Unknown ConsultationType: $from")
        }
    }
}

@JvmRecord
@Serializable
data class DoctorListDto(
    val arztPraxisDatas: List<DoctorDataDto> = emptyList()
)

@JvmRecord
@Serializable
data class DoctorDataDto(
    val keineSprechzeiten: Boolean,
    val name: String? = null,
    val tel: String? = null,
    val handy: String? = null,
    val email: String? = null,
    val strasse: String? = null,
    val hausnummer: String? = null,
    val plz: String? = null,
    val ort: String? = null,
    val tsz: List<ConsultationDaysDto> = emptyList()
)

@JvmRecord
@Serializable
data class ConsultationDaysDto(
    val d: String,
    val t: String,
    val tszDesTyps: List<ConsultationHoursDto> = emptyList()
)

@JvmRecord
@Serializable
data class ConsultationHoursDto(
    val typ: String,
    val sprechzeiten: List<TimeFrameDto> = emptyList()
)

@JvmRecord
@Serializable
data class TimeFrameDto(
    val zeit: String
)
