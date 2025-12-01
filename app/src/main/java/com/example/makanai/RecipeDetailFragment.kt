package com.example.makanai

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.Date
import java.util.UUID

class RecipeDetailFragment : Fragment(R.layout.fragment_recipe_detail) {

    private val args: RecipeDetailFragmentArgs by navArgs()
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val storage = Firebase.storage

    // Comment UI Variables
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var commentInput: EditText
    private lateinit var commentsTitle: TextView

    // Comment Image Variables
    private lateinit var previewCard: CardView
    private lateinit var previewImage: ImageView
    private var selectedCommentImageUri: Uri? = null

    // Image Picker Launcher
    private val pickImage = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedCommentImageUri = uri
            previewCard.visibility = View.VISIBLE
            previewImage.load(uri) {
                crossfade(true)
            }
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

        // The Container for dynamic step layouts
        val stepsContainer: LinearLayout = view.findViewById(R.id.detail_steps_container)

        val backButton: ImageButton = view.findViewById(R.id.back_button)
        val likeButton: ImageButton = view.findViewById(R.id.like_button)

        // Comment Views
        val commentsRecyclerView: RecyclerView = view.findViewById(R.id.comments_recycler_view)
        commentsTitle = view.findViewById(R.id.comments_title)
        commentInput = view.findViewById(R.id.comment_input)
        val sendButton: ImageButton = view.findViewById(R.id.comment_send_button)

        val addImageButton: ImageButton = view.findViewById(R.id.btn_add_comment_image)
        previewCard = view.findViewById(R.id.comment_image_preview_card)
        previewImage = view.findViewById(R.id.comment_image_preview)
        val removeImageButton: ImageButton = view.findViewById(R.id.remove_comment_image)

        // --- 2. Get Recipe ID ---
        val recipeId = args.recipeId.toString()

