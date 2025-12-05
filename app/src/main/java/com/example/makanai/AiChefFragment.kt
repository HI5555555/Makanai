package com.example.makanai

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch
import org.json.JSONObject

class AiChefFragment : Fragment(R.layout.fragment_ai_chef) {

    private val API_KEY = BuildConfig.GEMINI_API_KEY

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Views
        val inputIngredients = view.findViewById<EditText>(R.id.ingredients_input)
        val generateButton = view.findViewById<Button>(R.id.btn_generate_recipe)
        val loadingView = view.findViewById<LinearLayout>(R.id.loading_view)
        val resultContainer = view.findViewById<LinearLayout>(R.id.result_container)

        // Recipe Text Views
        val titleView = view.findViewById<TextView>(R.id.ai_recipe_title)
        val descView = view.findViewById<TextView>(R.id.ai_recipe_desc)
        val ingredientsView = view.findViewById<TextView>(R.id.ai_recipe_ingredients)
        val stepsView = view.findViewById<TextView>(R.id.ai_recipe_steps)

        // Nutrition Views
        val tvCal = view.findViewById<TextView>(R.id.ai_cal)
        val tvProtein = view.findViewById<TextView>(R.id.ai_protein)
        val tvFat = view.findViewById<TextView>(R.id.ai_fat)
        val tvCarb = view.findViewById<TextView>(R.id.ai_carbs)
        val tvFiber = view.findViewById<TextView>(R.id.ai_fiber_val)
        val pbFiber = view.findViewById<ProgressBar>(R.id.ai_fiber_progress)
        val tvVit = view.findViewById<TextView>(R.id.ai_vit_val)
        val pbVit = view.findViewById<ProgressBar>(R.id.ai_vit_progress)
        val tvMin = view.findViewById<TextView>(R.id.ai_min_val)
        val pbMin = view.findViewById<ProgressBar>(R.id.ai_min_progress)
        val tvTips = view.findViewById<TextView>(R.id.ai_health_tips)

        // Gemini Model
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = API_KEY
        )

        generateButton.setOnClickListener {
            val ingredients = inputIngredients.text.toString()

            if (ingredients.isBlank()) {
                // Show a generic error message since we don't know the language yet
                Toast.makeText(context, "Please enter ingredients", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // UI State
            loadingView.visibility = View.VISIBLE
            resultContainer.visibility = View.GONE
            generateButton.isEnabled = false

            // --- INTELLIGENT MULTI-LANGUAGE PROMPT ---
            val prompt = """
                You are a professional chef and nutritionist.
                User ingredients: "$ingredients"
                
                Task:
                1. Detect the language of the user's ingredients (e.g., English, Thai, Japanese).
                2. Create ONE delicious recipe using these ingredients.
                3. WRITE THE RECIPE IN THE SAME LANGUAGE AS THE INGREDIENTS.
                4. Analyze the nutrition.
                
                Output strictly valid JSON only (no markdown, no code blocks).
                
                JSON Structure (Keys must be English, Values must be in the detected language):
                {
                  "title": "Recipe Name (in detected language)",
                  "description": "Short description (in detected language)",
                  "ingredients_text": "• Item 1\n• Item 2... (in detected language)",
                  "steps_text": "1. Step one\n2. Step two... (in detected language)",
                  "nutrition": {
                     "calories": "350 kcal",
                     "protein": "20g",
                     "fat": "15g",
                     "carbs": "40g",
                     "fiber": "5g",
                     "fiber_score": 60,   // Integer 0-100
                     "vitamin": "Vit C",
                     "vitamin_score": 80, // Integer 0-100
                     "mineral": "Iron",
                     "mineral_score": 40, // Integer 0-100
                     "tips": "Health benefits (in detected language)..."
                  }
                }
            """.trimIndent()

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = generativeModel.generateContent(prompt)

                    // Clean JSON string (remove markdown if AI adds it)
                    var jsonText = response.text ?: ""
                    jsonText = jsonText.replace("```json", "").replace("```", "").trim()

                    val json = JSONObject(jsonText)
                    val nut = json.getJSONObject("nutrition")

                    // 1. Set Recipe Text
                    titleView.text = json.optString("title", "Recipe")
                    descView.text = json.optString("description", "")
                    ingredientsView.text = json.optString("ingredients_text", "")
                    stepsView.text = json.optString("steps_text", "")

                    // 2. Set Nutrition Data
                    tvCal.text = nut.optString("calories")
                    tvProtein.text = nut.optString("protein")
                    tvFat.text = nut.optString("fat")
                    tvCarb.text = nut.optString("carbs")

                    tvFiber.text = nut.optString("fiber")
                    pbFiber.progress = nut.optInt("fiber_score")

                    tvVit.text = nut.optString("vitamin")
                    pbVit.progress = nut.optInt("vitamin_score")

                    tvMin.text = nut.optString("mineral")
                    pbMin.progress = nut.optInt("mineral_score")

                    tvTips.text = nut.optString("tips")

                    // Show Result
                    loadingView.visibility = View.GONE
                    resultContainer.visibility = View.VISIBLE

                } catch (e: Exception) {
                    loadingView.visibility = View.GONE
                    Log.e("AiChef", "Error", e)

                    // Handle Server Overload (503)
                    if (e.message?.contains("503") == true || e.message?.contains("overloaded") == true) {
                        val errorMsg = "Server busy. Please try again."
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    generateButton.isEnabled = true
                }
            }
        }
    }
}