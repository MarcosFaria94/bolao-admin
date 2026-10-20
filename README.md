# Bolão dos Amigos

App Android do Administrador do bolão. Registra aportes, apostas, apurações, rateios e o extrato do mês. Cada registro vira um **comprovante único** (imagem no estilo do papel térmico dos bancos, com código de autenticação) que vai ao grupo do WhatsApp junto com os anexos originais; no texto da mensagem vai só o título, como nos modelos do termo.

Os dados (integrantes, chaves Pix, saldo, comprovantes) ficam **só no celular**. Este repositório tem apenas o código: nenhum dado pessoal, nem o termo. Os dois arquivos do termo (sem assinaturas e assinado) são anexados pelo Administrador em Ajustes.

## Instalar pela primeira vez

1. No celular, abra a página **Releases** deste repositório e baixe o arquivo `bolao-admin-X.Y.Z.apk` da versão mais recente.
2. Abra o arquivo baixado. O Android vai pedir para permitir a instalação pelo navegador: permita e toque em **Instalar**.
3. Ao abrir o app, faça o cadastro inicial (ou restaure um backup).

## Como funciona

- **Biometria**: o app abre com a digital (ou o bloqueio do celular) e volta a pedir depois de alguns minutos fora dele. A extinção do bolão também pede a biometria.
- **Notificações**: a aba com o sino reúne tudo o que falta fazer (aporte, aposta do dia, comprovante da Caixa, envio de comprovante ao grupo, apuração a confirmar, extrato), a mais recente primeiro; o número vermelho no sino é a quantidade. Tocar numa pendência abre a tela que resolve. No celular chegam só dois avisos: **Ação necessária** e **Aposta premiada** (com o valor).
- **Apuração automática**: depois do sorteio o app confere a aposta na Caixa e, no dia seguinte (a partir das 8h), registra a apuração sozinho quando não há nada a confirmar. O envio do comprovante ao grupo vira uma pendência. Se faltar algum dado (valor de uma faixa, Time do Coração...), a pendência pede a confirmação do Administrador.
- **Conferência com a Caixa**: ao registrar uma aposta, o app lê o comprovante da Caixa (imagem ou PDF, com o reconhecimento de texto do próprio celular) e compara o concurso e os números. Se forem diferentes, a aposta e o comprovante do bolão ficam como na Caixa.
- **Registro que só cresce**: nada é alterado nem desfeito depois de registrado; todo registro pede confirmação.
- **Comprovante único**: os blocos seguem os modelos da Parte 4 do termo; no fim de todo comprovante vêm o REGISTRO e a AUTENTICAÇÃO. O código de autenticação é um SHA-256 encadeado com os comprovantes anteriores, e cada anexo guarda a sua impressão digital. Em Ajustes > **Verificar integridade** o app confere tudo e também confere um código impresso num comprovante.
- **Índices k1 e k2**: as modalidades aparecem em ordem de k1 (retorno matemático esperado de uma aposta simples), calculado com os dados da Caixa e os parâmetros em `app/src/main/assets/parametros_k.json`.
- **Saldo**: tocar no Saldo Líquido gera um documento do saldo para mandar ao grupo; ele não é registrado nem fica guardado.
- **Auditoria**: em Extrato, um PDF com todos os comprovantes do período e os anexos originais, em ordem cronológica.

## Modo de teste

Para conhecer o app (ou experimentar uma versão nova) sem digitar nada e sem mexer nos dados reais: na primeira abertura, ou em Ajustes > **Modo de teste**, toque em **Experimentar com dados de exemplo**.

- O app cria um mês anterior completo (aportes, 10 apostas apuradas com um prêmio, rateio e extrato) e o mês atual até ontem, com o último jogo esperando a apuração (que sai sozinha quando a Caixa responde). Quando a Caixa responde, esse último jogo usa um concurso de verdade.
- Os dados de exemplo ficam numa pasta separada; os reais não são lidos nem alterados. Uma faixa vermelha "Modo de teste" aparece em todas as telas e os comprovantes saem com a faixa "TESTE".
- As mensagens abrem o WhatsApp com a linha *TESTE* no começo. Os avisos no celular começam com "Teste ·".
- **Sair** (na faixa ou em Ajustes) apaga os dados de exemplo e volta aos dados reais.

## Atualizações

Toda alteração enviada para a branch `main` gera uma versão nova automaticamente (GitHub Actions). Ao abrir, o app confere se há versão nova e instala com um toque. Na primeira atualização o Android pede para liberar "Instalar apps desconhecidos" para o app.

## Notificações no HyperOS (Poco/Xiaomi)

Para os avisos chegarem na hora certa: Ajustes do app > ative **Inicialização automática** e, em **Economia de bateria**, escolha **Sem restrições**.

## Backup

Ajustes > **Fazer backup** grava um `.zip` em `Downloads/Bolao` com todos os dados, comprovantes e anexos. Para voltar, use **Restaurar** (ou a opção na primeira abertura do app).

## Build

- Kotlin + Jetpack Compose, `minSdk` 29. Leitura do comprovante da Caixa com o ML Kit (Google Play Services, no celular).
- O APK é assinado no GitHub Actions. A chave fica cifrada em `keystore/bolao.p12.enc`; a senha é o segredo `KEYSTORE_PASSWORD` do repositório. **Guarde essa senha**: sem ela não é possível gerar atualizações que instalem por cima da versão atual.
- O número da versão é a base de `gradle.properties` mais a contagem de commits (ex.: `0.4.8`).
- Fontes dos comprovantes: VT323 e Jost (SIL Open Font License, em `app/src/main/assets/fontes`).
