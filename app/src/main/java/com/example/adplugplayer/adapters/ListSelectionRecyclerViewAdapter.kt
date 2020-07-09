package com.example.adplugplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.adplugplayer.R

class ListSelectionRecyclerViewAdapter(

    val listSong: MutableList<String>, val mOnListListener: OnListListener) :
    RecyclerView.Adapter<ListSelectionRecyclerViewAdapter.ListSelectionViewHolder>() {

    var mIdItemSelected = -1

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ListSelectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.list_selection_view_holder,
                parent,
                false)

        return ListSelectionViewHolder(view, mOnListListener)
    }

    override fun getItemCount(): Int {
        return listSong.size
    }

    override fun onBindViewHolder(holder: ListSelectionViewHolder, position: Int) {
        holder.listTitle.text = listSong[position]
    }

    public class ListSelectionViewHolder :
        RecyclerView.ViewHolder, View.OnClickListener {

        val listTitle: TextView
        val onListListener: OnListListener

        constructor(itemView: View, onListListener: OnListListener) : super(itemView) {
            this.listTitle = itemView.findViewById(R.id.itemString)
            this.onListListener = onListListener
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            onListListener.onListClick(adapterPosition)
        }
    }

    interface OnListListener {
        fun onListClick(position: Int)
    }

}