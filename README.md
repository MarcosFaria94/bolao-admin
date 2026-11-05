# Bolão dos Amigos

App Android do Administrador do bolão (de 2 ou mais integrantes) ou de quem joga sozinho (modo individual). Registra aportes, apostas, apurações, rateios e o extrato do mês. Cada registro vira um **comprovante único** (imagem no estilo do papel térmico dos bancos, com código de autenticação) que vai ao grupo do WhatsApp junto com os anexos originais; no texto da mensagem vai só o título, como nos modelos do termo.

Os dados (integrantes, chaves Pix, saldo, comprovantes) ficam **só no celular**. Este repositório tem apenas o código: nenhum dado pessoal, nem o termo. Os dois arquivos do termo (sem assinaturas e assinado) são anexados pelo Administrador em Ajustes.

## Instalar pela primeira vez

1. No celular, abra a página **Releases** deste repositório e baixe o arquivo `bolao-admin-X.Y.Z.apk` da versão mais recente.
2. Abra o arquivo baixado. O Android vai pedir para permitir a instalação pelo navegador: permita e toque em **Instalar**.
3. Ao abrir o app, faça o cadastro inicial (ou restaure um backup): escolha **Bolão** (o grupo, com a quantidade de integrantes, no mínimo 2) ou **Individual**.

## Como funciona

