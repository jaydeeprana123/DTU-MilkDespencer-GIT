package com.imdc.milkdespencer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.imdc.milkdespencer.R
import com.imdc.milkdespencer.adapter.UserAdapter.UserViewHolder
import com.imdc.milkdespencer.roomdb.entities.User

class UserAdapter(private val userList: List<User>) : RecyclerView.Adapter<UserViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameTextView: TextView

        init {
            usernameTextView =
                itemView.findViewById(R.id.usernameTextView) // Replace with your TextView id
        }

        fun bind(user: User) {
            usernameTextView.text = user.username
            /*if (user.getUserType() != 0) {
            }*/
            // Bind other user data if needed
        }
    }
}
