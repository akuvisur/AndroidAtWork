package com.example.screenloggerdialogtest

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.core.content.ContextCompat
import java.util.Calendar

class MainActivity : FragmentActivity() {

    private var study_start_ts = 0
    private var study_state : Int = 0

    private lateinit var mainAdapter : ViewPagerAdapter
    private lateinit var viewPager : ViewPager2

    private lateinit var studyPhaseSlider : Slider
    private lateinit var studyPhaseText : TextView
    private var studyPhaseSliderValue : Float = 0.0f

    companion object {
        const val REQUEST_CODE_OVERLAY = 100 // Arbitrary value for overlay permission callback
        const val REQUEST_CODE_NOTIFICATION = 200 // value for notification permission callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("states", Context.MODE_PRIVATE)
        study_start_ts = sharedPrefs.getInt("study_start_ts", 0)

        setContentView(R.layout.activity_main)
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()

        // test firebase
        val userData = hashMapOf(
            "name" to "John Doe",
            "age" to 30,
            "email" to "johndoe@example.com",
            "ts" to System.currentTimeMillis()
        )

        // Send the data to the database under the "users" node
        FirebaseUtils.sendEntryToDatabase(
            path = "users/user_1", // Path in the database (e.g., "users/user_1")
            data = userData,
            onSuccess = {
                // Handle success
                Toast.makeText(this, "Data sent successfully!", Toast.LENGTH_SHORT).show()
            },
            onFailure = { exception ->
                // Handle failure
                Toast.makeText(this, "Failed to send data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        )
        // end firebase test

        studyPhaseSlider = findViewById(R.id.studyPhaseSlider)
        studyPhaseSlider.isEnabled = false
        studyPhaseText = findViewById(R.id.studyPhaseText)

        viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        mainAdapter = ViewPagerAdapter(this)
        viewPager.adapter = mainAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "Info"
                }
                1 -> {
                    tab.text = "Data"
                }
                2 -> {
                    tab.text = "Settings"
                }
            }
        }.attach()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // this launches the permission request
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_NOTIFICATION)
            }
        } else {
            true // For Android versions below 13, permissions are granted by default
        }

        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, BaselineService::class.java) // Build the intent for the service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            }
            else {
                Log.d("MAIN:ONCREATE", "SDK level too low")
            }
        } else {
            // this prompts user to the settings screen to enable overlay
            requestOverlayPermission()
        }

        study_state = getStudyState(this)

        studyPhaseText.text = "Study phase: " + when (study_state) {
            STUDY_STATE_FIRST_LAUNCH -> "Start"
            STUDY_STATE_CONSENT_GIVEN -> "User input required before baseline"
            STUDY_STATE_BASELINE_ONGOING -> "Baseline"
            STUDY_STATE_POST_BASELINE -> "User input required before Intervention #1"
            STUDY_STATE_INT1 -> "Intervention #1"
            STUDY_STATE_POST_INT1 -> "User input required before Intervention #2"
            STUDY_STATE_INT2 -> "Intervention #2"
            STUDY_STATE_POST_INT2_SURVEY_REQUIRED -> "Post Intervention 2 - Survey responses required"
            STUDY_STATE_COMPLETE -> "Study complete"
            else -> "Unknown state"
        }
        // TODO: Add day counter

        studyPhaseSliderValue = when {
            study_state < STUDY_STATE_BASELINE_ONGOING -> 0f
            study_state < STUDY_STATE_INT1 -> 1f
            study_state < STUDY_STATE_INT2 -> 2f
            study_state <= STUDY_STATE_POST_INT2_SURVEY_REQUIRED -> 3f
            study_state <= STUDY_STATE_COMPLETE -> 4f
            else -> 0f
        }
        studyPhaseSlider.value = studyPhaseSliderValue
        // TODO: Add daily calculation of value to show progress

        val sharedPrefs = getSharedPreferences("states", Context.MODE_PRIVATE)
        // always update study info values
        study_start_ts = sharedPrefs.getInt("study_start_ts", 0)
    }

    fun refreshUI() {
        this.recreate()
    }

   private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                // Permission granted, now show the overlay
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this, "Overlay permission is required.", Toast.LENGTH_SHORT).show()
            }
        }
        else if (requestCode == REQUEST_CODE_NOTIFICATION) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
        }
        else {
            Toast.makeText(this, "Notifications required", Toast.LENGTH_SHORT).show()
        }
    }

    class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        private val fragments: List<Fragment> = listOf(
            InfoFragment(),
            DataCollectedFragment(),
            SettingsFragment()
        )

        override fun getItemCount(): Int {
            return fragments.size
        }

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }
    }

    class InfoFragment : Fragment() {
        private lateinit var inflaterView : View
        private lateinit var bedTimeInput : EditText

        lateinit var startOnboardingButton: Button
        lateinit var allowNotificationButton: Button
        lateinit var disableBatteryManagementButton: Button
        lateinit var consentSpaiButton: Button
        lateinit var consentSASButton: Button
        lateinit var startBaselineButton: Button
        lateinit var averageUsageText: TextView
        lateinit var reduceUsageSlider: Slider
        lateinit var reduceUsageText: TextView
        lateinit var allowNotificationIcon: ImageView

        lateinit var enableOverlayButton: Button
        lateinit var popupTestButton: Button
        lateinit var startInterventionButton: Button
        lateinit var baselineProgressSlider: Slider
        lateinit var intervention1ProgressSlider: Slider
        lateinit var startIntervention2Button: Button
        lateinit var intervention2ProgressSlider: Slider
        lateinit var postStudyExitSurveyButton: Button
        lateinit var postStudySpaiButton: Button
        lateinit var completeStudyButton: Button

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            var studyState = getStudyState(requireContext())
            if (studyState == STUDY_STATE_FIRST_LAUNCH) {
                inflaterView = inflater.inflate(R.layout.first_launch_layout, container, false)

                startOnboardingButton = inflaterView.findViewById<Button>(R.id.startOnboardingButton)
                startOnboardingButton.setOnClickListener {
                    val intent = Intent(activity, OnboardingActivity::class.java)
                    startActivity(intent)
                }
            }

            else if (studyState == STUDY_STATE_CONSENT_GIVEN) {
                // Additional actions after consent is given
                // e.g., prepare the app for baseline tracking, show notification prompt
                inflaterView = inflater.inflate(R.layout.consent_given_layout, container, false)

                allowNotificationButton = inflaterView.findViewById(R.id.allowNotificationButton)
                disableBatteryManagementButton = inflaterView.findViewById(R.id.disableBatteryManagementButton)
                consentSpaiButton = inflaterView.findViewById(R.id.consentSpaiButton)
                startBaselineButton = inflaterView.findViewById(R.id.startBaselineButton)
                allowNotificationIcon = inflaterView.findViewById(R.id.allowNotificationIcon)

                var notifications_allowed : Boolean = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if ((requireActivity() as MainActivity).checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        // still false! :)
                        notifications_allowed = false
                        }
                } else {
                    notifications_allowed = true
                    allowNotificationButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_gray_200))
                    allowNotificationButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_gray_600))
                }
                allowNotificationIcon.isVisible = notifications_allowed
                allowNotificationButton.isEnabled = !notifications_allowed
                allowNotificationButton.setOnClickListener {
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_NOTIFICATION)
                }

                disableBatteryManagementButton.setOnClickListener {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                    Toast.makeText(requireContext(), "Please find this application and disable battery management, then return to the application", Toast.LENGTH_LONG).show()
                }

                consentSpaiButton.setOnClickListener {
                    val intent = Intent(activity, SPAIActivity::class.java)
                    intent.putExtra("source", "consent_given")
                    startActivity(intent)
                }

                consentSASButton.setOnClickListener {
                    val intent = Intent(activity, SASSVActivity::class.java)
                    intent.putExtra("source", "consent_given")
                    startActivity(intent)
                }

                startBaselineButton.setOnClickListener {
                    setStudyState(requireContext(), STUDY_STATE_BASELINE_ONGOING)
                    setStudyTimestamp(requireContext(), BASELINE_START_TIMESTAMP, System.currentTimeMillis().toLong())
                    (requireActivity() as MainActivity).refreshUI()
                }

            }

            else if (studyState == STUDY_STATE_BASELINE_ONGOING) {
                // Actions for baseline ongoing
                // e.g., initialize baseline tracking, display tracking progress
                inflaterView = inflater.inflate(R.layout.baseline_ongoing_layout, container, false)
            }

            else if (studyState == STUDY_STATE_POST_BASELINE) {
                // Actions for post-baseline phase
                // e.g., gather baseline data, prepare for intervention
                inflaterView = inflater.inflate(R.layout.post_baseline_layout, container, false)

                bedTimeInput = inflaterView.findViewById<EditText>(R.id.bedtimeInput)
                bedTimeInput.setOnClickListener {
                    val calendar = Calendar.getInstance()
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val minute = calendar.get(Calendar.MINUTE)

                    val timePicker = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                        bedTimeInput.setText(
                            String.format(
                                "%02d:%02d",
                                selectedHour,
                                selectedMinute
                            )
                        )
                    }, hour, minute, true)
                    timePicker.show()
                }
            }

            else if (studyState == STUDY_STATE_INT1) {
                // Actions for Intervention Phase 1
                // e.g., initiate intervention routines, set up reminders
                inflaterView = inflater.inflate(R.layout.intervention1_layout, container, false)
            }

            else if (studyState == STUDY_STATE_POST_INT1) {
                // Actions after Intervention Phase 1
                // e.g., prompt user feedback on phase, analyze results
                inflaterView = inflater.inflate(R.layout.post_int1_layout, container, false)
            }

            else if (studyState == STUDY_STATE_INT2) {
                // Actions for Intervention Phase 2
                // e.g., adjust intervention parameters, set up next steps
                inflaterView = inflater.inflate(R.layout.intervention2_layout, container, false)
            }

            else if (studyState == STUDY_STATE_POST_INT2_SURVEY_REQUIRED) {
                // Actions for post Intervention Phase 2 survey
                // e.g., collect survey data, prepare for final analysis
                inflaterView = inflater.inflate(R.layout.post_int2_survey_required_layout, container, false)
            }

            else if (studyState == STUDY_STATE_COMPLETE) {
                // Actions for study completion
                // e.g., show summary, provide exit message, share results
                inflaterView = inflater.inflate(R.layout.complete_layout, container, false)
            } else {
                // Optional fallback layout and actions
                inflaterView = inflater.inflate(R.layout.fragment_info, container, false)
            }

            return inflaterView
        }
    }

    class DataCollectedFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            var inflaterView = if (getStudyState(requireContext()) < STUDY_STATE_INT1) {
                // no data shown before intervention 1
                inflater.inflate(R.layout.fragment_data_collected, container, false)
            }
            else {
                // data visualisation after intervention 1
                inflater.inflate(R.layout.fragment_data_collected, container, false)
            }
            // Inflate the layout for this fragment
            return inflaterView
        }
    }

    // settings fragment is always the same
    // some fields are disabled field.isEnabled = false prior to INT1
    class SettingsFragment : Fragment() {
        private lateinit var studyStateSpinner: Spinner
        private var isUserInteracting = false

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            // Inflate the layout for this fragment
            val view = inflater.inflate(R.layout.fragment_settings, container, false)

            // Find the button using the inflated view
            val startOnboardingButton: Button = view.findViewById(R.id.startOnBoardingButton)
            startOnboardingButton.setOnClickListener {
                // Start the MainActivity for onboarding
                val intent = Intent(activity, OnboardingActivity::class.java)
                startActivity(intent)
            }

            val spaiButton: Button = view.findViewById(R.id.SPAI)
            spaiButton.setOnClickListener {
                // Start the MainActivity for onboarding
                val intent = Intent(activity, SPAIActivity::class.java)
                startActivity(intent)
            }

            val sasButton : Button = view.findViewById(R.id.SAS_SV)
            sasButton.setOnClickListener {
                // Start the MainActivity for onboarding
                val intent = Intent(activity, SASSVActivity::class.java)
                startActivity(intent)
            }


            studyStateSpinner = view.findViewById<Spinner>(R.id.study_state_spinner)

            // Array of study states with descriptions
            val studyStates = arrayOf(
                "0 - Onboarding",
                "1 - Pre-baseline",
                "2 - Baseline",
                "3 - Pre-INT1",
                "4 - INT1",
                "5 - Post-INT1",
                "6 - INT2",
                "7 - Post-INT2",
                "8 - Study Complete"
            )

            // Set up the ArrayAdapter
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, studyStates)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            studyStateSpinner.adapter = adapter

            studyStateSpinner.setOnTouchListener { _, _ ->
                isUserInteracting = true
                false // Allow the touch event to propagate
            }
            studyStateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    if (isUserInteracting) {
                        var selectedState = position // Store the selected index (0-8)
                        // Optional: Show a Toast message for demonstration
                        setStudyState(requireContext(), selectedState)
                        (requireActivity() as MainActivity).refreshUI()
                        // Reset the interaction flag
                        isUserInteracting = false
                    }

                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Do nothing, or handle if needed
                }
            }
            studyStateSpinner.setSelection(getStudyState(requireContext()))

            return view
        }
    }
}