- **Biometria**: o app abre com a digital (ou o bloqueio do celular) e volta a pedir depois de alguns minutos fora dele. Tela dividida, mudar o tamanho da divisão ou girar o celular não pedem de novo. A extinção do bolão também pede a biometria.
- **Abas**: Home, Jogos e Ajustes (ícones grandes na barra de baixo). Deslizar o dedo para os lados troca de aba, em roda: na Home, deslizar para a direita vai para os Ajustes; nos Ajustes, para a esquerda, volta para a Home. Tocar na aba em que já se está volta a lista dela ao começo. Na barra de cima de todas as abas, à direita do título, fica o sino (grande) das **Notificações**.
- **Puxar para baixo**: em qualquer aba (do alto da lista), abre as Notificações; lá, puxar para baixo de novo abre os Comprovantes; nos Comprovantes, puxar para baixo volta para a tela de antes das Notificações. Nos Ajustes, empurrar além do fim da lista abre os **Créditos**. O aviso do puxão mostra o nome da tela que vai abrir (ou para onde volta). É preciso puxar um bom pedaço (um puxão curto, como ao rolar a lista até o começo, não abre nada).
- **Telas que fecham deslizando**: Notificações, Comprovantes, Filtrar, Saldo, Aporte, Rateio, Extrato e as telas dos Ajustes fecham deslizando o dedo para qualquer lado (e voltam para onde se estava; o Saldo volta para a Home). Aporte, Rateio, Extrato, Filtrar, Saldo, Notificações, Comprovantes e as telas dos Ajustes não têm a seta de voltar na barra de cima.
- **Home**: o saldo líquido (com o provisionado e o extra ao lado), os atalhos de Aporte, Rateio e Extrato, a próxima aposta (no dia de aposta, o **Apostar** pulsa até a aposta do dia ser registrada) e o **Histórico** de tudo o que entrou e saiu: a data de cada dia numa etiqueta com uma linha, cada item no seu cartão, com a hora do registro e a situação numa etiqueta colorida. A aposta premiada tem o "Premiada" em dourado; o prêmio diz "Resgatado" (o valor dele em dourado). Na linha do Histórico, o resumo do filtro fica à esquerda do **Filtrar**, que abre uma tela com vários tipos e meses de uma vez e **Limpar filtro**; o padrão é todos os tipos no mês atual, e o filtro volta a ele sozinho ao trocar de tela (as Notificações e os Comprovantes não mexem nele).
- **Notificações**: o sino amarelo ou puxar qualquer aba para baixo. Dois grupos, como os Ajustes: **Pendências**: no alto, se houver, **Esperando comprovante** e **Comprovantes a conferir**, só com o número (o atalho abre os Comprovantes); depois, o que falta fazer (aporte, aposta do dia, aposta a registrar, **Enviar ao grupo**, apuração a confirmar, extrato), a mais recente primeiro, cada uma com o ícone, o título e uma linha. O número vermelho no sino é a quantidade; tocar numa pendência abre a tela que resolve. **Meus jogos**: cada jogo registrado que ainda não foi sorteado (e os que falta registrar) numa linha: a bola da modalidade com o concurso, a aposta, o sorteio e a etiqueta (Sem comprovante, A conferir, Falta registrar); as dezenas ficam na página da aposta. No celular chegam só dois avisos: **Ação necessária** e **Aposta premiada** (sem o valor: o prêmio é surpresa ao abrir a aposta no app).
- **Toque do prêmio principal**: quando uma aposta acerta o prêmio principal (a 1ª faixa; na Dupla Sena, a sena dos dois sorteios), o celular toca como uma chamada: 30 segundos, vibrando, no volume do alarme (mesmo no silencioso). Para pelo **Parar o toque** do aviso, ao abrir o app ou no fim do tempo. Em Ajustes > **Notificações**: liga e desliga, escolhe a música (um arquivo de áudio ou um toque do celular) e **Testar**.
- **Apuração automática**: depois do sorteio (a partir das 21h; aos domingos, do meio-dia) o app confere a aposta na Caixa, de meia em meia hora até o rateio oficial sair, mesmo com o app fechado. A apuração sai sozinha no mesmo dia, assim que a Caixa publica o rateio oficial; no bolão, o envio do comprovante ao grupo vira uma pendência; no modo individual, vem junto o aviso do prêmio. Não há botão de apurar: só quando falta algum dado (valor de uma faixa, Time do Coração...) a pendência pede a confirmação do Administrador.
- **Registro antes do comprovante**: a aposta e o rateio são registrados sem o comprovante (o valor já sai do saldo). Até a conferência eles ainda podem ser descartados (o valor volta). Antes de registrar uma aposta, o app confere na Caixa se o concurso ainda está recebendo apostas; se já foi sorteado, a aposta passa para o próximo (o mesmo vale para as apostas em andamento).
- **Comprovantes** (o atalho das Pendências, ou puxar as Notificações para baixo): escolha de uma vez vários PDFs (os comprovantes da Caixa e os Pix do rateio) e toque em **Registrar comprovantes**. O app lê cada arquivo (reconhecimento de texto do próprio celular) e põe no lugar certo: cada comprovante da Caixa na aposta que espera por ele e cada Pix no integrante do rateio. Se os números do comprovante forem outros, a aposta fica como no comprovante (com o aviso); arquivo repetido é descartado; aposta registrada duas vezes por engano (igual a outra que já tem comprovante) é descartada; Pix com valor diferente da cota é descartado. Depois vem a **Conferência**: **Aprovar todos** cria os comprovantes do bolão, que ficam prontos para enviar ao grupo ali mesmo.
- **Jogos**: a Caixa é consultada de novo a cada vez que se entra na aba (e na página de um jogo), para o concurso nunca ser um que já foi sorteado; se a consulta chega com o volante aberto, o concurso dele acompanha.
- **Página do jogo**: em Jogos, tocar numa modalidade abre a página dela com o prêmio, o sorteio e o volante da aposta (Aposta Nº, o lápis dos dados do concurso e o registro); segurar a modalidade (um pouco mais que um toque longo comum) abre direto os Detalhes. Na barra de cima: **← Voltar** e **Detalhes →** (o último resultado, as regras e as probabilidades, que ficam à direita da página: deslizar o dedo para a esquerda também abre e, para a direita, a página volta como estava). Nos Detalhes, a barra mostra **← Aposta** (volta para o volante) e **Voltar →**. Os dois "Voltar" (e deslizar para a direita na página do jogo, ou para a esquerda nos Detalhes) saem da página, de volta para a tela de onde ela foi aberta (a Home, os Jogos, as Notificações...). Quando a Caixa ainda não divulgou o prêmio estimado ou a data do próximo sorteio (logo depois de um sorteio), aparece **Não divulgado**; a data entra sozinha quando sair.
- **Time do Coração (Timemania)**: a lista dos 80 times, um por linha em ordem alfabética, com o brasão de cada um (buscado na Wikipédia na primeira vez e guardado no celular; sem internet, um escudo genérico no lugar), a UF ao lado e a busca. Tocar em **Nome** ou **UF**, no alto da lista, classifica por aquela coluna; tocar de novo inverte. No fim, dá para digitar outro time.
- **Aposta em andamento**: ao sair da página do jogo, a aposta fica em andamento se o volante estiver completo (todas as dezenas escolhidas); faltando dezena, é descartada sozinha.
- **Registro que só cresce**: nada é alterado nem desfeito depois da conferência; todo registro pede confirmação.
- **Comprovante único**: os blocos seguem os modelos da Parte 4 do termo (Saldo atual, aporte, aposta, apuração, rateio e extrato); no fim de todo comprovante vêm o REGISTRO e a AUTENTICAÇÃO. O app guarda um SHA-256 encadeado com os comprovantes anteriores, e cada anexo guarda a sua impressão digital; no comprovante vai um código curto de 8 algarismos (ex.: 4829-1736). Em Ajustes > **Integridade** o app confere tudo e também confere um código impresso num comprovante.
- **Aposta registrada**: aberta pelo Histórico ou por um aviso, aparece na página do jogo dela: registro, situação e (depois do sorteio) se acumulou; os números com os acertos; a apuração, quando ainda falta; e os dois comprovantes para ver e enviar. O comprovante da apuração traz o resultado (APOSTA PREMIADA ou APOSTA NÃO PREMIADA) e os acertos (ex.: ACERTOS: 3/5, os acertos sobre o total sorteado) e, embaixo do bloco do termo, os números sorteados e o rateio oficial da Caixa (com a faixa da aposta destacada).
- **WhatsApp**: o comprovante vai como imagem, junto com os anexos originais, e o título vai no texto.
- **Datas**: nas telas, ontem, hoje e amanhã aparecem por extenso; nos comprovantes e nas mensagens vai sempre a data.
- **Índices k1 e k2**: as modalidades aparecem em ordem de k1 (retorno matemático esperado de uma aposta simples), calculado com os dados da Caixa, o preço da aposta de Ajustes e os parâmetros em `app/src/main/assets/parametros_k.json`. O concurso em destaque é o de maior k1. Se o preço de Ajustes for diferente do usado nos parâmetros, aparece um aviso em Jogos e em Notificações.
- **Auditoria das regras**: a cada concurso novo, o app refaz o rateio com as regras do `parametros_k.json` e compara com o que a Caixa pagou em cada faixa. Três concursos seguidos fora das regras viram o aviso "Regras da (modalidade) mudaram: os índices podem estar errados". Quando a Caixa muda uma regra, o arquivo guarda a regra antiga até o último concurso em que ela valeu (Mega-Sena: quina e quadra com 10% cada a partir do concurso 3073).
- **Numeração**: aportes, apostas e rateios são numerados no mês (Nº1 = o primeiro do mês), como no termo; a numeração recomeça a cada mês.
- **Saldo**: tocar no Saldo Líquido gera o "Saldo atual" do termo para mandar ao grupo (só a imagem, sem texto); ele não é registrado nem fica guardado.
- **Ajustes**: uma lista em grupos, como os Ajustes do Android: **Bolão** (Integrantes, Aporte e preços, Termo), **Segurança** (Integridade, Auditoria, Backup), **Celular** (Notificações: a apuração automática e o toque do prêmio; Atualização, Modo de teste) e **Sobre** (Créditos, Extinção do Bolão). Cada linha mostra a situação à direita (ex.: "Falta anexar" em vermelho, "Nova versão" em dourado) e abre a sua tela. Em **Créditos**: o app foi criado usando o Claude (tocar abre a página do Claude na Google Play) e a tecnologia usada.
- **Auditoria**: em Ajustes, um PDF com todos os comprovantes do período e os anexos originais, em ordem cronológica.

