package com.example.screenloggerdialogtest

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class TSRQActivity : AppCompatActivity() {

    /*  Original questions:
        "TSRQ1" to "Other people would be mad at me if I didn't take my medications or check my glucose.",
        "TSRQ2" to "I find it a personal challenge to take my medications or check my glucose.",
        "TSRQ3" to "I personally believe that controlling my diabetes will improve my health.",
        "TSRQ4" to "I would feel guilty if I didn't do what my doctor said.",
        "TSRQ5" to "I want my doctor to think I'm a good patient.",
        "TSRQ6" to "I would feel bad about myself if I didn't take care of my health.",
        "TSRQ7" to "It's exciting to try to keep my glucose in a healthy range.",
        "TSRQ8" to "I don't want other people to be disappointed in me.",
        "TSRQ9" to "Other people would be upset with me if I didn't follow my diet or exercise.",
        "TSRQ10" to "I personally believe that these actions are important in remaining healthy.",
        "TSRQ11" to "I would be ashamed of myself if I didn't follow my diet and exercise.",
        "TSRQ12" to "It is easier to do what I'm told than to think about it.",
        "TSRQ13" to "I've carefully thought about my diet and exercising and believe it's the right thing to do.",
        "TSRQ14" to "I want others to see that I can follow my diet and stay fit.",
        "TSRQ15" to "I just do it because my doctor said to.",
        "TSRQ16" to "I feel personally that watching my diet and exercising are the best things for me.",
        "TSRQ17" to "I'd feel guilty if I didn't watch my diet and exercise.",
        "TSRQ18" to "Exercising regularly and following my diet are choices I really want to make.",
        "TSRQ19" to "It's a challenge to learn how to live with diabetes."
     */

    private val listOfQuestions = mapOf(
        "TSRQ1" to "Other people would be mad at me if I didn't reduce my smartphone use.",
        "TSRQ2" to "I find it a personal challenge to reduce my smartphone use.",
        "TSRQ3" to "I personally believe that controlling my smartphone use will improve my health.",
        "TSRQ4" to "I would feel guilty if I didn't do what the researcher asked.",
        "TSRQ5" to "I want the researcher to think I'm a good participant.",
        "TSRQ6" to "I would feel bad about myself if I didn't reduce my smartphone use.",
        "TSRQ7" to "It's exciting to try to reduce my smartphone use.",
        "TSRQ8" to "I don't want other people to be disappointed in me.",
        "TSRQ9" to "Other people would be upset with me if I didn't reduce my smartphone use.",
        "TSRQ10" to "I personally believe that these actions are important in remaining healthy.",
        "TSRQ11" to "I would be ashamed of myself if I didn't reduce my smartphone use.",
        "TSRQ12" to "It is easier to do what I'm told than to think about it.",
        "TSRQ13" to "I've carefully thought about reducing my smartphone use and believe it's the right thing to do.",
        "TSRQ14" to "I want others to see that I can reduce my smartphone use and stay healthy.",
        "TSRQ15" to "I just do it because researcher asked me to.",
        "TSRQ16" to "I feel personally that reducing my smartphone use is the best thing for me.",
        "TSRQ17" to "I'd feel guilty if I didn't reduce my smartphone use.",
        "TSRQ18" to "Reducing smartphone use is a choice I really want to make.",
        "TSRQ19" to "It's a challenge to learn how to spend less time on my smartphone."
    )

    /*
        Autonomous Regulation: 2, 3, 7, 10, 13, 16, 18, 19
        Controlled Regulation: 1, 4, 5, 6, 8, 9, 11, 12, 14, 15, 17
     */

    private val responses = mutableMapOf<String, TSRQAnswer>()
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tsrq)

        val questionsContainer = findViewById<LinearLayout>(R.id.questions_container)
        submitButton = findViewById(R.id.submit_button)

        // Loop through each question in the list
        listOfQuestions.forEach { (tsrqId, questionText) ->
            val options = arrayOf(
                "1", // Not at all true
                "2", // Slightly true
                "3", // Somewhat true
                "4", // Mostly true
                "5", // Very true
                "6", // Extremely true
                "7"  // Definitely true
            )

            val questionLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 8)
                }
                setPadding(0, 16, 0, 16)

                background = GradientDrawable().apply {
                    setColor(Color.WHITE) // Background color
                    setStroke(1, Color.LTGRAY) // Border color
                    cornerRadius = 8f // Rounded corners
                }
                elevation = 4f // Shadow elevation
            }

            // TextView with question and red asterisk
            val questionTextView = TextView(this).apply {
                text = "* $questionText"
                setTextColor(Color.RED)
                setPadding(8, 0, 8, 0)
            }
            questionLayout.addView(questionTextView)

            // RadioGroup for response options
            val radioGroup = RadioGroup(this).apply {
                orientation = RadioGroup.HORIZONTAL
                setOnCheckedChangeListener { _, checkedId ->
                    val response = checkedId
                    val TSRQAnswer = TSRQAnswer(tsrqId, response)
                    updateResponse(tsrqId, TSRQAnswer)
                    updateQuestionText(questionTextView, questionText, true)
                    checkAllAnswered()
                }
            }

            // Add seven radio buttons for the 7-point scale
            var but_id = 0
            options.forEachIndexed { i, option ->
                val radioButton = RadioButton(this).apply {
                    but_id += 1
                    id = but_id
                    text = option
                }
                radioButton.setOnClickListener {
                    Log.d("TSRQ", "clicked + $questionText")
                }
                radioGroup.addView(radioButton)
            }

            val preText = TextView(this).apply {
                text = "1 = not at all true .. 4 = somewhat true .. 7 = very true"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f) // Set size in SP
                setTypeface(null, Typeface.ITALIC) // Set text to italic
            }

            questionLayout.addView(preText)
            questionLayout.addView(radioGroup)
            questionsContainer.addView(questionLayout)
        }

        submitButton.setOnClickListener {
            // Save responses logic here, e.g., storing them to database or SharedPreferences
            Toast.makeText(this, "Responses submitted!", Toast.LENGTH_SHORT).show()

            Log.d("TSRQ", responses.toString())

            setStudyVariable(this, TSRQ_SUBMITTED, 1)

            FirebaseUtils.sendEntryToDatabase(
                path = "users/${FirebaseUtils.getCurrentUserUID()}/questionnaires/tsrq/${System.currentTimeMillis()}",
                data = responses,
                onSuccess = {
                },
                onFailure = { exception ->
                }
            )

            finish()
        }
    }

    private fun updateResponse(id: String, TSRQAnswer: TSRQAnswer) {
        responses[id] = TSRQAnswer  // Add or update the response in the map by ID
    }

    private fun updateQuestionText(textView: TextView, question: String, answered: Boolean) {
        textView.text = if (answered) question else "* $question"
        textView.setTextColor(if (answered) Color.BLACK else Color.RED)
    }

    private fun checkAllAnswered() {
        // Enable submit button only if all questions are answered
        submitButton.isEnabled = responses.size == listOfQuestions.size
    }
}

data class TSRQAnswer(val questionId: String, val response: Int)
