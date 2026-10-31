# YSpan — Preview

> **Preview / 開発途中のプレビュー版です。安定版ではありません。**
> 現在の動作確認対象はFabric 1.16.5とPaper 26.3の組み合わせです。
> 今後、対応バージョンと機能を段階的に拡張する予定です。実装時期や全バージョン対応を保証するものではありません。

**English:** This is an early preview, not a stable release. The current prototype targets Fabric 1.16.5 with Paper 26.3. Broader version support and functionality are planned; availability and timelines are not guaranteed.

**YSpanは、専用Mod＋サーバープラグインで、1.16.5から新しいサーバーのY=0未満を表示・移動・操作するための試作品です。ローカルの実クライアントで動作を確認しています。**

全バージョン対応版や完成した互換プラグインではありません。実運用サーバーには導入していません。

## 検証済みの組み合わせ

| 項目 | 検証環境 |
|---|---|
| クライアント | Minecraft Java 1.16.5 / Fabric Loader 0.16.14 |
| クライアント実行環境 | Apple Silicon macOS / Java 21.0.7 |
| サーバー | Paper 26.3 build 230 / Java 25.0.1 |
| 互換プラグイン | ViaVersion 5.12.2 / ViaBackwards 5.12.2 |
| テスト接続 | 127.0.0.1:25576 / 架空ユーザーBZProbe |

開発用のクラスだけでなく、`release/` と同じ配布JARを通常のFabricモード（development=false）で読み込んで確認しました。

## 実測した結果

Y=-64は床ブロックとして表示・当たり判定を確認しています。その床の上の足元Y=-63での検証であり、足元Y=-64そのものの移動は未検証です。Y=-65以下は対応範囲外です。

- Y=-2で床を表示し、床の当たり判定を使って立ち、約1.90ブロック横に歩けた。
- Y=-2の金ブロックを標準の破壊パケットで壊し、標準の設置パケットで丸石を置けた。サーバー記録とクライアント表示の両方で一致。
- Y=-63（床はY=-64）、Y=0、Y=256、Y=318にそれぞれ立ち、床の表示・衝突を確認。
- エンドへ移動してから地下へ戻っても、地下データを再取得できた。
- Y=-3にある実際のエンドポータルへ歩いて入り、エンドへ到着。サーバーのPlayerPortalEventはEND_PORTAL、出発位置Y=-2.464159…を記録。
- 専用のポータル描画用オブジェクトも生成し、ポータル模様が表示されることを画像で確認。
- 同じワールド・同じ床で補助機能を無効にすると、クライアントは床をvoid_airと扱い、床が黒く消える現象が再現。

証拠は `evidence/final-packaged-client.log`、`final-packaged-server.log`、`probe-records.jsonl`、画像に保存しています。`verification.json` は `python3 verify-evidence.py` で実ログから照合できます。初期試験の失敗・試験条件の修正ログは区別して保存し、最終成功の証拠には使用しません。

## 方式

サーバーは通常のViaBackwards転送に加えて、専用通信 `arils:belowzero` でY<0とY>=256のセクションと明るさを送ります。ブロック状態はViaBackwardsのマッピングで1.16.5向けに変換します。

Modは不足セクションを別のメモリ領域に保持し、1.16.5のブロック参照・衝突・描画グリッド・照明・高さに由来する霧を補います。**座標の偽装や、ワールド全体の持ち上げは行いません。** 実際のマイナス座標を維持します。破壊・設置・移動は標準のMinecraftパケットを使い、サーバー側の通常処理が判定します。

## プレビュー版のダウンロード

