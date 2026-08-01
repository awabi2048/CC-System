# GUI runtime contract

可逆GUI providerを登録するconsumerは、登録前にCC-Systemの契約版を確認してください。

```kotlin
val api = server.servicesManager.load(CCSystemAPI::class.java)
if (api == null || api.guiRuntimeContractVersion != CCSystemAPI.GUI_RUNTIME_CONTRACT_VERSION) {
    logger.severe("CC-System GUI runtime contract versionが一致しないため、このプラグインを無効化します")
    server.pluginManager.disablePlugin(this)
    return
}
```

契約版2はimmutable reversible state ABIを要求します。旧版向けconsumerを新版CC-Systemと混在させず、上記確認をprovider登録やGUI listener登録より前に実行してください。

restore providerは `context.stateSnapshot` と `context.restoreInteraction` を使用します。`getState(): MenuReversibleProviderState` と `getInteraction(): MenuReversibleInteractionContext` は旧binaryのリンク失敗を防ぐためだけに残しており、旧state実体は復元しません。

snapshot codecは深さ32、ノード4096、単一collection 1024要素、単一文字列65536 UTF-8 byte、正規化後262144 byteを上限とします。超過したcaptureは `INVALID_STATE_DEPTH` または `INVALID_STATE_SIZE` で失敗します。
