# agent-status-panel-app

Claude Code と Codex の作業の進捗をスマートフォンで受け取るアプリです。送信側は [agent-status-panel](https://github.com/andoshin11/agent-status-panel) です。

> [!NOTE]
> まだアプリのコードはありません。Flutter で、Android から先に作る予定です。

## 予定

| | Android | iOS |
|---|---|---|
| 順番 | 先に対応 | Android のあとに対応 |
| 表示 | Live Updates（Android 16 以降） | Live Activity（iOS 16.1 以降） |
| 表示部分の実装 | Kotlin（`Notification.ProgressStyle`） | SwiftUI（Widget Extension） |
| 更新の経路 | FCM のデータメッセージを受けて、同じ通知を更新する | FCM 経由の APNs 配信で、アプリを起動せずに更新する |
| 必要なもの | 実機（Android 16 以降） | 実機、Apple Developer Program |

一覧画面や設定など、通知以外の画面は Flutter で共通にします。Live Updates と Live Activity の見た目はそれぞれの OS のネイティブコードで作ります。

## 受け取るメッセージ

FCM のデータメッセージの形式は、送信側の [README](https://github.com/andoshin11/agent-status-panel#送る内容) にあります。アプリでは次のように扱います。

- `id` ごとに通知を 1 つ表示します。
- `at` が、その `id` で最後に受けたものより古いメッセージは捨てます。FCM は順序を保証しないためです。
- `active` は進行中の通知にします。`finished` は通常の通知に置き換えて、`ended` は通知を消します。
- `v` が知らない値のメッセージは無視します。

## ライセンス

MIT
