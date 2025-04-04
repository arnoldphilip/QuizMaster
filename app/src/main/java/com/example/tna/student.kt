package com.example.tna

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Tasks

class student : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var textViewHello: TextView
    private lateinit var editTextCode: EditText
    private lateinit var buttonStartQuiz: Button
    private lateinit var buttonLogout: Button
    private lateinit var listViewHistory: ListView

    private var userId: String? = null
    private val quizHistoryList = mutableListOf<String>()
    private lateinit var historyAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student)

        // Initialize UI elements
        textViewHello = findViewById(R.id.textViewHello)
        editTextCode = findViewById(R.id.editTextCode)
        buttonStartQuiz = findViewById(R.id.buttonStartQuiz)
        buttonLogout = findViewById(R.id.buttonLogout)
        listViewHistory = findViewById(R.id.listViewHistory)

        db = FirebaseFirestore.getInstance()

        // Use custom list item layout if needed
        historyAdapter = ArrayAdapter(this, R.layout.quiz_colour, quizHistoryList)
        listViewHistory.adapter = historyAdapter

        userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            fetchUserName()
            fetchQuizHistory()
        } else {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_LONG).show()
            finish()
        }

        buttonStartQuiz.setOnClickListener {
            val enteredCode = editTextCode.text.toString().trim()
            if (enteredCode.isNotEmpty()) {
                checkQuizCode(enteredCode)
            } else {
                Toast.makeText(this, "Please enter a quiz code", Toast.LENGTH_SHORT).show()
            }
        }

        buttonLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun fetchUserName() {
        db.collection("users").document(userId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "User"
                    textViewHello.text = "Hello, $name"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching user: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkQuizCode(code: String) {
        val quizRef = db.collection("quizzes").document(code)

        quizRef.get().addOnSuccessListener { quizDoc ->
            if (quizDoc.exists()) {
                val quizResultRef = quizRef.collection("quiz_result").document(userId!!)

                quizResultRef.get().addOnSuccessListener { resultDoc ->
                    if (resultDoc.exists()) {
                        Toast.makeText(this, "You have already completed this quiz!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Code verified! Starting Quiz...", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, quiztest::class.java)
                        intent.putExtra("QUIZ_CODE", code)
                        startActivity(intent)
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Error checking quiz attempt", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Invalid quiz code", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchQuizHistory() {
        if (userId == null) return

        db.collection("quizzes").get()
            .addOnSuccessListener { quizDocs ->
                quizHistoryList.clear()

                val tasks = quizDocs.map { quizDoc ->
                    val quizCode = quizDoc.id
                    db.collection("quizzes").document(quizCode)
                        .collection("quiz_result").document(userId!!)
                        .get()
                        .continueWith { resultTask ->
                            val resultDoc = resultTask.result
                            if (resultDoc.exists()) {
                                val score = resultDoc.getLong("score") ?: 0
                                val total = resultDoc.getLong("totalQuestions") ?: 0
                                "Quiz Code: $quizCode - Score: $score/$total"
                            } else {
                                null
                            }
                        }
                }

                // Wait for all tasks to complete
                Tasks.whenAllSuccess<String>(tasks)
                    .addOnSuccessListener { results ->
                        quizHistoryList.addAll(results.filterNotNull())
                        historyAdapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error processing history", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching quizzes: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        fetchQuizHistory() // Refresh history whenever activity resumes
    }

}
