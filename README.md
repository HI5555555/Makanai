# Makanai
# UX-UI Figma:
https://www.figma.com/make/xVBWMo4hYIfm33BYOxgSta/Makanai-App-UI-Design?node-id=0-1&p=f&t=hQGeOIv1oqKVUzI2-0

# Makanaiプロジェクトへようこそ！ 🍳
### GitHubワークフロー・シンプルガイド

このガイドは、プロジェクトをクリーンで安全、かつ管理しやすく保つための、標準的な「フィーチャーブランチ・ワークフロー」について説明するものです。タスクごとに従うべきブランチ構造と正確な手順を説明します。

---

## ブランチ戦略 (「なぜ」やるのか)

私たちのプロジェクトを「本を書くこと」に例えてみましょう。

### `main` 🌳 (出版された本)
* **目的:** これはアプリの最終的で公式なリリースバージョンです。完璧に動作することがわかっている「ゴールデンコピー」です。
* **ルール:** このブランチは**絶対に触らないでください**。新しいメジャーバージョンをリリースする準備ができたときだけ、`develop` の内容をマージします。

### `develop` 🛠️ (最終草稿)
* **目的:** これが私たちのメインの開発ブランチです。次のリリースの準備ができた *完成済みの機能* がすべて含まれています。あなたが機能を完成させたら、ここにマージされます。
* **ルール:** このブランチに直接プッシュすることは**絶対にしないでください**。すべての変更は「プルリクエスト」を経由する必要があります。新しいタスクを開始するときは、*常に*このブランチから始めます。

### `feature/...` ✨ (個人のノート)
* **目的:** これはあなた個人のための一時的な作業スペースです。*すべてのタスク*ごとに新しいブランチを作成します (例: `feature/login-screen`, `feature/search-page-ui`, `fix/homepage-bug`)。
* **ルール:** あなたがコードを書くのは、このブランチだけです。これはあなた専用なので、他の誰かの作業を壊してしまう心配がありません。

---

## ステップバイステップ・ワークフロー (「どうやるか」)

新しいタスクを開始するときは、**毎回**この手順に従ってください。

### ステップ1: 作業開始 (同期)

コードを書き始める前に、まずメインの `develop` ブランチの最新バージョンを取得する必要があります。

1.  Android Studioの**右下**にあるブランチ名をクリックします。
2.  「Remote Branches」リストから `origin/develop` を選択します。
3.  ポップアップで **Checkout** を選択します。これで `develop` ブランチに切り替わりました。
4.  **`Cmd + T`** (Mac) または **`Ctrl + T`** (Windows) を押して「Update Project」を実行します。これにより、チームメイトの最新の変更がすべて取り込まれます。

### ステップ2: タスクブランチの作成

最新のコードを取得したら、作業するための安全なブランチを新しく作成します。

1.  右下のブランチ名 (`develop`) をクリックします。
2.  **"+ New Branch"** を選択します。
3.  ブランチ名を `feature/` や `fix/` などのプレフィックス（接頭辞）を付けて命名します。
    * **良い例:** `feature/login-ui`
    * **良い例:** `fix/search-button-crash`
4.  **Enter**キーを押します。これで新しいブランチに切り替わり、安全にコードを書き始められます。

### ステップ3: 作業 (コミット & プッシュ)

ここでコーディング作業を行います。

1.  **コードを書く:** 機能を実装したり、新しいファイルを作成したりします。
2.  **こまめにコミット:** 小さな区切りがついたら、作業内容を保存（コミット）します。**`Cmd + K`** (Mac) または **`Ctrl + K`** (Windows) を押して「Commit」ウィンドウを開きます。
    * 変更したファイルにチェックを入れます。
    * わかりやすいコミットメッセージを書きます (例: "メールアドレスとパスワードの入力欄を追加")。
    * **"Commit"** をクリックします。
3.  **ブランチをプッシュ:** 一日の終わりや作業が完了したら、新しいブランチをGitHubにアップロードする**必要があります**。これはあなたの作業のバックアップになります。
    * **`Cmd + Shift + K`** (Mac) または **`Ctrl + Shift + K`** (Windows) を押します。
    * **"Push"** をクリックします。これであなたの `feature` ブランチがGitHubにアップロードされました。

### ステップ4: マージのリクエスト (プルリクエストの作成)

あなたの機能が**100%完成**し、チームにレビューしてもらう準備ができたら、「プルリクエスト (PR)」を使ってマージをリクエストします。

1.  ブラウザでGitHubのプロジェクトページにアクセスします。
2.  あなたの新しいブランチに関する黄色のバーが表示されます。**"Compare & pull request"** をクリックします。
3.  **ここが最も重要です:**
    * **`base`** ブランチを **`develop`** に設定します。(これはあなたのコードが *マージされる先* のブランチです)
    * **`compare`** ブランチをあなたの **`feature/login-ui`** ブランチに設定します。(これはあなたのコードが *含まれている* ブランチです)
