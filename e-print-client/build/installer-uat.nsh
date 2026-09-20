!macro killEPrintProcesses
  DetailPrint "Stopping ${PRODUCT_NAME} background processes..."
  ${nsProcess::KillProcess} "${APP_EXECUTABLE_FILENAME}" $0
  nsExec::Exec `$SYSDIR\taskkill.exe /F /T /IM "${APP_EXECUTABLE_FILENAME}"`
  Pop $0
  Sleep 1000
  ${nsProcess::KillProcess} "${APP_EXECUTABLE_FILENAME}" $0
  nsExec::Exec `$SYSDIR\taskkill.exe /F /T /IM "${APP_EXECUTABLE_FILENAME}"`
  Pop $0
!macroend

!macro customInit
  !insertmacro killEPrintProcesses
!macroend

!macro customUnInit
  !insertmacro killEPrintProcesses
!macroend

!macro customCheckAppRunning
  !insertmacro killEPrintProcesses
!macroend

!macro customUnInstall
  !insertmacro killEPrintProcesses
  DeleteRegValue HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "electron.app.${PRODUCT_NAME}"
  DeleteRegValue HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "electron.app.EPrintClient-UAT"
  DeleteRegValue HKLM "Software\Microsoft\Windows\CurrentVersion\Run" "electron.app.${PRODUCT_NAME}"
  DeleteRegValue HKLM "Software\Microsoft\Windows\CurrentVersion\Run" "electron.app.EPrintClient-UAT"
!macroend
