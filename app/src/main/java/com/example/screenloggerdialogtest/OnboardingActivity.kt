package com.example.screenloggerdialogtest

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator


class OnboardingActivity : AppCompatActivity() {

    var onboarding_position = 0

    private lateinit var viewPager: ViewPager2
    private lateinit var nextButton: Button
    private lateinit var prevButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.onboarding_activity)

        viewPager = findViewById(R.id.viewPager)
        nextButton = findViewById(R.id.nextButton)
        prevButton = findViewById(R.id.prevButton)

        viewPager.adapter = OnboardingPagerAdapter(this)
        TabLayoutMediator(findViewById(R.id.tabLayout), viewPager) { tab, position ->
            tab.text = "Step ${position + 1}"
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // Check if we're on the last page
                if (position == viewPager.adapter?.itemCount?.minus(1)) {
                    nextButton.text = "Complete"
                } else {
                    nextButton.text = "Next"
                }

                // Enable/Disable the previous button based on the page position
                prevButton.isEnabled = position != 0
            }
        })

        nextButton.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem < viewPager.adapter?.itemCount!! - 1) {
                viewPager.setCurrentItem(currentItem + 1, true)
            } else {
                // This is the last page, so "Complete" action should be triggered
                finish() // or any other final action
            }

            // if we clicked next, then the prev button should be enabled
            prevButton.isEnabled = true
        }

        // Previous button
        prevButton.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem > 0) {
                viewPager.setCurrentItem(currentItem - 1)
            }

            // Disable prevButton on the first page
            prevButton.isEnabled = viewPager.currentItem > 0

            // Reset nextButton text to "Next" if not on the last page
            // if we click previous we are always not on the last page
            nextButton.text = "Next"
        }

        // Initial button state - disable prevButton if on the first page
        prevButton.isEnabled = viewPager.currentItem > 0

        viewPager.isUserInputEnabled = false // Disable swiping
    }

    override fun onResume() {
        super.onResume()

        val sharedPrefs = getSharedPreferences("onboarding", Context.MODE_PRIVATE)
        onboarding_position = sharedPrefs.getInt("onboarding_position", 0)

    }

    private inner class OnboardingPagerAdapter(activity: OnboardingActivity) : FragmentStateAdapter(activity) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ConsentFragment()
                1 -> InformationFragment()
                2 -> StudyInformationFragment()
                //3 -> NotificationPermissionFragment()

                else -> ConsentFragment()
            }
        }

        override fun getItemCount(): Int {
            return 3 // Total number of steps
        }
    }

    fun enableNextButton(enable: Boolean) {
        nextButton.isEnabled = enable
    }

}

class ConsentFragment : Fragment() {

    private lateinit var consentCheckBox: CheckBox
    private lateinit var sharedPreferences: SharedPreferences
    private var consentGiven: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_consent, container, false)
        consentCheckBox = view.findViewById(R.id.consentCheckBox)

        sharedPreferences = requireActivity().getSharedPreferences(STUDY_STATE_SHAREDPREFS, Context.MODE_PRIVATE)
        consentGiven = sharedPreferences.getInt(CONSENT_GIVEN, 0)
        consentCheckBox.isChecked = (consentGiven == 1)
        (activity as OnboardingActivity).enableNextButton(consentCheckBox.isChecked)

        consentCheckBox.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putInt(CONSENT_GIVEN, if (isChecked) 1 else 0)
            editor.apply() // Save changes
            (activity as OnboardingActivity).enableNextButton(isChecked)
        }

        return view
    }
}

class InformationFragment : Fragment() {
    private lateinit var emailEditText: EditText
    private lateinit var prolificEditText: EditText
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var ageEditText : EditText
    private lateinit var genderGroup: RadioGroup

    private lateinit var occupationSpinner: Spinner
    private lateinit var ethnicitySpinner: Spinner


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_participant_information, container, false)

        emailEditText = view.findViewById(R.id.infoEmail)
        prolificEditText = view.findViewById(R.id.infoProlific)
        ageEditText = view.findViewById(R.id.infoAge)
        genderGroup = view.findViewById(R.id.genderGroup)
        occupationSpinner = view.findViewById(R.id.occupationSpinner)
        ethnicitySpinner = view.findViewById(R.id.ethnicitySpinner)

        sharedPreferences = requireActivity().getSharedPreferences(STUDY_STATE_SHAREDPREFS, Context.MODE_PRIVATE)

        loadStoredData()

        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Enable next button if the email field is not empty
                (activity as OnboardingActivity).enableNextButton(isValidEmail(s.toString()))
                val editor = sharedPreferences.edit()
                editor.putString("contact_email", s.toString())
                editor.apply() // Save changes
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        prolificEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val editor = sharedPreferences.edit()
                editor.putString("prolific_id", s.toString())
                editor.apply() // Save changes
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        ageEditText.inputType = InputType.TYPE_CLASS_NUMBER

        genderGroup.setOnCheckedChangeListener { _, checkedId ->
            val gender = genderGroup.findViewById<RadioButton>(checkedId).text.toString()
            Toast.makeText(requireContext(), gender, Toast.LENGTH_SHORT).show()
        }


        val occupationOptions = arrayOf("-", "Student", "Unemployed", "Full-time", "Part-time", "Other")
        val occupationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, occupationOptions)
        occupationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        occupationSpinner.adapter = occupationAdapter

        occupationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Get the selected item as a string
                val selectedOccupation = parent.getItemAtPosition(position).toString()
                // Handle the selection, e.g., store in Firebase
                //Toast.makeText(requireContext(), selectedOccupation, Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Optional: handle the case where no item is selected if needed
            }
        }


        val ethnicityOptions = arrayOf("-", "White", "Hispanic or Latino", "Black or African American",
            "Asian", "Native American or Alaska Native",
            "Native Hawaiian or Other Pacific Islander",
            "Middle Eastern or North African",
            "Two or more races", "Other")
        val ethnicityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, ethnicityOptions)
        ethnicityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ethnicitySpinner.adapter = ethnicityAdapter

        ethnicitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Get the selected item as a string
                val selectedEthnicity = parent.getItemAtPosition(position).toString()
                // Handle the selection, e.g., store in Firebase
                //Toast.makeText(requireContext(), selectedEthnicity, Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Optional: handle the case where no item is selected if needed
            }
        }

        return view
    }

    private fun loadStoredData() {
        // Get stored email and prolific ID
        val storedEmail = sharedPreferences.getString("contact_email", "")
        val storedProlificId = sharedPreferences.getString("prolific_id", "")

        // Populate the EditText fields if data exists
        emailEditText.setText(storedEmail)
        prolificEditText.setText(storedProlificId)

        // Enable the next button based on the loaded email
        (activity as OnboardingActivity).enableNextButton(isValidEmail(storedEmail))
    }

    private fun isValidEmail(email: String?): Boolean {
        // Check if the email is valid using Patterns
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

class StudyInformationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_study_information, container, false)
        return view
    }
}
