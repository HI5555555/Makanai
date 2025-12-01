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
import java.util.Date
import java.util.UUID

class AddPostFragment : Fragment(R.layout.fragment_add_post) {

    // Containers
    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var stepsContainer: LinearLayout

    // Main Image Views
    private lateinit var mainImagePreview: ImageView
    private lateinit var mainUploadPlaceholder: LinearLayout

    // Inputs
    private lateinit var inputName: EditText
    private lateinit var inputDesc: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var inputTime: EditText
    private lateinit var inputServings: EditText
    private lateinit var spinnerDifficulty: Spinner
    private lateinit var btnPublish: Button

    // Variables to store selected images
    private var mainImageUri: Uri? = null
    private val stepImageUris = HashMap<View, Uri>() // Map Step View -> Image Uri

    private var currentImageTargetView: View? = null
    private var isPickingMainImage = true

    // NEW: Updated Categories and Units
    private val categories = listOf("朝食", "昼食", "夕食", "軽食", "デザート", "ヘルシー")
    private val unitList = listOf("g", "kg", "ml", "l", "個", "本", "枚", "少々", "適量")

    // Image Picker
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

        // Initialize Views
        ingredientsContainer = view.findViewById(R.id.ingredients_container)
        stepsContainer = view.findViewById(R.id.steps_container)
        mainImagePreview = view.findViewById(R.id.image_preview)
        mainUploadPlaceholder = view.findViewById(R.id.upload_placeholder)
        inputName = view.findViewById(R.id.input_name)
        inputDesc = view.findViewById(R.id.input_desc)
        inputTime = view.findViewById(R.id.input_time)
        inputServings = view.findViewById(R.id.input_servings)
        spinnerCategory = view.findViewById(R.id.spinner_category)
        spinnerDifficulty = view.findViewById(R.id.spinner_difficulty)
        btnPublish = view.findViewById(R.id.btn_publish)
        val backButton = view.findViewById<ImageButton>(R.id.back_button)

        // Setup Spinners
        setupSimpleSpinner(spinnerCategory, categories)
        setupSpinnerFromResource(spinnerDifficulty, R.array.recipe_difficulties)

        // Listeners
        view.findViewById<View>(R.id.image_upload_container).setOnClickListener {
            isPickingMainImage = true
            pickImage.launch("image/*")
        }

        view.findViewById<TextView>(R.id.btn_add_ingredient).setOnClickListener { addIngredientInput() }
        view.findViewById<TextView>(R.id.btn_add_step).setOnClickListener { addStepInput() }
        backButton.setOnClickListener { findNavController().popBackStack() }
        btnPublish.setOnClickListener { startUploadProcess() }

        // Initialize Empty Rows
        // Clear any XML placeholder rows to start fresh with Kotlin logic
        if (ingredientsContainer.childCount > 0) ingredientsContainer.removeAllViews()
        if (stepsContainer.childCount > 0) stepsContainer.removeAllViews()

