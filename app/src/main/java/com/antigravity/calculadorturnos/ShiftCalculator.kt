package com.antigravity.calculadorturnos

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Representa los 4 turnos fijos de 6 horas, ordenados según la secuencia
 * de rotación inversa ("hacia atrás") posterior a cada Descanso Hebdomadario (DH):
 * Tarde -> Mañana -> Noche -> Vespertino -> Tarde
 */
enum class ShiftType(
    val indiceRotacion: Int,
    val nombre: String,
    val horario: String
) {
    TARDE(0, "Tarde", "11:00 a 17:00"),
    MANANA(1, "Mañana", "05:00 a 11:00"),
    NOCHE(2, "Noche", "23:00 a 05:00"),
    VESPERTINO(3, "Vespertino", "17:00 a 23:00");

    val etiquetaCompleta: String
        get() = "$nombre ($horario)"

    companion object {
        /** Orden cronológico para presentar en el selector desplegable */
        val ORDEN_MENU: List<ShiftType> = listOf(MANANA, TARDE, VESPERTINO, NOCHE)

        fun porIndiceRotacion(indice: Int): ShiftType {
            val normalizado = Math.floorMod(indice, entries.size)
            return entries.first { it.indiceRotacion == normalizado }
        }
    }
}

/**
 * Días de la semana para el Descanso Hebdomadario (DH) fijo,
 * indexados de 0 (Lunes) a 6 (Domingo).
 */
enum class WeekdayDH(val indiceISO: Int, val nombre: String) {
    LUNES(0, "Lunes"),
    MARTES(1, "Martes"),
    MIERCOLES(2, "Miércoles"),
    JUEVES(3, "Jueves"),
    VIERNES(4, "Viernes"),
    SABADO(5, "Sábado"),
    DOMINGO(6, "Domingo")
}

/**
 * Resultado estructurado del cálculo de turno para una fecha futura.
 */
data class ShiftCalculationResult(
    val fechaReferencia: LocalDate,
    val fechaFutura: LocalDate,
    val esDescanso: Boolean,
    val turnoAsignado: ShiftType?,
    val dhTranscurridos: Int,
    val diasTotales: Int
) {
    companion object {
        private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }

    val fechaFuturaFormateada: String
        get() = fechaFutura.format(FORMATO_FECHA)

    /** Título corto destacado para la tarjeta de resultados */
    val tituloDestacado: String
        get() = if (esDescanso || turnoAsignado == null) {
            "¡Es tu día de descanso (DH)!"
        } else {
            "Turno ${turnoAsignado.nombre} (${turnoAsignado.horario})"
        }

    /** Mensaje descriptivo completo según las reglas del negocio */
    val mensajeDetalle: String
        get() = if (esDescanso || turnoAsignado == null) {
            "El día $fechaFuturaFormateada es tu día de descanso (DH)."
        } else {
            "El día $fechaFuturaFormateada te corresponde el Turno ${turnoAsignado.nombre} (${turnoAsignado.horario})."
        }
}

/**
 * Motor matemático de cálculo de turnos para el esquema 6x1 con rotación inversa.
 */
object ShiftCalculator {

    /**
     * Convierte el día de la semana ISO-8601 de Java (`1 = Lunes ... 7 = Domingo`)
     * al índice base cero (`0 = Lunes ... 6 = Domingo`).
     */
    fun obtenerIndiceDiaISO(fecha: LocalDate): Int {
        return fecha.dayOfWeek.value - 1
    }

    /**
     * Normaliza cualquier fecha al primer día de su ciclo laboral de 7 días
     * (el día inmediatamente posterior a su Descanso Hebdomadario anterior).
     *
     * Fórmula matemática:
     * `posicionEnCiclo = (diaSemanaISO - diaDH - 1) mod 7`
     * `inicioCiclo = fecha - posicionEnCiclo días`
     */
    fun obtenerInicioCicloLaboral(fecha: LocalDate, diaDH: WeekdayDH): LocalDate {
        val diaISO = obtenerIndiceDiaISO(fecha)
        val posicionEnCiclo = Math.floorMod(diaISO - diaDH.indiceISO - 1, 7)
        return fecha.minusDays(posicionEnCiclo.toLong())
    }

    /**
     * Calcula la cantidad exacta de cierres de ciclo (DHs transcurridos) entre
     * la fecha de referencia y la fecha futura.
     */
    fun contarDHTranscurridos(
        fechaReferencia: LocalDate,
        fechaFutura: LocalDate,
        diaDH: WeekdayDH
    ): Int {
        val inicioRef = obtenerInicioCicloLaboral(fechaReferencia, diaDH)
        val inicioFut = obtenerInicioCicloLaboral(fechaFutura, diaDH)
        val diasEntreInicios = ChronoUnit.DAYS.between(inicioRef, inicioFut)
        return (diasEntreInicios / 7L).toInt()
    }

    /**
     * Calcula el turno correspondiente en la fecha futura o indica si es día de descanso (DH).
     */
    fun calcularTurno(
        fechaReferencia: LocalDate,
        turnoReferencia: ShiftType,
        diaDH: WeekdayDH,
        fechaFutura: LocalDate
    ): ShiftCalculationResult {
        require(!fechaFutura.isBefore(fechaReferencia)) {
            "La fecha futura a consultar no puede ser anterior a la fecha de referencia."
        }

        val diasTotales = ChronoUnit.DAYS.between(fechaReferencia, fechaFutura).toInt()
        val dhTranscurridos = contarDHTranscurridos(fechaReferencia, fechaFutura, diaDH)

        // Si la fecha futura cae exactamente en el día fijo de DH, es descanso
        val diaFuturaISO = obtenerIndiceDiaISO(fechaFutura)
        if (diaFuturaISO == diaDH.indiceISO) {
            return ShiftCalculationResult(
                fechaReferencia = fechaReferencia,
                fechaFutura = fechaFutura,
                esDescanso = true,
                turnoAsignado = null,
                dhTranscurridos = dhTranscurridos,
                diasTotales = diasTotales
            )
        }

        // Rotación inversa: avanzamos `dhTranscurridos` posiciones sobre la secuencia
        // Tarde -> Mañana -> Noche -> Vespertino (módulo 4)
        val nuevoIndice = turnoReferencia.indiceRotacion + dhTranscurridos
        val turnoAsignado = ShiftType.porIndiceRotacion(nuevoIndice)

        return ShiftCalculationResult(
            fechaReferencia = fechaReferencia,
            fechaFutura = fechaFutura,
            esDescanso = false,
            turnoAsignado = turnoAsignado,
            dhTranscurridos = dhTranscurridos,
            diasTotales = diasTotales
        )
    }
}
