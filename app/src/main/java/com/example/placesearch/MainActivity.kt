package com.example.placesearch

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import com.example.placesearch.databinding.ActivityMainBinding
import com.example.placesearch.model.SearchResult
import com.example.placesearch.network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var lon=""
    private var lat=""
    private var name=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lon=""
        lat=""
        name=""

        binding.searchAuto.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(query: CharSequence?, p1: Int, p2: Int, p3: Int) {
                getResults(query.toString())
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

        binding.map.setOnClickListener {
            if (TextUtils.isEmpty(lat)){
                Toast.makeText(this, "Please provide location", Toast.LENGTH_SHORT).show()
            }else{
                startActivity(Intent(this,MapActivity::class.java).apply {
                    putExtra("lat",lat)
                    putExtra("lon",lon)
                })
            }
        }
    }

    private fun getResults(query:String) {
        binding.progress.visibility=View.VISIBLE
        ApiService().search(query).enqueue(object :Callback<List<SearchResult>>{
            override fun onResponse(
                call: Call<List<SearchResult>>,
                response: Response<List<SearchResult>>
            ) {
                if (!response.isSuccessful){
                    binding.progress.visibility=View.GONE
                    Log.d("MainActivity","response code ${response.code()}")
                    return
                }
                if (response.body() != null){
                    val autoadapter=ArrayAdapter(this@MainActivity,R.layout.drop_down,response.body()!!)
                    (binding.searchAuto as? AutoCompleteTextView)?.apply {
                        setAdapter(autoadapter)
                        binding.progress.visibility=View.GONE
                        setOnItemClickListener { adapterView, view, i, l ->
                            val item=autoadapter.getItem(i) as? SearchResult
                            lat= item?.lat.toString()
                            lon=item?.lon.toString()
                            name=item?.display_name.toString()
                            binding.lat.text="Latitude:$lat"
                            binding.lon.text="Longitude:$lon"
                            binding.name.text=name
                        }
                    }
                    autoadapter.notifyDataSetChanged()
                }
            }
            override fun onFailure(call: Call<List<SearchResult>>, t: Throwable) {
                binding.progress.visibility=View.GONE
                Log.d("MainActivity",t.message.toString())
            }

        })
    }
}