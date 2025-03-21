package com.example.mobileappdev2025

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileInputStream
import java.util.Scanner

data class WordDefinition(val word: String, val definition: String, var streak: Int = 0)

class MainActivity() : AppCompatActivity(), Parcelable {
    private val ADD_WORD_CODE = 1234
    private lateinit var myAdapter: ArrayAdapter<String> // connect from data to gui
    private var dataDefList = ArrayList<String>() // data
    private var wordDefinition = mutableListOf<WordDefinition>()
    private var score: Int = 0
    private var streak: Int = 0
    private var longestStreak: Int = 0

    constructor(parcel: Parcel) : this() {
        score = parcel.readInt()
        streak = parcel.readInt()
        longestStreak = parcel.readInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadStats()
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        loadWordsFromDisk()

        pickNewWordAndLoadDataList()
        setupList()

        val defList = findViewById<ListView>(R.id.dynamic_def_list)
        defList.setOnItemClickListener { _, _, index, _ ->
            val correctDefinition = wordDefinition[0].definition
            val selectedDefinition = dataDefList[index]

           /* if (selectedDefinition == correctDefinition) { // Correct answer
                wordDefinition[0].streak++ // increment streak for correct word
                streak = wordDefinition[0].streak // Update global streak
                score += streak // increment score based on streak
                if (streak > longestStreak) {
                    longestStreak = streak // track longest streak
                }
            } else { // Incorrect answer
                wordDefinition[0].streak = 0 // Reset streak for the current word
                streak = 0 // Reset global streak
            }

            saveWordsToDisk()
            updateScoreAndStreakUI()
            */pickNewWordAndLoadDataList()
            myAdapter.notifyDataSetChanged()


        }
    }

    private fun updateScoreAndStreakUI() {
        findViewById<TextView>(R.id.scoreTextView).text = "Score: $score"
        findViewById<TextView>(R.id.streakTextView).text = "Streak: $streak"
    }

    override fun onDestroy() {
        saveStats()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_WORD_CODE && resultCode == RESULT_OK && data != null) {
            val word = data.getStringExtra("word") ?: ""
            val def = data.getStringExtra("def") ?: "" // Ensure both word and def are valid

            if (word.isNotBlank() && def.isNotBlank()) {
                // Add the new word to the list with a default streak of 0
                wordDefinition.add(WordDefinition(word, def))

                // Save the updated list to disk
                saveWordsToDisk()

                // Refresh the UI with the new word added
                pickNewWordAndLoadDataList()
                myAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun saveStats() {
        val file = File(applicationContext.filesDir, "user_stats.csv")
        file.writeText("$score,$streak,$longestStreak")
    }

    private fun loadStats() {
        val file = File(applicationContext.filesDir, "user_stats.csv")
        if (file.exists()) {
            val scanner = Scanner(file)
            if (scanner.hasNextLine()) {
                val stats = scanner.nextLine().split(",")
                score = stats[0].toInt()
                streak = stats[1].toInt()
                longestStreak = stats[2].toInt()
            }
        }
    }

    private fun loadWordsFromDisk() {
        val file = File(applicationContext.filesDir, "user_data.csv")
        if (file.exists()) {
            val readResult = FileInputStream(file)
            val scanner = Scanner(readResult)
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                val wd = line.split("|")
                // Add word with streak, if no streak exists, set it to 0
                wordDefinition.add(WordDefinition(wd[0], wd[1], wd.getOrNull(2)?.toInt() ?: 0))
            }
        } else {
            // Load default words if no file exists
            val reader = Scanner(resources.openRawResource(R.raw.default_words))
            while (reader.hasNextLine()) {
                val line = reader.nextLine()
                val wd = line.split("|")
                wordDefinition.add(WordDefinition(wd[0], wd[1]))
                file.appendText("${wd[0]}|${wd[1]}|0\n") // Initialize streak to 0
            }
        }
    }

    private fun saveWordsToDisk() {
        val file = File(applicationContext.filesDir, "user_data.csv")
        file.writeText("") // Clear the file before writing
        for (wd in wordDefinition) {
            file.appendText("${wd.word}|${wd.definition}|${wd.streak}\n")
        }
    }

    private fun pickNewWordAndLoadDataList() {
        // Sort words by streak (lower streaks appear first)
        wordDefinition.sortBy { it.streak }

        // Clear previous definitions
        dataDefList.clear()

        // Add up to 4 definitions
        for (i in 0 until minOf(4, wordDefinition.size)) {
            dataDefList.add(wordDefinition[i].definition)
        }

        // Display the word with the lowest streak
        findViewById<TextView>(R.id.word).text = wordDefinition[0].word

        // Shuffle the definitions to randomize their order
        dataDefList.shuffle()
    }

    private fun setupList() {
        myAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dataDefList)
        val defList = findViewById<ListView>(R.id.dynamic_def_list)
        defList.adapter = myAdapter
    }

    fun openStats(view: View) {
        val myIntent = Intent(this, StatsActivity::class.java)
        myIntent.putExtra("score", score.toString())
        myIntent.putExtra("streak", streak.toString())
        myIntent.putExtra("longestStreak", longestStreak.toString())
        startActivity(myIntent)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(score)
        parcel.writeInt(streak)
        parcel.writeInt(longestStreak)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MainActivity> {
        override fun createFromParcel(parcel: Parcel): MainActivity {
            return MainActivity(parcel)
        }

        override fun newArray(size: Int): Array<MainActivity?> {
            return arrayOfNulls(size)
        }
    }

    fun openAddWord(view: View) {
        val myIntent = Intent(this, AddWordActivity::class.java)
        startActivityForResult(myIntent, ADD_WORD_CODE)
    }
}