        addIngredientInput()
        addStepInput()
    }

    // --- LOGIC: Dynamic Fields ---

    private fun addIngredientInput() {
        // Inflate new 3-part ingredient layout
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_ingredient_input, ingredientsContainer, false)

        // Setup Unit Spinner
        val unitSpinner = view.findViewById<Spinner>(R.id.ingredient_unit_spinner)
        setupSimpleSpinner(unitSpinner, unitList)

        // Setup Remove Button
        // Note: item_ingredient_input.xml doesn't have a remove button in the code I gave previously.
        // If you want one, add it to the XML. For now, assuming no remove button on this complex row
        // OR you can add a long-click listener to remove.
        // Let's skip the remove button for brevity unless your XML has it.

        ingredientsContainer.addView(view)
    }

    private fun addStepInput() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_step_input, stepsContainer, false)

        val stepNum = view.findViewById<TextView>(R.id.step_number)
        stepNum.text = (stepsContainer.childCount + 1).toString()

        val btnRemove = view.findViewById<ImageButton>(R.id.remove_button)
        btnRemove.setOnClickListener {
            stepsContainer.removeView(view)
            stepImageUris.remove(view)
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

    // --- LOGIC: Upload & Save ---

    private fun startUploadProcess() {
        if (mainImageUri == null || inputName.text.isEmpty()) {
            Toast.makeText(context, "Title and Main Image are required", Toast.LENGTH_SHORT).show()
            return
        }
        setLoading(true)

        val filename = UUID.randomUUID().toString()
        val ref = Firebase.storage.reference.child("recipe_images/$filename.jpg")

        ref.putFile(mainImageUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    uploadStepImagesRecursive(uri.toString(), 0, mutableListOf())
                }
            }
            .addOnFailureListener {
                setLoading(false)
                Toast.makeText(context, "Main image upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadStepImagesRecursive(mainImageUrl: String, index: Int, stepDataList: MutableList<HashMap<String, String>>) {
        if (index >= stepsContainer.childCount) {
            saveToFirestore(mainImageUrl, stepDataList)
            return
        }

        val stepView = stepsContainer.getChildAt(index)
        val text = stepView.findViewById<EditText>(R.id.step_input).text.toString()
        val imageUri = stepImageUris[stepView]

        if (imageUri != null) {
            val filename = UUID.randomUUID().toString()
            val ref = Firebase.storage.reference.child("recipe_step_images/$filename.jpg")

            ref.putFile(imageUri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                    stepDataList.add(hashMapOf("text" to text, "imageUrl" to downloadUrl.toString()))
                    uploadStepImagesRecursive(mainImageUrl, index + 1, stepDataList)
                }
            }.addOnFailureListener {
                stepDataList.add(hashMapOf("text" to text, "imageUrl" to ""))
                uploadStepImagesRecursive(mainImageUrl, index + 1, stepDataList)
            }
        } else {
            if (text.isNotEmpty()) {
                stepDataList.add(hashMapOf("text" to text, "imageUrl" to ""))
            }
            uploadStepImagesRecursive(mainImageUrl, index + 1, stepDataList)
        }
    }

    private fun saveToFirestore(mainImageUrl: String, stepsData: List<HashMap<String, String>>) {
        val user = Firebase.auth.currentUser ?: return
        val db = Firebase.firestore

        // Collect 3-Part Ingredients
        val ingredientsList = mutableListOf<Map<String, String>>()
        for (i in 0 until ingredientsContainer.childCount) {
            val row = ingredientsContainer.getChildAt(i)
            val nameInput = row.findViewById<EditText>(R.id.ingredient_name_input)
            val quantityInput = row.findViewById<EditText>(R.id.ingredient_quantity_input)
            val unitSpinner = row.findViewById<Spinner>(R.id.ingredient_unit_spinner)

            val name = nameInput.text.toString().trim()
            val quantity = quantityInput.text.toString().trim()
            val unit = unitSpinner.selectedItem.toString()

            if (name.isNotEmpty()) {
                ingredientsList.add(mapOf("name" to name, "quantity" to quantity, "unit" to unit))
            }
        }

        db.collection("users").document(user.uid).get().addOnSuccessListener { userDoc ->
            val authorName = userDoc.getString("name") ?: "Chef"

            val recipeMap = hashMapOf(
                "authorId" to user.uid,
                "authorName" to authorName,
                "title" to inputName.text.toString(),
                "description" to inputDesc.text.toString(),
                "category" to spinnerCategory.selectedItem.toString(),
                "prepTime" to inputTime.text.toString(),
                "servings" to inputServings.text.toString(),
                "difficulty" to spinnerDifficulty.selectedItem.toString(),
                "imageUrl" to mainImageUrl,
                "ingredients" to ingredientsList, // Now List<Map>
                "steps" to stepsData,
                "likes" to 0,
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("recipes").add(recipeMap)
                .addOnSuccessListener {
                    setLoading(false)
                    Toast.makeText(context, "Recipe Published!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                .addOnFailureListener {
                    setLoading(false)
                    Toast.makeText(context, "Error saving recipe", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Helpers
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
        btnPublish.text = if (isLoading) "Publishing..." else "レシピを公開"
    }
}