package com.example.placesearch.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.placesearch.R
import com.example.placesearch.databinding.AlertLayoutBinding
import com.example.placesearch.databinding.SearchLayoutBinding
import com.example.placesearch.model.SearchResult

class SearchAdapter(private var searchlist:List<SearchResult>,
                    private val context: Context):RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    class ViewHolder(val binding: SearchLayoutBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view=SearchLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return searchlist.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result=searchlist[position]
        holder.binding.place.text=result.display_name

        holder.itemView.setOnClickListener {
            showAlert(result)
        }
    }

    private fun showAlert(result: SearchResult) {
        val view=LayoutInflater.from(context).inflate(R.layout.alert_layout,null)
        val alertbinding=AlertLayoutBinding.bind(view)
        alertbinding.location.text=result.display_name
        alertbinding.latitude.text="Latitide:${result.lat}"
        alertbinding.longitude.text="Longitude:${result.lon}"
        AlertDialog.Builder(context)
            .setTitle("Location Details")
            .setPositiveButton("Ok"){dialog,_->
                dialog.dismiss()
            }
            .setView(view)
            .create().show()
    }
}