配布ファイルは [GitHub Releases](https://github.com/atemoyacode-sudo/YSpan/releases) にあります。現在の公開版は **v0.1.0-preview.1** です。

YSpanは公開プロジェクト名です。検証済みのJARは初期試作時のファイル名・内部ID・版番号（`0.1.0-experimental`）を維持しています。名称の変更に合わせた再ビルドは行っていません。

## ファイル

- `release/below-zero-client-0.1.0-experimental.jar`：友達側のFabric 1.16.5用Mod。Fabric APIは不要。
- `release/BelowZeroBridge-0.1.0-experimental.jar`：Paper 26.3用プラグイン。ViaVersion/ViaBackwardsが必要。
- `client/src/`、`server/src/`：ソース。
- `evidence/`：実クライアント・サーバーの試験ログとスクリーンショット。

片方だけ入れても地下対応は完成しません。通常はサーバーの応答があるまでModの高さ拡張は無効です。クライアント切断時に補助状態を解除します。

## 導入する場合の条件

この実験では本番導入・友達のクライアント操作を行っていません。友達の通常環境とは別のFabric 1.16.5プロファイルと、バックアップしたテストサーバーで先に検証してください。友達側のゲームディレクトリの `mods` にクライアントJAR、サーバーの `plugins` にプラグインJARを置く構成です。Paperプラグイン側はサーバー再起動が必要です。`/reload` は使いません。

**配布JARのデフォルトではテスト地形生成は無効（test-fixture=false）です。** テストコードはループバック接続と専用ワールド名の両方が一致する場合だけ地形を作成します。実行用のオフライン設定はローカル検証フォルダだけにあり、本番サーバーの設定ではありません。

## 今後の拡張予定

- 対応するMinecraftクライアント・環境の拡大。
- 地下・上空の描画、照明、ブロックエンティティ同期の改善。
- 高速移動、複数人、長時間運用時の挙動と性能の改善。
- 導入手順と設定の整備。

以下の未対応項目は今後の検討対象です。対応済みと誤解しないでください。

## 未確認・制約

- **1.16.5専用。** 全Javaバージョン・Bedrock対応は実装していません。
- Lunar、Forge、OptiFine、Sodium等との組み合わせ、Windows/Linux、友達の実環境は未確認。
- 実運用サーバーのPaper build 223上、実際の要塞、複数人、長時間運用、Raspberry Pi上の負荷は未検証。
- 地下・上空の補助送信範囲は現在半径2チャンク。バニラの描画距離全体を保証しません。
- サーバーから未受信のセクションは空気となるため、受信前の高速移動では一時的な表示・予測のずれが起こり得ます。
- ブロックエンティティは描画用の初期オブジェクトを作りますが、看板の文章や特殊NBTの完全同期は未実装。
- 流体、レッドストーン、ピストン、乗り物、Mob、特殊ブロックの挙動の網羅試験は行っていません。
- 一般的なブロック変化は巡回更新、破壊・設置はイベント後の優先再送です。設定のrefresh-ticksは予約項目で、この試作では使用していません。
- 1.16.5の内部処理全体を現行版相当にしたものではなく、ここに挙げた試験範囲の実証です。

## ビルドと検証記録

クライアントは `client/` のGradle wrapperを使用します。JDK 21で `./gradlew build` を実行するとMod JARを生成します。通常の `runClient` は自動接続や自動操作を行いません。`-PbelowzeroProbe` はローカルの専用検証環境だけで使う明示的なテストオプションです。

サーバー側のコンパイルにはJDK 25、Paper API 26.3.build.230-stable、ViaVersion 5.12.2、Paperの依存ライブラリが必要です。これらは別途取得してください。

```sh
python3 build-server.py --paper-api /path/to/paper-api.jar --viaversion /path/to/ViaVersion.jar --libraries /path/to/paper-libraries
```

`python3 verify-evidence.py` は、同梱された既存の試験記録とJARのハッシュを照合します。Minecraftを起動する追加試験ではありません。

Minecraft本体、完全な公式マッピング、通常のランチャー設定、アカウント認証情報、実サーバーのワールドや接続情報は同梱しません。ログのBZProbeと座標は専用テストワールド上の架空プレイヤー・試験用座標です。公開ログのUUIDと接続ソケット情報は置換済みです。

## 根拠資料

- ViaBackwardsの既知の高さ制限： https://github.com/ViaVersion/ViaBackwards#known-issues
- 地下のセクション・更新を除外する実装： https://github.com/ViaVersion/ViaBackwards/blob/master/common/src/main/java/com/viaversion/viabackwards/protocol/v1_17to1_16_4/rewriter/BlockItemPacketRewriter1_17.java
- Paper 26.3プロジェクト設定： https://docs.papermc.io/paper/dev/project-setup/

上記は調査資料です。このModの成功の根拠は同梱した実クライアント・サーバーの記録です。
