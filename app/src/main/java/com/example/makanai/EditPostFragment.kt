package com.example.makanai

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.UUID

class EditPostFragment : Fragment(R.layout.fragment_add_post) {

    // Removed Safe Args line to prevent build errors
    // private val args: EditPostFragmentArgs by navArgs()

    private val db = Firebase.firestore
    private val storage = Firebase.storage

    // UI Variables
    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var stepsContainer: LinearLayout
    private lateinit var mainImagePreview: ImageView
    private lateinit var mainUploadPlaceholder: LinearLayout
    private lateinit var inputName: EditText
    private lateinit var inputDesc: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var inputTime: EditText
    private lateinit var inputServings: EditText
    private lateinit var spinnerDifficulty: Spinner
    private lateinit var btnPublish: Button
    private lateinit var titleTextView: TextView

    private var mainImageUri: Uri? = null
    private var existingImageUrl: String = ""
    private val stepImageUris = HashMap<View, Uri>()
    private val existingStepImageUrls = HashMap<View, String>()

    private var currentImageTargetView: View? = null
    private var isPickingMainImage = true

    private val categories = listOf("朝食", "昼食", "夕食", "軽食", "デザート", "ヘルシー")
    private val unitList = listOf("g", "kg", "ml", "l", "個", "本", "枚", "少々", "適量")

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (isPickingMainImage) {
                mainImageUri = uri
                mainUploadPlaceholder.visibility = View.GONE
                mainImagePreview.visibility = View.VISIBLE
                mainImagePreview.load(uri)
            } else {
                currentImageTargetView?.let { stepView ->
                    stepImageUris[stepView] = uri
                    val preview = stepView.findViewById<ImageView>(R.id.step_image_preview)
                    val placeholder = stepView.findViewById<ImageView>(R.id.step_image_placeholder)
                    placeholder.visibility = View.GONE
                    preview.visibility = View.VISIBLE
                    preview.load(uri)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. Initialize Views (Fixed IDs) ---
        ingredientsContainer = view.findViewById(R.id.ingredients_container)
        stepsContainer = view.findViewById(R.id.steps_container)
        mainImagePreview = view.findViewById(R.id.image_preview)
        mainUploadPlaceholder = view.findViewById(R.id.upload_placeholder)
        inputName = view.findViewById(R.id.recipe_name_input)
        inputDesc = view.findViewById(R.id.description_input)
        inputTime = view.findViewById(R.id.prep_time_input)
        inputServings = view.findViewById(R.id.servings_input)
        spinnerCategory = view.findViewById(R.id.category_spinner)
        spinnerDifficulty = view.findViewById(R.id.difficulty_spinner)
        btnPublish = view.findViewById(R.id.publish_button_bottom)
        titleTextView = view.findViewById(R.id.add_post_title)

        // Change UI text to "Edit"
        titleTextView.text = "レシピを編集"
        btnPublish.text = "更新する"

        setupSimpleSpinner(spinnerCategory, categories)
        setupSpinnerFromResource(spinnerDifficulty, R.array.recipe_difficulties)

        // --- 2. Listeners (Fixed IDs) ---
        view.findViewById<View>(R.id.image_upload_container).setOnClickListener {
            isPickingMainImage = true
            pickImage.launch("image/*")
        }

        // FIX: Use 'add_ingredient_button' not 'btn_add_ingredient'
        view.findViewById<TextView>(R.id.add_ingredient_button).setOnClickListener { addIngredientInput() }
        // FIX: Use 'add_step_button' not 'btn_add_step'
        view.findViewById<TextView>(R.id.add_step_button).setOnClickListener { addStepInput() }

        view.findViewById<ImageButton>(R.id.back_button_add_post).setOnClickListener { findNavController().popBackStack() }
        view.findViewById<Button>(R.id.post_button_top).setOnClickListener { startUpdateProcess() }
        btnPublish.setOnClickListener { startUpdateProcess() }

        // --- 3. LOAD EXISTING DATA ---
        // Get ID manually from arguments bundle
        val recipeId = arguments?.getString("recipeId")

        if (recipeId != null) {
            loadRecipeData(recipeId)
        } else {
            Toast.makeText(context, "Error: No Recipe ID", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun loadRecipeData(recipeId: String) {
        db.collection("recipes").document(recipeId).get().addOnSuccessListener { document ->
            val recipe = document.toObject(Recipe::class.java)
            recipe?.let { r ->
                inputName.setText(r.title)
                inputDesc.setText(r.description)
                inputTime.setText(r.prepTime)
                inputServings.setText(r.servings)

                val catIndex = categories.indexOf(r.category)
                if (catIndex >= 0) spinnerCategory.setSelection(catIndex)

                // Load Difficulty (Simple string check)
                val diffArray = resources.getStringArray(R.array.recipe_difficulties)
                val diffIndex = diffArray.indexOf(r.difficulty)
                if (diffIndex >= 0) spinnerDifficulty.setSelection(diffIndex)

                // Main Image
                existingImageUrl = r.imageUrl
                if (existingImageUrl.isNotEmpty()) {
                    mainUploadPlaceholder.visibility = View.GONE
                    mainImagePreview.visibility = View.VISIBLE
                    mainImagePreview.load(existingImageUrl)
                }

                // Load Ingredients
                ingredientsContainer.removeAllViews()
                for (ing in r.ingredients) {
                    // Handle potential String vs Map issue safely
                    if (ing is Map<*, *>) {
                        val name = ing["name"] as? String ?: ""
                        val qty = ing["quantity"] as? String ?: ""
                        val unit = ing["unit"] as? String ?: ""
                        addIngredientInput(name, qty, unit)
                    }
                }
                // Ensure at least one row if empty
                if (ingredientsContainer.childCount == 0) addIngredientInput()

                // Load Steps
                stepsContainer.removeAllViews()
                for (step in r.steps) {
                    if (step is Map<*, *>) {
                        val text = step["text"] as? String ?: ""
                        val imgUrl = step["imageUrl"] as? String ?: ""
                        addStepInput(text, imgUrl)
                    }
                }
                if (stepsContainer.childCount == 0) addStepInput()
            }
        }
    }

    private fun addIngredientInput(name: String = "", qty: String = "", unit: String = "") {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_ingredient_input, ingredientsContainer, false)

        val nameInput = view.findViewById<EditText>(R.id.ingredient_name_input)
        val qtyInput = view.findViewById<EditText>(R.id.ingredient_quantity_input)
        val unitSpinner = view.findViewById<Spinner>(R.id.ingredient_unit_spinner)

        setupSimpleSpinner(unitSpinner, unitList)

        nameInput.setText(name)
        qtyInput.setText(qty)
        if (unit.isNotEmpty()) {
            val index = unitList.indexOf(unit)
            if (index >= 0) unitSpinner.setSelection(index)
        }
        ingredientsContainer.addView(view)
    }

    private fun addStepInput(text: String = "", imageUrl: String = "") {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_step_input, stepsContainer, false)

        val stepNum = view.findViewById<TextView>(R.id.step_number)
        val stepInput = view.findViewById<EditText>(R.id.step_input)
        val preview = view.findViewById<ImageView>(R.id.step_image_preview)
        val placeholder = view.findViewById<ImageView>(R.id.step_image_placeholder)
        val btnRemove = view.findViewById<ImageButton>(R.id.remove_button)

        stepNum.text = (stepsContainer.childCount + 1).toString()
        stepInput.setText(text)

        if (imageUrl.isNotEmpty()) {
            placeholder.visibility = View.GONE
            preview.visibility = View.VISIBLE
            preview.load(imageUrl)
            existingStepImageUrls[view] = imageUrl
        }

        btnRemove.setOnClickListener {
            stepsContainer.removeView(view)
            stepImageUris.remove(view)
            existingStepImageUrls.remove(view)
            renumberSteps()
        }

        val imageContainer = view.findViewById<View>(R.id.step_image_container)
        imageContainer.setOnClickListener {
            isPickingMainImage = false
            currentImageTargetView = view
            pickImage.launch("image/*")
        }
        stepsContainer.addView(view)
    }

    private fun startUpdateProcess() {
        setLoading(true)
        // Upload new image if selected, otherwise proceed with existing logic...
        if (mainImageUri != null) {
            val filename = UUID.randomUUID().toString()
            val ref = Firebase.storage.reference.child("recipe_images/$filename.jpg")
            ref.putFile(mainImageUri!!).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    uploadStepImagesRecursive(uri.toString(), 0, mutableListOf())
                }
            }
        } else {
            uploadStepImagesRecursive(existingImageUrl, 0, mutableListOf())
        }
    }

