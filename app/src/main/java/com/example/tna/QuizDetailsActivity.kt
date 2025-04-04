package com.example.tna

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class QuizDetailsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var textViewQuizCode: TextView
    private lateinit var listViewStudents: ListView
    private lateinit var quizCode: String
    private val studentResultsList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_details) // Make sure this matches your XML file name

        db = FirebaseFirestore.getInstance()
        textViewQuizCode = findViewById(R.id.textViewQuizCode)
        listViewStudents = findViewById(R.id.listViewStudents)

        quizCode = intent.getStringExtra("QUIZ_CODE") ?: ""

        textViewQuizCode.text = "Quiz Code: $quizCode"

        loadStudentResults()
    }

    private fun loadStudentResults() {
        db.collection("quizzes")
            .document(quizCode)
            .collection("quiz_result")
            .get()
            .addOnSuccessListener { documents ->
                studentResultsList.clear()
                for (document in documents) {
                    val name = document.getString("studentName") ?: "Unknown"
                    val score = document.getLong("score") ?: 0
                    val total = document.getLong("totalQuestions") ?: 0
                    studentResultsList.add("$name - Score: $score/$total")
                }
                val adapter = ArrayAdapter(this, R.layout.quiz_colour, studentResultsList)
                listViewStudents.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading results", Toast.LENGTH_SHORT).show()
            }
    }


}
