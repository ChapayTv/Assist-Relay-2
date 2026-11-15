# Assist Relay 2

Перехватывает `ACTION_VOICE_COMMAND` (его шлёт LibrePods при долгом нажатии на AirPod),
проигрывает сигнал активации в наушниках и выполняет `su -c "cmd voiceinteraction show"` (нужен root).
Если root недоступен или команда не сработала: `ACTION_ASSIST`, затем выбранное приложение.

Сборка: GitHub Actions (артефакт `assist-relay-apk`) или Android Studio.
