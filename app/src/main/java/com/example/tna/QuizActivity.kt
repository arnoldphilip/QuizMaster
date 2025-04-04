package com.example.tna

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class QuizActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var linearLayoutQuestions: LinearLayout
    private lateinit var buttonSubmit: Button
    private lateinit var textViewQuizCode: TextView

    private var quizCode: String = ""
    private var numQuestions: Int = 0
    private val questionViews = mutableListOf<QuestionView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        db = FirebaseFirestore.getInstance()
        linearLayoutQuestions = findViewById(R.id.linearLayoutQuestions)
        buttonSubmit = findViewById(R.id.buttonSubmit)
        textViewQuizCode = findViewById(R.id.textViewQuizCode)

        // Get quiz code from intent
        quizCode = intent.getStringExtra("QUIZ_CODE") ?: ""

        // Fetch number of questions
        db.collection("quizzes").document(quizCode).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    numQuestions = document.getLong("numQuestions")?.toInt() ?: 0
                    textViewQuizCode.text = "Quiz Code: $quizCode"
                    generateQuestionFields(numQuestions)
                } else {
                    Toast.makeText(this, "Quiz not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading quiz details", Toast.LENGTH_SHORT).show()
            }

        buttonSubmit.setOnClickListener {
            saveQuestions()
        }
    }

    private fun generateQuestionFields(count: Int) {
        linearLayoutQuestions.removeAllViews()
        questionViews.clear()

        for (i in 1..count) {
            val questionView = QuestionView(this, i)
            linearLayoutQuestions.addView(questionView.view)
            questionViews.add(questionView)
        }
    }

    private fun saveQuestions() {
        val questionsData = mutableListOf<Map<String, Any>>()

        for (qView in questionViews) {
            val questionText = qView.editTextQuestion.text.toString().trim()
            val option1 = qView.editTextOption1.text.toString().trim()
            val option2 = qView.editTextOption2.text.toString().trim()
            val option3 = qView.editTextOption3.text.toString().trim()
            val option4 = qView.editTextOption4.text.toString().trim()

            val selectedRadioButtonId = qView.radioGroup.checkedRadioButtonId
            if (questionText.isEmpty() || option1.isEmpty() || option2.isEmpty() || option3.isEmpty() || option4.isEmpty() || selectedRadioButtonId == -1) {
                Toast.makeText(this, "Fill all fields and select an answer", Toast.LENGTH_SHORT).show()
                return
            }

            // Get the selected answer as index (1, 2, 3, or 4)
            val selectedOptionIndex = when (selectedRadioButtonId) {
                qView.radioButton1.id -> 1
                qView.radioButton2.id -> 2
                qView.radioButton3.id -> 3
                qView.radioButton4.id -> 4
                else -> -1
            }

            if (selectedOptionIndex == -1) {
                Toast.makeText(this, "Error selecting answer", Toast.LENGTH_SHORT).show()
                return
            }

            val questionMap = mapOf(
                "question" to questionText,
                "option1" to option1,
                "option2" to option2,
                "option3" to option3,
                "option4" to option4,
                "correctOption" to selectedOptionIndex // ✅ Store answer as 1, 2, 3, or 4
            )

            questionsData.add(questionMap)
        }

        val batch = db.batch()
        for ((index, question) in questionsData.withIndex()) {
            val questionRef = db.collection("quizzes").document(quizCode)
                .collection("questions").document("Q${index + 1}")
            batch.set(questionRef, question)
        }

        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "Quiz saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Error saving quiz", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * QuestionView - Custom view for each question
 */
class QuestionView(context: android.content.Context, questionNumber: Int) {
    val view: View = View.inflate(context, R.layout.item_question, null)
    val editTextQuestion: EditText = view.findViewById(R.id.editTextQuestion)
    val editTextOption1: EditText = view.findViewById(R.id.editTextOption1)
    val editTextOption2: EditText = view.findViewById(R.id.editTextOption2)
    val editTextOption3: EditText = view.findViewById(R.id.editTextOption3)
    val editTextOption4: EditText = view.findViewById(R.id.editTextOption4)
    val radioGroup: RadioGroup = view.findViewById(R.id.radioGroupAnswers)

    val radioButton1: RadioButton = view.findViewById(R.id.radioButton1)
    val radioButton2: RadioButton = view.findViewById(R.id.radioButton2)
    val radioButton3: RadioButton = view.findViewById(R.id.radioButton3)
    val radioButton4: RadioButton = view.findViewById(R.id.radioButton4)

    init {
        view.findViewById<TextView>(R.id.textViewQuestionNumber).text = "Question $questionNumber"
    }
}
