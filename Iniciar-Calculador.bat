@echo off
chcp 65001 >nul
cd /d "%~dp0"
python calculador_turnos.py
echo.
pause
