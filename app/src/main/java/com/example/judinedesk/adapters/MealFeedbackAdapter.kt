package com.example.judinedesk.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.judinedesk.R
import com.example.judinedesk.models.MealFeedback
import java.text.SimpleDateFormat
import java.util.*

class MealFeedbackAdapter(private var feedbackList: List<MealFeedback>) :
    RecyclerView.Adapter<MealFeedbackAdapter.FeedbackViewHolder>() {

    class FeedbackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStudentName: TextView = itemView.findViewById(R.id.tvStudentName)
        val tvFeedback: TextView = itemView.findViewById(R.id.tvFeedback)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feedback, parent, false)
        return FeedbackViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
        val feedback = feedbackList[position]

        holder.tvStudentName.text = feedback.studentName
        holder.tvFeedback.text = feedback.feedback
        holder.ratingBar.rating = feedback.rating

        val timeFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        holder.tvTime.text = timeFormat.format(Date(feedback.timestamp))
    }

    override fun getItemCount(): Int = feedbackList.size

    fun updateData(newFeedback: List<MealFeedback>) {
        feedbackList = newFeedback
        notifyDataSetChanged()
    }
}