## Modo individual

Para quem joga sozinho, bem enxuto: só as apostas e os prêmios, sem comprovantes, sem termo, sem aporte, rateio nem extrato, e sem autenticação. O saldo começa em zero, cai a cada aposta (mostra quanto se gastou) e sobe com os prêmios. Qualquer aposta pode ser excluída (com o prêmio dela), e em Ajustes > **Apagar tudo** o app volta ao cadastro a qualquer momento (com a confirmação e a digital).

## Modo de teste

Para conhecer o app (ou experimentar uma versão nova) sem digitar nada e sem mexer nos dados reais: na primeira abertura, ou em Ajustes > **Modo de teste**, toque em **Experimentar com dados de exemplo**.

- O app cria um mês anterior completo (aportes, as apostas dos dias programados apuradas com um ou dois prêmios, às vezes um aporte adicional, rateio e extrato) e o mês atual até ontem, com o último jogo esperando a apuração (que sai sozinha quando a Caixa responde). Quando a Caixa responde, esse último jogo usa um concurso de verdade.
- Cada exemplo sai diferente: a quantidade de integrantes (sem cadastro real), as modalidades, os prêmios, o rateio, os horários e os valores. Às vezes há uma aposta esperando o comprovante e um rateio esperando os Pix: nos Comprovantes, **Usar comprovantes de exemplo** cria os comprovantes (de vez em quando, um com outros números ou repetido) para experimentar a leitura e a conferência.
- Os dados de exemplo ficam numa pasta separada; os reais não são lidos nem alterados. Uma faixa vermelha "Modo de teste" aparece em todas as telas e os comprovantes saem com a faixa "TESTE".
- As mensagens abrem o WhatsApp com a linha *TESTE* no começo. Os avisos no celular começam com "Teste ·".
- **Sair** (na faixa ou em Ajustes) apaga os dados de exemplo e volta aos dados reais.

