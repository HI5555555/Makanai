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

class AddPostFragment : Fragment() {

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

    // Map to store Step View -> Image Uri
    // We use the View object itself as the key to know which step has which image
    private val stepImageUris = HashMap<View, Uri>()

    // Track which view requested the image picker
    private var currentImageTargetView: View? = null
    private var isPickingMainImage = true

    // Image Picker
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (isPickingMainImage) {
                // Set Main Image
                mainImageUri = uri
                mainUploadPlaceholder.visibility = View.GONE
                mainImagePreview.visibility = View.VISIBLE
                mainImagePreview.load(uri)
            } else {
                // Set Step Image
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_post, container, false)
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

        setupSpinner(spinnerCategory, R.array.recipe_categories)
        setupSpinner(spinnerDifficulty, R.array.recipe_difficulties)

        // Listeners
        view.findViewById<View>(R.id.image_upload_container).setOnClickListener {
            isPickingMainImage = true
            pickImage.launch("image/*")
        }

        view.findViewById<TextView>(R.id.btn_add_ingredient).setOnClickListener { addIngredientInput() }
        view.findViewById<TextView>(R.id.btn_add_step).setOnClickListener { addStepInput() }
        view.findViewById<ImageButton>(R.id.back_button).setOnClickListener { findNavController().popBackStack() }
        btnPublish.setOnClickListener { startUploadProcess() }

        // Initial Fields
        if (ingredientsContainer.childCount > 0) {
            setupDeleteButton(ingredientsContainer.getChildAt(0), ingredientsContainer, false)
        }
        if (stepsContainer.childCount > 0) {
            // The XML <include> doesn't automatically have the listener,
            // we remove it and re-add via code to ensure consistency or just setup listener
            val initialStep = stepsContainer.getChildAt(0)
            stepsContainer.removeView(initialStep)
            addStepInput() // Add fresh logic-connected step
        } else {
            addStepInput()
        }
        addIngredientInput() // Ensure one ingredient
    }

    // --- LOGIC ---

    private fun addStepInput() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_step_input, stepsContainer, false)

        // Setup Step Number
        val stepNum = view.findViewById<TextView>(R.id.step_number)
        stepNum.text = (stepsContainer.childCount + 1).toString()

        // Setup Delete
        val btnRemove = view.findViewById<ImageButton>(R.id.remove_button)
        btnRemove.setOnClickListener {
            stepsContainer.removeView(view)
            stepImageUris.remove(view) // Remove image if step is deleted
            renumberSteps()
        }

        // Setup Image Click
        val imageContainer = view.findViewById<View>(R.id.step_image_container)
        imageContainer.setOnClickListener {
            isPickingMainImage = false
            currentImageTargetView = view
            pickImage.launch("image/*")
        }

        stepsContainer.addView(view)
    }

    private fun addIngredientInput() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_ingredient_input, ingredientsContainer, false)
        val btnRemove = view.findViewById<ImageButton>(R.id.remove_button)
        btnRemove.setOnClickListener { ingredientsContainer.removeView(view) }
        ingredientsContainer.addView(view)
    }

    // --- UPLOAD PROCESS ---

    private fun startUploadProcess() {
        if (mainImageUri == null || inputName.text.isEmpty()) {
            Toast.makeText(context, "Title and Main Image are required", Toast.LENGTH_SHORT).show()
            return
        }
        setLoading(true)

        // 1. Upload Main Image
        val filename = UUID.randomUUID().toString()
        val ref = Firebase.storage.reference.child("recipe_images/$filename.jpg")

        ref.putFile(mainImageUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    // Main image done, now upload step images
                    uploadStepImagesRecursive(uri.toString(), 0, mutableListOf())
                }
            }
            .addOnFailureListener {
                setLoading(false)
                Toast.makeText(context, "Main image upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    // Recursive function to upload step images one by one
    private fun uploadStepImagesRecursive(mainImageUrl: String, index: Int, stepDataList: MutableList<HashMap<String, String>>) {
        if (index >= stepsContainer.childCount) {
            // All steps processed, save to Firestore
            saveToFirestore(mainImageUrl, stepDataList)
            return
        }

        val stepView = stepsContainer.getChildAt(index)
        val text = stepView.findViewById<EditText>(R.id.step_input).text.toString()
        val imageUri = stepImageUris[stepView]

        if (imageUri != null) {
            // This step has an image, upload it
            val filename = UUID.randomUUID().toString()
            val ref = Firebase.storage.reference.child("recipe_step_images/$filename.jpg")

            ref.putFile(imageUri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                    // Add step data with image URL
                    stepDataList.add(hashMapOf("text" to text, "imageUrl" to downloadUrl.toString()))
                    // Next step
                    uploadStepImagesRecursive(mainImageUrl, index + 1, stepDataList)
                }
            }.addOnFailureListener {
                // If upload fails, maybe save without image or stop? Let's save without image for robustness
                stepDataList.add(hashMapOf("text" to text, "imageUrl" to ""))
                uploadStepImagesRecursive(mainImageUrl, index + 1, stepDataList)
            }
        } else {
            // No image for this step
            if (text.isNotEmpty()) {
                stepDataList.add(hashMapOf("text" to text, "imageUrl" to ""))
            }
            uploadStepImagesRecursive(mainImageUrl, index + 1, stepDataList)
        }
    }

    private fun saveToFirestore(mainImageUrl: String, stepsData: List<HashMap<String, String>>) {
        val user = Firebase.auth.currentUser ?: return

        // Collect Ingredients
        val ingredientsList = mutableListOf<String>()
        for (i in 0 until ingredientsContainer.childCount) {
            val text = ingredientsContainer.getChildAt(i).findViewById<EditText>(R.id.ingredient_input).text.toString()
            if (text.isNotEmpty()) ingredientsList.add(text)
        }

        val recipeMap = hashMapOf(
            "authorId" to user.uid,
            "authorName" to (user.displayName ?: "Chef"),
            "title" to inputName.text.toString(),
            "description" to inputDesc.text.toString(),
            "category" to spinnerCategory.selectedItem.toString(),
            "prepTime" to inputTime.text.toString(),
            "servings" to inputServings.text.toString(),
            "difficulty" to spinnerDifficulty.selectedItem.toString(),
            "imageUrl" to mainImageUrl,
            "ingredients" to ingredientsList,
            "steps" to stepsData, // Now saving structured data for steps
            "likes" to 0,
            "createdAt" to System.currentTimeMillis()
        )

        Firebase.firestore.collection("recipes").add(recipeMap)
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

    // Helpers
    private fun setupSpinner(spinner: Spinner, arrayResId: Int) {
        ArrayAdapter.createFromResource(requireContext(), arrayResId, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = it
        }
    }

    private fun setupDeleteButton(view: View, container: LinearLayout, isStep: Boolean) {
        view.findViewById<ImageButton>(R.id.remove_button).setOnClickListener {
            container.removeView(view)
            if(isStep) {
                stepImageUris.remove(view)
                renumberSteps()
            }
        }
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