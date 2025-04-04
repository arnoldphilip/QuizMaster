package com.example.tna

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpButton = findViewById<Button>(R.id.signUpButton)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            db.collection("users").document(userId).get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        val name = document.getString("name") ?: "User"
                                        val type = document.getString("userType") // Fetch from "usertype" instead of "type"

                                        Toast.makeText(this, "Welcome, $name ($type)", Toast.LENGTH_SHORT).show()

                                        if (type == "Student") { // Ensure lowercase check matches Firestore value
                                            val intent = Intent(this, student::class.java)
                                            intent.putExtra("USER_UID", userId)
                                            startActivity(intent)
                                            finish()
                                        } else if (type == "Teacher") {
                                            val intent = Intent(this, TeacherActivity::class.java)
                                            intent.putExtra("USER_UID", userId)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            Toast.makeText(this, "Invalid user type: $type", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Toast.makeText(this, "User document does not exist!", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error fetching user: ${e.message}", Toast.LENGTH_LONG).show()
                                }

                        }
                    } else {
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        signUpButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
