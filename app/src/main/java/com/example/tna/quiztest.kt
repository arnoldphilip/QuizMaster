package com.example.tna

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class quiztest : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var textViewQuestion: TextView
    private lateinit var radioGroupAnswers: RadioGroup
    private lateinit var buttonNext: Button
    private lateinit var buttonPrevious: Button
    private lateinit var buttonSubmit: Button

    private var quizCode: String = ""
    private var questionsList = mutableListOf<Map<String, Any>>()
    private var currentQuestionIndex = 0
    private var score = 0
    private var totalQuestions = 0
    private var selectedAnswer: String? = null
    private var userId: String? = null
    private var studentName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiztest)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        userId = auth.currentUser?.uid
        quizCode = intent.getStringExtra("QUIZ_CODE") ?: ""

        // Initialize UI elements
        textViewQuestion = findViewById(R.id.textViewQuestion)
        radioGroupAnswers = findViewById(R.id.radioGroupAnswers)
        buttonNext = findViewById(R.id.buttonNext)
        buttonPrevious = findViewById(R.id.buttonPrevious)
        buttonSubmit = findViewById(R.id.buttonSubmit)

        buttonSubmit.visibility = View.GONE // Hide submit initially
        buttonPrevious.visibility = View.GONE // Hide previous initially

        loadStudentName()
        loadQuestions()

        buttonNext.setOnClickListener {
            if (selectedAnswer == null) {
                Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkAnswer()
            moveToNextQuestion()
        }

        buttonPrevious.setOnClickListener {
            moveToPreviousQuestion()
        }

        buttonSubmit.setOnClickListener {
            if (selectedAnswer == null) {
                Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
            } else {
                checkAnswer()
                submitQuiz()
            }
        }
    }

    private fun loadStudentName() {
        if (userId != null) {
            db.collection("users").document(userId!!).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        studentName = document.getString("name") ?: "Unknown"
                    }
                }
                .addOnFailureListener {
                    studentName = "Unknown"
                }
        }
    }

    private fun loadQuestions() {
        db.collection("quizzes").document(quizCode)
            .collection("questions").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val question = document.getString("question") ?: ""
                    val correctAnswerIndex = document.getLong("correctOption")?.toInt() ?: -1

                    val optionsMap = mutableMapOf<Int, String>()
                    document.getString("option1")?.let { optionsMap[1] = it }
                    document.getString("option2")?.let { optionsMap[2] = it }
                    document.getString("option3")?.let { optionsMap[3] = it }
                    document.getString("option4")?.let { optionsMap[4] = it }

                    val options = listOf(
                        optionsMap[1] ?: "",
                        optionsMap[2] ?: "",
                        optionsMap[3] ?: "",
                        optionsMap[4] ?: ""
                    )

                    val correctAnswer = optionsMap[correctAnswerIndex] ?: ""

                    val questionData = mapOf(
                        "question" to question,
                        "answer" to correctAnswer,
                        "options" to options
                    )
                    questionsList.add(questionData)
                }
                totalQuestions = questionsList.size
                if (totalQuestions > 0) {
                    showQuestion()
                } else {
                    Toast.makeText(this, "No questions found", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading questions", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showQuestion() {
        val questionData = questionsList[currentQuestionIndex]
        textViewQuestion.text =
            "Question ${currentQuestionIndex + 1}:\n\n${questionData["question"] as String}"

        val options = questionData["options"] as List<String>
        radioGroupAnswers.removeAllViews()
        for (option in options) {
            val radioButton = RadioButton(this)
            radioButton.text = option
            radioButton.setTextColor(
                resources.getColor(
                    android.R.color.black,
                    theme
                )
            ) // ✅ Set text color to black
            radioGroupAnswers.addView(radioButton)
        }

        radioGroupAnswers.setOnCheckedChangeListener { _, checkedId ->
            val selectedRadioButton = findViewById<RadioButton>(checkedId)
            selectedAnswer = selectedRadioButton.text.toString()
        }

        updateButtonVisibility()
    }

    private fun checkAnswer() {
        val correctAnswer = questionsList[currentQuestionIndex]["answer"] as String
        if (selectedAnswer == correctAnswer) {
            score++
        }
    }

    private fun moveToNextQuestion() {
        currentQuestionIndex++
        selectedAnswer = null
        showQuestion()
    }

    private fun moveToPreviousQuestion() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--
            selectedAnswer = null
            showQuestion()
        }
    }

    private fun updateButtonVisibility() {
        buttonPrevious.visibility = if (currentQuestionIndex > 0) View.VISIBLE else View.GONE
        buttonNext.visibility =
            if (currentQuestionIndex < totalQuestions - 1) View.VISIBLE else View.GONE
        buttonSubmit.visibility =
            if (currentQuestionIndex == totalQuestions - 1) View.VISIBLE else View.GONE
    }

    private fun submitQuiz() {
        if (userId == null || studentName == null) {
            Toast.makeText(this, "User data missing, cannot save results", Toast.LENGTH_SHORT).show()
            return
        }

        val resultData = hashMapOf(
            "quizCode" to quizCode,
            "userId" to userId,
            "studentName" to studentName,
            "score" to score,
            "totalQuestions" to totalQuestions,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("quizzes")
            .document(quizCode)
            .collection("quiz_result")
            .document(userId!!) // Use UID as the document ID
            .set(resultData)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Quiz submitted! Your score: $score/$totalQuestions",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error saving result", Toast.LENGTH_SHORT).show()
            }
    }

}

