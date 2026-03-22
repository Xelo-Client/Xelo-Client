package com.origin.launcher.Adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.origin.launcher.R

class CreditsAdapter(
    private val context: Context,
    private val cards: List<CreditCard>
) : RecyclerView.Adapter<CreditsAdapter.ViewHolder>() {

    data class CreditCard(
        val handle: String,
        val username: String,
        val picUrl: String,
        val tagline: String
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(context)
            .inflate(R.layout.credit_card_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val c = cards[position]

        Glide.with(context).load(c.picUrl).circleCrop().into(holder.profilePic)
        holder.profileName.text = c.username
        holder.profileTagline.text = c.tagline

        when (c.handle) {
            "Yami" -> {
                val gold = Color.parseColor("#FFD700")
                holder.profileName.setTextColor(gold)
                holder.profileTagline.setTextColor(gold)
            }
            "VCX" -> {
                val neonBlue = Color.parseColor("#1F51FF")
                holder.profileName.setTextColor(neonBlue)
                holder.profileTagline.setTextColor(neonBlue)
            }
            else -> {
                holder.profileName.setTextColor(Color.WHITE)
                holder.profileTagline.setTextColor(Color.WHITE)
            }
        }

        holder.itemView.setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/${c.username}"))
            context.startActivity(i)
        }
    }

    override fun getItemCount(): Int = cards.size

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val profilePic: ImageView = v.findViewById(R.id.profile_pic)
        val profileName: TextView = v.findViewById(R.id.profile_name)
        val profileTagline: TextView = v.findViewById(R.id.profile_tagline)
    }
}