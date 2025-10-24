package com.example.makanai

// A simple "object" to act as a fake database
object RecipeRepository {

    private val recipes = listOf(
        Recipe(
            id = 1,
            title = "Classic Granola Bowl",
            imageResId = R.drawable.img_granola,
            category = "朝食",
            author = "Chef Kosen",
            mainDescription = "栄養たっぷりで美味しい朝食メニュー。一日を元気にスタートするのにぴったりです！",
            prepTime = "10分",
            servings = "1人分",
            difficulty = "簡単",
            ingredients = listOf(
                "グラノーラ 1カップ",
                "ヨーグルト 1カップ",
                "新鮮なベリー 1/2カップ",
                "スライスピーチ 1/2個"
            ),
            steps = listOf(
                "ボウルにヨーグルトを入れます。",
                "グラノーラを上に追加します。",
                "新鮮なベリーとピーチで飾ります。"
            ),
            likes = 120
        ),
        Recipe(
            id = 2,
            title = "Mediterranean Bowl",
            imageResId = R.drawable.img_buddha_bowl,
            category = "昼食",
            author = "Yuki Tanaka",
            mainDescription = "新鮮な野菜、キヌア、タヒニドレッシングのヘルシーでカラフルな一品。",
            prepTime = "20分",
            servings = "2人分",
            difficulty = "簡単",
            ingredients = listOf(
                "キヌア 1カップ",
                "ひよこ豆 1缶",
                "きゅうり 1個",
                "トマト 2個",
                "タヒニドレッシング 大さじ2"
            ),
            steps = listOf(
                "キヌアを炊きます。",
                "野菜を刻みます。",
                "すべてをボウルに盛り付け、ドレッシングをかけます。"
            ),
            likes = 312
        ),
        Recipe(
            id = 3,
            title = "Fresh Fruit Salad",
            imageResId = R.drawable.img_fruit_salad,
            category = "デザート",
            author = "Ai Sato",
            mainDescription = "Refreshing and delicious",
            prepTime = "15分",
            servings = "4人分",
            difficulty = "簡単",
            ingredients = listOf(
                "イチゴ 1カップ",
                "ブルーベリー 1カップ",
                "キウイ 2個",
                "ミントの葉"
            ),
            steps = listOf(
                "すべての果物をスライスします。",
                "大きなボウルで混ぜます。",
                "ミントを飾ります。"
            ),
            likes = 250
        )
    )

    fun getRecipes(): List<Recipe> {
        return recipes
    }

    // A new function to find a single recipe by its ID
    fun getRecipeById(id: Int): Recipe? {
        return recipes.find { it.id == id }
    }
}