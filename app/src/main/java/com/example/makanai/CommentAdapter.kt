package com.example.makanai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import java.text.SimpleDateFormat
import java.util.Locale

class CommentAdapter(private var comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val authorImage: ImageView = itemView.findViewById(R.id.comment_author_image)
        val authorName: TextView = itemView.findViewById(R.id.comment_author_name)
        val timestamp: TextView = itemView.findViewById(R.id.comment_timestamp)
        val text: TextView = itemView.findViewById(R.id.comment_text)
        // These might be null if you haven't updated item_comment.xml yet
        val commentImageCard: CardView? = itemView.findViewById(R.id.comment_image_card)
        val commentImage: ImageView? = itemView.findViewById(R.id.comment_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]

        // 1. Author Profile Pic
        if (comment.authorImageUrl.isNotEmpty()) {
            holder.authorImage.load(comment.authorImageUrl) {
                transformations(CircleCropTransformation())
                placeholder(R.drawable.ic_profile)
            }
        } else {
            holder.authorImage.setImageResource(R.drawable.ic_profile)
        }

        holder.authorName.text = comment.authorName
        holder.text.text = comment.text

        val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
        holder.timestamp.text = sdf.format(comment.timestamp)

        // 2. Comment Image Logic
        if (holder.commentImageCard != null && holder.commentImage != null) {
            if (comment.commentImageUrl.isNotEmpty()) {
                holder.commentImageCard.visibility = View.VISIBLE
                holder.commentImage.load(comment.commentImageUrl) {
                    crossfade(true)
                }
            } else {
                holder.commentImageCard.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = comments.size

    fun updateData(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }
}