package com.example.screenloggerdialogtest

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ScreenOffQuestionnaire : AppCompatActivity() {

    private lateinit var whatdoinkCommunicationButton: Button
    private lateinit var whatdoinkLeisureButton: Button
    private lateinit var whatdoinkUtilitiesButton: Button
    private lateinit var whatdoinkOtherButton: Button

    private lateinit var purposeWorkButton: Button
    private lateinit var purposePersonalButton: Button
    private lateinit var purposeMixButton: Button
    private lateinit var purposeNotApplicableButton: Button

    private lateinit var triggerExternalButton: Button
    private lateinit var triggerInnerButton: Button
    private lateinit var triggerNoReasonButton: Button

    private val row1 by lazy {
        listOf(whatdoinkCommunicationButton, whatdoinkLeisureButton, whatdoinkUtilitiesButton, whatdoinkOtherButton)
    }
    private val row2 by lazy {
        listOf(purposeWorkButton, purposePersonalButton, purposeMixButton, purposeNotApplicableButton)
    }
    private val row3 by lazy {
        listOf(triggerExternalButton, triggerInnerButton, triggerNoReasonButton)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_screenoffquestionnaire)

        val radioGroup = findViewById<RadioGroup>(R.id.radioGroup)
        val submitButton = findViewById<Button>(R.id.submitButton)

        whatdoinkCommunicationButton = findViewById(R.id.whatdoink_communication)
        whatdoinkLeisureButton = findViewById(R.id.whatdoink_leisure)
        whatdoinkUtilitiesButton = findViewById(R.id.whatdoink_utilities)
        whatdoinkOtherButton = findViewById(R.id.whatdoink_other)

        purposeWorkButton = findViewById(R.id.purpose_work)
        purposePersonalButton = findViewById(R.id.purpose_personal)
        purposeMixButton = findViewById(R.id.purpose_mix)
        purposeNotApplicableButton = findViewById(R.id.purpose_not_applicable)

        triggerExternalButton = findViewById(R.id.trigger_external)
        triggerInnerButton = findViewById(R.id.trigger_inner)
        triggerNoReasonButton = findViewById(R.id.trigger_noreason)

        // for some reason the colors for these buttons need to be set here too
        whatdoinkCommunicationButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.blue_800)
        whatdoinkLeisureButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.blue_700)
        whatdoinkUtilitiesButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.blue_600)
        whatdoinkOtherButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.blue_500)

        purposeWorkButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.deep_purple_700)
        purposePersonalButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.deep_purple_500)
        purposeMixButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.deep_purple_300)
        purposeNotApplicableButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.deep_purple_300)

        triggerExternalButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.teal_700)
        triggerInnerButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.teal_500)
        triggerNoReasonButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.teal_300)


        setupButtonGroups(this)

        submitButton.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            if (selectedId != -1) {
                val selectedText = findViewById<RadioButton>(selectedId).text.toString()

                val row1Selection = getSelectedButtonText(this, row1)
                val row2Selection = getSelectedButtonText(this, row2)
                val row3Selection = getSelectedButtonText(this, row3)

                // Optional: save to SharedPreferences, Firebase, log, etc.
                Log.d("Questionnaire", "Selected reason: $selectedText")
                val data = ScreenOffResponse(
                    whatdoinkSelection = row1Selection,
                    purposeSelection = row2Selection,
                    triggerSelection = row3Selection,
                    selection = selectedText
                )

                FirebaseUtils.sendEntryToDatabase("users/${FirebaseUtils.getCurrentUserUID()}/screen_off_questionnaires//${System.currentTimeMillis()}",data)
                Toast.makeText(this, "Thank you. You can now close your phone.", Toast.LENGTH_SHORT).show()
                finish() // Close activity
            } else {
                Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getSelectedButtonText(c: Context, buttons: List<Button>): String? {
        val whiteColorStateList = ContextCompat.getColorStateList(c, R.color.white)
        return buttons.find { it.backgroundTintList == whiteColorStateList }?.text?.toString()
    }

    private fun setupButtonGroups(c : Context) {
        setupRowButtons(c, row1)
        setupRowButtons(c, row2)
        setupRowButtons(c, row3)
    }

    // Function to set up selection logic for a row of buttons
    fun setupRowButtons(c : Context, buttons: List<Button>) {
        // Store original background for each button in this row
        val originalBackgroundTints = buttons.associateWith { it.backgroundTintList }

        buttons.forEach { button ->
            button.setOnClickListener {
                // Reset all buttons in the row to their original background tint
                buttons.forEach { btn ->
                    btn.backgroundTintList = originalBackgroundTints[btn]
                    btn.setTextColor(ContextCompat.getColor(c, R.color.white))
                }
                // Highlight the selected button by setting the background tint to white
                button.backgroundTintList = ContextCompat.getColorStateList(c, R.color.white)
                button.setTextColor(ContextCompat.getColor(c, R.color.blue_gray_900))
            }
        }
    }

    data class ScreenOffResponse(
        val whatdoinkSelection: String?,
        val purposeSelection: String?,
        val triggerSelection: String?,
        val selection: String?
    )
}