        // --- 3. Fetch Recipe Data ---
        db.collection("recipes").document(recipeId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val recipe = document.toObject(Recipe::class.java)

                    recipe?.let { r ->
                        // --- MERGED UPDATE START ---

                        // Load Main Info
                        if (r.imageUrl.isNotEmpty()) {
                            recipeImage.load(r.imageUrl) { crossfade(true) }
                        }
                        title.text = r.title
                        description.text = r.description
                        prepTime.text = r.prepTime
                        servings.text = r.servings
                        difficulty.text = r.difficulty

                        // Load Ingredients (Formatted nicely)
                        // Note: Assuming Recipe.kt has List<Map<String, String>> from AddPost
                        // Format Ingredients List
                        val formattedIngredients = StringBuilder()
                        for (ingredientMap in r.ingredients) {
                            // Check if it's a Map (new structure) or String (old structure compatibility)
                            if (ingredientMap is Map<*, *>) {
                                val name = ingredientMap["name"] as? String ?: ""
                                val quantity = ingredientMap["quantity"] as? String ?: ""
                                val unit = ingredientMap["unit"] as? String ?: ""
                                if (name.isNotEmpty()) {
                                    formattedIngredients.append("・ $name $quantity$unit\n")
                                }
                            } else if (ingredientMap is String) {
                                // Fallback for old data
                                formattedIngredients.append("・ $ingredientMap\n")
                            }
                        }
                        ingredientsList.text = formattedIngredients.toString().trim()

                        // Load Steps (Dynamic Layouts)
                        stepsContainer.removeAllViews()
                        r.steps.forEachIndexed { index, stepMap ->
                            // Inflate layout
                            val stepView = LayoutInflater.from(context).inflate(R.layout.item_step_display, stepsContainer, false)

                            // Find views
                            val numView = stepView.findViewById<TextView>(R.id.step_number)
                            val textView = stepView.findViewById<TextView>(R.id.step_text)
                            val imgCard = stepView.findViewById<CardView>(R.id.step_image_card)
                            val imgView = stepView.findViewById<ImageView>(R.id.step_image)

                            // Set Data
                            numView.text = (index + 1).toString()
                            textView.text = stepMap["text"] ?: ""

                            val imgUrl = stepMap["imageUrl"]

                            if (!imgUrl.isNullOrEmpty()) {
                                imgCard.visibility = View.VISIBLE
                                imgView.load(imgUrl) {
                                    crossfade(true)
                                    placeholder(R.drawable.comment_input_background)
                                }
                            } else {
                                imgCard.visibility = View.GONE
                            }
                            stepsContainer.addView(stepView)
                        }

                        // Load Author Info
                        if (r.authorId.isNotEmpty()) {
                            loadAuthorInfo(r.authorId, authorName, authorImage, authorDesc)
                        } else {
                            authorName.text = "Unknown Chef"
                        }
                        // --- MERGED UPDATE END ---
                    }
                }
            }

        // --- 4. Like Button Logic ---
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val recipeRef = db.collection("recipes").document(recipeId)

            recipeRef.addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
                val isLiked = likedBy.contains(currentUser.uid)

                if (isLiked) {
                    likeButton.setImageResource(R.drawable.ic_heart_outline) // Use filled if available
                    likeButton.setColorFilter(Color.parseColor("#FF6347"))
                } else {
                    likeButton.setImageResource(R.drawable.ic_heart_outline)
                    likeButton.setColorFilter(Color.BLACK)
                }
            }
        }

        likeButton.setOnClickListener {
            if (currentUser == null) {
                Toast.makeText(context, "Please login to like", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val recipeRef = db.collection("recipes").document(recipeId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(recipeRef)
                val currentLikes = snapshot.getLong("likes") ?: 0
                val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()

                if (likedBy.contains(currentUser.uid)) {
                    val newLikes = if (currentLikes > 0) currentLikes - 1 else 0
                    transaction.update(recipeRef, "likes", newLikes)
                    transaction.update(recipeRef, "likedBy", FieldValue.arrayRemove(currentUser.uid))
                } else {
                    transaction.update(recipeRef, "likes", currentLikes + 1)
                    transaction.update(recipeRef, "likedBy", FieldValue.arrayUnion(currentUser.uid))
                }
            }
        }

        // --- 5. Comments Setup ---
        commentAdapter = CommentAdapter(emptyList())
        commentsRecyclerView.adapter = commentAdapter
        fetchComments(recipeId)

        // --- 6. Click Listeners ---
        backButton.setOnClickListener { findNavController().popBackStack() }

        addImageButton.setOnClickListener { pickImage.launch("image/*") }

        removeImageButton.setOnClickListener {
            selectedCommentImageUri = null
            previewCard.visibility = View.GONE
        }

        sendButton.setOnClickListener { handleSendComment(recipeId) }
    }

    // --- LOGIC FUNCTIONS ---

    private fun handleSendComment(recipeId: String) {
        val text = commentInput.text.toString().trim()
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(context, "Please login to comment", Toast.LENGTH_SHORT).show()
            return
        }

        val imageUri = selectedCommentImageUri
        if (text.isEmpty() && imageUri == null) return

        commentInput.isEnabled = false
        Toast.makeText(context, "Posting...", Toast.LENGTH_SHORT).show()

        if (imageUri != null) {
            val filename = UUID.randomUUID().toString()
            val ref = storage.reference.child("comment_images/$filename.jpg")

            ref.putFile(imageUri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    saveCommentToFirestore(recipeId, user, text, uri.toString())
                }
            }.addOnFailureListener {
                commentInput.isEnabled = true
                Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            saveCommentToFirestore(recipeId, user, text, "")
        }
    }

    private fun saveCommentToFirestore(recipeId: String, user: com.google.firebase.auth.FirebaseUser, text: String, imageUrl: String) {
        db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
            val authorName = document.getString("name") ?: "User"
            val authorImage = document.getString("profileImageUrl") ?: ""

            val newComment = Comment(
                id = "",
                authorId = user.uid,
                authorName = authorName,
                authorImageUrl = authorImage,
                text = text,
                commentImageUrl = imageUrl,
                timestamp = Date()
            )

            db.collection("recipes").document(recipeId).collection("comments")
                .add(newComment)
                .addOnSuccessListener {
                    commentInput.setText("")
                    commentInput.isEnabled = true
                    selectedCommentImageUri = null
                    previewCard.visibility = View.GONE
                    Toast.makeText(context, "Comment Posted!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    commentInput.isEnabled = true
                    Toast.makeText(context, "Failed to post", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun fetchComments(recipeId: String) {
        db.collection("recipes").document(recipeId).collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                val commentsList = mutableListOf<Comment>()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        try {
                            val comment = doc.toObject(Comment::class.java)
                            commentsList.add(comment)
                        } catch (e: Exception) {
                            Log.e("RecipeDetail", "Error parsing comment", e)
                        }
                    }
                }
                commentAdapter.updateData(commentsList)
                commentsTitle.text = "コメント (${commentsList.size})"
            }
    }

    private fun loadAuthorInfo(authorId: String, nameView: TextView, imageView: ImageView, descView: TextView) {
        db.collection("users").document(authorId).get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    val name = userDoc.getString("name")
                    val bio = userDoc.getString("bio")
                    val photoUrl = userDoc.getString("profileImageUrl")

                    nameView.text = name ?: "Chef"
                    descView.text = bio ?: "Home Cook"

                    if (!photoUrl.isNullOrEmpty()) {
                        imageView.load(photoUrl) {
                            transformations(CircleCropTransformation())
                            placeholder(R.drawable.ic_profile)
                        }
                    }
                }
            }
    }
}