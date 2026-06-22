# Venda Fácil

[![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Versão](https://img.shields.io/badge/versão-1.6.0-155e54)](https://github.com/WellingtonDiasCF/venda-facil-android/releases)
[![Licença](https://img.shields.io/badge/licença-MIT-e59a36)](LICENSE)

Aplicativo Android para organizar produtos, vendas, estoque, clientes e despesas de pequenos negócios. Os dados ficam no próprio aparelho e o uso não depende de cadastro, internet ou servidor externo.

## Recursos

- Cadastro de produtos com preço, custo opcional, ícone e controle de estoque opcional.
- Registro rápido de vendas, descontos, observações e diferentes formas de pagamento.
- Cadastro de clientes e controle de vendas fiadas.
- Recebimentos totais ou parciais, com seleção de várias vendas e filtro por período.
- Histórico de pagamentos por cliente.
- Envio de pendências e histórico diretamente pelo WhatsApp.
- Controle de despesas únicas ou recorrentes.
- Relatórios por período com faturamento, lucro estimado, valores recebidos e pendentes.
- Exportação de relatórios em CSV e compartilhamento pelo WhatsApp.
- Backup completo em arquivo JSON, com importação e restauração no próprio aplicativo.

## Privacidade

O Venda Fácil funciona de forma local. Produtos, vendas, clientes, pagamentos e despesas são armazenados somente no aparelho. Nenhuma informação é enviada automaticamente para serviços externos.

O compartilhamento de relatórios e backups acontece apenas quando o usuário escolhe essa opção.

## Requisitos

- Android 7.0 ou superior (`API 24`).
- Java 17.
- Android SDK 35.

## Compilação

Clone o repositório e execute:

```bash
./gradlew assembleDebug
```

No Windows:

```powershell
.\gradlew.bat assembleDebug
```

O APK de desenvolvimento será criado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Para gerar uma versão de produção, crie um arquivo `keystore.properties` na raiz com as informações da sua chave:

```properties
storeFile=signing/sua-chave.jks
storePassword=sua-senha
keyAlias=seu-alias
keyPassword=sua-senha
```

Depois execute:

```bash
./gradlew assembleRelease
```

O arquivo de propriedades e a pasta de assinatura são ignorados pelo Git.

## Estrutura

```text
app/src/main/java/com/vendafacil/app/
├── MainActivity.java   # Telas e fluxos do aplicativo
├── LocalStore.java     # Persistência local e backup
└── Models.java         # Produtos, vendas, clientes e despesas
```

## Licença

Distribuído sob a licença MIT. Consulte [LICENSE](LICENSE) para mais informações.
