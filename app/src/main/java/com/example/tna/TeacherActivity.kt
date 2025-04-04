package com.example.tna

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TeacherActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var textViewWelcome: TextView
    private lateinit var buttonLogout: Button
    private lateinit var buttonGenerateQuiz: Button
    private lateinit var buttonRefresh: Button
    private lateinit var listViewQuizzes: ListView

    private val quizList = mutableListOf<String>()
    private lateinit var quizAdapter: ArrayAdapter<String>
    private var teacherId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        textViewWelcome = findViewById(R.id.textViewWelcome)
        buttonLogout = findViewById(R.id.buttonLogout)
        buttonGenerateQuiz = findViewById(R.id.buttonGenerateQuiz)
        buttonRefresh = findViewById(R.id.buttonRefresh)
        listViewQuizzes = findViewById(R.id.listViewQuizzes)

        // ✅ Use custom adapter to display black text in listView
        quizAdapter = ArrayAdapter(this, R.layout.quiz_colour, quizList)
        listViewQuizzes.adapter = quizAdapter

        val currentUser = auth.currentUser
        if (currentUser != null) {
            teacherId = currentUser.uid
            fetchTeacherName()
            loadExistingQuizzes()
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
        }

        buttonLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        buttonGenerateQuiz.setOnClickListener {
            askNumberOfQuestions()
        }

        buttonRefresh.setOnClickListener {
            loadExistingQuizzes()
        }

        // ✅ Connect Teacher to Quiz Details when clicking on a quiz
        listViewQuizzes.setOnItemClickListener { _, _, position, _ ->
            val quizCode = quizList[position]
            val intent = Intent(this, QuizDetailsActivity::class.java)
            intent.putExtra("QUIZ_CODE", quizCode)
            startActivity(intent)
        }
    }

    private fun fetchTeacherName() {
        db.collection("users").document(teacherId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "Teacher"
                    textViewWelcome.text = "Welcome, $name"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching user: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun askNumberOfQuestions() {
        val input = EditText(this)
        input.hint = "Enter number of questions"

        AlertDialog.Builder(this)
            .setTitle("Generate Quiz")
            .setMessage("How many questions?")
            .setView(input)
            .setPositiveButton("Generate") { _, _ ->
                val numQuestions = input.text.toString().toIntOrNull()
                if (numQuestions != null && numQuestions > 0) {
                    val quizCode = generateQuizCode()
                    saveQuizCode(quizCode, numQuestions)
                } else {
                    Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateQuizCode(): String {
        return (100000..999999).random().toString()
    }

    private fun saveQuizCode(quizCode: String, numQuestions: Int) {
        val quizData = hashMapOf(
            "quizCode" to quizCode,
            "numQuestions" to numQuestions,
            "teacherId" to teacherId
        )

        db.collection("quizzes").document(quizCode)
            .set(quizData)
            .addOnSuccessListener {
                Toast.makeText(this, "Quiz $quizCode created!", Toast.LENGTH_SHORT).show()
                loadExistingQuizzes()

                val intent = Intent(this, QuizActivity::class.java)
                intent.putExtra("QUIZ_CODE", quizCode)
                intent.putExtra("NUM_QUESTIONS", numQuestions)
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error creating quiz", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadExistingQuizzes() {
        db.collection("quizzes")
            .whereEqualTo("teacherId", teacherId)
            .get()
            .addOnSuccessListener { documents ->
                quizList.clear()
                for (document in documents) {
                    val quizCode = document.getString("quizCode") ?: ""
                    quizList.add(quizCode)
                }
                quizAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading quizzes", Toast.LENGTH_SHORT).show()
            }
    }
}
