import Foundation

// MARK: - Modelos de Dominio

/// Representa los 4 turnos fijos de 6 horas, ordenados según la secuencia
/// de rotación inversa ("hacia atrás") posterior a cada Descanso Hebdomadario (DH):
/// Tarde -> Mañana -> Noche -> Vespertino -> Tarde
enum ShiftType: Int, CaseIterable, Identifiable {
    case tarde = 0
    case manana = 1
    case noche = 2
    case vespertino = 3

    var id: Int { rawValue }

    var nombre: String {
        switch self {
        case .manana: return "Mañana"
        case .tarde: return "Tarde"
        case .vespertino: return "Vespertino"
        case .noche: return "Noche"
        }
    }

    var horario: String {
        switch self {
        case .manana: return "05:00 a 11:00"
        case .tarde: return "11:00 a 17:00"
        case .vespertino: return "17:00 a 23:00"
        case .noche: return "23:00 a 05:00"
        }
    }

    var icono: String {
        switch self {
        case .manana: return "sunrise.fill"
        case .tarde: return "sun.max.fill"
        case .vespertino: return "sunset.fill"
        case .noche: return "moon.stars.fill"
        }
    }

    /// Orden cronológico para mostrar en los selectores de interfaz de usuario
    static let ordenMenu: [ShiftType] = [.manana, .tarde, .vespertino, .noche]
}

/// Días de la semana indexados de 0 (Lunes) a 6 (Domingo), equivalente a Python `date.weekday()`.
enum WeekdayDH: Int, CaseIterable, Identifiable {
    case lunes = 0
    case martes = 1
    case miercoles = 2
    case jueves = 3
    case viernes = 4
    case sabado = 5
    case domingo = 6

    var id: Int { rawValue }

    var nombre: String {
        switch self {
        case .lunes: return "Lunes"
        case .martes: return "Martes"
        case .miercoles: return "Miércoles"
        case .jueves: return "Jueves"
        case .viernes: return "Viernes"
        case .sabado: return "Sábado"
        case .domingo: return "Domingo"
        }
    }
}

/// Resultado estructurado del cálculo de turno para una fecha futura.
struct ShiftCalculationResult {
    let fechaReferencia: Date
    let fechaFutura: Date
    let esDescanso: Bool
    let turnoAsignado: ShiftType?
    let dhTranscurridos: Int
    let diasTotales: Int

    /// Formatea la fecha en DD/MM/AAAA
    var fechaFuturaFormateada: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "es_ES")
        formatter.dateFormat = "dd/MM/yyyy"
        return formatter.string(from: fechaFutura)
    }

    /// Mensaje principal amigable siguiendo el formato de negocio especificado
    var mensajePrincipal: String {
        if esDescanso || turnoAsignado == nil {
            return "El día \(fechaFuturaFormateada) es tu día de descanso (DH)."
        }
        guard let turno = turnoAsignado else { return "" }
        return "El día \(fechaFuturaFormateada) te corresponde el Turno \(turno.nombre) (\(turno.horario))."
    }
}

// MARK: - Motor Matemático de Cálculo (6x1 Inverso)

enum ShiftCalculatorError: LocalizedError {
    case fechaFuturaAnteriorAReferencia

    var errorDescription: String? {
        switch self {
        case .fechaFuturaAnteriorAReferencia:
            return "La fecha futura a consultar no puede ser anterior a la fecha de referencia."
        }
    }
}

struct ShiftCalculator {

    private static var calendario: Calendar = {
        var cal = Calendar(identifier: .gregorian)
        cal.locale = Locale(identifier: "es_ES")
        return cal
    }()

    /// Operador módulo euclidiano positivo.
    /// En Swift el operador `%` puede devolver valores negativos (ej. `-1 % 7 == -1`),
    /// por lo que esta función garantiza siempre un resultado en `0..<(n-1)` igual que en Python.
    private static func mod(_ a: Int, _ n: Int) -> Int {
        let r = a % n
        return r >= 0 ? r : r + n
    }

