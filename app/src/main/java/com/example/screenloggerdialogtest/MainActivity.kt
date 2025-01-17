package com.example.screenloggerdialogtest

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
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
import java.time.LocalDateTime
import java.util.Calendar
import kotlin.math.min

class MainActivity : FragmentActivity() {

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

        setContentView(R.layout.activity_main)

        FirebaseUtils.authenticateUser(
            onSuccess = { uid ->
                Toast.makeText(this, "Authentication succeeded! UID: $uid", Toast.LENGTH_SHORT).show()
            },
            onFailure = { errorMessage ->
                Toast.makeText(this, "Authentication failed: $errorMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()

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

        lateinit var startOnboardingButton: Button
        lateinit var allowNotificationButton: Button
        lateinit var disableBatteryManagementButton: Button
        lateinit var consentSpaiButton: Button
        lateinit var consentSASButton: Button
        lateinit var startBaselineButton: Button
        lateinit var allowNotificationIcon: ImageView
        lateinit var consentSpaiIcon: ImageView
        lateinit var consentSASIcon: ImageView

        lateinit var baselineProgressSlider: Slider

        lateinit var averageUsageText: TextView
        lateinit var reduceUsageSlider: Slider
        lateinit var reduceUsageText: TextView
        lateinit var bedTimeInput : EditText
        lateinit var enableOverlayButton: Button
        lateinit var intervention1TestButton: Button
        lateinit var startInterventionButton: Button


        lateinit var intervention1ProgressSlider: Slider
        lateinit var startIntervention2Button: Button
        lateinit var intervention2ProgressSlider: Slider
        lateinit var postStudyExitSurveyButton: Button
        lateinit var postStudySpaiButton: Button
        lateinit var completeStudyButton: Button

        @SuppressLint("NewApi", "DefaultLocale")
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {

            var studyStateVars = getStudyStateVariables(requireContext())

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
                consentSASButton = inflaterView.findViewById(R.id.consentSASButton)
                startBaselineButton = inflaterView.findViewById(R.id.startBaselineButton)
                allowNotificationIcon = inflaterView.findViewById(R.id.allowNotificationIcon)
                consentSpaiIcon = inflaterView.findViewById(R.id.spaiIcon)
                consentSASIcon = inflaterView.findViewById(R.id.sasIcon)

                var notificationsAllowed : Boolean = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if ((requireActivity() as MainActivity).checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        // still false! :)
                        notificationsAllowed = false
                        }
                } else {
                    notificationsAllowed = true
                    allowNotificationButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_gray_200))
                    allowNotificationButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_gray_600))
                }
                allowNotificationIcon.isVisible = notificationsAllowed
                allowNotificationButton.isEnabled = !notificationsAllowed
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

                if (studyStateVars[SPAI_1_SUBMITTED] == 1) {
                    consentSpaiIcon.visibility = View.VISIBLE
                    disableConsentButton(consentSpaiButton)
                } else {
                    consentSpaiIcon.visibility = View.GONE
                }

                consentSASButton.setOnClickListener {
                    val intent = Intent(activity, SASSVActivity::class.java)
                    intent.putExtra("source", "consent_given")
                    startActivity(intent)
                }

                if (studyStateVars[SASSV_1_SUBMITTED] == 1) {
                    consentSASIcon.visibility = View.VISIBLE
                    disableConsentButton(consentSASButton)
                } else {
                    consentSASIcon.visibility = View.GONE
                }

                startBaselineButton.setOnClickListener {
                    setStudyState(requireContext(), STUDY_STATE_BASELINE_ONGOING)
                    setStudyTimestamp(requireContext(), BASELINE_START_TIMESTAMP, System.currentTimeMillis())
                    (requireActivity() as MainActivity).refreshUI()

                    val baselineData = hashMapOf(
                        BASELINE_START_TIMESTAMP to System.currentTimeMillis(),
                        "ts" to System.currentTimeMillis()
                    )

                    FirebaseUtils.sendEntryToDatabase(
                        path = "users/${FirebaseUtils.getCurrentUserUID()}/study_state_info",
                        data = baselineData,
                        onSuccess = {
                            //Toast.makeText(requireContext(), "Screen event sent successfully", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { exception ->
                            // Handle failure
                            //Toast.makeText(requireContext(), "Failed to send data: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }

                if (!((studyStateVars[SASSV_1_SUBMITTED] == 1) && (studyStateVars[SPAI_1_SUBMITTED] ==1) && notificationsAllowed)) {
                    disableConsentButton(startBaselineButton)
                }

            }

            else if (studyState == STUDY_STATE_BASELINE_ONGOING) {
                // Actions for baseline ongoing
                // e.g., initialize baseline tracking, display tracking progress
                inflaterView = inflater.inflate(R.layout.baseline_ongoing_layout, container, false)

                val baselineDay = calculateStudyPeriodDay(BASELINE_START_TIMESTAMP, requireContext())
                if (baselineDay > BASELINE_DURATION) {
                    setStudyState(requireContext(), STUDY_STATE_POST_BASELINE)
                    (requireActivity() as MainActivity).refreshUI()
                }

                val intent = Intent(requireContext(), BaselineService::class.java) // Build the intent for the service
                (requireActivity() as MainActivity).startForegroundService(intent)

                FirebaseUtils.fetchUsageTotal(LocalDateTime.now()) { usage ->
                    Log.d("FIREBASE", "Total Usage All Days: $usage")
                }

                baselineProgressSlider = inflaterView.findViewById(R.id.baselineProgressSlider)
                baselineProgressSlider.value = min(baselineDay.toFloat(), BASELINE_DURATION.toFloat())
            }

            else if (studyState == STUDY_STATE_POST_BASELINE) {
                // stop previous service
                val baselineServiceIntent = Intent(requireContext(), BaselineService::class.java)
                (requireActivity() as MainActivity).stopService(baselineServiceIntent)

                var usageAverage = 0L
                // Actions for post-baseline phase
                // e.g., gather baseline data, prepare for intervention
                inflaterView = inflater.inflate(R.layout.post_baseline_layout, container, false)

                averageUsageText = inflaterView.findViewById(R.id.averageUsageText)
                reduceUsageSlider = inflaterView.findViewById(R.id.settingsReduceUsageSlider)
                reduceUsageText = inflaterView.findViewById(R.id.reduceUsageText)
                bedTimeInput = inflaterView.findViewById(R.id.bedtimeInput)
                enableOverlayButton = inflaterView.findViewById(R.id.enableOverlayButton)
                intervention1TestButton = inflaterView.findViewById(R.id.intervention1TestButton)
                startInterventionButton = inflaterView.findViewById(R.id.startInterventionButton)

                FirebaseUtils.fetchUsageTotal(LocalDateTime.now()) { usage ->
                    val totalUsageAllDays = usage.values.sum()
                    val averageUsageMillis = if (usage.isNotEmpty()) totalUsageAllDays / usage.size else 0L

                    val hours = averageUsageMillis / (1000 * 60 * 60)
                    val minutes = (averageUsageMillis / (1000 * 60)) % 60

                    val usageFormatted = String.format("%02d:%02d", hours, minutes)
                    val usageText = String.format(
                        averageUsageText.context.getString(R.string.daily_usage),
                        usageFormatted
                    )
                    averageUsageText.text = usageText
                    usageAverage = averageUsageMillis
                }

                reduceUsageSlider.setLabelFormatter {
                    value -> "-${value.toInt()}%"
                }

                var reduceUsageTouched = false
                var reducedUsageMillis = 0L
                reduceUsageSlider.addOnChangeListener { slider, value, _ ->
                    // Convert the slider value to a percentage (negative)
                    val percentageReduction = -value.toInt()

                    // Calculate the reduced usage in milliseconds
                    reducedUsageMillis = usageAverage * (100 + percentageReduction) / 100

                    // Convert reduced usage to HH:mm format
                    val hours = reducedUsageMillis / (1000 * 60 * 60)
                    val minutes = (reducedUsageMillis / (1000 * 60)) % 60
                    val reducedUsageFormatted = String.format("%02dh %02dm", hours, minutes)

                    // Update the TextView with the reduced usage
                    reduceUsageText.text = String.format(
                        reduceUsageText.context.getString(R.string.usage_limit_text),
                        reducedUsageFormatted
                    )
                    reduceUsageTouched = true
                }
                reduceUsageText.text = "Move the slider to select between 10% and 50% goal in reduction of smartphone use"

                bedTimeInput.setOnClickListener {
                    val calendar = Calendar.getInstance()
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val minute = calendar.get(Calendar.MINUTE)

                    val timePicker = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                        bedTimeInput.setText(
                            String.format(
                                "%02dh %02dm",
                                selectedHour,
                                selectedMinute
                            )
                        )
                    }, hour, minute, true)
                    timePicker.show()
                }

                enableOverlayButton = inflaterView.findViewById(R.id.enableOverlayButton)
                enableOverlayButton.setOnClickListener {
                    (requireActivity() as MainActivity).requestOverlayPermission()
                }

                var overlaysAllowed = false
                if (Settings.canDrawOverlays(requireContext())) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        overlaysAllowed = true
                        // button disabled if already enabled
                        disableINT1Button(enableOverlayButton)
                    }
                    else {
                        Log.d("MAIN:ONCREATE", "SDK level too low")
                    }
                }

                var interventionTested = false
                intervention1TestButton.setOnClickListener {
                    UnlockDialog().showBedtimeClosingDialog(requireContext(), 0,0,0,0)
                    //
                    interventionTested = true
                }
                intervention1TestButton.isEnabled = overlaysAllowed

                startInterventionButton.setOnClickListener {
                    // TODO send setup data to firebase
                    // TODO and store to local vars
                    Log.d("MAIN", bedTimeInput.text.toString())
                    if (overlaysAllowed && bedTimeInput.text.isNotEmpty() && reduceUsageTouched && interventionTested) {
                        val c = requireContext()
                        setStudyState(c, STUDY_STATE_INT1)
                        setStudyVariable(c, BASELINE_USAGE_AVERAGE, usageAverage)
                        setStudyVariable(c, INT_SMARTPHONE_USAGE_LIMIT_GOAL, reducedUsageMillis)
                        setStudyVariable(c, INT_SMARTPHONE_USAGE_LIMIT_PERCENTAGE, reduceUsageSlider.value.toInt())
                        setStudyVariable(c, BEDTIME_GOAL, parseBedtimeToMinutes(bedTimeInput.text.toString()))
                        setStudyTimestamp(c, INT1_START_TIMESTAMP, System.currentTimeMillis())

                        val goalData = hashMapOf(
                            INT1_START_TIMESTAMP to System.currentTimeMillis(),
                            INT_SMARTPHONE_USAGE_LIMIT_GOAL to reducedUsageMillis,
                            BEDTIME_GOAL to parseBedtimeToMinutes(bedTimeInput.text.toString()),
                            "ts" to System.currentTimeMillis()
                        )

                        FirebaseUtils.sendEntryToDatabase(
                            path = "users/${FirebaseUtils.getCurrentUserUID()}/study_state_info",
                            data = goalData,
                            onSuccess = {
                                //Toast.makeText(requireContext(), "Screen event sent successfully", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { exception ->
                                // Handle failure
                                //Toast.makeText(requireContext(), "Failed to send data: ${exception.message}", Toast.LENGTH_LONG).show()
                            }
                        )

                        (requireActivity() as MainActivity).refreshUI()
                    }
                    // prompt user to do the missing thing!
                    else {
                        if (!overlaysAllowed) {
                            Toast.makeText(requireContext(), "Please allow overlays before starting Intervention Phase #1", Toast.LENGTH_SHORT).show()
                        }
                        else if (bedTimeInput.text.isEmpty()) {
                            Toast.makeText(requireContext(), "Please give your preferred bedtime before starting Intervention Phase #1", Toast.LENGTH_SHORT).show()
                        }
                        else if (!reduceUsageTouched) {
                            Toast.makeText(requireContext(), "Please set a smartphone usage goal before starting Intervention Phase #1", Toast.LENGTH_SHORT).show()
                        }
                        else if (!interventionTested) {
                            Toast.makeText(requireContext(), "Please test the intervention screen before starting Intervention Phase #1", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            }

            else if (studyState == STUDY_STATE_INT1) {
                // e.g., initiate intervention routines, set up reminders
                inflaterView = inflater.inflate(R.layout.intervention1_layout, container, false)

                // INT1 requires overlay permission
                if (Settings.canDrawOverlays(requireContext())) {
                    //TODO change baseline to INT1 service?
                    val intent = Intent(requireContext(), INT1Service::class.java) // Build the intent for the service
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        (requireActivity() as MainActivity).startForegroundService(intent)
                    }
                    else {
                        Log.d("MAIN:ONCREATE", "SDK level too low")
                    }
                } else {
                    // this prompts user to the settings screen to enable overlay
                    Toast.makeText(requireContext(), "Please enable overlay permission", Toast.LENGTH_LONG).show()
                    (requireActivity() as MainActivity).requestOverlayPermission()
                }

                val int1Day = calculateStudyPeriodDay(INT1_START_TIMESTAMP, requireContext())
                if (int1Day > INT1_DURATION) {
                    setStudyState(requireContext(), STUDY_STATE_POST_INT1)
                    (requireActivity() as MainActivity).refreshUI()
                }

                val intent = Intent(requireContext(), BaselineService::class.java) // Build the intent for the service
                (requireActivity() as MainActivity).startForegroundService(intent)

                intervention1ProgressSlider = inflaterView.findViewById(R.id.intervention1ProgressSlider)
                intervention1ProgressSlider.value = min(int1Day.toFloat(), INT1_DURATION.toFloat())

            }

            else if (studyState == STUDY_STATE_POST_INT1) {
                // Actions after Intervention Phase 1
                // e.g., prompt user feedback on phase, analyze results
                inflaterView = inflater.inflate(R.layout.post_int1_layout, container, false)

                startIntervention2Button = inflaterView.findViewById(R.id.startIntervention2Button)
                startIntervention2Button.setOnClickListener {
                    setStudyState(requireContext(), STUDY_STATE_INT2)
                    setStudyTimestamp(requireContext(), INT2_START_TIMESTAMP, System.currentTimeMillis())

                    val goalData = hashMapOf(
                        INT2_START_TIMESTAMP to System.currentTimeMillis(),
                        "ts" to System.currentTimeMillis()
                    )

                    FirebaseUtils.sendEntryToDatabase(
                        path = "users/${FirebaseUtils.getCurrentUserUID()}/study_state_info",
                        data = goalData,
                        onSuccess = {
                            //Toast.makeText(requireContext(), "Screen event sent successfully", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { exception ->
                            // Handle failure
                            //Toast.makeText(requireContext(), "Failed to send data: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                    )

                    (requireActivity() as MainActivity).refreshUI()
                }
            }

            else if (studyState == STUDY_STATE_INT2) {
                // Actions for Intervention Phase 2
                // e.g., adjust intervention parameters, set up next steps
                inflaterView = inflater.inflate(R.layout.intervention2_layout, container, false)

                val int2Day = calculateStudyPeriodDay(INT2_START_TIMESTAMP, requireContext())
                if (int2Day > INT2_DURATION) {
                    setStudyState(requireContext(), STUDY_STATE_POST_INT2_SURVEY_REQUIRED)
                    (requireActivity() as MainActivity).refreshUI()
                }

                val intent = Intent(requireContext(), INT2Service::class.java) // Build the intent for the service
                (requireActivity() as MainActivity).startForegroundService(intent)

                intervention2ProgressSlider = inflaterView.findViewById(R.id.intervention2ProgressSlider)
                intervention2ProgressSlider.value = min(int2Day.toFloat(), INT2_DURATION.toFloat())

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

        private fun disableConsentButton(b : Button) {
            b.isEnabled = false
            b.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_gray_200))
            b.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_gray_600))
        }

        private fun disableINT1Button(b : Button) {
            b.isEnabled = false
            b.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_200))
            b.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_600))
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
        private lateinit var settingsReduceUsageSlider: Slider
        private lateinit var settingsReduceUsageText : TextView
        private lateinit var settingsLayout : LinearLayout
        private lateinit var passkeyEditText : EditText
        private lateinit var passkeyText : TextView
        private lateinit var settingsGoalLayout : LinearLayout

        private var isUserInteracting = false
        private var usageAverage = 0L

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            // Inflate the layout for this fragment
            val view = inflater.inflate(R.layout.fragment_settings, container, false)

            settingsReduceUsageSlider = view.findViewById(R.id.settingsReduceUsageSlider)
            settingsReduceUsageText = view.findViewById(R.id.settingsReduceUsageText)
            settingsLayout = view.findViewById(R.id.settingsHiddenLayout)
            passkeyEditText = view.findViewById<EditText>(R.id.passkeyEditText)
            passkeyText = view.findViewById(R.id.passkeyText)
            settingsGoalLayout = view.findViewById(R.id.settingsGoalLayout)

            usageAverage = getStudyVariable(requireContext(), BASELINE_USAGE_AVERAGE, 0L)

            settingsReduceUsageSlider.setLabelFormatter {
                    value -> "-${value.toInt()}%"
            }

            var reducedUsageMillis = 0L
            settingsReduceUsageSlider.addOnChangeListener { slider, value, _ ->
                // Convert the slider value to a percentage (negative)
                val percentageReduction = -value.toInt()

                // Calculate the reduced usage in milliseconds
                reducedUsageMillis = usageAverage * (100 + percentageReduction) / 100

                // Convert reduced usage to HH:mm format
                val hours = reducedUsageMillis / (1000 * 60 * 60)
                val minutes = (reducedUsageMillis / (1000 * 60)) % 60
                val reducedUsageFormatted = String.format("%02dh %02dm", hours, minutes)

                settingsReduceUsageText.text = String.format(
                    settingsReduceUsageText.context.getString(R.string.usage_limit_text),
                    reducedUsageFormatted
                )

                // update values here automatically
                setStudyVariable(requireContext(), INT_SMARTPHONE_USAGE_LIMIT_GOAL, reducedUsageMillis)
                setStudyVariable(requireContext(), INT_SMARTPHONE_USAGE_LIMIT_PERCENTAGE, value.toInt())
            }

            val usageGoal = getStudyVariable(requireContext(), INT_SMARTPHONE_USAGE_LIMIT_PERCENTAGE, 10)
            settingsReduceUsageSlider.value = usageGoal.toFloat()
            settingsReduceUsageSlider.setLabelFormatter {
                    value -> "-${value.toInt()}%"
            }

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

            settingsLayout.visibility = View.GONE

            passkeyEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    if (s.toString() == "7621") {
                        // Passkey is correct
                        settingsLayout.visibility = View.VISIBLE
                        passkeyText.text = "Passkey correct!"
                    }
                }
            })

            if (getStudyState(requireContext()) < 3) {
                settingsGoalLayout.visibility = View.GONE
            }

            return view
        }
    }
}
