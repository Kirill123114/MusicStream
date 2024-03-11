package com.smokermobilecompany.musicstream.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.smokermobilecompany.musicstream.adapter.SongsListAdapter
import com.smokermobilecompany.musicstream.databinding.ActivitySongsListBinding
import com.smokermobilecompany.musicstream.models.CategoryModel

class SongsListActivity : AppCompatActivity() {

    companion object {
        lateinit var category: CategoryModel
    }

    private lateinit var binding: ActivitySongsListBinding
    private lateinit var songsListAdapter: SongsListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySongsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.nameTextView.text = category.name
        Glide.with(binding.coverImageView).load(category.coverUrl)
            .apply(
                RequestOptions().transform(RoundedCorners(32))
            )
            .into(binding.coverImageView)

        binding.deletePlaylistBtn.setOnClickListener {
            deletePlaylist(category.name)
            finish()
        }

        setupSongsListRecyclerView()
    }

    private fun deletePlaylist(playlistName: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("category")
            .whereEqualTo("name", playlistName)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                        .addOnSuccessListener {
                            Log.d("delete", "DocumentSnapshot successfully deleted!")
                        }
                        .addOnFailureListener { e ->
                            Log.w("delete", "Error deleting document", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("delete", "Error getting documents", e)
            }
    }


    private fun setupSongsListRecyclerView() {
        songsListAdapter = SongsListAdapter(category.songs)
        binding.songsListRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.songsListRecyclerView.adapter = songsListAdapter
    }
}