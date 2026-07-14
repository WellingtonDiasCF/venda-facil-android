# Venda Fácil para Android

**Português** · [English](README.en.md) · [Español](README.es.md)

[![Android 7.0+](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)](https://github.com/WellingtonDiasCF/venda-facil-android/releases/latest)
[![Versão 1.6.0](https://img.shields.io/badge/versão-1.6.0-155e54)](https://github.com/WellingtonDiasCF/venda-facil-android/releases/latest)
[![Licença MIT](https://img.shields.io/badge/licença-MIT-e59a36)](LICENSE)

Controle produtos, vendas, estoque, clientes, fiado e despesas diretamente no celular. Não precisa criar conta, contratar servidor ou ficar conectado à internet.

## Baixar o aplicativo

[**Baixar Venda Fácil 1.6.0 — nova instalação**](https://github.com/WellingtonDiasCF/venda-facil-android/releases/download/v1.6.0/VendaFacil-1.6.0.apk)

Quem já usava uma versão de teste deve baixar o [APK de atualização](https://github.com/WellingtonDiasCF/venda-facil-android/releases/download/v1.6.0/VendaFacil-1.6.0-atualizacao.apk) para manter a compatibilidade da assinatura.

Requer Android 7.0 ou superior.

## Instalação

1. Baixe o APK pelo link acima.
2. Abra o arquivo no celular.
3. Se o Android pedir permissão, autorize a instalação de aplicativos para o navegador ou gerenciador de arquivos usado no download.
4. Toque em **Instalar** e depois em **Abrir**.

Baixe somente pela [página oficial de versões](https://github.com/WellingtonDiasCF/venda-facil-android/releases). O aplicativo ainda não está publicado na Play Store.

## O que você consegue fazer

- Cadastrar produtos com preço, custo, ícone e estoque opcional.
- Registrar vendas, descontos, observações e formas de pagamento.
- Controlar vendas fiadas por cliente.
- Receber valores totais ou parciais e consultar o histórico.
- Enviar pendências para o cliente pelo WhatsApp.
- Registrar despesas únicas ou recorrentes.
- Consultar faturamento, lucro estimado, valores recebidos e pendentes.
- Exportar relatórios em CSV.
- Criar e restaurar um backup completo dos dados.

## Primeiros passos

1. Abra **Produtos** e cadastre o que você vende.
2. Use **Vender** para montar a venda e escolher a forma de pagamento.
3. Cadastre o cliente antes de marcar uma venda como fiado.
4. Consulte **Clientes** para registrar recebimentos.
5. Use **Relatórios** para acompanhar o período e gerar CSV.

## Seus dados e privacidade

Produtos, clientes, vendas e despesas ficam armazenados no próprio aparelho. O aplicativo não envia informações automaticamente para servidores externos.

O compartilhamento pelo WhatsApp e a exportação de arquivos só acontecem quando você escolhe essas opções.

## Backup e atualização

Faça um backup antes de trocar de aparelho ou instalar uma atualização importante. Na tela inicial, use **Enviar backup pelo WhatsApp** ou salve o arquivo em outro local seguro. Para recuperar os dados, escolha **Importar e restaurar backup**.

Não desinstale o aplicativo antes de atualizar, pois o Android pode apagar os dados locais. Se você já tinha uma versão de teste, use o APK de atualização indicado na seção de download.

## Dúvidas frequentes

**Funciona sem internet?**

Sim. A internet só é necessária para baixar o APK ou usar recursos externos, como o WhatsApp.

**Preciso pagar ou criar conta?**

Não. O aplicativo funciona localmente e não exige cadastro.

**Posso usar em mais de um celular ao mesmo tempo?**

Cada instalação mantém seus próprios dados. Para levar as informações a outro aparelho, exporte e restaure um backup.

**Onde vejo as mudanças de cada versão?**

Consulte o [histórico de versões](CHANGELOG.md).

<details>
<summary>Informações para desenvolvimento</summary>

### Requisitos

- Java 17
- Android SDK 35

### Compilação

```bash
./gradlew assembleDebug
```

No Windows:

```powershell
.\gradlew.bat assembleDebug
```

O APK será criado em `app/build/outputs/apk/debug/app-debug.apk`.

Para uma versão assinada, crie `keystore.properties` na raiz e execute `assembleRelease`. O arquivo de propriedades e a pasta de assinatura são ignorados pelo Git.

</details>

## Licença

Distribuído sob a [licença MIT](LICENSE).
