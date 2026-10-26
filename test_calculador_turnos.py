#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Pruebas unitarias para el Calculador de Turnos (Esquema 6x1).
"""

import unittest
from datetime import date

from calculador_turnos import (
    calcular_turno_en_fecha,
    contar_dh_transcurridos,
    validar_dia_dh,
    validar_fecha,
    validar_turno,
)


class TestCalculadorTurnos(unittest.TestCase):
    """Verifica las reglas del negocio, validaciones y rotación cíclica."""

    def test_secuencia_rotacion_hacia_atras(self):
        """
        Verifica el orden de rotación post-DH:
        Tarde -> Mañana -> Noche -> Vespertino -> Tarde
        """
        # Fecha base: Domingo 25/10/2026, DH fijo: Martes (1)
        fecha_ref = date(2026, 10, 25)
        dia_dh = validar_dia_dh("Martes")
        turno_inicial = validar_turno("Tarde")

        # 1. Lunes 26/10/2026 (mismo ciclo, 0 DHs transcurridos) -> Tarde
        res_0 = calcular_turno_en_fecha(fecha_ref, turno_inicial, dia_dh, date(2026, 10, 26))
        self.assertFalse(res_0.es_descanso)
        self.assertEqual(res_0.dh_transcurridos, 0)
        self.assertEqual(res_0.turno_asignado.nombre, "Tarde")

        # 2. Martes 27/10/2026 (día de descanso DH) -> Sin turno
        res_dh = calcular_turno_en_fecha(fecha_ref, turno_inicial, dia_dh, date(2026, 10, 27))
        self.assertTrue(res_dh.es_descanso)
        self.assertIsNone(res_dh.turno_asignado)
        self.assertEqual(
            res_dh.formatear_salida(),
            "El día 27/10/2026 es tu día de descanso (DH).",
        )

        # 3. Miércoles 28/10/2026 (1 DH transcurrido) -> Mañana
        res_1 = calcular_turno_en_fecha(fecha_ref, turno_inicial, dia_dh, date(2026, 10, 28))
        self.assertEqual(res_1.dh_transcurridos, 1)
        self.assertEqual(res_1.turno_asignado.nombre, "Mañana")

        # 4. Miércoles 04/11/2026 (2 DHs transcurridos) -> Noche
        res_2 = calcular_turno_en_fecha(fecha_ref, turno_inicial, dia_dh, date(2026, 11, 4))
        self.assertEqual(res_2.dh_transcurridos, 2)
        self.assertEqual(res_2.turno_asignado.nombre, "Noche")

        # 5. Miércoles 11/11/2026 (3 DHs transcurridos) -> Vespertino
        res_3 = calcular_turno_en_fecha(fecha_ref, turno_inicial, dia_dh, date(2026, 11, 11))
        self.assertEqual(res_3.dh_transcurridos, 3)
        self.assertEqual(res_3.turno_asignado.nombre, "Vespertino")

        # 6. Miércoles 18/11/2026 (4 DHs transcurridos: ciclo completo) -> Tarde
        res_4 = calcular_turno_en_fecha(fecha_ref, turno_inicial, dia_dh, date(2026, 11, 18))
        self.assertEqual(res_4.dh_transcurridos, 4)
        self.assertEqual(res_4.turno_asignado.nombre, "Tarde")

    def test_ejemplo_formato_salida_vespertino(self):
        """
        Verifica los dos formatos exactos de salida solicitados para el 25/11/2026.
        """
        fecha_ref = date(2026, 10, 25)
        fecha_futura = date(2026, 11, 25)  # Miércoles

        # Caso 1: DH Martes, Turno inicial Noche -> tras 5 DHs (5 % 4 = 1 paso) -> Vespertino
        res_turno = calcular_turno_en_fecha(
            fecha_referencia=fecha_ref,
            turno_referencia=validar_turno("Noche"),
            dia_dh=validar_dia_dh("Martes"),
            fecha_futura=fecha_futura,
        )
        self.assertEqual(
            res_turno.formatear_salida(),
            "El día 25/11/2026 te corresponde el Turno Vespertino (17:00 a 23:00).",
        )

        # Caso 2: DH Miércoles -> 25/11/2026 cae exactamente en Miércoles (DH)
        res_descanso = calcular_turno_en_fecha(
            fecha_referencia=fecha_ref,
            turno_referencia=validar_turno("Noche"),
            dia_dh=validar_dia_dh("Miércoles"),
            fecha_futura=fecha_futura,
        )
        self.assertEqual(
            res_descanso.formatear_salida(),
            "El día 25/11/2026 es tu día de descanso (DH).",
        )

    def test_validaciones_de_entradas(self):
        """Comprueba tolerancia a mayúsculas/tildes y detección de fechas inválidas."""
        self.assertEqual(validar_turno("mañana").nombre, "Mañana")
        self.assertEqual(validar_turno("MANANA").nombre, "Mañana")
        self.assertEqual(validar_dia_dh("miercoles"), 2)
        self.assertEqual(validar_dia_dh("Sábado"), 5)

        with self.assertRaises(ValueError):
            validar_fecha("31/02/2026")  # Fecha inexistente

        with self.assertRaises(ValueError):
            validar_fecha("24/10/2026", fecha_referencia=date(2026, 10, 25))  # Pasado


if __name__ == "__main__":
    unittest.main()
