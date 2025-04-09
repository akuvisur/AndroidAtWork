package com.example.screenloggerdialogtest

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SPAIActivity : AppCompatActivity() {

    // Define SPAI questions as a map
    private val listOfQuestions = mapOf(
        "SPAI1" to "I was told more than once that I spent too much time on smartphone.",
        "SPAI2" to "I feel uneasy once I stop smartphone for a certain period of time",
        "SPAI3" to "I find that I have been hooking on smartphone longer and longer",
        "SPAI4" to "I feel restless and irritable when the smartphone is unavailable",
        "SPAI5" to "I feel very vigorous upon smartphone use regardless of the fatigues experienced",
        "SPAI6" to "I use smartphone for a longer period of time and spend more money than I had intended",
        "SPAI7" to "Although using smartphone has brought negative effects on my interpersonal relationships, the amount of time remains unreduced",
        "SPAI8" to "I have slept less than four hours due to using smartphone more than once",
        "SPAI9" to "I have increased substantial amount of time using smartphone per week in recent 3 months",
        "SPAI10" to "I feel distressed or down once I cease using smartphone for a certain period of time",
        "SPAI11" to "I fail to control the impulse to use smartphone",
        "SPAI12" to "I find myself indulged on the smartphone at the cost of hanging out with friends",
        "SPAI13" to "I feel aches and soreness in the back or eye discomforts due to excessive smartphone use",
        "SPAI14" to "The idea of using smartphone comes as the first thought on mind when waking up each morning",
        "SPAI15" to "To use smartphone has exercised certain negative effects on my schoolwork or job performance",
        "SPAI16" to "I feel missing something after stopping smartphone for a certain period of time",
        "SPAI17" to "My interaction with family members is decreased on account of smartphone use",
        "SPAI18" to "My recreational activities are reduced due to smartphone use",
        "SPAI19" to "I feel the urge to use my smartphone again right after I stopped using it",
        "SPAI20" to "My life would be joyless hadn’t there been smartphone",
        "SPAI21" to "Surfing the smartphone has exercised negative effects on my physical health.",
        "SPAI22" to "I try to spend less time on smartphone, but the efforts were in vain",
        "SPAI23" to "My smartphone use is a habit and as a result my sleep quality and overall sleep time has decreased.",
        "SPAI24" to "I need to spend an increasing amount of time on smartphone to achieve same satisfaction as before",
        "SPAI25" to "I cannot have meal without smartphone use",
        "SPAI26" to "I feel tired on daytime due to late-night use of smartphone"
    )

    private val responses = mutableMapOf<String, SPAIAnswer>()
    private lateinit var submitButton: Button
    private lateinit var source: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spai)

        source = intent.getStringExtra("source").toString()

        val questionsContainer = findViewById<LinearLayout>(R.id.questions_container)
        submitButton = findViewById(R.id.submit_button)

        // Loop through each question in the list
        listOfQuestions.forEach { (spai_id, questionText) ->
            val options = arrayOf(
                "1", // Does not match my experience at all
                "2", // Probably does not match my experience
                "3", // Probably matches my experience
                "4"  // Definitely matches my experience
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
                    val SPAIAnswer = SPAIAnswer(spai_id, response)
                    updateResponse(spai_id, SPAIAnswer)
                    updateQuestionText(questionTextView, questionText, true)
                    checkAllAnswered()
                }
            }

            // Add four radio buttons to the radio group
            var but_id = 0
            options.forEachIndexed { i, option ->
                val radioButton = RadioButton(this).apply {
                    but_id += 1
                    id = but_id
                    text = option
                }
                radioButton.setOnClickListener {
                    Log.d("SPAI", "clicked + $questionText")
                }
                radioGroup.addView(radioButton)
            }

            val preText = TextView(this).apply {
                text = "1 = strongly disagree, 2 = somewhat disagree, ..., 4 = strongly agree"
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

            Log.d("SPAI", responses.toString())

            if (source == "consent_given") {
                setStudyVariable(this, SPAI_1_SUBMITTED, 1)
                val submissionTime = SPAIAnswer("pre_questionnaire", responses.size)
                responses["submission_time"] = submissionTime
            }
            else {
                setStudyVariable(this, SPAI_2_SUBMITTED, 1)
                val submissionTime = SPAIAnswer("post_questionnaire", responses.size)
                responses["submission_time"] = submissionTime
            }

            FirebaseUtils.sendEntryToDatabase(
                path = "users/${FirebaseUtils.getCurrentUserUID()}/questionnaires/spai/${System.currentTimeMillis()}",
                data = responses,
                onSuccess = {
                },
                onFailure = { exception ->
                }
            )

            finish()
        }
    }

    private fun updateResponse(id: String, answer: SPAIAnswer) {
        responses[id] = answer  // Add or update the response in the map by ID
    }

    private fun updateQuestionText(textView: TextView, question: String, answered: Boolean) {
        textView.text = if (answered) question else "* $question"
        textView.setTextColor(if (answered) Color.BLACK else Color.RED)
    }

    private fun checkAllAnswered() {
        // debug = enable right away
        submitButton.isEnabled = true
        // Enable submit button only if all questions are answered
        //submitButton.isEnabled = responses.size == listOfQuestions.size
    }
}

data class SPAIAnswer(val questionId: String, val response: Int)