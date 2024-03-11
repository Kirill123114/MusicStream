package com.smokermobilecompany.musicstream.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import com.smokermobilecompany.musicstream.exoplayer.MyExoplayer
import com.smokermobilecompany.musicstream.R
import com.smokermobilecompany.musicstream.adapter.CategoryAdapter
import com.smokermobilecompany.musicstream.adapter.SectionSongListAdapter
import com.smokermobilecompany.musicstream.adapter.SongsListAdapter
import com.smokermobilecompany.musicstream.databinding.ActivityMainBinding
import com.smokermobilecompany.musicstream.models.CategoryModel
import com.smokermobilecompany.musicstream.models.SongModel
import com.smokermobilecompany.musicstream.ui.register.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var songsListAdapter: SongsListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getCategoriesRealTime()
        setupSection(
            "section_1",
            binding.section1MainLayout,
            binding.section1Title,
            binding.section1RecyclerView
        )
        setupSection(
            "section_2",
            binding.section2MainLayout,
            binding.section2Title,
            binding.section2RecyclerView
        )
        setupSection(
            "section_3",
            binding.section3MainLayout,
            binding.section3Title,
            binding.section3RecyclerView
        )
        setupMostlyPlayed(
            "mostly_played",
            binding.mostlyPlayedMainLayout,
            binding.mostlyPlayedTitle,
            binding.mostlyPlayedRecyclerView
        )

        binding.optionBtn.setOnClickListener {
            showPopupMenu()
        }

        binding.createPlaylistBtn.setOnClickListener {
            showCreatePlaylistDialog()
        }

        setupSongsListRecyclerView()
    }

    @SuppressLint("MissingInflatedId")
    private fun showCreatePlaylistDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_create_playlist, null)
        val editTextPlaylistName = dialogView.findViewById<EditText>(R.id.editTextPlaylistName)

        builder.setView(dialogView)
            .setTitle("Создание плэй-листа")
            .setPositiveButton("Создать") { dialog, _ ->
                val playlistName = editTextPlaylistName.text.toString()
                if (playlistName.isNotEmpty()) {
                    createPlaylist(playlistName)
                } else {
                    Toast.makeText(
                        this,
                        "Пожалуйста, введите название плэй-листа",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun createPlaylist(playlistName: String) {
        val db = FirebaseFirestore.getInstance()

        val category = hashMapOf(
            "name" to playlistName,
            "coverUrl" to "https://firebasestorage.googleapis.com/v0/b/musicproject-ddc81.appspot.com/o/playlist.png?alt=media&token=656070a4-32d6-4f59-832d-1a61dad44fdb",
            "songs" to emptyList<String>()
        )

        db.collection("category").document(playlistName)
            .set(category)
            .addOnSuccessListener { documentReference ->
                Log.d("ctr", "DocumentSnapshot added with ID: ${documentReference}")
            }
            .addOnFailureListener { e ->
                Log.w("ctr", "Error adding document", e)
            }
    }

    private fun setupSongsListRecyclerView() {
        getSongsIdList { songsIdList ->
            songsListAdapter = SongsListAdapter(songsIdList)
            binding.musicRecyclerView.layoutManager = LinearLayoutManager(this)
            binding.musicRecyclerView.adapter = songsListAdapter
        }
    }

    private fun getSongsIdList(callback: (List<String>) -> Unit) {
        val listId = mutableListOf<String>()
        FirebaseFirestore.getInstance().collection("songs")
            .get().addOnSuccessListener { documents ->
                for (document in documents) {
                    val song = document.toObject(SongModel::class.java)
                    song.let {
                        listId.add(song.id)
                    }
                }
                callback(listId)
            }
    }

    private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, binding.optionBtn)
        val inflator = popupMenu.menuInflater
        inflator.inflate(R.menu.option_menu, popupMenu.menu)
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.logout -> {
                    logout()
                    true
                }
            }
            false
        }
    }

    private fun logout() {
        MyExoplayer.getInstance()?.release()
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        showPlayerView()
    }

    @SuppressLint("SetTextI18n")
    private fun showPlayerView() {
        binding.playerView.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }
        MyExoplayer.getCurrentSong()?.let {
            binding.playerView.visibility = View.VISIBLE
            binding.songTitleTextView.text = "Now Playing : " + it.title
            Glide.with(binding.songCoverImageView).load(it.coverUrl)
                .apply(
                    RequestOptions().transform(RoundedCorners(32))
                ).into(binding.songCoverImageView)
        } ?: run {
            binding.playerView.visibility = View.GONE
        }
    }

    private fun getCategoriesRealTime() {
        FirebaseFirestore.getInstance().collection("category")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val categoryList = snapshot.toObjects(CategoryModel::class.java)
                    setupCategoryRecyclerView(categoryList)
                }
            }
    }

    private fun setupCategoryRecyclerView(categoryList: List<CategoryModel>) {
        categoryAdapter = CategoryAdapter(categoryList)
        binding.categoriesRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.categoriesRecyclerView.adapter = categoryAdapter
    }

    private fun setupSection(
        id: String,
        mainLayout: RelativeLayout,
        titleView: TextView,
        recyclerView: RecyclerView
    ) {
        FirebaseFirestore.getInstance().collection("sections")
            .document(id)
            .get().addOnSuccessListener {
                val section = it.toObject(CategoryModel::class.java)
                section?.apply {
                    mainLayout.visibility = View.VISIBLE
                    titleView.text = name
                    recyclerView.layoutManager = LinearLayoutManager(
                        this@MainActivity,
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    recyclerView.adapter = SectionSongListAdapter(songs)
                    mainLayout.setOnClickListener {
                        SongsListActivity.category = section
                        startActivity(Intent(this@MainActivity, SongsListActivity::class.java))
                    }
                }
            }
    }

    private fun setupMostlyPlayed(
        id: String,
        mainLayout: RelativeLayout,
        titleView: TextView,
        recyclerView: RecyclerView
    ) {
        FirebaseFirestore.getInstance().collection("sections")
            .document(id)
            .get().addOnSuccessListener {
                FirebaseFirestore.getInstance().collection("songs")
                    .orderBy("count", Query.Direction.DESCENDING)
                    .limit(5)
                    .get().addOnSuccessListener { songListSnapshot ->
                        val songsModelList = songListSnapshot.toObjects<SongModel>()
                        val songsIdList = songsModelList.map {
                            it.id
                        }.toList()
                        val section = it.toObject(CategoryModel::class.java)
                        section?.apply {
                            section.songs = songsIdList
                            mainLayout.visibility = View.VISIBLE
                            titleView.text = name
                            recyclerView.layoutManager = LinearLayoutManager(
                                this@MainActivity,
                                LinearLayoutManager.HORIZONTAL,
                                false
                            )
                            recyclerView.adapter = SectionSongListAdapter(songs)
                            mainLayout.setOnClickListener {
                                SongsListActivity.category = section
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        SongsListActivity::class.java
                                    )
                                )
                            }
                        }
                    }
            }
    }
}