package com.example.makanai

import android.app.AlertDialog // Added for Delete Dialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
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

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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

        // Delete and Edit Buttons
        val deleteButton: ImageButton = view.findViewById(R.id.delete_button)
        val editButton: ImageButton = view.findViewById(R.id.edit_button)

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
                        // Load Main Info
                        if (r.imageUrl.isNotEmpty()) {
                            recipeImage.load(r.imageUrl) { crossfade(true) }
                        }
                        title.text = r.title
                        description.text = r.description
                        prepTime.text = r.prepTime
                        servings.text = r.servings
                        difficulty.text = r.difficulty

                        // --- Format Ingredients (Handling new Map structure) ---
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
                                // Fallback for old string data
                                formattedIngredients.append("・ $item\n")
                            }
                        }
                        ingredientsList.text = formattedIngredients.toString().trim()

                        // --- Load Steps (Dynamic) ---
                        stepsContainer.removeAllViews()
                        r.steps.forEachIndexed { index, stepMap ->
                            val stepView = LayoutInflater.from(context).inflate(R.layout.item_step_display, stepsContainer, false)

                            stepView.findViewById<TextView>(R.id.step_number).text = (index + 1).toString()
                            stepView.findViewById<TextView>(R.id.step_text).text = stepMap["text"] ?: ""

                            val imgUrl = stepMap["imageUrl"]
                            val imgCard = stepView.findViewById<CardView>(R.id.step_image_card)
                            val imgView = stepView.findViewById<ImageView>(R.id.step_image)

                            if (!imgUrl.isNullOrEmpty()) {
                                imgCard.visibility = View.VISIBLE
                                imgView.load(imgUrl) { crossfade(true) }
                            } else {
                                imgCard.visibility = View.GONE
                            }
                            stepsContainer.addView(stepView)
                        }

                        // --- Load Author Info ---
                        if (r.authorId.isNotEmpty()) {
                            loadAuthorInfo(r.authorId, authorName, authorImage, authorDesc)
                        } else {
                            authorName.text = "Unknown Chef"
                        }

                        // --- Check Ownership (Show Edit/Delete) ---
                        val currentUser = auth.currentUser
                        if (currentUser != null && r.authorId == currentUser.uid) {
                            deleteButton.visibility = View.VISIBLE
                            editButton.visibility = View.VISIBLE

                            deleteButton.setOnClickListener {
                                showDeleteConfirmationDialog(recipeId)
                            }
                            editButton.setOnClickListener {
                                val action = RecipeDetailFragmentDirections.actionRecipeDetailFragmentToEditPostFragment(recipeId)
                                findNavController().navigate(action)
                            }
                        } else {
                            deleteButton.visibility = View.GONE
                            editButton.visibility = View.GONE
                        }
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
                    likeButton.setImageResource(R.drawable.ic_heart_outline) // Should be filled icon ideally
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

        // --- 5. Setup Comments ---
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

    // --- HELPER FUNCTIONS ---

    private fun loadAuthorInfo(authorId: String, nameView: TextView, imageView: ImageView, descView: TextView) {
        db.collection("users").document(authorId).get().addOnSuccessListener { userDoc ->
            if (userDoc.exists()) {
                nameView.text = userDoc.getString("name") ?: "Chef"
                descView.text = userDoc.getString("bio") ?: "Home Cook"
                val photoUrl = userDoc.getString("profileImageUrl")
                if (!photoUrl.isNullOrEmpty()) {
                    imageView.load(photoUrl) {
                        transformations(CircleCropTransformation())
                        placeholder(R.drawable.ic_profile)
                    }
                }
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

    private fun handleSendComment(recipeId: String) {
        val text = commentInput.text.toString().trim()
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "Please login", Toast.LENGTH_SHORT).show()
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

    private fun showDeleteConfirmationDialog(recipeId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Recipe")
            .setMessage("Are you sure you want to delete this recipe? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteRecipe(recipeId)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteRecipe(recipeId: String) {
        db.collection("recipes").document(recipeId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Recipe deleted", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error deleting recipe: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}