    /// Convierte el componente `.weekday` de Apple (1 = Domingo ... 7 = Sábado)
    /// al índice ISO usado en la fórmula matemática (0 = Lunes ... 6 = Domingo).
    static func obtenerIndiceDiaISO(para fecha: Date) -> Int {
        let appleWeekday = calendario.component(.weekday, from: fecha) // 1..7
        return mod(appleWeekday + 5, 7) // Lunes(2)->0, ..., Domingo(1)->6
    }

    /// Normaliza cualquier fecha al primer día de su ciclo laboral de 7 días
    /// (el día inmediatamente posterior a su Descanso Hebdomadario anterior).
    ///
    /// Fórmula matemática:
    /// `posicionEnCiclo = (diaSemanaISO - diaDH - 1) mod 7`
    /// `inicioCiclo = fecha - posicionEnCiclo días`
    static func obtenerInicioCicloLaboral(fecha: Date, diaDH: WeekdayDH) -> Date {
        let fechaLimpia = calendario.startOfDay(for: fecha)
        let diaISO = obtenerIndiceDiaISO(para: fechaLimpia)
        let posicionEnCiclo = mod(diaISO - diaDH.rawValue - 1, 7)

        return calendario.date(byAdding: .day, value: -posicionEnCiclo, to: fechaLimpia) ?? fechaLimpia
    }

    /// Calcula la cantidad exacta de cierres de ciclo (DHs transcurridos) entre
    /// la fecha de referencia y la fecha futura.
    static func contarDHTranscurridos(
        fechaReferencia: Date,
        fechaFutura: Date,
        diaDH: WeekdayDH
    ) -> Int {
        let inicioRef = obtenerInicioCicloLaboral(fecha: fechaReferencia, diaDH: diaDH)
        let inicioFut = obtenerInicioCicloLaboral(fecha: fechaFutura, diaDH: diaDH)

        let diasEntreInicios = calendario.dateComponents([.day], from: inicioRef, to: inicioFut).day ?? 0
        return diasEntreInicios / 7
    }

    /// Calcula el turno correspondiente o determina si es día de descanso (DH).
    static func calcularTurno(
        fechaReferencia: Date,
        turnoReferencia: ShiftType,
        diaDH: WeekdayDH,
        fechaFutura: Date
    ) throws -> ShiftCalculationResult {
        let refLimpia = calendario.startOfDay(for: fechaReferencia)
        let futLimpia = calendario.startOfDay(for: fechaFutura)

        if futLimpia < refLimpia {
            throw ShiftCalculatorError.fechaFuturaAnteriorAReferencia
        }

        let diasTotales = calendario.dateComponents([.day], from: refLimpia, to: futLimpia).day ?? 0
        let dhTranscurridos = contarDHTranscurridos(
            fechaReferencia: refLimpia,
            fechaFutura: futLimpia,
            diaDH: diaDH
        )

        // Si la fecha futura cae exactamente en el día fijo de DH, es descanso
        let diaFuturaISO = obtenerIndiceDiaISO(para: futLimpia)
        if diaFuturaISO == diaDH.rawValue {
            return ShiftCalculationResult(
                fechaReferencia: refLimpia,
                fechaFutura: futLimpia,
                esDescanso: true,
                turnoAsignado: nil,
                dhTranscurridos: dhTranscurridos,
                diasTotales: diasTotales
            )
        }

        // Rotación "hacia atrás": como ShiftType.rawValue está ordenado según
        // la secuencia post-DH (Tarde -> Mañana -> Noche -> Vespertino),
        // avanzamos `dhTranscurridos` posiciones módulo 4.
        let totalTurnos = ShiftType.allCases.count
        let nuevoIndice = mod(turnoReferencia.rawValue + dhTranscurridos, totalTurnos)
        let turnoAsignado = ShiftType(rawValue: nuevoIndice) ?? turnoReferencia

        return ShiftCalculationResult(
            fechaReferencia: refLimpia,
            fechaFutura: futLimpia,
            esDescanso: false,
            turnoAsignado: turnoAsignado,
            dhTranscurridos: dhTranscurridos,
            diasTotales: diasTotales
        )
    }
}
