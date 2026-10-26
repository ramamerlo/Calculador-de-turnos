import SwiftUI

struct ContentView: View {
    // MARK: - Estados de Entrada del Usuario
    @State private var fechaReferencia: Date = Date()
    @State private var turnoReferencia: ShiftType = .manana
    @State private var diaDH: WeekdayDH = .martes
    @State private var fechaFutura: Date = Calendar.current.date(byAdding: .day, value: 30, to: Date()) ?? Date()

    // MARK: - Estados de Resultado y Error
    @State private var resultado: ShiftCalculationResult?
    @State private var mensajeError: String?

    var body: some View {
        NavigationStack {
            Form {
                // SECCIÓN 1: Situación Actual de Referencia
                Section {
                    DatePicker(
                        "Fecha de referencia",
                        selection: $fechaReferencia,
                        displayedComponents: [.date]
                    )

                    Picker("Turno en fecha de referencia", selection: $turnoReferencia) {
                        ForEach(ShiftType.ordenMenu) { turno in
                            Label(
                                "\(turno.nombre) (\(turno.horario))",
                                systemImage: turno.icono
                            )
                            .tag(turno)
                        }
                    }

                    Picker("Día fijo de descanso (DH)", selection: $diaDH) {
                        ForEach(WeekdayDH.allCases) { dia in
                            Text(dia.nombre).tag(dia)
                        }
                    }
                } header: {
                    Text("Datos de Referencia (Esquema 6x1)")
                } footer: {
                    Text("Rotación inversa post-DH: Tarde → Mañana → Noche → Vespertino.")
                }

                // SECCIÓN 2: Fecha Futura a Consultar
                Section("Consulta Futura") {
                    DatePicker(
                        "Fecha a consultar",
                        selection: $fechaFutura,
                        in: fechaReferencia...,
                        displayedComponents: [.date]
                    )

                    Button(action: ejecutarCalculo) {
                        HStack {
                            Spacer()
                            Image(systemName: "calendar.badge.clock")
                            Text("Calcular Turno")
                                .fontWeight(.semibold)
                            Spacer()
                        }
                        .padding(.vertical, 6)
                    }
                    .buttonStyle(.borderedProminent)
                    .listRowInsets(EdgeInsets(top: 10, leading: 16, bottom: 10, trailing: 16))
                }

                // SECCIÓN 3: Tarjeta de Resultado Destacado
                if let error = mensajeError {
                    Section("Atención") {
                        Label(error, systemImage: "exclamationmark.triangle.fill")
                            .foregroundStyle(.red)
                    }
                } else if let res = resultado {
                    Section("Resultado del Cálculo") {
                        VStack(spacing: 14) {
                            Image(
                                systemName: res.esDescanso
                                    ? "bed.double.fill"
                                    : (res.turnoAsignado?.icono ?? "clock.fill")
                            )
                            .font(.system(size: 44))
                            .foregroundStyle(res.esDescanso ? .green : .accentColor)

                            Text(
                                res.esDescanso
                                    ? "Día de Descanso (DH)"
                                    : "Turno \(res.turnoAsignado?.nombre ?? "")"
                            )
                            .font(.system(size: 28, weight: .bold, design: .rounded))
                            .multilineTextAlignment(.center)

                            if let turno = res.turnoAsignado, !res.esDescanso {
                                Text("Horario: \(turno.horario)")
                                    .font(.title3.weight(.medium))
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 6)
                                    .background(Color.accentColor.opacity(0.12))
                                    .clipShape(Capsule())
                            }

                            Text(res.mensajePrincipal)
                                .font(.body)
                                .multilineTextAlignment(.center)
                                .foregroundStyle(.secondary)
                                .padding(.top, 2)

                            Divider()

                            HStack {
                                Label("\(res.diasTotales) día(s)", systemImage: "calendar")
                                Spacer()
                                Label("\(res.dhTranscurridos) rotación(es) DH", systemImage: "arrow.counterclockwise")
                            }
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                    }
                }
            }
            .navigationTitle("Calculador de Turnos")
            .environment(\.locale, Locale(identifier: "es_ES"))
            .onChange(of: fechaReferencia) { _, nuevaFechaRef in
                if fechaFutura < nuevaFechaRef {
                    fechaFutura = nuevaFechaRef
                }
            }
        }
    }

    // MARK: - Acción de Cálculo
    private func ejecutarCalculo() {
        do {
            mensajeError = nil
            resultado = try ShiftCalculator.calcularTurno(
                fechaReferencia: fechaReferencia,
                turnoReferencia: turnoReferencia,
                diaDH: diaDH,
                fechaFutura: fechaFutura
            )
        } catch {
            resultado = nil
            mensajeError = error.localizedDescription
        }
    }
}

#Preview {
    ContentView()
}
