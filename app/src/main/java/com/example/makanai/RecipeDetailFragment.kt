package com.example.makanai

import android.app.AlertDialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Date
import java.util.UUID
import android.view.LayoutInflater
class RecipeDetailFragment : Fragment(R.layout.fragment_recipe_detail) {

    private val args: RecipeDetailFragmentArgs by navArgs()
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val storage = Firebase.storage

    // AI Model
    private val API_KEY = BuildConfig.GEMINI_API_KEY
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = API_KEY
    )

    // UI Variables
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var commentInput: EditText
    private lateinit var commentsTitle: TextView
    private lateinit var previewCard: CardView
    private lateinit var previewImage: ImageView

    // AI UI Variables
    private lateinit var aiButton: LinearLayout
    private lateinit var aiPanel: LinearLayout
    private lateinit var tvCal: TextView
    private lateinit var tvProtein: TextView
    private lateinit var tvFat: TextView
    private lateinit var tvCarb: TextView
    private lateinit var tvTips: TextView

    // Detailed Nutrients UI
    private lateinit var tvFiberVal: TextView
    private lateinit var pbFiber: ProgressBar
    private lateinit var tvVitVal: TextView
    private lateinit var pbVit: ProgressBar
    private lateinit var tvMinVal: TextView
    private lateinit var pbMin: ProgressBar

    private var selectedCommentImageUri: Uri? = null
    private var currentIngredientsText: String = "" // Store ingredients for AI

    private val pickImage = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedCommentImageUri = uri
            previewCard.visibility = View.VISIBLE
            previewImage.load(uri) { crossfade(true) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. Find Views ---
        val recipeImage: ImageView = view.findViewById(R.id.detail_recipe_image)
        val title: TextView = view.findViewById(R.id.detail_recipe_title)
        val authorName: TextView = view.findViewById(R.id.author_name)
        val authorDesc: TextView = view.findViewById(R.id.author_desc)
        val authorImage: ImageView = view.findViewById(R.id.author_image)
        val description: TextView = view.findViewById(R.id.detail_description)
        val prepTime: TextView = view.findViewById(R.id.detail_prep_time)
        val servings: TextView = view.findViewById(R.id.detail_servings)
        val difficulty: TextView = view.findViewById(R.id.detail_difficulty)
        val ingredientsList: TextView = view.findViewById(R.id.detail_ingredients_list)
        val stepsContainer: LinearLayout = view.findViewById(R.id.detail_steps_container)
        val backButton: ImageButton = view.findViewById(R.id.back_button)
        val likeButton: ImageButton = view.findViewById(R.id.like_button)
        val deleteButton: ImageButton = view.findViewById(R.id.delete_button)
        val editButton: ImageButton = view.findViewById(R.id.edit_button)

        val commentsRecyclerView: RecyclerView = view.findViewById(R.id.comments_recycler_view)
        commentsTitle = view.findViewById(R.id.comments_title)
        commentInput = view.findViewById(R.id.comment_input)
        val sendButton: ImageButton = view.findViewById(R.id.comment_send_button)
        val addImageButton: ImageButton = view.findViewById(R.id.btn_add_comment_image)
        previewCard = view.findViewById(R.id.comment_image_preview_card)
        previewImage = view.findViewById(R.id.comment_image_preview)
        val removeImageButton: ImageButton = view.findViewById(R.id.remove_comment_image)

        // AI Views
        aiButton = view.findViewById(R.id.btn_ai_analysis)
        aiPanel = view.findViewById(R.id.ai_analysis_panel)
        tvCal = view.findViewById(R.id.ai_cal)
        tvProtein = view.findViewById(R.id.ai_protein)
        tvFat = view.findViewById(R.id.ai_fat)
        tvCarb = view.findViewById(R.id.ai_carbs)
        tvTips = view.findViewById(R.id.ai_health_tips)

        // Detailed Nutrient Views
        tvFiberVal = view.findViewById(R.id.ai_fiber_val)
        pbFiber = view.findViewById(R.id.ai_fiber_progress)
        tvVitVal = view.findViewById(R.id.ai_vit_val)
        pbVit = view.findViewById(R.id.ai_vit_progress)
        tvMinVal = view.findViewById(R.id.ai_min_val)
        pbMin = view.findViewById(R.id.ai_min_progress)

        val recipeId = args.recipeId.toString()

        // --- 2. Fetch Recipe Data ---
        db.collection("recipes").document(recipeId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val recipe = document.toObject(Recipe::class.java)
                    recipe?.let { r ->
                        if (r.imageUrl.isNotEmpty()) recipeImage.load(r.imageUrl) { crossfade(true) }
                        title.text = r.title
                        description.text = r.description
                        prepTime.text = r.prepTime
                        servings.text = r.servings
                        difficulty.text = r.difficulty

                        // Format Ingredients
                        val formattedIngredients = StringBuilder()
                        for (item in r.ingredients) {
                            if (item is Map<*, *>) {
                                val name = item["name"] ?: ""
                                val qty = item["quantity"] ?: ""
                                val unit = item["unit"] ?: ""
                                if (name.toString().isNotEmpty()) {
                                    formattedIngredients.append("・ $name $qty$unit\n")
                                }
                            } else {
                                formattedIngredients.append("・ $item\n")
                            }
                        }
                        currentIngredientsText = formattedIngredients.toString().trim()
                        ingredientsList.text = currentIngredientsText

                        // Load Steps
                        stepsContainer.removeAllViews()
                        r.steps.forEachIndexed { index, stepMap ->
                            val stepView = LayoutInflater.from(context).inflate(R.layout.item_step_display, stepsContainer, false)
                            stepView.findViewById<TextView>(R.id.step_number).text = (index + 1).toString()
                            stepView.findViewById<TextView>(R.id.step_text).text = stepMap["text"] ?: ""
                            val imgUrl = stepMap["imageUrl"]
                            if (!imgUrl.isNullOrEmpty()) {
                                val imgCard = stepView.findViewById<CardView>(R.id.step_image_card)
                                val imgView = stepView.findViewById<ImageView>(R.id.step_image)
                                imgCard.visibility = View.VISIBLE
                                imgView.load(imgUrl) { crossfade(true) }
                            }
                            stepsContainer.addView(stepView)
                        }

                        if (r.authorId.isNotEmpty()) loadAuthorInfo(r.authorId, authorName, authorImage, authorDesc)

                        // Check Ownership
                        val currentUser = auth.currentUser
                        if (currentUser != null && r.authorId == currentUser.uid) {
                            deleteButton.visibility = View.VISIBLE
                            editButton.visibility = View.VISIBLE
                            deleteButton.setOnClickListener { showDeleteConfirmationDialog(recipeId) }
                            editButton.setOnClickListener {
                                val action = RecipeDetailFragmentDirections.actionRecipeDetailFragmentToEditPostFragment(recipeId)
                                findNavController().navigate(action)
                            }
                        }
                    }
                }
            }

        // --- 3. AI Analysis Button Logic ---
        aiButton.setOnClickListener {
            if (aiPanel.visibility == View.VISIBLE) {
                aiPanel.visibility = View.GONE
            } else {
                aiPanel.visibility = View.VISIBLE
                // Only analyze if we have ingredients and haven't done it yet
                if (currentIngredientsText.isNotEmpty() && tvCal.text == "--- kcal") {
                    performAiAnalysis(currentIngredientsText)
                }
            }
        }

        // --- 4. Like Logic ---
        setupLikeButton(recipeId, likeButton)

        // --- 5. Comments ---
        commentAdapter = CommentAdapter(emptyList())
        commentsRecyclerView.adapter = commentAdapter
        commentsRecyclerView.isNestedScrollingEnabled = false // Fix nested scroll issue
        fetchComments(recipeId)

        // --- 6. Listeners ---
        backButton.setOnClickListener { findNavController().popBackStack() }
        addImageButton.setOnClickListener { pickImage.launch("image/*") }
        removeImageButton.setOnClickListener { selectedCommentImageUri = null; previewCard.visibility = View.GONE }
        sendButton.setOnClickListener { handleSendComment(recipeId) }
    }

    // --- AI ANALYSIS FUNCTION ---
    private fun performAiAnalysis(ingredients: String) {
        tvTips.text = "AIが分析中..."
        // Reset bars
        pbFiber.progress = 0
        pbVit.progress = 0
        pbMin.progress = 0

        val prompt = """
            You are a nutritionist. Analyze these ingredients for 1 serving:
            $ingredients
            
            Output ONLY a JSON object. NO markdown. NO explanation.
            Target language: Japanese (日本語).
            
            Required JSON Format:
            {
              "calories": "123 kcal",
              "protein": "12g",
              "fat": "10g",
              "carbs": "30g",
              "fiber": "5g",          // String value
              "fiber_score": 50,      // Integer 0-100 for progress bar
              "vitamin": "ビタミンC",   // Main vitamin found
              "vitamin_score": 80,    // Integer 0-100
              "mineral": "鉄分",       // Main mineral found
              "mineral_score": 40,    // Integer 0-100
              "health_benefits": "• Point 1\n• Point 2\n• Point 3"
            }
        """.trimIndent()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                var text = response.text ?: ""
                text = text.replace("```json", "").replace("```", "").trim()

                val json = JSONObject(text)

                // Macros
                tvCal.text = json.optString("calories", "---")
                tvProtein.text = json.optString("protein", "---")
                tvFat.text = json.optString("fat", "---")
                tvCarb.text = json.optString("carbs", "---")

                // Details
                tvFiberVal.text = json.optString("fiber", "-")
                tvVitVal.text = json.optString("vitamin", "-")
                tvMinVal.text = json.optString("mineral", "-")

                // Progress Bars (Animate or Set)
                pbFiber.setProgress(json.optInt("fiber_score", 0), true)
                pbVit.setProgress(json.optInt("vitamin_score", 0), true)
                pbMin.setProgress(json.optInt("mineral_score", 0), true)

                // Health Tips
                tvTips.text = json.optString("health_benefits", "解析できませんでした")

            } catch (e: Exception) {
                Log.e("AI_Analysis", "Error", e)
                tvTips.text = "分析エラー: サーバーが混み合っています"
            }
        }
    }

    // --- HELPER FUNCTIONS ---

    private fun setupLikeButton(recipeId: String, likeButton: ImageButton) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val recipeRef = db.collection("recipes").document(recipeId)
            recipeRef.addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
                if (likedBy.contains(currentUser.uid)) {
                    likeButton.setImageResource(R.drawable.ic_heart_outline)
                    likeButton.setColorFilter(Color.parseColor("#FF6347"))
                } else {
                    likeButton.setImageResource(R.drawable.ic_heart_outline)
                    likeButton.setColorFilter(Color.BLACK)
                }
            }
            likeButton.setOnClickListener {
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(recipeRef)
                    val currentLikes = snapshot.getLong("likes") ?: 0
                    val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
                    if (likedBy.contains(currentUser.uid)) {
                        transaction.update(recipeRef, "likes", if(currentLikes>0) currentLikes-1 else 0)
                        transaction.update(recipeRef, "likedBy", FieldValue.arrayRemove(currentUser.uid))
                    } else {
                        transaction.update(recipeRef, "likes", currentLikes + 1)
                        transaction.update(recipeRef, "likedBy", FieldValue.arrayUnion(currentUser.uid))
                    }
                }
            }
        } else {
            likeButton.setOnClickListener { Toast.makeText(context, "Please login", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun handleSendComment(recipeId: String) {
        val text = commentInput.text.toString().trim()
        val user = auth.currentUser
        if (user == null) { Toast.makeText(context, "Login required", Toast.LENGTH_SHORT).show(); return }
        if (text.isEmpty() && selectedCommentImageUri == null) return

        commentInput.isEnabled = false
        Toast.makeText(context, "Posting...", Toast.LENGTH_SHORT).show()

        if (selectedCommentImageUri != null) {
            val filename = UUID.randomUUID().toString()
            val ref = storage.reference.child("comment_images/$filename.jpg")
            ref.putFile(selectedCommentImageUri!!).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri -> saveCommentToFirestore(recipeId, user, text, uri.toString()) }
            }.addOnFailureListener { commentInput.isEnabled = true }
        } else {
            saveCommentToFirestore(recipeId, user, text, "")
        }
    }

    private fun saveCommentToFirestore(recipeId: String, user: com.google.firebase.auth.FirebaseUser, text: String, imageUrl: String) {
        db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
            val authorName = document.getString("name") ?: "User"
            val authorImage = document.getString("profileImageUrl") ?: ""
            val newComment = Comment("", user.uid, authorName, authorImage, text, imageUrl, Date())

            db.collection("recipes").document(recipeId).collection("comments").add(newComment)
                .addOnSuccessListener {
                    commentInput.setText("")
                    commentInput.isEnabled = true
                    selectedCommentImageUri = null
                    previewCard.visibility = View.GONE
                    Toast.makeText(context, "Posted!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun fetchComments(recipeId: String) {
        db.collection("recipes").document(recipeId).collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, _ ->
                val list = snapshots?.toObjects(Comment::class.java) ?: emptyList()
                commentAdapter.updateData(list)
                commentsTitle.text = "コメント (${list.size})"
            }
    }

    private fun loadAuthorInfo(authorId: String, nameView: TextView, imageView: ImageView, descView: TextView) {
        db.collection("users").document(authorId).get().addOnSuccessListener { userDoc ->
            if (userDoc.exists()) {
                nameView.text = userDoc.getString("name") ?: "Chef"
                descView.text = userDoc.getString("bio") ?: "Home Cook"
                val photoUrl = userDoc.getString("profileImageUrl")
                if (!photoUrl.isNullOrEmpty()) imageView.load(photoUrl) { transformations(CircleCropTransformation()); placeholder(R.drawable.ic_profile) }
            }
        }
    }

    private fun showDeleteConfirmationDialog(recipeId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete")
            .setMessage("Delete this recipe?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("recipes").document(recipeId).delete().addOnSuccessListener { findNavController().popBackStack() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}