    private fun uploadStepImagesRecursive(mainImageUrl: String, index: Int, stepDataList: MutableList<HashMap<String, String>>) {
        if (index >= stepsContainer.childCount) {
            updateFirestore(mainImageUrl, stepDataList)
            return
        }

        val stepView = stepsContainer.getChildAt(index)
        val text = stepView.findViewById<EditText>(R.id.step_input).text.toString()
        val newImageUri = stepImageUris[stepView]
        val oldImageUrl = existingStepImageUrls[stepView] ?: ""

        if (newImageUri != null) {
            val filename = UUID.randomUUID().toString()
            val ref = Firebase.storage.reference.child("recipe_step_images/$filename.jpg")
            ref.putFile(newImageUri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                    stepDataList.add(hashMapOf("text" to text, "imageUrl" to downloadUrl.toString()))
                    uploadStepImagesRecursive(mainImageUrl, index + 1, stepDataList)
                }
            }
        } else {
            stepDataList.add(hashMapOf("text" to text, "imageUrl" to oldImageUrl))
            uploadStepImagesRecursive(mainImageUrl, index + 1, stepDataList)
        }
    }

    private fun updateFirestore(mainImageUrl: String, stepsData: List<HashMap<String, String>>) {
        val ingredientsList = mutableListOf<Map<String, String>>()
        for (i in 0 until ingredientsContainer.childCount) {
            val row = ingredientsContainer.getChildAt(i)
            val name = row.findViewById<EditText>(R.id.ingredient_name_input).text.toString().trim()
            val quantity = row.findViewById<EditText>(R.id.ingredient_quantity_input).text.toString().trim()
            val unit = row.findViewById<Spinner>(R.id.ingredient_unit_spinner).selectedItem.toString()

            if (name.isNotEmpty()) {
                ingredientsList.add(mapOf("name" to name, "quantity" to quantity, "unit" to unit))
            }
        }

        val updates = mapOf(
            "title" to inputName.text.toString(),
            "description" to inputDesc.text.toString(),
            "category" to spinnerCategory.selectedItem.toString(),
            "prepTime" to inputTime.text.toString(),
            "servings" to inputServings.text.toString(),
            "difficulty" to spinnerDifficulty.selectedItem.toString(),
            "imageUrl" to mainImageUrl,
            "ingredients" to ingredientsList,
            "steps" to stepsData
        )

        // Get ID safely
        val recipeId = arguments?.getString("recipeId") ?: return

        db.collection("recipes").document(recipeId).update(updates)
            .addOnSuccessListener {
                setLoading(false)
                Toast.makeText(context, "Recipe Updated!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener {
                setLoading(false)
                Toast.makeText(context, "Update Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSpinnerFromResource(spinner: Spinner, arrayResId: Int) {
        ArrayAdapter.createFromResource(requireContext(), arrayResId, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = it
        }
    }
    private fun setupSimpleSpinner(spinner: Spinner, items: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }
    private fun renumberSteps() {
        for (i in 0 until stepsContainer.childCount) {
            stepsContainer.getChildAt(i).findViewById<TextView>(R.id.step_number).text = (i + 1).toString()
        }
    }
    private fun setLoading(isLoading: Boolean) {
        btnPublish.isEnabled = !isLoading
        btnPublish.text = if (isLoading) "Updating..." else "更新する"
    }
}