4.  タイトルを書き、右側でチームメイトを **"Reviewers"** (レビュアー) に指定し、**"Create pull request"** をクリックします。

### ステップ5: レビュー & マージ (チームワーク！)

1.  チームメイトがあなたのコードをレビューし、コメントや変更リクエストを残すかもしれません。
2.  変更をリクエストされた場合は、Android Studioで**同じ `feature` ブランチ**上で修正を行います。修正をコミットしてプッシュすれば、プルリクエストは自動的に更新されます。
3.  あなたのPRが **Approved** (承認) されたら、誰か (またはあなた) が緑色の **"Merge pull request"** ボタンをクリックします。

**おめでとうございます！** これであなたの機能は安全に `develop` ブランチに取り込まれました。GitHub上で古い `feature` ブランチを削除し、次のタスクのためにステップ1に戻ることができます。

# Welcome to the Makanai Project! 🍳
### A Simple Guide to Our GitHub Workflow

To keep our project clean, safe, and easy to manage, we follow a standard **"Feature Branch Workflow."** This guide explains our branch structure and the exact steps you need to follow for *every* task you work on.

---

## Our Branch Strategy (The "Why")

Think of our project like a book being written.

### `main` 🌳 (The Published Book)
* **Purpose:** This is the final, official, released version of our app. It is our "golden" copy that we know works perfectly.
* **Rule:** **NEVER** touch this branch. We only update it (merge `develop` into it) when we are ready to launch a new major version.

### `develop` 🛠️ (The Final Draft)
* **Purpose:** This is our main integration branch. It contains all the *finished* features that are ready for the next release. When you finish your feature, it gets merged here.
* **Rule:** **NEVER** push directly to this branch. All changes *must* come from a Pull Request. You will *always* start new tasks from here.

### `feature/...` ✨ (Your Personal Notebook)
* **Purpose:** This is your temporary, personal workspace. You create a new one for *every single task* (e.g., `feature/login-screen`, `feature/search-page-ui`, `fix/homepage-bug`).
* **Rule:** This is the only branch you write code on. Because it's yours, you can't break anything for anyone else.

---

## Your Step-by-Step Workflow (The "How")

Follow these steps **every time** you start a new task.

### Step 1: Start Your Day (Sync Up)

Before you write any code, you must get the latest version of the main `develop` branch.

1.  In Android Studio, click the branch name in the **bottom-right corner**.
2.  Select `origin/develop` from the "Remote Branches" list.
3.  In the pop-up, choose **Checkout**. You are now on the `develop` branch.
4.  Press **`Cmd + T`** (Mac) or **`Ctrl + T`** (Windows) to "Update Project." This pulls all the latest changes from your teammates. 

### Step 2: Create Your Task Branch

Now that you have the latest code, create your own safe branch to work in.

1.  Click the branch name (`develop`) in the bottom-right corner.
2.  Select **"+ New Branch"**.
3.  Name your branch using a prefix like `feature/` or `fix/`.
    * **Good name:** `feature/login-ui`
    * **Good name:** `fix/search-button-crash`
4.  Press **Enter**. You are now on your new branch, and it's safe to write code.

### Step 3: Do Your Work (Commit & Push)

This is where you do your coding.

1.  **Write Code:** Build your feature, create new files, etc.
2.  **Commit Often:** When you complete a small part, save it. Press **`Cmd + K`** (Mac) or **`Ctrl + K`** (Windows) to open the "Commit" window.
    * Check the files you changed.
    * Write a clear commit message (e.g., "Add email and password fields").
    * Click **"Commit"**.
3.  **Push Your Branch:** At the end of the day, or when you're done, you *must* upload your new branch to GitHub. This backs up your work.
    * Press **`Cmd + Shift + K`** (Mac) or **`Ctrl + Shift + K`** (Windows).
    * Click **"Push"**. Your `feature` branch is now on GitHub.

### Step 4: Ask to Merge (Open a Pull Request)

When your feature is **100% complete** and ready for the team to see, you will merge it using a Pull Request (PR).

1.  Go to our GitHub project page in your browser.
2.  You'll see a yellow bar for your new branch. Click **"Compare & pull request"**.
3.  **THIS IS THE MOST IMPORTANT PART:**
    * Set the **`base`** branch to **`develop`**. (This is where your code is going *to*).
    * Set the **`compare``** branch to your **`feature/login-ui`** branch. (This is where your code is coming *from*). 
4.  Write a title, add your teammates as **"Reviewers"** on the right, and click **"Create pull request"**.

### Step 5: Review & Merge (Teamwork!)

1.  Your teammates will review your code and may leave comments or request changes.
2.  If they ask for changes, just make them in Android Studio on the **same `feature` branch**. Commit and push the changes—the Pull Request will update automatically.
3.  Once your PR is **Approved**, one person will click the green **"Merge pull request"** button.

**Congratulations!** Your feature is now safely in the `develop` branch. You can delete your old `feature` branch from GitHub and go back to Step 1 for your next task.
