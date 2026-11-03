# agent-status-panel-android

Claude Code と Codex の作業の進捗を、Android のホーム画面のウィジェットで見るネイティブアプリ（Kotlin）です。送信側は [agent-status-panel](https://github.com/andoshin11/agent-status-panel) です。通知は、エージェントが操作を待っているときだけ出します。

## ウィジェット

ホーム画面に、[Milestone](https://github.com/andoshin11/orca-milestone) のダークモードと同じ配色とデザインのウィジェットを置けます。

- 実行中のすべてのセッションを、タスクを開いた状態のカードで並べます（ゴール、完了数と進捗バー、タスク一覧）。
- ウィジェットの縦幅いっぱいに表示して、収まらない分はスクロールできます。
- 未完了のセッションが先で、完了したものが後です。それぞれの中では、今動いているものが上です。あとは現れた順に並ぶので、スクロール中に順番が入れ替わりません。
- 順序が前後して届いた古いメッセージは捨てます。
- Mac mini が止まって終了の連絡が来なくても、更新のないセッションは 8 日で消えます。
- アプリの **ホーム画面に追加する** か、ホーム画面のウィジェット一覧から追加します。

## 通知

エージェントが入力や許可を待ち始めると、ワークスペースごとに音の鳴る通知を 1 つ出します。待たなくなると、通知は自動で消えます。ウィジェットでは、そのワークスペースに「要確認」と表示して先頭に並べます。タスクのないワークスペースも、待っている間はウィジェットに出ます。

## 必要なもの

- Android 13 以上
- JDK 21 と Android SDK（Platform 36）
- Firebase プロジェクトに登録した Android アプリの `google-services.json`

`google-services.json` はリポジトリに入れません。Firebase コンソールから取得して、`app/` に置いてください。CI はダミーの `ci/google-services.json` でビルドします。

## ビルドとインストール

```sh
./gradlew installDebug
```

## 設定

1. アプリの **ホーム画面に追加する** でウィジェットを置きます。
2. **通知を許可する** で通知を許可します。
3. 表示された FCM 登録トークンをコピーして、Mac mini の `config.json` の `deviceTokens` に設定します。

更新が遅れるときは、**電池の最適化の設定を開く** からこのアプリを最適化の対象外にしてください。

## 受け取るメッセージ

FCM のデータメッセージの形式は、送信側の [README](https://github.com/andoshin11/agent-status-panel#送る内容) にあります。

## 開発

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## ライセンス

MIT
