package com.antigravity.calculadorturnos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Paleta monocromática "Total Dark"
private val PureBlack = Color(0xFF000000)
private val DeepGray = Color(0xFF0D0D0D)
private val BorderGray = Color(0xFF333333)
private val MutedGray = Color(0xFF9E9E9E)
private val PureWhite = Color(0xFFFFFFFF)

private val TotalDarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    secondary = PureWhite,
    onSecondary = PureBlack,
    background = PureBlack,
    onBackground = PureWhite,
    surface = DeepGray,
    onSurface = PureWhite,
    surfaceVariant = DeepGray,
    onSurfaceVariant = MutedGray,
    outline = BorderGray
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
        )
        setContent {
            MaterialTheme(colorScheme = TotalDarkColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PureBlack
                ) {
                    ShiftCalculatorScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftCalculatorScreen() {
    val formatoFecha = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    // Estados del formulario
    var fechaReferencia by remember { mutableStateOf(LocalDate.now()) }
    var turnoActual by remember { mutableStateOf(ShiftType.MANANA) }
    var diaDH by remember { mutableStateOf(WeekdayDH.MARTES) }
    var fechaFutura by remember { mutableStateOf(LocalDate.now().plusDays(30)) }

    // Estados de diálogos y menús desplegables
    var mostrarPickerReferencia by remember { mutableStateOf(false) }
    var mostrarPickerFutura by remember { mutableStateOf(false) }
    var expandirMenuTurno by remember { mutableStateOf(false) }
    var expandirMenuDH by remember { mutableStateOf(false) }

    // Estados del resultado
    var resultado by remember { mutableStateOf<ShiftCalculationResult?>(null) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. LOGOTIPO VECTORIAL MINIMALISTA 6x1
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(BorderStroke(1.5.dp, PureWhite), RoundedCornerShape(22.dp))
                .background(PureBlack),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Logotipo Calculador de Turnos",
                modifier = Modifier.size(92.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "CALCULADOR DE TURNOS",
            color = PureWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = "ESQUEMA ROTATIVO 6x1 INVERSO",
            color = MutedGray,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
        )

        // 2. SELECTOR DE FECHA DE REFERENCIA
        MonochromeDateField(
            label = "Fecha de referencia",
            value = fechaReferencia.format(formatoFecha),
            onClick = { mostrarPickerReferencia = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. DROPDOWN: TURNO ACTUAL
        ExposedDropdownMenuBox(
            expanded = expandirMenuTurno,
            onExpandedChange = { expandirMenuTurno = !expandirMenuTurno },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = turnoActual.etiquetaCompleta,
                onValueChange = {},
                readOnly = true,
                label = { Text("Turno actual") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandirMenuTurno)
                },
                colors = monochromeTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expandirMenuTurno,
                onDismissRequest = { expandirMenuTurno = false },
                modifier = Modifier
                    .background(DeepGray)
                    .border(BorderStroke(1.dp, BorderGray), RoundedCornerShape(8.dp))
            ) {
                ShiftType.ORDEN_MENU.forEach { opcion ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = opcion.etiquetaCompleta,
                                color = PureWhite,
                                fontWeight = if (opcion == turnoActual) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            turnoActual = opcion
                            expandirMenuTurno = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. DROPDOWN: DÍA DE DESCANSO (DH) FIJO
        ExposedDropdownMenuBox(
            expanded = expandirMenuDH,
            onExpandedChange = { expandirMenuDH = !expandirMenuDH },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = diaDH.nombre,
                onValueChange = {},
                readOnly = true,
                label = { Text("Día de Descanso (DH) fijo") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandirMenuDH)
                },
                colors = monochromeTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expandirMenuDH,
                onDismissRequest = { expandirMenuDH = false },
                modifier = Modifier
                    .background(DeepGray)
                    .border(BorderStroke(1.dp, BorderGray), RoundedCornerShape(8.dp))
            ) {
                WeekdayDH.entries.forEach { dia ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = dia.nombre,
                                color = PureWhite,
                                fontWeight = if (dia == diaDH) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            diaDH = dia
                            expandirMenuDH = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. SELECTOR DE FECHA FUTURA A CONSULTAR
        MonochromeDateField(
            label = "Fecha futura a consultar",
            value = fechaFutura.format(formatoFecha),
            onClick = { mostrarPickerFutura = true }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 6. BOTÓN PRINCIPAL: BLANCO CON TEXTO NEGRO
        Button(
            onClick = {
                try {
                    mensajeError = null
                    resultado = ShiftCalculator.calcularTurno(
                        fechaReferencia = fechaReferencia,
                        turnoReferencia = turnoActual,
                        diaDH = diaDH,
                        fechaFutura = fechaFutura
                    )
                } catch (e: IllegalArgumentException) {
                    resultado = null
                    mensajeError = e.message
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = PureWhite,
                contentColor = PureBlack
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "CALCULAR",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 7. TARJETA DESTACADA DE RESULTADOS
        if (mensajeError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, PureWhite), RoundedCornerShape(14.dp))
                    .background(DeepGray, RoundedCornerShape(14.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mensajeError ?: "",
                    color = PureWhite,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else if (resultado != null) {
            val res = resultado!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.5.dp, PureWhite), RoundedCornerShape(16.dp))
                    .background(DeepGray, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RESULTADO PARA EL ${res.fechaFuturaFormateada}",
                    color = MutedGray,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = res.tituloDestacado,
                    color = PureWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = res.mensajeDetalle,
                    color = MutedGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${res.diasTotales} día(s) de diferencia",
                        color = MutedGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${res.dhTranscurridos} rotación(es) DH",
                        color = MutedGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // DIÁLOGO DATEPICKER: FECHA DE REFERENCIA
    if (mostrarPickerReferencia) {
        MonochromeDatePickerDialog(
            fechaInicial = fechaReferencia,
            onDateSelected = { nuevaFecha ->
                fechaReferencia = nuevaFecha
                if (fechaFutura.isBefore(nuevaFecha)) {
                    fechaFutura = nuevaFecha
                }
                mostrarPickerReferencia = false
            },
            onDismiss = { mostrarPickerReferencia = false }
        )
    }

    // DIÁLOGO DATEPICKER: FECHA FUTURA
    if (mostrarPickerFutura) {
        MonochromeDatePickerDialog(
            fechaInicial = fechaFutura,
            onDateSelected = { nuevaFecha ->
                fechaFutura = nuevaFecha
                mostrarPickerFutura = false
            },
            onDismiss = { mostrarPickerFutura = false }
        )
    }
}

@Composable
private fun MonochromeDateField(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            colors = monochromeTextFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        // Superposición transparente para capturar el toque en todo el campo
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonochromeDatePickerDialog(
    fechaInicial: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val millisInicial = remember(fechaInicial) {
        fechaInicial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = millisInicial)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val fecha = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onDateSelected(fecha)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("ACEPTAR", color = PureWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = MutedGray)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = DeepGray)
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                containerColor = DeepGray,
                titleContentColor = MutedGray,
                headlineContentColor = PureWhite,
                weekdayContentColor = MutedGray,
                subheadContentColor = PureWhite,
                dayContentColor = PureWhite,
                selectedDayContainerColor = PureWhite,
                selectedDayContentColor = PureBlack,
                todayContentColor = PureWhite,
                todayDateBorderColor = PureWhite
            )
        )
    }
}

@Composable
private fun monochromeTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = PureWhite,
    unfocusedTextColor = PureWhite,
    focusedContainerColor = DeepGray,
    unfocusedContainerColor = DeepGray,
    focusedBorderColor = PureWhite,
    unfocusedBorderColor = BorderGray,
    focusedLabelColor = PureWhite,
    unfocusedLabelColor = MutedGray,
    focusedTrailingIconColor = PureWhite,
    unfocusedTrailingIconColor = PureWhite
)
