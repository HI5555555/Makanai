package com.example.makanai

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch
// --- Import Markwon ---
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin

class AiChefFragment : Fragment(R.layout.fragment_ai_chef) {

    private val API_KEY = BuildConfig.GEMINI_API_KEY

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputIngredients = view.findViewById<EditText>(R.id.ingredients_input)
        val generateButton = view.findViewById<Button>(R.id.btn_generate_recipe)
        val loadingView = view.findViewById<LinearLayout>(R.id.loading_view)
        val resultContainer = view.findViewById<LinearLayout>(R.id.result_container)
        val resultText = view.findViewById<TextView>(R.id.result_text)

        // 1. Initialize Markwon with Table Plugin
        val markwon = Markwon.builder(requireContext())
            .usePlugin(TablePlugin.create(requireContext()))
            .build()

        // Initialize Gemini Model
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = API_KEY
        )

        generateButton.setOnClickListener {
            val ingredients = inputIngredients.text.toString()

            if (ingredients.isBlank()) {
                Toast.makeText(context, "Please enter ingredients / 食材を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // UI: Show Loading
            loadingView.visibility = View.VISIBLE
            resultContainer.visibility = View.GONE
            generateButton.isEnabled = false

            // --- INTELLIGENT PROMPT ---
            val prompt = """
                You are a professional chef assistant. 
                User ingredients: "$ingredients"
                
                Task:
                1. Detect the language of the user's input (English or Japanese).
                2. Create ONE delicious recipe using these ingredients in that SAME language.
                3. Provide the recipe Name, Time, Ingredients list, and Step-by-step instructions.
                4. AFTER the recipe, provide a "Nutrition Facts" section with estimated KCAL, Protein, Fat, and Carbs per serving.
                
                Output Format (Strictly follow this visual style):
                
                # [Recipe Name]
                
                **Time:** [Time] | **Servings:** [Servings]
                
                ## Ingredients
                * [Ingredient 1]
                * [Ingredient 2]
                ...
                
                ## Instructions
                1. [Step 1]
                2. [Step 2]
                ...
                
                ---
                ## Nutrition (Per Serving)
                | Nutrient | Amount |
                | :--- | :--- |
                | **Calories** | [Value] kcal |
                | **Protein** | [Value] g |
                | **Fat** | [Value] g |
                | **Carbs** | [Value] g |
                
            """.trimIndent()

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = generativeModel.generateContent(prompt)

                    // UI: Show Result
                    loadingView.visibility = View.GONE
                    resultContainer.visibility = View.VISIBLE

                    // --- RENDER MARKDOWN HERE ---
                    markwon.setMarkdown(resultText, response.text ?: "")

                } catch (e: Exception) {
                    loadingView.visibility = View.GONE
                    Log.e("AiChef", "Error generating recipe", e)

                    // Detect Server Overload (503)
                    if (e.message?.contains("503") == true || e.message?.contains("overloaded") == true) {
                        resultText.text = "⚠️ AIサーバーが混み合っています。\n\n少し待ってからもう一度お試しください。\n(Google Gemini API Busy)"
                        resultContainer.visibility = View.VISIBLE
                    } else {
                        // Other errors (API Key, Network, etc.)
                        val errorMessage = if (e.message?.contains("API key") == true) {
                            "API Key Error. Please check configuration."
                        } else {
                            "エラーが発生しました: ${e.localizedMessage}"
                        }
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                } finally {
                    generateButton.isEnabled = true
                }
            }
        }
    }
}