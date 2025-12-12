package com.example.makanai

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        // --- 1. Find Views ---
        val inputIngredients = view.findViewById<EditText>(R.id.ingredients_input)
        val generateButton = view.findViewById<Button>(R.id.btn_generate_recipe)
        val loadingView = view.findViewById<LinearLayout>(R.id.loading_view)
        val resultContainer = view.findViewById<LinearLayout>(R.id.result_container)

        // Recipe Text Views
        val titleView = view.findViewById<TextView>(R.id.ai_recipe_title)
        val descView = view.findViewById<TextView>(R.id.ai_recipe_desc)
        val ingredientsView = view.findViewById<TextView>(R.id.ai_recipe_ingredients)
        val stepsView = view.findViewById<TextView>(R.id.ai_recipe_steps)

        // Nutrition Views (Macros)
        val tvCal = view.findViewById<TextView>(R.id.ai_cal)
        val tvProtein = view.findViewById<TextView>(R.id.ai_protein)
        val tvFat = view.findViewById<TextView>(R.id.ai_fat)
        val tvCarb = view.findViewById<TextView>(R.id.ai_carbs)

        // Nutrition Views (Details & Progress)
        val tvFiber = view.findViewById<TextView>(R.id.ai_fiber_val)
        val pbFiber = view.findViewById<ProgressBar>(R.id.ai_fiber_progress)
        val tvVit = view.findViewById<TextView>(R.id.ai_vit_val)
        val pbVit = view.findViewById<ProgressBar>(R.id.ai_vit_progress)
        val tvMin = view.findViewById<TextView>(R.id.ai_min_val)
        val pbMin = view.findViewById<ProgressBar>(R.id.ai_min_progress)
        val tvTips = view.findViewById<TextView>(R.id.ai_health_tips)

        // --- 2. Initialize Gemini Model --
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = API_KEY
        )

        generateButton.setOnClickListener {
            val ingredients = inputIngredients.text.toString()

            if (ingredients.isBlank()) {
                Toast.makeText(context, "食材を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // UI State: Loading
            loadingView.visibility = View.VISIBLE
            resultContainer.visibility = View.GONE
            generateButton.isEnabled = false

            // --- 3. ROBUST PROMPT ---
            val prompt = """
    You are a professional chef assistant. 
    User Input: "$ingredients"
    
    Task:
    1. Analyze the user's input to DETECT THE LANGUAGE (e.g., Lao, Japanese, English).
    2. IF the input does NOT contain food ingredients (e.g., "camera", "hello", "car"), 
       RETURN A JSON with "is_food": false and a "description" error message in the detected language.
    3. IF the input contains valid ingredients, create a recipe using MAINLY the provided ingredients.
       
    CRITICAL INGREDIENT RULES: 
    - Use the user's input ingredients as the main components.
    - You may ONLY add basic pantry staples (e.g., Salt, Pepper, Oil, Soy Sauce, Sugar, Water).
    - **MARKING EXTRA INGREDIENTS:** For any ingredient that was NOT provided by the user (basic staples), you MUST append "(if available)" in the detected language.
      - If Japanese: Add "(あれば)" (e.g., "醤油 (あれば)")
      - If Lao: Add "(ຖ້ามີ)" or equivalent.
      - If English: Add "(if available)".
    
    CRITICAL LANGUAGE RULE:
    - The JSON **Keys** (e.g., "title", "nutrition") must remain in **English**.
    - The JSON **Values** (e.g., Recipe Name, Steps, Description) MUST be in the **DETECTED LANGUAGE** of the User Input.
    - If the user types in Japanese, the content MUST be in Japanese.
    - If the user types in Lao, the content MUST be in Lao.
    
    Output strictly valid JSON only (no markdown).
    
    JSON Structure:
    {
      "is_food": true,
      "title": "Recipe Name",
      "description": "Short description",
      "ingredients_text": "• Item 1\n• Item 2...",
      "steps_text": "1. Step one\n2. Step two...",
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
         "tips": "Health benefits here..."
      }
    }
""".trimIndent()

            // --- 4. Call AI ---
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = generativeModel.generateContent(prompt)

                    // Clean JSON string
                    var jsonText = response.text ?: ""
                    jsonText = jsonText.replace("```json", "").replace("```", "").trim()

                    val json = JSONObject(jsonText)
                    val isFood = json.optBoolean("is_food", true)

                    if (!isFood) {
                        // Handle Non-Food Input
                        loadingView.visibility = View.GONE
                        val errorMsg = json.optString("description", "食材を入力してください")
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()

                    } else {
                        // Handle Valid Recipe
                        val nut = json.getJSONObject("nutrition")

                        // Set Text
                        titleView.text = json.optString("title", "Recipe")
                        descView.text = json.optString("description", "")
                        ingredientsView.text = json.optString("ingredients_text", "")
                        stepsView.text = json.optString("steps_text", "")

                        // Set Macros
                        tvCal.text = nut.optString("calories")
                        tvProtein.text = nut.optString("protein")
                        tvFat.text = nut.optString("fat")
                        tvCarb.text = nut.optString("carbs")

                        // Set Details & Bars
                        tvFiber.text = nut.optString("fiber")
                        pbFiber.progress = nut.optInt("fiber_score")

                        tvVit.text = nut.optString("vitamin")
                        pbVit.progress = nut.optInt("vitamin_score")

                        tvMin.text = nut.optString("mineral")
                        pbMin.progress = nut.optInt("mineral_score")

                        tvTips.text = nut.optString("tips")

                        // Show UI
                        loadingView.visibility = View.GONE
                        resultContainer.visibility = View.VISIBLE
                    }

                } catch (e: Exception) {
                    loadingView.visibility = View.GONE
                    Log.e("AiChef", "Error", e)

                    if (e.message?.contains("503") == true) {
                        Toast.makeText(context, "Server busy. Please try again.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    generateButton.isEnabled = true
                }
            }
        }
    }
}