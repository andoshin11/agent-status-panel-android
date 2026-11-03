# agent-status-panel-android

Claude Code と Codex の作業の進捗を、Android の [Live Updates](https://developer.android.com/develop/ui/views/notifications/live-update) で受け取るネイティブアプリ（Kotlin）です。送信側は [agent-status-panel](https://github.com/andoshin11/agent-status-panel) です。

## できること

- セッションごとに通知を 1 つ出して、ゴール、実行中のステップ、完了数を表示します。
- タスク数に応じて区切った進捗バーと、ステータスバーのチップに `2/5` のような完了数を表示します。
- 全タスクが完了すると、音の鳴る通常の通知に置き換えます。セッションが終わると通知を消します。
- 順序が前後して届いた古いメッセージは捨てます。自分で閉じた Live Update は、そのセッションが完了するか終わるまで出しません。
- Mac mini が止まって終了の連絡が来なくても、更新のない通知は 3 時間で消えます。

## ウィジェット

ホーム画面に、[Milestone](https://github.com/andoshin11/orca-milestone) のダークモードと同じ配色とデザインのウィジェットを置けます。

- 実行中のすべてのセッションを、タスクを開いた状態のカードで並べます（ゴール、完了数と進捗バー、タスク一覧）。
- ウィジェットの縦幅いっぱいに表示して、収まらない分はスクロールできます。
- 作業中のセッションが先で、完了したものが後です。それぞれ現れた順に並ぶので、スクロール中に順番が入れ替わりません。
- アプリの **ホーム画面に追加する** か、ホーム画面のウィジェット一覧から追加します。

Milestone の「入力待ち」や「要確認」は、送信側がエージェントの状態を送っていないので表示しません。

## 必要なもの

- Android 13 以上。Live Updates は Android 16 以上で、それより古い端末では通常の通知になります。
- JDK 21 と Android SDK（Platform 36）
- Firebase プロジェクトに登録した Android アプリの `google-services.json`

`google-services.json` はリポジトリに入れません。Firebase コンソールから取得して、`app/` に置いてください。CI はダミーの `ci/google-services.json` でビルドします。

## ビルドとインストール

```sh
./gradlew installDebug
```

## 設定

1. アプリを開いて、通知を許可します。
2. **Live Updates** の表示が「利用可能」になっているか確かめます。オフなら設定を開いて有効にします。
3. **プレビュー通知** で、30 秒間の表示を確かめます。
4. 表示された FCM 登録トークンをコピーして、Mac mini の `config.json` の `deviceTokens` に設定します。

### Galaxy（One UI）の場合

Now Bar に出るかどうかは、One UI の版と設定によって変わる可能性があります。ベータ版の One UI 8 では、開発者オプションの「Live notifications for all apps」を有効にする必要があったと報じられています。

通知が遅れるときは、**電池の最適化の設定を開く** からこのアプリを最適化の対象外にしてください。

## 受け取るメッセージ

FCM のデータメッセージの形式は、送信側の [README](https://github.com/andoshin11/agent-status-panel#送る内容) にあります。

## 開発

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## ライセンス

MIT
