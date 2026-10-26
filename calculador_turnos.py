#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Calculador de Turnos (Esquema 6x1 con Rotación Inversa)
-------------------------------------------------------
Programa de línea de comandos para calcular el turno de trabajo de un empleado
en una fecha futura específica, basado en un esquema de rotación cíclica 6x1.
"""

from __future__ import annotations

import argparse
import sys
import unicodedata
from dataclasses import dataclass
from datetime import date, datetime, timedelta


# ============================================================================
# 1. DEFINICIÓN DE CONSTANTES Y REGLAS DEL NEGOCIO
# ============================================================================

@dataclass(frozen=True)
class Turno:
    """Representa un turno de trabajo fijo de 6 horas."""
    nombre: str
    horario: str


# Orden de rotación "hacia atrás" después de cada Descanso Hebdomadario (DH):
# Tarde -> Mañana -> Noche -> Vespertino -> Tarde
# Al ordenar la tupla siguiendo exactamente la secuencia de rotación post-DH,
# cada DH transcurrido incrementa el índice en +1 (módulo 4).
ORDEN_ROTACION_TURNOS: tuple[Turno, ...] = (
    Turno(nombre="Tarde", horario="11:00 a 17:00"),
    Turno(nombre="Mañana", horario="05:00 a 11:00"),
    Turno(nombre="Noche", horario="23:00 a 05:00"),
    Turno(nombre="Vespertino", horario="17:00 a 23:00"),
)

# Orden de presentación cronológico para el menú interactivo
TURNOS_MENU: tuple[Turno, ...] = (
    Turno(nombre="Mañana", horario="05:00 a 11:00"),
    Turno(nombre="Tarde", horario="11:00 a 17:00"),
    Turno(nombre="Vespertino", horario="17:00 a 23:00"),
    Turno(nombre="Noche", horario="23:00 a 05:00"),
)

# Mapeo de días de la semana según el estándar de Python date.weekday()
# donde Lunes = 0, Martes = 1, ..., Domingo = 6.
DIAS_SEMANA: tuple[str, ...] = (
    "Lunes",
    "Martes",
    "Miércoles",
    "Jueves",
    "Viernes",
    "Sábado",
    "Domingo",
)

FORMATO_FECHA = "%d/%m/%Y"


@dataclass(frozen=True)
class ResultadoCalculo:
    """Contiene el resultado detallado del cálculo de turno para una fecha dada."""
    fecha_referencia: date
    fecha_futura: date
    es_descanso: bool
    turno_asignado: Turno | None
    dh_transcurridos: int
    dias_totales: int

    def formatear_salida(self) -> str:
        """Devuelve el mensaje principal con el formato exacto esperado."""
        fecha_str = self.fecha_futura.strftime(FORMATO_FECHA)
        if self.es_descanso or self.turno_asignado is None:
            return f"El día {fecha_str} es tu día de descanso (DH)."
        return (
            f"El día {fecha_str} te corresponde el Turno "
            f"{self.turno_asignado.nombre} ({self.turno_asignado.horario})."
        )


# ============================================================================
# 2. FUNCIONES DE NORMALIZACIÓN Y VALIDACIÓN DE ENTRADAS
# ============================================================================

def normalizar_texto(texto: str) -> str:
    """
    Elimina espacios extremos, convierte a minúsculas y remueve tildes/diacríticos
    (preservando la 'ñ' o permitiendo equivalencia con 'n') para facilitar
    comparaciones flexibles de entradas del usuario.
    """
    limpio = texto.strip().lower()
    descompuesto = unicodedata.normalize("NFD", limpio)
    return "".join(c for c in descompuesto if unicodedata.category(c) != "Mn")


def validar_fecha(fecha_str: str, fecha_referencia: date | None = None) -> date:
    """
    Valida que una cadena tenga el formato DD/MM/AAAA, corresponda a una fecha
    real del calendario y, si se provee fecha_referencia, no sea anterior a ella.
    """
    try:
        fecha = datetime.strptime(fecha_str.strip(), FORMATO_FECHA).date()
    except ValueError as exc:
        raise ValueError(
            f"Fecha inválida '{fecha_str}'. Utiliza el formato DD/MM/AAAA "
            "(ejemplo: 25/11/2026) y asegúrate de que sea una fecha real."
        ) from exc

    if fecha_referencia is not None and fecha < fecha_referencia:
        ref_str = fecha_referencia.strftime(FORMATO_FECHA)
        raise ValueError(
            f"La fecha a consultar ({fecha.strftime(FORMATO_FECHA)}) no puede ser "
            f"anterior a la fecha de referencia ({ref_str})."
        )

    return fecha


def validar_turno(entrada_turno: str) -> Turno:
    """
    Valida y devuelve el objeto Turno correspondiente al nombre o número de opción
    ingresado por el usuario (ej. 'Mañana', 'manana', 'Tarde', '1'..'4').
    """
    clave = normalizar_texto(entrada_turno)

    # Soporte para selección numérica del menú (1 a 4)
    if clave.isdigit():
        indice_menu = int(clave) - 1
        if 0 <= indice_menu < len(TURNOS_MENU):
            nombre_elegido = TURNOS_MENU[indice_menu].nombre
            for turno in ORDEN_ROTACION_TURNOS:
                if turno.nombre == nombre_elegido:
                    return turno

    # Soporte para búsqueda por nombre (con o sin tilde / eñe)
    for turno in ORDEN_ROTACION_TURNOS:
        if normalizar_texto(turno.nombre) == clave:
            return turno

    nombres_validos = ", ".join(t.nombre for t in TURNOS_MENU)
    raise ValueError(
        f"Turno inválido '{entrada_turno}'. Opciones válidas: {nombres_validos}."
    )


def validar_dia_dh(entrada_dia: str) -> int:
    """
    Valida el día de la semana del Descanso Hebdomadario (DH) y devuelve su índice
    entero según el estándar ISO/Python (0 = Lunes ... 6 = Domingo).
    Acepta tanto el nombre del día (con o sin tilde) como su número del 1 al 7.
    """
    clave = normalizar_texto(entrada_dia)

    # Soporte para selección numérica (1 = Lunes ... 7 = Domingo)
    if clave.isdigit():
        numero = int(clave)
        if 1 <= numero <= 7:
            return numero - 1

    # Soporte por nombre del día
    for indice, nombre_dia in enumerate(DIAS_SEMANA):
        if normalizar_texto(nombre_dia) == clave:
            return indice

    dias_validos = ", ".join(DIAS_SEMANA)
    raise ValueError(
        f"Día de la semana inválido '{entrada_dia}'. Opciones válidas: {dias_validos}."
    )


# ============================================================================
# 3. LÓGICA MATEMÁTICA DEL CÁLCULO DE CICLOS Y ROTACIÓN
# ============================================================================

def obtener_inicio_ciclo_laboral(fecha: date, dia_dh: int) -> date:
    """
    Calcula la fecha exacta en que inició el ciclo laboral de 7 días al que
    pertenece `fecha`.

    Explicación matemática:
    -----------------------
    En el esquema 6x1 con DH fijo en `dia_dh` (0=Lunes ... 6=Domingo):
    - Cada ciclo de 7 días comienza el día inmediatamente posterior al DH:
      `dia_inicio_ciclo = (dia_dh + 1) % 7`
    - Consta de 6 días consecutivos de trabajo (posiciones 0 a 5 del ciclo)
      y finaliza en el día de descanso DH (posición 6 del ciclo).
    - Para cualquier fecha, su posición relativa dentro de su ciclo actual (0 a 6)
      se obtiene mediante aritmética modular:
          posicion_en_ciclo = (fecha.weekday() - (dia_dh + 1)) % 7
                            = (fecha.weekday() - dia_dh - 1) % 7
    - Al restar `posicion_en_ciclo` días a `fecha`, normalizamos cualquier fecha
      al primer día de su ciclo laboral (siempre el día siguiente al DH anterior).
    """
    posicion_en_ciclo = (fecha.weekday() - dia_dh - 1) % 7
    return fecha - timedelta(days=posicion_en_ciclo)


def contar_dh_transcurridos(
    fecha_referencia: date,
    fecha_futura: date,
    dia_dh: int,
) -> int:
    """
    Calcula cuántos ciclos laborales (o Descansos Hebdomadarios que provocan
    rotación de turno) separan la fecha de referencia de la fecha futura.

    Explicación matemática:
    -----------------------
    1. Normalizamos `fecha_referencia` y `fecha_futura` al primer día de sus
       respectivos ciclos laborales de 7 días: `inicio_ref` e `inicio_futura`.
    2. Como ambos puntos de anclaje caen en el mismo día de la semana
       (`(dia_dh + 1) % 7`), la diferencia `(inicio_futura - inicio_ref).days`
       es siempre un múltiplo exacto de 7.
    3. Al dividir de forma entera entre 7 (`// 7`), obtenemos exactamente la
       cantidad de cierres de ciclo (DHs cumplidos) entre ambas fechas.
       Cada cierre de ciclo equivale a un desplazamiento en la rotación.
    """
    inicio_ref = obtener_inicio_ciclo_laboral(fecha_referencia, dia_dh)
    inicio_futura = obtener_inicio_ciclo_laboral(fecha_futura, dia_dh)
    return (inicio_futura - inicio_ref).days // 7


def calcular_turno_en_fecha(
    fecha_referencia: date,
    turno_referencia: Turno,
    dia_dh: int,
    fecha_futura: date,
) -> ResultadoCalculo:
    """
    Determina si la fecha futura corresponde a un día de descanso (DH) o calcula
    qué turno le corresponde aplicando la rotación cíclica hacia atrás.
    """
    if fecha_futura < fecha_referencia:
        raise ValueError(
            "La fecha futura no puede ser anterior a la fecha de referencia."
        )

    dias_totales = (fecha_futura - fecha_referencia).days
    dh_transcurridos = contar_dh_transcurridos(fecha_referencia, fecha_futura, dia_dh)

    # Regla 4: Si la fecha futura cae exactamente en el día de la semana definido
    # como DH, no se asigna turno sino que se indica día de descanso.
    if fecha_futura.weekday() == dia_dh:
        return ResultadoCalculo(
            fecha_referencia=fecha_referencia,
            fecha_futura=fecha_futura,
            es_descanso=True,
            turno_asignado=None,
            dh_transcurridos=dh_transcurridos,
            dias_totales=dias_totales,
        )

    # Regla 3: Desplazamiento cíclico "hacia atrás" según los DHs transcurridos.
    # Como ORDEN_ROTACION_TURNOS ya está dispuesto en el sentido de rotación
    # (Tarde -> Mañana -> Noche -> Vespertino -> Tarde), basta con sumar la
    # cantidad de DHs transcurridos y aplicar módulo 4.
    indice_actual = ORDEN_ROTACION_TURNOS.index(turno_referencia)
    nuevo_indice = (indice_actual + dh_transcurridos) % len(ORDEN_ROTACION_TURNOS)
    turno_futura = ORDEN_ROTACION_TURNOS[nuevo_indice]

    return ResultadoCalculo(
        fecha_referencia=fecha_referencia,
        fecha_futura=fecha_futura,
        es_descanso=False,
        turno_asignado=turno_futura,
        dh_transcurridos=dh_transcurridos,
        dias_totales=dias_totales,
    )


# ============================================================================
# 4. INTERFAZ DE LÍNEA DE COMANDOS (INTERACTIVA Y POR ARGUMENTOS)
# ============================================================================

def solicitar_entrada_interactiva(
    mensaje: str,
    validador,
):
    """Solicita un dato por consola hasta que el validador lo acepte sin errores."""
    while True:
        try:
            respuesta = input(mensaje)
            return validador(respuesta)
        except ValueError as err:
            print(f"  [!] Error: {err}\n")


def ejecutar_modo_interactivo() -> None:
    """Ejecuta el asistente interactivo en consola."""
    fecha_actual = date.today()
    nombre_dia_actual = DIAS_SEMANA[fecha_actual.weekday()]

    print("=" * 64)
    print("               CALCULADOR DE TURNOS (ESQUEMA 6x1)               ")
    print("=" * 64)
    print(
        f"Fecha actual de referencia (automática): "
        f"{fecha_actual.strftime(FORMATO_FECHA)} ({nombre_dia_actual})"
    )
    print("-" * 64)

    # 1. Selección del turno en la fecha de referencia
    print("\nTurnos disponibles:")
    for idx, t in enumerate(TURNOS_MENU, start=1):
        print(f"  {idx}. {t.nombre:<11} ({t.horario})")

    turno_ref = solicitar_entrada_interactiva(
        "\nIngresa tu turno actual (nombre o número 1-4): ",
        validar_turno,
    )

    # 2. Selección del día fijo de Descanso Hebdomadario (DH)
    print("\nDías de la semana para Descanso Hebdomadario (DH):")
    for idx, nombre_dia in enumerate(DIAS_SEMANA, start=1):
        print(f"  {idx}. {nombre_dia}")

    dia_dh = solicitar_entrada_interactiva(
        "\nIngresa tu día fijo de descanso DH (Lunes a Domingo o 1-7): ",
        validar_dia_dh,
    )

    if fecha_actual.weekday() == dia_dh:
        print(
            f"  [i] Nota: Hoy ({nombre_dia_actual}) es tu día de descanso (DH). "
            f"Se toma el Turno {turno_ref.nombre} como el del ciclo que cierra hoy."
        )

    # 3. Ingreso de la fecha futura a consultar
    fecha_futura = solicitar_entrada_interactiva(
        "\nIngresa la fecha futura a consultar (DD/MM/AAAA): ",
        lambda texto: validar_fecha(texto, fecha_referencia=fecha_actual),
    )

    # 4. Cálculo y presentación del resultado
    resultado = calcular_turno_en_fecha(
        fecha_referencia=fecha_actual,
        turno_referencia=turno_ref,
        dia_dh=dia_dh,
        fecha_futura=fecha_futura,
    )

    print("\n" + "=" * 64)
    print("RESULTADO:")
    print(f"  {resultado.formatear_salida()}")
    print("-" * 64)
    print(
        f"Detalle del cálculo: {resultado.dias_totales} día(s) de diferencia | "
        f"{resultado.dh_transcurridos} rotación(es) post-DH aplicadas."
    )
    print("=" * 64)


def construir_parser() -> argparse.ArgumentParser:
    """Configura los argumentos opcionales de línea de comandos."""
    parser = argparse.ArgumentParser(
        description=(
            "Calculador de turnos laborales para esquema 6x1 con rotación "
            "inversa post-DH (Tarde -> Mañana -> Noche -> Vespertino -> Tarde)."
        )
    )
    parser.add_argument(
        "-t",
        "--turno",
        help="Turno en la fecha de referencia (Mañana, Tarde, Vespertino, Noche).",
    )
    parser.add_argument(
        "-d",
        "--dh",
        help="Día fijo de Descanso Hebdomadario (Lunes a Domingo).",
    )
    parser.add_argument(
        "-f",
        "--fecha",
        help="Fecha futura a consultar en formato DD/MM/AAAA.",
    )
    parser.add_argument(
        "-r",
        "--fecha-ref",
        help=(
            "Fecha de referencia en formato DD/MM/AAAA "
            "(opcional; por defecto se toma la fecha actual del sistema)."
        ),
    )
    return parser


def main() -> int:
    """Punto de entrada principal del programa."""
    # Configurar salida UTF-8 en consolas de Windows si es posible
    if hasattr(sys.stdout, "reconfigure"):
        try:
            sys.stdout.reconfigure(encoding="utf-8")
        except Exception:
            pass

    parser = construir_parser()
    args = parser.parse_args()

    # Si se pasan argumentos por CLI, ejecutamos en modo directo (no interactivo)
    if args.turno or args.dh or args.fecha:
        if not (args.turno and args.dh and args.fecha):
            parser.error(
                "Para usar el modo directo debes indicar --turno, --dh y --fecha "
                "(o ejecutar sin argumentos para usar el modo interactivo)."
            )
        try:
            fecha_ref = (
                validar_fecha(args.fecha_ref)
                if args.fecha_ref
                else date.today()
            )
            turno_ref = validar_turno(args.turno)
            dia_dh = validar_dia_dh(args.dh)
            fecha_futura = validar_fecha(args.fecha, fecha_referencia=fecha_ref)

            resultado = calcular_turno_en_fecha(
                fecha_referencia=fecha_ref,
                turno_referencia=turno_ref,
                dia_dh=dia_dh,
                fecha_futura=fecha_futura,
            )
            print(resultado.formatear_salida())
            return 0
        except ValueError as err:
            print(f"Error: {err}", file=sys.stderr)
            return 1

    # Sin argumentos: modo interactivo amigable
    try:
        ejecutar_modo_interactivo()
        return 0
    except (KeyboardInterrupt, EOFError):
        print("\nOperación cancelada por el usuario.")
        return 130


if __name__ == "__main__":
    sys.exit(main())