## Atualizações

Toda alteração enviada para a branch `main` gera uma versão nova automaticamente (GitHub Actions). Ao abrir, o app confere se há versão nova e instala com um toque. Na primeira atualização o Android pede para liberar "Instalar apps desconhecidos" para o app.

## Apuração com o app fechado (HyperOS, Poco/Xiaomi)

O app acorda sozinho na hora do resultado para conferir, apurar e avisar do prêmio. Para o celular não pausar o app: em Ajustes > **Notificações**, permita os avisos, toque em **Liberar** na bateria (sem restrições) e, em **Inicialização automática**, ative o app.

## Backup

Ajustes > **Backup** > **Fazer backup** grava um `.zip` em `Downloads/Bolao` com todos os dados, comprovantes e anexos. Para voltar, use **Restaurar** (ou a opção na primeira abertura do app).

## Build

- Kotlin + Jetpack Compose, `minSdk` 29. Leitura do comprovante da Caixa com o ML Kit (Google Play Services, no celular).
- O APK é assinado no GitHub Actions. A chave fica cifrada em `keystore/bolao.p12.enc`; a senha é o segredo `KEYSTORE_PASSWORD` do repositório. **Guarde essa senha**: sem ela não é possível gerar atualizações que instalem por cima da versão atual.
- O número da versão é a base de `gradle.properties` mais a contagem de commits (ex.: `0.5.9`).
- Fontes dos comprovantes: VT323 e Jost (SIL Open Font License, em `app/src/main/assets/fontes`).
