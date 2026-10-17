# Bolão Admin

App Android do Administrador do bolão. Registra aportes, apostas, apurações, rateios e o extrato do mês, e monta as mensagens do grupo exatamente no formato do termo do bolão.

Os dados (integrantes, chaves Pix, saldo, comprovantes) ficam **só no celular**. Este repositório tem apenas o código.

## Instalar pela primeira vez

1. No celular, abra a página **Releases** deste repositório e baixe o arquivo `bolao-admin-X.Y.Z.apk` da versão mais recente.
2. Abra o arquivo baixado. O Android vai pedir para permitir a instalação pelo navegador: permita e toque em **Instalar**.
3. Ao abrir o app, faça o cadastro inicial (ou restaure um backup).

## Atualizações

Toda alteração enviada para a branch `main` gera uma versão nova automaticamente (GitHub Actions). Ao abrir, o app confere se há versão nova e instala com um toque. Na primeira atualização o Android pede para liberar "Instalar apps desconhecidos" para o Bolão Admin.

## Notificações no HyperOS (Poco/Xiaomi)

Para os lembretes chegarem na hora certa: Ajustes do app > ative **Inicialização automática** e, em **Economia de bateria**, escolha **Sem restrições**.

## Backup

Ajustes > **Fazer backup** grava um `.zip` em `Downloads/Bolao` com todos os dados e anexos. Para voltar, use **Restaurar** (ou a opção na primeira abertura do app).

## Build

- Kotlin + Jetpack Compose, `minSdk` 29.
- O APK é assinado no GitHub Actions. A chave fica cifrada em `keystore/bolao.p12.enc`; a senha é o segredo `KEYSTORE_PASSWORD` do repositório. **Guarde essa senha**: sem ela não é possível gerar atualizações que instalem por cima da versão atual.
- O número da versão é a contagem de commits (ex.: `0.1.12`).
