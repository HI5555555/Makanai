package com.example.makanai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
// We no longer need the ShapeableImageView import

class CommentAdapter(private val comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // This line is now a simple ImageView
        val authorImage: ImageView = itemView.findViewById(R.id.comment_author_image)

        val authorName: TextView = itemView.findViewById(R.id.comment_author_name)
        val timestamp: TextView = itemView.findViewById(R.id.comment_timestamp)
        val text: TextView = itemView.findViewById(R.id.comment_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]

        holder.authorImage.setImageResource(comment.authorImageResId)
        holder.authorName.text = comment.authorName
        holder.timestamp.text = comment.timestamp
        holder.text.text = comment.text
    }

    override fun getItemCount() = comments.size
}