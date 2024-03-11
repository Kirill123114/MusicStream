package com.smokermobilecompany.musicstream.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.smokermobilecompany.musicstream.exoplayer.MyExoplayer
import com.smokermobilecompany.musicstream.R
import com.smokermobilecompany.musicstream.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var exoPlayer: ExoPlayer

    private var playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            showGif(isPlaying)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MyExoplayer.getCurrentSong()?.apply {
            binding.songTitleTextView.text = title
            binding.songSubtitleTextView.text = subtitle
            Glide.with(binding.songCoverImageView).load(coverUrl)
                .circleCrop()
                .into(binding.songCoverImageView)
            Glide.with(binding.songGifImageView).load(R.drawable.media_playing)
                .circleCrop()
                .into(binding.songGifImageView)
            exoPlayer = MyExoplayer.getInstance()!!
            binding.playerView.player = exoPlayer
            binding.playerView.showController()
            exoPlayer.addListener(playerListener)
        }

        binding.addToPlaylistBtn.setOnClickListener {
            addToPlaylist()
        }
    }

    private fun addToPlaylist() {
        val db = FirebaseFirestore.getInstance()
        val playlists = ArrayList<String>()
        db.collection("category")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    playlists.add(document.id)
                }

                showPlaylistDialog(playlists)
            }
            .addOnFailureListener { exception ->
                Log.w("addToPlaylist", "Error getting documents: ", exception)
            }
    }

    private fun showPlaylistDialog(playlists: ArrayList<String>) {
        val db = FirebaseFirestore.getInstance()
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите плейлист")
            .setSingleChoiceItems(playlists.toTypedArray(), -1) { dialog, which -> }
            .setPositiveButton("Добавить") { dialog, which ->
                val listView = (dialog as AlertDialog).listView
                val selectedPosition = listView.checkedItemPosition
                if (selectedPosition != -1) {
                    val selectedPlaylist = playlists[selectedPosition]
                    db.collection("category").document(selectedPlaylist)
                        .update("songs", FieldValue.arrayUnion(MyExoplayer.getCurrentSong()?.id))
                        .addOnSuccessListener {
                            Log.d(
                                "addplay",
                                "Document ${MyExoplayer.getCurrentSong()?.id} successfully updated!"
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.w("addplay", "Error updating document", e)
                        }
                }
            }
            .setNegativeButton("Отмена") { dialog, which ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.removeListener(playerListener)
    }

    fun showGif(show: Boolean) {
        if (show)
            binding.songGifImageView.visibility = View.VISIBLE
        else
            binding.songGifImageView.visibility = View.INVISIBLE
    }
}